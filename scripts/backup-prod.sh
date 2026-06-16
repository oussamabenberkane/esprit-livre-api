#!/usr/bin/env bash
# On-demand snapshot of the Esprit Livre prod server.
# Usage:
#   scripts/backup-prod.sh           # take a new backup
#   scripts/backup-prod.sh --prune   # take a new backup, then delete backups
#                                    # older than the 10 most recent
#
# Destination: $EL_BACKUP_ROOT, defaulting to ~/OneDrive/Desktop/app-backups.
# Runs from Git Bash on Windows: rsync is not available (tar over ssh instead),
# and `ln -s` silently deep-copies, so "latest" is a marker file, not a symlink.
set -euo pipefail

SSH_HOST="personal"
LOCAL_ROOT="${EL_BACKUP_ROOT:-${HOME}/OneDrive/Desktop/app-backups}"
REMOTE_ENV_FILE="/root/esprit-livre/api/.env"
KEEP_LAST=10
TIMESTAMP="$(date +%Y-%m-%d-%H%M%S)"
REMOTE_STAGE="/root/.backup-staging-$$-${TIMESTAMP}"
LOCAL_PARTIAL="${LOCAL_ROOT}/.partial-${TIMESTAMP}"
LOCAL_FINAL="${LOCAL_ROOT}/${TIMESTAMP}"

PRUNE=0
for arg in "$@"; do
    case "$arg" in
        --prune) PRUNE=1 ;;
        *) echo "unknown arg: $arg" >&2; exit 2 ;;
    esac
done

