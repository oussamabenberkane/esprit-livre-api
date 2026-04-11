#!/usr/bin/env bash
# Restore an Esprit Livre prod backup produced by scripts/backup-prod.sh.
#
# Usage:
#   scripts/restore-prod.sh [<backup-dir>] [component flags] [--execute] [--yes]
#
# Components:
#   --postgres       drop & recreate el-prod-db, pg_restore from postgres.dump
#   --keycloak       kc.sh import --override true (realm jhipster)
#   --media          wipe & repopulate the espritlivre_api_media volume
#   --tls            extract tls-letsencrypt.tar.gz into /etc/, restart nginx
#   --host-config    print manual extraction instructions (NO automated action)
#   --all            select every component above
#
# Default is DRY RUN — prints the commands it would run. Pass --execute to
# actually run them. Destructive components (postgres/keycloak/media) then
# require a typed-timestamp confirmation unless --yes is also passed. --tls
# alone prompts for a y/N confirmation unless --yes.
#
# <backup-dir> defaults to ~/backups/el/latest.
set -euo pipefail

SSH_HOST="personal"
LOCAL_ROOT="${HOME}/backups/el"
REMOTE_STAGE="/root/.restore-staging-$$"

BACKUP_DIR=""
DO_POSTGRES=0
DO_KEYCLOAK=0
DO_MEDIA=0
DO_TLS=0
DO_HOSTCONFIG=0
EXECUTE=0
ASSUME_YES=0

usage() {
    sed -n '2,19p' "$0" | sed 's/^# \{0,1\}//'
}

for arg in "$@"; do
    case "$arg" in
        --postgres)    DO_POSTGRES=1 ;;
        --keycloak)    DO_KEYCLOAK=1 ;;
        --media)       DO_MEDIA=1 ;;
        --tls)         DO_TLS=1 ;;
        --host-config) DO_HOSTCONFIG=1 ;;
        --all)         DO_POSTGRES=1; DO_KEYCLOAK=1; DO_MEDIA=1; DO_TLS=1; DO_HOSTCONFIG=1 ;;
        --execute)     EXECUTE=1 ;;
        --yes)         ASSUME_YES=1 ;;
        -h|--help)     usage; exit 0 ;;
        --*)           echo "unknown flag: $arg" >&2; usage >&2; exit 2 ;;
        *)             BACKUP_DIR="$arg" ;;
    esac
done

BACKUP_DIR="${BACKUP_DIR:-${LOCAL_ROOT}/latest}"
[[ -e "$BACKUP_DIR" ]] || { echo "backup dir does not exist: $BACKUP_DIR" >&2; exit 1; }
BACKUP_DIR="$(readlink -f "$BACKUP_DIR")"
[[ -d "$BACKUP_DIR" ]] || { echo "not a directory: $BACKUP_DIR" >&2; exit 1; }
BACKUP_TS="$(basename "$BACKUP_DIR")"

SELECTED=$((DO_POSTGRES + DO_KEYCLOAK + DO_MEDIA + DO_TLS + DO_HOSTCONFIG))
if (( SELECTED == 0 )); then
    echo "no components selected — pick at least one (--postgres, --keycloak, --media, --tls, --host-config, --all)" >&2
    exit 2
fi

