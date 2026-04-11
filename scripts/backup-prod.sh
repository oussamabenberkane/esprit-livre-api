#!/usr/bin/env bash
# On-demand snapshot of the Esprit Livre prod server.
# Usage:
#   scripts/backup-prod.sh           # take a new backup
#   scripts/backup-prod.sh --prune   # take a new backup, then delete backups
#                                    # older than the 10 most recent
set -euo pipefail

SSH_HOST="personal"
LOCAL_ROOT="${HOME}/backups/el"
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

log "dumping postgres (pg_dump -F c)"
ssh "$SSH_HOST" "docker exec espritlivre-postgres pg_dump -U el -F c el-prod-db > '$REMOTE_STAGE/postgres.dump'" \
    || fail "pg_dump failed"

log "exporting keycloak realm (live)"
# kc.sh export writes into the container; pull it out with docker cp.
# Note: kc.sh exits non-zero because the running KC already holds the management
# port (9000). The export itself completes before that — verify the realm file
# exists rather than trusting the exit code.
ssh "$SSH_HOST" "
    set -e
    docker exec espritlivre-keycloak rm -rf /tmp/kc-export
    docker exec espritlivre-keycloak /opt/keycloak/bin/kc.sh export \
        --dir /tmp/kc-export --realm jhipster --users realm_file >/dev/null 2>&1 || true
    docker exec espritlivre-keycloak test -f /tmp/kc-export/jhipster-realm.json
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

log "archiving host config (compose, nginx, .env)"
ssh "$SSH_HOST" "tar -C /root -czf '$REMOTE_STAGE/host-config.tar.gz' esprit-livre" \
    || fail "host-config tar failed"

log "archiving TLS certs (/etc/letsencrypt)"
ssh "$SSH_HOST" "tar -C /etc -czf '$REMOTE_STAGE/tls-letsencrypt.tar.gz' letsencrypt" \
    || fail "tls tar failed"

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
        for f in *.dump *.tar.gz; do
            [ -f \"\$f\" ] || continue
            size=\$(stat -c%s \"\$f\")
            sha=\$(sha256sum \"\$f\" | awk '{print \$1}')
            printf '  %-28s %12d  %s\n' \"\$f\" \"\$size\" \"\$sha\"
        done
    } > MANIFEST.txt
" || fail "manifest build failed"

# 2. Transfer ----------------------------------------------------------------
log "transferring to ${LOCAL_PARTIAL}"
mkdir -p "$LOCAL_PARTIAL"
rsync -aq --info=progress2 "${SSH_HOST}:${REMOTE_STAGE}/" "$LOCAL_PARTIAL/" \
    || fail "rsync failed"

# 3. Atomic finalize ---------------------------------------------------------
mv "$LOCAL_PARTIAL" "$LOCAL_FINAL"
ln -sfn "$LOCAL_FINAL" "$LOCAL_ROOT/latest"
log "finalized ${LOCAL_FINAL}"

# 4. Summary -----------------------------------------------------------------
END_EPOCH=$(date +%s)
ELAPSED=$((END_EPOCH - START_EPOCH))
TOTAL=$(du -sh "$LOCAL_FINAL" | awk '{print $1}')
echo
echo "=== backup complete ==="
echo "location : $LOCAL_FINAL"
echo "symlink  : $LOCAL_ROOT/latest"
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