log()  { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
fail() { printf '[FAIL] %s\n' "$*" >&2; exit 1; }

cleanup_remote() {
    ssh "$SSH_HOST" "rm -rf '$REMOTE_STAGE'" 2>/dev/null || true
}
cleanup_local_partial() {
    rm -rf "$LOCAL_PARTIAL" 2>/dev/null || true
}
trap 'cleanup_remote; cleanup_local_partial' EXIT

mkdir -p "$LOCAL_ROOT"
START_EPOCH=$(date +%s)
log "starting backup ${TIMESTAMP}"

# 1. Remote capture ----------------------------------------------------------
log "preparing remote staging dir"
ssh "$SSH_HOST" "mkdir -p '$REMOTE_STAGE'"

# DB names come from the server's .env so this never drifts from the compose
# config. Keycloak keeps its own database (KC_DB_NAME) in the same postgres —
# it holds all users/credentials and must be dumped alongside the app DB.
log "dumping postgres (app db, keycloak db, globals)"
ssh "$SSH_HOST" "
    set -e
    DB_USER=\$(grep -E '^DB_USER=' '$REMOTE_ENV_FILE' | cut -d= -f2)
    DB_NAME=\$(grep -E '^DB_NAME=' '$REMOTE_ENV_FILE' | cut -d= -f2)
    KC_DB_NAME=\$(grep -E '^KC_DB_NAME=' '$REMOTE_ENV_FILE' | cut -d= -f2)
    : \"\${DB_USER:?missing from .env}\" \"\${DB_NAME:?missing from .env}\" \"\${KC_DB_NAME:?missing from .env}\"
    docker exec espritlivre-postgres pg_dump -U \"\$DB_USER\" -F c \"\$DB_NAME\" > '$REMOTE_STAGE/postgres-app.dump'
    docker exec espritlivre-postgres pg_dump -U \"\$DB_USER\" -F c \"\$KC_DB_NAME\" > '$REMOTE_STAGE/postgres-keycloak.dump'
    docker exec espritlivre-postgres pg_dumpall -U \"\$DB_USER\" --globals-only > '$REMOTE_STAGE/postgres-globals.sql'
" || fail "postgres dumps failed"

log "exporting keycloak realm (live)"
# kc.sh export writes into the container; pull it out with docker cp.
# Note: kc.sh exits non-zero because the running KC already holds the management
# port (9000). The export itself completes before that — verify the realm file
# exists rather than trusting the exit code.
# The export spawns a SECOND JVM inside the 1G-capped container; it inherits the
# container's MaxRAMPercentage=50 and can trip the cgroup OOM killer (seen
# 2026-06-12). Cap the export JVM's heap and retry, since the OOM is timing-
# dependent on the live KC's footprint.
ssh "$SSH_HOST" "
    set -e
    ok=0
    for attempt in 1 2 3; do
        docker exec espritlivre-keycloak rm -rf /tmp/kc-export
        docker exec -e JAVA_OPTS_APPEND='-XX:MaxRAMPercentage=25.0' espritlivre-keycloak \
            /opt/keycloak/bin/kc.sh export --dir /tmp/kc-export --realm jhipster --users realm_file \
            > /tmp/kc-export-attempt.log 2>&1 || true
        if docker exec espritlivre-keycloak test -f /tmp/kc-export/jhipster-realm.json; then
            ok=1
            break
        fi
        echo \"keycloak export attempt \$attempt failed, retrying\" >&2
    done
    if [ \$ok -ne 1 ]; then
        echo 'keycloak export failed after 3 attempts; last log lines:' >&2
        tail -5 /tmp/kc-export-attempt.log >&2
        rm -f /tmp/kc-export-attempt.log
        exit 1
    fi
    rm -f /tmp/kc-export-attempt.log
    docker cp espritlivre-keycloak:/tmp/kc-export '$REMOTE_STAGE/keycloak-export'
    docker exec espritlivre-keycloak rm -rf /tmp/kc-export
    tar -C '$REMOTE_STAGE' -czf '$REMOTE_STAGE/keycloak-realm.tar.gz' keycloak-export
    rm -rf '$REMOTE_STAGE/keycloak-export'
" || fail "keycloak export failed"

log "archiving api media volume"
ssh "$SSH_HOST" "
    docker run --rm \
        -v espritlivre_api_media:/src:ro \
        -v '$REMOTE_STAGE':/out \
        alpine tar -C /src -czf /out/media.tar.gz .
" || fail "media tar failed"

# Redis holds the WhatsApp agent's conversation history + webhook dedup. It's
# TTL'd/ephemeral, but cheap to capture. SAVE forces a synchronous RDB write so
# the on-disk dump.rdb is current before we tar the volume read-only.
log "snapshotting redis (whatsapp agent state)"
ssh "$SSH_HOST" "
    set -e
    docker exec espritlivre-redis redis-cli SAVE >/dev/null
    docker run --rm \
        -v espritlivre_redis_data:/src:ro \
        -v '$REMOTE_STAGE':/out \
        alpine tar -C /src -czf /out/redis.tar.gz .
" || fail "redis snapshot failed"

log "archiving host config (compose, nginx, .env)"
ssh "$SSH_HOST" "tar -C /root -czf '$REMOTE_STAGE/host-config.tar.gz' esprit-livre" \
    || fail "host-config tar failed"

log "archiving TLS certs (/etc/letsencrypt)"
ssh "$SSH_HOST" "tar -C /etc -czf '$REMOTE_STAGE/tls-letsencrypt.tar.gz' letsencrypt" \
    || fail "tls tar failed"

# Cert renewal lives outside /root/esprit-livre: root's crontab calls
# /root/renew-espritlivre-certs.sh, and acme.sh keeps account keys + issued
# certs in /root/.acme.sh. Without these a restored host can't renew.
log "archiving cert renewal infra (crontab, renew script, acme.sh)"
ssh "$SSH_HOST" "
    set -e
    mkdir -p '$REMOTE_STAGE/host-extras'
    crontab -l > '$REMOTE_STAGE/host-extras/crontab-root.txt'
    cp /root/renew-espritlivre-certs.sh '$REMOTE_STAGE/host-extras/'
    cp -a /root/.acme.sh '$REMOTE_STAGE/host-extras/acme.sh'
    tar -C '$REMOTE_STAGE' -czf '$REMOTE_STAGE/host-extras.tar.gz' host-extras
    rm -rf '$REMOTE_STAGE/host-extras'
" || fail "host-extras tar failed"

log "building manifest"
ssh "$SSH_HOST" "
    cd '$REMOTE_STAGE'
    {
        echo 'Esprit Livre prod backup'
        echo 'timestamp: ${TIMESTAMP}'
        echo 'host: \$(hostname)'
        echo 'docker:'
        docker ps --format '  {{.Names}}\t{{.Image}}' | grep espritlivre || true
        echo
        echo 'files:'
        for f in *.dump *.sql *.tar.gz; do
            [ -f \"\$f\" ] || continue
            size=\$(stat -c%s \"\$f\")
            sha=\$(sha256sum \"\$f\" | awk '{print \$1}')
            printf '  %-28s %12d  %s\n' \"\$f\" \"\$size\" \"\$sha\"
        done
    } > MANIFEST.txt
" || fail "manifest build failed"

# 2. Transfer ----------------------------------------------------------------
log "transferring to ${LOCAL_PARTIAL} (tar over ssh)"
mkdir -p "$LOCAL_PARTIAL"
ssh "$SSH_HOST" "tar -C '$REMOTE_STAGE' -cf - ." | tar -C "$LOCAL_PARTIAL" -xf - \
    || fail "transfer failed"

log "verifying checksums against manifest"
(cd "$LOCAL_PARTIAL" && \
    awk '/^files:/{f=1;next} f && NF==3 {print $3 "  " $1}' MANIFEST.txt | sha256sum -c --quiet) \
    || fail "checksum verification failed"

# 3. Atomic finalize ---------------------------------------------------------
mv "$LOCAL_PARTIAL" "$LOCAL_FINAL"
printf '%s\n' "$TIMESTAMP" > "$LOCAL_ROOT/latest.txt"
log "finalized ${LOCAL_FINAL}"

# 4. Summary -----------------------------------------------------------------
END_EPOCH=$(date +%s)
ELAPSED=$((END_EPOCH - START_EPOCH))
TOTAL=$(du -sh "$LOCAL_FINAL" | awk '{print $1}')
echo
echo "=== backup complete ==="
echo "location : $LOCAL_FINAL"
echo "latest   : $LOCAL_ROOT/latest.txt -> $TIMESTAMP"
echo "size     : $TOTAL"
echo "elapsed  : ${ELAPSED}s"
echo "contents :"
(cd "$LOCAL_FINAL" && ls -lh | awk 'NR>1 {printf "  %-28s %s\n", $NF, $5}')

# 5. Retention ---------------------------------------------------------------
echo
mapfile -t ALL_BACKUPS < <(
    find "$LOCAL_ROOT" -mindepth 1 -maxdepth 1 -type d \
        -regextype posix-extended -regex '.*/[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{6}$' \
        -printf '%f\n' | sort -r
)
TOTAL_COUNT=${#ALL_BACKUPS[@]}
if (( TOTAL_COUNT > KEEP_LAST )); then
    TO_PRUNE=("${ALL_BACKUPS[@]:$KEEP_LAST}")
    echo "retention: ${TOTAL_COUNT} backups present, keeping last ${KEEP_LAST}."
    if (( PRUNE == 1 )); then
        echo "pruning ${#TO_PRUNE[@]} old backup(s):"
        for b in "${TO_PRUNE[@]}"; do
            echo "  rm -rf $LOCAL_ROOT/$b"
            rm -rf "${LOCAL_ROOT:?}/$b"
        done
    else
        echo "would prune (pass --prune to actually delete):"
        for b in "${TO_PRUNE[@]}"; do
            echo "  $LOCAL_ROOT/$b"
        done
    fi
else
    echo "retention: ${TOTAL_COUNT}/${KEEP_LAST} backups, nothing to prune."
fi