log()  { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
fail() { printf '[FAIL] %s\n' "$*" >&2; exit 1; }

# ---- manifest verification ----
log "verifying manifest checksums in ${BACKUP_DIR}"
[[ -f "$BACKUP_DIR/MANIFEST.txt" ]] || fail "MANIFEST.txt missing in $BACKUP_DIR"
MANIFEST_LINES=$(awk '/^  [a-zA-Z]/ && NF==3 {print $3"  "$1}' "$BACKUP_DIR/MANIFEST.txt")
[[ -n "$MANIFEST_LINES" ]] || fail "MANIFEST.txt has no file entries"
( cd "$BACKUP_DIR" && printf '%s\n' "$MANIFEST_LINES" | sha256sum -c --quiet ) \
    || fail "manifest checksum mismatch — backup is corrupt"

# ---- build plan ----
PLAN=()
(( DO_POSTGRES ))   && PLAN+=("postgres")
(( DO_KEYCLOAK ))   && PLAN+=("keycloak")
(( DO_MEDIA ))      && PLAN+=("media")
(( DO_TLS ))        && PLAN+=("tls")
(( DO_HOSTCONFIG )) && PLAN+=("host-config")

DESTRUCTIVE=0
(( DO_POSTGRES + DO_KEYCLOAK + DO_MEDIA > 0 )) && DESTRUCTIVE=1

NEEDS_REMOTE=0
(( DO_POSTGRES + DO_KEYCLOAK + DO_MEDIA + DO_TLS > 0 )) && NEEDS_REMOTE=1

echo
echo "=== restore plan ==="
echo "backup   : $BACKUP_DIR"
echo "target   : $SSH_HOST"
echo "steps    : ${PLAN[*]}"
echo "mode     : $( (( EXECUTE )) && echo EXECUTE || echo DRY-RUN )"
echo

# ---- plan printing ----

plan_postgres() {
cat <<EOF
----- postgres -----
docker cp '$REMOTE_STAGE/postgres.dump' espritlivre-postgres:/tmp/postgres.dump
docker stop espritlivre-api
docker exec espritlivre-postgres psql -U el -d postgres -c 'DROP DATABASE IF EXISTS "el-prod-db";'
docker exec espritlivre-postgres psql -U el -d postgres -c 'CREATE DATABASE "el-prod-db" OWNER el;'
docker exec espritlivre-postgres pg_restore -U el -d el-prod-db /tmp/postgres.dump
docker exec espritlivre-postgres rm /tmp/postgres.dump
docker start espritlivre-api

EOF
}

plan_keycloak() {
cat <<EOF
----- keycloak -----
tar -C '$REMOTE_STAGE' -xzf '$REMOTE_STAGE/keycloak-realm.tar.gz'
docker exec espritlivre-keycloak rm -rf /tmp/kc-import
docker cp '$REMOTE_STAGE/keycloak-export' espritlivre-keycloak:/tmp/kc-import
docker exec espritlivre-keycloak /opt/keycloak/bin/kc.sh import --dir /tmp/kc-import --override true
    # (tolerates non-zero exit from port-9000 bind conflict; verifies via log marker)
docker exec espritlivre-keycloak rm -rf /tmp/kc-import
docker restart espritlivre-keycloak

EOF
}

plan_media() {
cat <<EOF
----- media -----
docker run --rm \\
    -v espritlivre_api_media:/dst \\
    -v '$REMOTE_STAGE/media.tar.gz':/src.tar.gz:ro \\
    alpine sh -c 'rm -rf /dst/* /dst/.[!.]* 2>/dev/null || true; tar -C /dst -xzf /src.tar.gz'

EOF
}

plan_tls() {
cat <<EOF
----- tls -----
tar -C /etc -xzf '$REMOTE_STAGE/tls-letsencrypt.tar.gz'
docker restart espritlivre-nginx

EOF
}

plan_hostconfig() {
cat <<EOF
----- host-config (MANUAL — not automated) -----
host-config restore is a disaster-recovery step for rebuilding a fresh
host. To extract manually:

    scp '$BACKUP_DIR/host-config.tar.gz' ${SSH_HOST}:/tmp/
    ssh ${SSH_HOST} 'tar -C /root -xzf /tmp/host-config.tar.gz && rm /tmp/host-config.tar.gz'
    ssh ${SSH_HOST} 'cd /root/esprit-livre/api && docker compose up -d'

EOF
}

for step in "${PLAN[@]}"; do
    case "$step" in
        postgres)    plan_postgres ;;
        keycloak)    plan_keycloak ;;
        media)       plan_media ;;
        tls)         plan_tls ;;
        host-config) plan_hostconfig ;;
    esac
done

if (( EXECUTE == 0 )); then
    echo "(dry-run — pass --execute to actually run these steps)"
    exit 0
fi

# ---- execute path ----

if (( DESTRUCTIVE == 1 && ASSUME_YES == 0 )); then
    destructive_list=""
    (( DO_POSTGRES )) && destructive_list+="postgres "
    (( DO_KEYCLOAK )) && destructive_list+="keycloak "
    (( DO_MEDIA ))    && destructive_list+="media "
    echo "!!! this will destroy prod data for: ${destructive_list}"
    echo "    backup timestamp: ${BACKUP_TS}"
    read -r -p "type the timestamp to confirm: " reply
    [[ "$reply" == "$BACKUP_TS" ]] || fail "confirmation mismatch — aborting"
elif (( DESTRUCTIVE == 0 && DO_TLS == 1 && ASSUME_YES == 0 )); then
    read -r -p "overwrite /etc/letsencrypt and restart espritlivre-nginx? [y/N] " reply
    [[ "$reply" == "y" || "$reply" == "Y" ]] || fail "aborted"
fi

