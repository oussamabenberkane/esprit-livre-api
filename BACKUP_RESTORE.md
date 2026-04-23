# Esprit Livre — prod backup & restore

This document is for **both the operator and any future agent** picking up
this system. If you're an agent: read the "Agent notes" section at the
bottom before touching anything.

## TL;DR

```bash
# Take a backup (dry-run of retention is always shown):
scripts/backup-prod.sh                 # or /backup-prod
scripts/backup-prod.sh --prune         # also delete backups beyond the last 10

# Preview a restore (default — no server contact past a read):
scripts/restore-prod.sh --all
scripts/restore-prod.sh --postgres

# Actually restore (prompts for typed timestamp unless --yes):
scripts/restore-prod.sh --postgres --execute
scripts/restore-prod.sh --all --execute --yes
```

Backups live in `~/backups/el/<YYYY-MM-DD-HHMMSS>/` with a `latest` symlink.
Default restore source is `~/backups/el/latest`.

## What a backup contains

A run dir has these files (sizes as of 2026-04-12, will grow):

| File | Source | ~Size |
|---|---|---|
| `postgres.dump` | `pg_dump -F c` of `el-prod-db` (hot, consistent) | 327K |
| `keycloak-realm.tar.gz` | `kc.sh export --realm jhipster --users realm_file` | 16K |
| `media.tar.gz` | tar of `espritlivre_api_media` volume | 8.0M |
| `host-config.tar.gz` | `/root/esprit-livre/` — compose, nginx confs, `.env` | 53M |
| `tls-letsencrypt.tar.gz` | `/etc/letsencrypt/` — certs + renewal state | 883K |
| `MANIFEST.txt` | sizes + sha256 of every file, container list | ~1K |

Whole thing ~62M. Transfer and compression overhead are negligible — don't
bother with rsync-partial/split-archive tricks unless sizes grow 10×.

Retention: keep last 10 by default. Pruning is dry-run unless `--prune` is
passed.

## Component semantics

### postgres
- Hot `pg_dump -F c` — safe to run while the stack is live. Self-consistent.
- Restore drops and recreates `el-prod-db`, then `pg_restore`.
- **Stops `espritlivre-api` before the restore, starts it after.** The API
  can't tolerate its DB disappearing mid-request.

### keycloak
- Live `kc.sh export` — no KC downtime.
- **kc.sh always exits non-zero** during export and import because it tries
  to bind the management port (9000), which the running KC container already
  holds. The operation itself completes before that bind failure.
- Backup verifies success by `test -f .../jhipster-realm.json`.
- Restore verifies success by `grep`ing the import log for
  `Realm .* imported|Import finished|KC-SERVICES0030`.
- Restore uses `--override true` so it's idempotent against an existing realm.
- KC is restarted at the end so it reloads the new realm state.

### media
- `espritlivre_api_media` holds user uploads (book covers, author pictures,
  category images, user avatars). Stateful, painful to lose.
- Tar via a throwaway `alpine` container that mounts the volume read-only.
- Restore wipes the volume contents first (`rm -rf /dst/*`) then untars.
  Safe because media files are write-once; no API process holds open handles
  across the restore.

### tls
- `/etc/letsencrypt/` lives **on the host**, not in a volume.
- Restore untars into `/etc/` and restarts `espritlivre-nginx`. Brief
  (~2 second) HTTPS blip.

### host-config (MANUAL)
- The compose file, nginx configs, and `.env` files under
  `/root/esprit-livre/`. `.env` holds credentials — treat backups accordingly.
- **Not automated by restore-prod.sh.** Restoring it only makes sense when
  rebuilding a fresh host; running it against the live server is almost
  never the right operation. The restore script prints manual `scp`+`tar`
  commands when `--host-config` is selected.

## Safety model (restore)

- **Dry-run by default**: prints every destructive command it would run,
  verifies `MANIFEST.txt` sha256s, does nothing else. Safe to run anytime.
- **`--execute` required** to actually touch the server.
- **Typed-timestamp confirmation** required for postgres, keycloak, or media
  unless `--yes` is also passed. Pastable, but you have to deliberately type
  or paste 17 characters — prevents accidental y+enter.
