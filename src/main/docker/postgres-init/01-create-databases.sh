#!/bin/bash
set -e

# Create the keycloak database if it doesn't exist
# POSTGRES_USER is the superuser (el), POSTGRES_DB is the main app DB (el-prod-db)
# Keycloak needs its own database (KC_DB_NAME=keycloak-prod)

KC_DB="${KC_DB_NAME:-keycloak}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE "${KC_DB}" OWNER ${POSTGRES_USER}'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${KC_DB}')
    \gexec
EOSQL

echo "Database '${KC_DB}' ready."
