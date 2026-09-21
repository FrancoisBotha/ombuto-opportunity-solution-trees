#!/bin/bash
# Nightly logical backup of both databases to S3.
#
# Backs up the APPLICATION database and the KEYCLOAK database. Both are needed:
# the app's own /api/admin/backup endpoint deliberately excludes user rows
# (identities live in Keycloak), so an application archive alone cannot rebuild
# a working system.
#
# Publishes a CloudWatch metric on success so the absence of a backup can be
# alarmed on - a cron job that dies silently looks exactly like one with
# nothing to do.
#
# Install:
#   sudo install -m 0755 backup-to-s3.sh /usr/local/bin/ombuto-backup
#   sudo install -m 0600 backup.env /etc/ombuto-backup.env   # edit first
#   sudo systemctl enable --now ombuto-backup.timer
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
METRIC_NAMESPACE="${METRIC_NAMESPACE:-Ombuto/Backup}"

# One backup at a time, even if a run overruns into the next schedule.
exec 9>/var/lock/ombuto-backup.lock
flock -n 9 || {
	echo "another backup is already running" >&2
	exit 0
}

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

dump_database() {
	local db="$1" out="$2"
	# --clean --if-exists makes the dump restorable over an existing database.
	docker compose -f "$COMPOSE_DIR/docker-compose.prod.yml" exec -T "$POSTGRES_SERVICE" \
		pg_dump --username "$APP_DB_USER" --dbname "$db" --format=custom --clean --if-exists |
		gzip -9 >"$out"

	# A dump that is suspiciously small is usually a failed dump.
	local size
	size="$(stat -c %s "$out")"
	if [ "$size" -lt 1024 ]; then
		echo "dump of $db is only ${size} bytes - refusing to upload" >&2
		return 1
	fi
	echo "dumped $db (${size} bytes compressed)"
}

APP_FILE="$WORK_DIR/${APP_DB_NAME}-${STAMP}.dump.gz"
KC_FILE="$WORK_DIR/${KC_DB_NAME}-${STAMP}.dump.gz"

dump_database "$APP_DB_NAME" "$APP_FILE"
dump_database "$KC_DB_NAME" "$KC_FILE"

# Server-side encryption; retention is handled by the bucket's lifecycle rule,
# so nothing here deletes anything.
aws s3 cp "$APP_FILE" "s3://${S3_BUCKET}/${S3_PREFIX}/app/" --region "$AWS_REGION" --sse aws:kms
aws s3 cp "$KC_FILE" "s3://${S3_BUCKET}/${S3_PREFIX}/keycloak/" --region "$AWS_REGION" --sse aws:kms

aws cloudwatch put-metric-data \
	--region "$AWS_REGION" \
	--namespace "$METRIC_NAMESPACE" \
	--metric-name BackupSucceeded \
	--value 1 \
	--unit Count

echo "backup ${STAMP} uploaded to s3://${S3_BUCKET}/${S3_PREFIX}/"
