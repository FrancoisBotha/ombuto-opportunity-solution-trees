#!/bin/bash
# Restores both databases from a pair of S3 backups. Destructive: it drops and
# recreates the contents of both databases.
#
# Usage:
#   ombuto-restore 20260921T020000Z          # restore that timestamp
#   ombuto-restore latest                    # restore the newest pair
#
# Run a restore drill on a spare instance every quarter. A backup nobody has
# restored is a hypothesis, not a backup.
set -euo pipefail

CONFIG_FILE="${OMBUTO_BACKUP_ENV:-/etc/ombuto-backup.env}"
# shellcheck disable=SC1090
[ -f "$CONFIG_FILE" ] && . "$CONFIG_FILE"

: "${S3_BUCKET:?set S3_BUCKET in $CONFIG_FILE}"
: "${APP_DB_NAME:?set APP_DB_NAME}"
: "${APP_DB_USER:?set APP_DB_USER}"
: "${KC_DB_NAME:?set KC_DB_NAME}"
S3_PREFIX="${S3_PREFIX:-ombuto-ost}"
AWS_REGION="${AWS_REGION:-eu-west-1}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/ombuto-ost/deploy}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgresql}"
COMPOSE="docker compose -f $COMPOSE_DIR/docker-compose.prod.yml"

STAMP="${1:?usage: ombuto-restore <timestamp|latest>}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

find_key() {
	local kind="$1" db="$2"
	if [ "$STAMP" = "latest" ]; then
		aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}/${kind}/" --region "$AWS_REGION" |
			sort | tail -1 | awk '{print $4}'
	else
		echo "${db}-${STAMP}.dump.gz"
	fi
}

APP_KEY="$(find_key app "$APP_DB_NAME")"
KC_KEY="$(find_key keycloak "$KC_DB_NAME")"
[ -n "$APP_KEY" ] && [ -n "$KC_KEY" ] || {
	echo "could not resolve backup keys" >&2
	exit 1
}

echo "about to restore:"
echo "  app:      $APP_KEY"
echo "  keycloak: $KC_KEY"
echo "This REPLACES the contents of both databases. Type 'restore' to continue."
read -r confirmation
[ "$confirmation" = "restore" ] || {
	echo "aborted"
	exit 1
}

aws s3 cp "s3://${S3_BUCKET}/${S3_PREFIX}/app/${APP_KEY}" "$WORK_DIR/app.dump.gz" --region "$AWS_REGION"
aws s3 cp "s3://${S3_BUCKET}/${S3_PREFIX}/keycloak/${KC_KEY}" "$WORK_DIR/kc.dump.gz" --region "$AWS_REGION"

# Stop the consumers so nothing writes mid-restore. PostgreSQL stays up.
$COMPOSE stop app keycloak

restore_database() {
	local file="$1" db="$2"
	gunzip -c "$file" |
		$COMPOSE exec -T "$POSTGRES_SERVICE" \
			pg_restore --username "$APP_DB_USER" --dbname "$db" --clean --if-exists --no-owner
	echo "restored $db"
}

restore_database "$WORK_DIR/app.dump.gz" "$APP_DB_NAME"
restore_database "$WORK_DIR/kc.dump.gz" "$KC_DB_NAME"

$COMPOSE start keycloak
$COMPOSE start app

echo "restore complete - check https://\${PUBLIC_HOST}/management/health once the app reports healthy"
