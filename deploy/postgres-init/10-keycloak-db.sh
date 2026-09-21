#!/bin/bash
# Runs once, on first start of an empty PostgreSQL data volume.
# Creates the second database Keycloak needs, beside the application's own.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
	CREATE USER ${KC_DB_USER} WITH PASSWORD '${KC_DB_PASSWORD}';
	CREATE DATABASE ${KC_DB_NAME} OWNER ${KC_DB_USER};
SQL

echo "created keycloak database ${KC_DB_NAME} owned by ${KC_DB_USER}"