- **`--tls --execute`** prompts plain y/N unless `--yes`.
- **Fail-fast, no rollback**: if any step errors, remaining steps are
  skipped and the script prints `applied` / `failed` / `skipped` and exits
  non-zero. Prod will be in a mixed state — investigate manually.
- **Restore order** is fixed: postgres → keycloak → media → tls → host-config.
  Don't change it: postgres must be restored before the API comes back up,
  and KC must reload its realm before handling auth against the restored DB.

## Server topology (facts, don't re-probe)

- SSH alias: **`personal`** → `root@185.187.235.22` (key-based auth).
- Compose project root: **`/root/esprit-livre/api/`**
- Compose file: **`/root/esprit-livre/api/docker-compose.yml`**
  (not `/root/esprit-livre/docker-compose.yml` — that path doesn't exist)
- Containers:
  - `espritlivre-api` — Spring Boot API **(NOT `espritlivre-app`)**
  - `espritlivre-postgres` — Postgres 17
  - `espritlivre-keycloak` — Keycloak 26.2.3
  - `espritlivre-nginx` — nginx:alpine (containerized, **not** on host)
  - `espritlivre-user-frontend`, `espritlivre-admin-frontend` — stateless
- Postgres: user `el`, db `el-prod-db`
- Keycloak: realm `jhipster`
- Named volumes: `espritlivre_postgres_data`, `espritlivre_api_media`,
  `espritlivre_keycloak_data`, `espritlivre_api_logs`, `espritlivre_nginx_cache`
- TLS certs: `/etc/letsencrypt/live/app.espritlivre.com/` (subdomains), `/etc/letsencrypt/live/espritlivre.com/` (apex, on the host)
- Public URL: `https://espritlivre.com/`

## Manual fallback procedure

Use only if the scripts are broken. `$B` = backup dir (e.g. `~/backups/el/latest`).

```bash
# 0. integrity
cd "$B" && sha256sum -c <(awk '/^  [a-zA-Z]/ && NF==3 {print $3"  "$1}' MANIFEST.txt)

# 1. postgres
scp "$B/postgres.dump" personal:/tmp/
ssh personal '
    docker cp /tmp/postgres.dump espritlivre-postgres:/tmp/postgres.dump
    docker stop espritlivre-api
    docker exec espritlivre-postgres psql -U el -d postgres -c "DROP DATABASE IF EXISTS \"el-prod-db\";"
    docker exec espritlivre-postgres psql -U el -d postgres -c "CREATE DATABASE \"el-prod-db\" OWNER el;"
    docker exec espritlivre-postgres pg_restore -U el -d el-prod-db /tmp/postgres.dump
    docker exec espritlivre-postgres rm /tmp/postgres.dump
    docker start espritlivre-api
    rm /tmp/postgres.dump
'

# 2. keycloak
scp "$B/keycloak-realm.tar.gz" personal:/tmp/
ssh personal '
    mkdir -p /tmp/kc && tar -C /tmp/kc -xzf /tmp/keycloak-realm.tar.gz
    docker exec -u 0 espritlivre-keycloak rm -rf /tmp/kc-import
    docker cp /tmp/kc/keycloak-export espritlivre-keycloak:/tmp/kc-import
    # kc.sh will exit non-zero — that is EXPECTED (port-9000 bind conflict).
    docker exec espritlivre-keycloak /opt/keycloak/bin/kc.sh import --dir /tmp/kc-import --override true || true
    docker exec -u 0 espritlivre-keycloak rm -rf /tmp/kc-import
    docker restart espritlivre-keycloak
    rm -rf /tmp/kc /tmp/keycloak-realm.tar.gz
'

# 3. media
scp "$B/media.tar.gz" personal:/tmp/
ssh personal '
    docker run --rm \
        -v espritlivre_api_media:/dst \
        -v /tmp/media.tar.gz:/src.tar.gz:ro \
        alpine sh -c "rm -rf /dst/* /dst/.[!.]* 2>/dev/null || true; tar -C /dst -xzf /src.tar.gz"
    rm /tmp/media.tar.gz
'

# 4. tls
scp "$B/tls-letsencrypt.tar.gz" personal:/tmp/
ssh personal '
    tar -C /etc -xzf /tmp/tls-letsencrypt.tar.gz
    docker restart espritlivre-nginx
    rm /tmp/tls-letsencrypt.tar.gz
'

# 5. host-config (ONLY on a fresh host)
scp "$B/host-config.tar.gz" personal:/tmp/
ssh personal '
    tar -C /root -xzf /tmp/host-config.tar.gz
    cd /root/esprit-livre/api && docker compose up -d
    rm /tmp/host-config.tar.gz
'
```

## Agent notes

If you're an agent asked to debug, extend, or re-run any of this:

**Known gotchas — each cost us a failed run:**

1. **`kc.sh export/import` always exits non-zero on a live KC container.** It
   completes the operation, then tries to start an HTTP management server on
   port 9000 which the running KC already holds → bind failure → non-zero.
   Don't "fix" the non-zero — tolerate it and verify via a side-effect (an
   output file for export, a log marker for import).

