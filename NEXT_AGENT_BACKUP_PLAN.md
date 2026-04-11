# Task: plan an on-demand backup of the Esprit Livre prod server

The user wants a single command they can run from their laptop that snapshots
the **entire prod server state** into a local directory on their machine. Your
job in this session is to **produce a plan and ask clarifying questions** —
not to implement yet. Do not write code or run destructive commands until the
user agrees on the plan.

## What you're working with

- **Server**: SSH alias `personal` in `~/.ssh/config` → `root@185.187.235.22`.
  Already set up, key-based auth works.
- **Stack on server** (all Docker, one compose project):
  - `espritlivre-postgres` — Postgres 17, user `el`, db `el-prod-db`, password
    in the container env (`POSTGRES_PASSWORD`). Data volume:
    `espritlivre_postgres_data` → `/var/lib/postgresql/data`.
  - `espritlivre-keycloak` — Keycloak 26.2.3. Holds realm config, clients,
    users, roles. Realm: `jhipster`. Has its own data volume.
  - `espritlivre-api` — Spring Boot app. Named volumes:
    - `espritlivre_api_media` → `/app/media` (book covers, author pictures,
      category images, user avatars — **stateful**, user-uploaded content)
    - `espritlivre_api_logs` → `/app/logs`
  - `espritlivre-user-frontend`, `espritlivre-admin-frontend`, `espritlivre-nginx`
    — stateless containers, images only.
- **Host-side config** you may also want to capture:
  - `docker-compose.yml` (or whatever orchestration file is on the server)
  - nginx config / TLS certs if they live on the host rather than in a volume
  - `.env` files holding secrets
- **Existing backup conventions**: earlier in this repo I already wrote pg_dump
  snapshots to `personal:/root/*-seed-*.dump` and copied one locally to
  `~/backups/el/`. The user's local backup root is `~/backups/el/`. Stick with
  that unless the user says otherwise.

## What "whole server state" probably means

At minimum:
1. **Postgres logical dump** — `pg_dump -F c` of `el-prod-db` (run inside the
   postgres container so you don't need a client on the host).
2. **Keycloak export** — realm export including users, or the keycloak data
   volume tarred. Decide which, and ask if unsure.
3. **API media volume** — tarball of `espritlivre_api_media` (this is the user
   content and is the most painful thing to lose).
4. **Compose file + env files** — so a restore can rebuild the stack.

Optional and worth asking about:
- API logs (`espritlivre_api_logs`) — probably not worth backing up.
- Full postgres data directory tarball on top of `pg_dump` (belt-and-braces,
  but only safe if postgres is stopped or you use `pg_basebackup`).
- TLS certs / letsencrypt state if managed on host.

## What the final deliverable should look like (the user's mental model)

Something the user types on their laptop — a shell script, a Makefile target,
or a Claude slash command — that:

1. Connects to `personal` via SSH.
2. Produces consistent snapshots of each component listed above in a temp dir
   on the server.
3. Streams them back to `~/backups/el/<timestamp>/` on the laptop (or a single
   tarball per run — ask the user which they prefer).
4. Prints a summary: what was captured, total size, location, time elapsed.
5. Exits non-zero on any failure.

The whole thing should be **idempotent and safe to interrupt** — a failed
run must not corrupt prior backups.

## Ask the user before building

These are open decisions. Do not pick for them:

1. **Trigger UX**: a bash script in `scripts/`? a Make target? a slash command
   under `.claude/commands/`? Something else?
2. **Local destination**: confirm `~/backups/el/` and the per-run subfolder
   naming (`YYYY-MM-DD-HHMMSS/`?).
3. **Retention**: keep everything forever? auto-prune after N backups or N
   days? none of the above?
4. **Keycloak strategy**: live realm export via `kc.sh export` (cleaner, works
   while KC is running) vs. tar of the data volume (requires briefly stopping
   KC for consistency). Ask which they want.
5. **Downtime tolerance**: are they OK with a ~10 second stop of Keycloak / API
   for consistency, or must everything stay hot? pg_dump is always hot-safe,
   but volume tars are only fully consistent with the container stopped.
6. **Secrets**: should `.env` files and TLS keys be included in the backup?
   (They contain credentials — user should explicitly opt in.) Should the
   final archive be encrypted (gpg / age)?
7. **Size expectations**: has the user ever seen how big `/app/media` and the
   postgres dir are on the server? If multi-GB, transfer strategy matters
   (rsync --partial? compression level? split archives?).
8. **Restore documentation**: should you also produce a `RESTORE.md` explaining
   how to reverse each step? Probably yes, but confirm.

## What you don't need to do

- Don't implement a restore flow yet — the user only asked for backup.
- Don't set up automated scheduling (cron, systemd timers) — this is an
  on-demand command, not a scheduled job.
- Don't touch the server state. Read-only SSH probes are fine; no writes
  until the user approves the plan.

## Start by

Reading this file, reading the repo's `CLAUDE.md` for project context, and
running a couple of read-only probes on the server to confirm the current
state (docker volumes, approximate sizes, whether a docker-compose file
exists on the host and where). Then present the plan and your questions in
one message. Wait for answers before building anything.
