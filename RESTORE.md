# Restoring an Esprit Livre prod backup

Backups produced by [scripts/backup-prod.sh](scripts/backup-prod.sh) live in
`~/backups/el/<timestamp>/`. Each run directory contains:

| File | What it is |
|---|---|
| `postgres.dump` | `pg_dump -F c` of `el-prod-db` |
| `keycloak-realm.tar.gz` | `kc.sh export` of realm `jhipster` (includes users) |
| `media.tar.gz` | contents of the `espritlivre_api_media` volume |
| `host-config.tar.gz` | `/root/esprit-livre/` — compose files, nginx confs, `.env` |
| `tls-letsencrypt.tar.gz` | `/etc/letsencrypt/` — certs + renewal state |
| `MANIFEST.txt` | sizes, sha256s, docker container list |

Below, `$B` is the backup dir (e.g. `~/backups/el/latest`) and `personal` is
the SSH alias of the target server. For a fresh server, rebuild the stack
*first* from `host-config.tar.gz`, then restore data into the running volumes.

## 0. Verify integrity

```bash
cd "$B"
cat MANIFEST.txt
sha256sum -c <(awk '/^  [^ ]+\.(dump|tar\.gz)/ {print $3"  "$1}' MANIFEST.txt)
```

## 1. Rebuild the stack skeleton (fresh server only)

```bash
scp "$B/host-config.tar.gz" personal:/tmp/
ssh personal '
    tar -C /root -xzf /tmp/host-config.tar.gz
    cd /root/esprit-livre/api
    docker compose up -d postgres keycloak   # bring up stateful services only
'
```

## 2. Restore Postgres

The dump is in custom format, so use `pg_restore`. Drop and recreate the DB
to get a clean state.

```bash
scp "$B/postgres.dump" personal:/tmp/
ssh personal '
    docker cp /tmp/postgres.dump espritlivre-postgres:/tmp/postgres.dump
    docker exec espritlivre-postgres psql -U el -d postgres -c "DROP DATABASE IF EXISTS \"el-prod-db\";"
    docker exec espritlivre-postgres psql -U el -d postgres -c "CREATE DATABASE \"el-prod-db\" OWNER el;"
    docker exec espritlivre-postgres pg_restore -U el -d el-prod-db --clean --if-exists /tmp/postgres.dump
    docker exec espritlivre-postgres rm /tmp/postgres.dump
'
```

## 3. Restore Keycloak realm

```bash
scp "$B/keycloak-realm.tar.gz" personal:/tmp/
ssh personal '
    docker cp /tmp/keycloak-realm.tar.gz espritlivre-keycloak:/tmp/
    docker exec espritlivre-keycloak sh -c "
        cd /tmp && tar -xzf keycloak-realm.tar.gz &&
        /opt/keycloak/bin/kc.sh import --dir /tmp/keycloak-export --override true
    "
    docker restart espritlivre-keycloak
'
```

If the realm already exists and you want a hard reset, delete it first via
the admin console or `kcadm.sh delete realms/jhipster`.

## 4. Restore API media volume

```bash
scp "$B/media.tar.gz" personal:/tmp/
ssh personal '
    docker run --rm \
        -v espritlivre_api_media:/dst \
        -v /tmp/media.tar.gz:/media.tar.gz:ro \
        alpine sh -c "rm -rf /dst/* /dst/.[!.]* 2>/dev/null; tar -C /dst -xzf /media.tar.gz"
    rm /tmp/media.tar.gz
'
```

## 5. Restore TLS certs (if migrating to a new host)

```bash
scp "$B/tls-letsencrypt.tar.gz" personal:/tmp/
ssh personal 'tar -C /etc -xzf /tmp/tls-letsencrypt.tar.gz && rm /tmp/tls-letsencrypt.tar.gz'
```

## 6. Bring the full stack up

```bash
ssh personal 'cd /root/esprit-livre/api && docker compose up -d'
```

Smoke-check:

```bash
ssh personal 'docker ps --format "{{.Names}}\t{{.Status}}" | grep espritlivre'
curl -sI https://app.espritlivre.com/ | head -1
```

## Notes

- `host-config.tar.gz` contains `.env` files with secrets. Handle accordingly.
- `media.tar.gz` and the Postgres dump are captured live; they are consistent
  with themselves but not necessarily with each other at the millisecond. For
  this app the gap is harmless (media is write-once and referenced by DB rows
  after upload completes).
- To restore into a *different* database name or user, edit the `CREATE
  DATABASE` line and use `pg_restore --no-owner --role=<newuser>`.