2. **`docker cp` writes files into the container as root**, but
   `docker exec <container> <cmd>` runs as the container's configured `USER`
   (uid 1000 for `keycloak`). A subsequent `rm -rf` of docker-cp'd paths will
   fail with `Permission denied`. Fix: `docker exec -u 0 <container> rm -rf ...`.
   Applies anywhere you docker-cp into a non-root container.

3. **The API container is `espritlivre-api`, not `espritlivre-app`.** A prior
   agent proposed restore commands using the wrong name — they fail silently
   on stop/start.

4. **Nginx is containerized (`espritlivre-nginx`), not a host service.** Do
   NOT `systemctl reload nginx`. Use `docker restart espritlivre-nginx`.

5. **The compose file is at `/root/esprit-livre/api/docker-compose.yml`**,
   not `/root/esprit-livre/docker-compose.yml`. `/root/esprit-livre/` has
   `admin/`, `api/`, `user/` subdirs — only `api/` holds the compose project.

**Design invariants — don't break these without asking the user:**

- Backup runs with zero downtime. All captures are hot-safe.
- Restore defaults to dry-run. `--execute` is always required.
- Destructive restore components require typed-timestamp confirmation.
- Retention is dry-run. Pruning is opt-in via `--prune`.
- Remote staging dirs are always cleaned up via `trap ... EXIT`.
- The final step of a backup is an atomic `mv` from `.partial-<ts>/` to the
  final dir — an interrupted transfer never leaves a valid-looking directory.
- `MANIFEST.txt` is the integrity contract. Restore verifies sha256s before
  touching the server; a checksum mismatch is a hard fail.
- `host-config` restore is manual-only, intentionally. Don't automate it.

**When asked to add features, prefer rejection over scope creep:**

- Automated scheduling (cron/systemd) — rejected. This is on-demand.
- Encryption (gpg/age) — rejected because the laptop is the security boundary.
  Revisit if the user changes storage strategy.
- Full Postgres data-directory tarball in addition to pg_dump — rejected;
  `pg_dump -F c` is authoritative for restore, and a volume tar requires
  stopping Postgres for consistency.
- Backing up `espritlivre_api_logs` — rejected; logs are not state.

**Operational context:**

- First successful test restore of all four components: 2026-04-12, against
  the 5-minute-old backup `2026-04-12-001520`. The keycloak cleanup bug
  (gotcha #2) was discovered and fixed during that test — it is not
  hypothetical.
- Server-side backups prior to the script era live in `personal:/root/*.dump`
  (e.g. `pre-real-seed-2026-04-11-102246.dump`). These are `pg_dump` output
  only, no companion files. Safe to ignore unless investigating a specific
  incident.

**If something fails mid-restore:**

1. The script exits non-zero and prints `applied` / `failed` / `skipped`.
2. Do not automatically retry `--all` — that will re-run already-applied
   destructive steps. Re-run only the failed component and anything after it.
3. Remote staging `/root/.restore-staging-<pid>` is cleaned by the trap even
   on failure. If the trap didn't fire (e.g. killed with SIGKILL), the
   staging dir is harmless to leave or to delete manually.
