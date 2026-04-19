#!/usr/bin/env bash
set -euo pipefail

# ─── Config ───────────────────────────────────────────────────────────────────
SSH_HOST="personal"
PG_CONTAINER="espritlivre-postgres"
PG_USER="el"
PG_DB="el-prod-db"
KC_CONTAINER="espritlivre-keycloak"
KC_REALM="jhipster"
KC_ADMIN="el-admin"
KC_ADMIN_PASSWORD="dDTQmrJFdbv4xL3y5rexQCLG3w6pGUKQNnZ"
KC_URL="http://localhost:8080"
# ──────────────────────────────────────────────────────────────────────────────

usage() {
  echo "Usage: $0 <email> [<email2> ...]"
  echo "Example: $0 user@example.com"
  exit 1
}

[[ $# -eq 0 ]] && usage

psql_cmd() {
  ssh "$SSH_HOST" "docker exec $PG_CONTAINER psql -U $PG_USER -d $PG_DB -t -A -c \"$1\""
}

kcadm() {
  ssh "$SSH_HOST" "docker exec $KC_CONTAINER /opt/keycloak/bin/kcadm.sh $*"
}

# Authenticate to Keycloak once
echo "Authenticating to Keycloak..."
kcadm config credentials \
  --server "$KC_URL" \
  --realm master \
  --user "$KC_ADMIN" \
  --password "$KC_ADMIN_PASSWORD" 2>&1 | grep -v "^$" || true

for EMAIL in "$@"; do
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "Processing: $EMAIL"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # ── 1. Look up user in PostgreSQL ──────────────────────────────────────────
  USER_ID=$(psql_cmd "SELECT id FROM jhi_user WHERE email = '$EMAIL' LIMIT 1;")

  if [[ -z "$USER_ID" ]]; then
    echo "  [DB] No user found with email '$EMAIL' — skipping DB deletion."
  else
    echo "  [DB] Found user: id=$USER_ID"

    # Count related rows
    ORDERS=$(psql_cmd "SELECT COUNT(*) FROM jhi_order WHERE user_id = '$USER_ID';")
    LIKES=$(psql_cmd "SELECT COUNT(*) FROM jhi_like WHERE user_id = '$USER_ID';")
    AUTHORITIES=$(psql_cmd "SELECT COUNT(*) FROM jhi_user_authority WHERE user_id = '$USER_ID';")
    echo "  [DB] Related rows — orders: $ORDERS, likes: $LIKES, authorities: $AUTHORITIES"

    # Delete in FK-safe order
    ssh "$SSH_HOST" "docker exec $PG_CONTAINER psql -U $PG_USER -d $PG_DB -c \"
      BEGIN;
      DELETE FROM order_item  WHERE order_id IN (SELECT id FROM jhi_order WHERE user_id = '$USER_ID');
      DELETE FROM jhi_order         WHERE user_id = '$USER_ID';
      DELETE FROM jhi_like          WHERE user_id = '$USER_ID';
      DELETE FROM jhi_user_authority WHERE user_id = '$USER_ID';
      DELETE FROM jhi_user           WHERE id      = '$USER_ID';
      COMMIT;
    \"" | grep -v "^$"
    echo "  [DB] Deleted."
  fi

  # ── 2. Look up & delete user in Keycloak ───────────────────────────────────
  KC_USER_JSON=$(kcadm get users -r "$KC_REALM" -q "email=$EMAIL" 2>&1)
  KC_USER_ID=$(echo "$KC_USER_JSON" | grep '"id"' | head -1 | sed 's/.*"id" : "\([^"]*\)".*/\1/')

  if [[ -z "$KC_USER_ID" ]]; then
    echo "  [KC] No user found in Keycloak — skipping."
  else
    echo "  [KC] Found user: id=$KC_USER_ID"
    kcadm delete "users/$KC_USER_ID" -r "$KC_REALM"
    echo "  [KC] Deleted."
  fi

  echo "  Done: $EMAIL"
done

echo ""
echo "All done."