cleanup_remote() {
    (( NEEDS_REMOTE )) && ssh "$SSH_HOST" "rm -rf '$REMOTE_STAGE'" 2>/dev/null || true
}
trap cleanup_remote EXIT

if (( NEEDS_REMOTE )); then
    log "staging backup to ${SSH_HOST}:${REMOTE_STAGE}"
    ssh "$SSH_HOST" "mkdir -p '$REMOTE_STAGE'"
    rsync -aq \
        --exclude host-config.tar.gz \
        "$BACKUP_DIR/" "${SSH_HOST}:${REMOTE_STAGE}/" \
        || fail "failed to stage backup on remote"
fi

run_postgres() {
    log "restore: postgres"
    ssh "$SSH_HOST" bash -s <<EOF
set -e
docker cp '$REMOTE_STAGE/postgres.dump' espritlivre-postgres:/tmp/postgres.dump
docker stop espritlivre-api >/dev/null
docker exec espritlivre-postgres psql -U el -d postgres -c 'DROP DATABASE IF EXISTS "el-prod-db";' >/dev/null
docker exec espritlivre-postgres psql -U el -d postgres -c 'CREATE DATABASE "el-prod-db" OWNER el;' >/dev/null
docker exec espritlivre-postgres pg_restore -U el -d el-prod-db /tmp/postgres.dump
docker exec espritlivre-postgres rm /tmp/postgres.dump
docker start espritlivre-api >/dev/null
EOF
}

run_keycloak() {
    log "restore: keycloak"
    ssh "$SSH_HOST" bash -s <<EOF
set -e
tar -C '$REMOTE_STAGE' -xzf '$REMOTE_STAGE/keycloak-realm.tar.gz'
# Use -u 0 for cleanup: docker cp writes files as root inside the container,
# but the default exec user is the keycloak user which can't delete them.
docker exec -u 0 espritlivre-keycloak rm -rf /tmp/kc-import
docker cp '$REMOTE_STAGE/keycloak-export' espritlivre-keycloak:/tmp/kc-import
set +e
docker exec espritlivre-keycloak /opt/keycloak/bin/kc.sh import \
    --dir /tmp/kc-import --override true > /tmp/kc-import.log 2>&1
set -e
if ! grep -qE 'Realm .* imported|Import finished|KC-SERVICES0030' /tmp/kc-import.log; then
    echo '--- kc.sh import output ---' >&2
    cat /tmp/kc-import.log >&2
    echo '--- end ---' >&2
    rm -f /tmp/kc-import.log
    exit 1
fi
rm -f /tmp/kc-import.log
docker exec -u 0 espritlivre-keycloak rm -rf /tmp/kc-import
docker restart espritlivre-keycloak >/dev/null
EOF
}

run_media() {
    log "restore: media"
    ssh "$SSH_HOST" bash -s <<EOF
set -e
docker run --rm \
    -v espritlivre_api_media:/dst \
    -v '$REMOTE_STAGE/media.tar.gz':/src.tar.gz:ro \
    alpine sh -c 'rm -rf /dst/* /dst/.[!.]* 2>/dev/null || true; tar -C /dst -xzf /src.tar.gz'
EOF
}

run_tls() {
    log "restore: tls"
    ssh "$SSH_HOST" bash -s <<EOF
set -e
tar -C /etc -xzf '$REMOTE_STAGE/tls-letsencrypt.tar.gz'
docker restart espritlivre-nginx >/dev/null
EOF
}

APPLIED=()
FAILED=""

run_step() {
    case "$1" in
        postgres)    run_postgres ;;
        keycloak)    run_keycloak ;;
        media)       run_media ;;
        tls)         run_tls ;;
        host-config) log "skipping host-config (manual-only component)"; return 0 ;;
    esac
}

for step in "${PLAN[@]}"; do
    if run_step "$step"; then
        APPLIED+=("$step")
    else
        FAILED="$step"
        break
    fi
done

echo
echo "=== restore result ==="
echo "applied: ${APPLIED[*]:-<none>}"
if [[ -n "$FAILED" ]]; then
    echo "failed : $FAILED"
    SKIPPED=()
    seen_failed=0
    for step in "${PLAN[@]}"; do
        if (( seen_failed )); then
            SKIPPED+=("$step")
        fi
        [[ "$step" == "$FAILED" ]] && seen_failed=1
    done
    [[ ${#SKIPPED[@]} -gt 0 ]] && echo "skipped: ${SKIPPED[*]}"
    echo
    echo "prod is now in a MIXED state. Investigate before retrying."
    exit 1
fi
echo "done."
