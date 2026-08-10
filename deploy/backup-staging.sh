#!/usr/bin/env bash
#
# Backup the Postgres database of the Pullwise staging deployment.
#
# Usage:
#   backup-staging.sh install    Install a daily cron backup on the server (idempotent)
#   backup-staging.sh run        Run an immediate backup now
#
# Backups are stored on the server at ~/$DEPLOY_DIR/backups/
# as `pullwise-<timestamp>.sql.gz` (pg_dump + gzip). Old backups are pruned
# after BACKUP_KEEP_DAYS days.
#
# Env vars (same as deploy-staging.sh):
#   STAGING_HOST        SSH host of the staging server (required)
#   STAGING_USER        SSH user (required)
#   STAGING_SSH_KEY     PEM private key (optional if ssh-agent is preloaded)
#   DEPLOY_DIR          Remote directory, default "pullwise"
#   BACKUP_KEEP_DAYS    Retention in days, default 7

set -euo pipefail

STAGING_HOST="${STAGING_HOST:-}"
STAGING_USER="${STAGING_USER:-}"
STAGING_SSH_KEY="${STAGING_SSH_KEY:-}"
DEPLOY_DIR="${DEPLOY_DIR:-pullwise}"
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"

COMMAND="${1:-run}"

if [[ -z "$STAGING_HOST" || -z "$STAGING_USER" ]]; then
  echo "::error::STAGING_HOST and STAGING_USER must be set." >&2
  exit 1
fi

SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null)
if [[ -n "$STAGING_SSH_KEY" ]]; then
  KEY_FILE="$(mktemp)"
  printf '%s\n' "$STAGING_SSH_KEY" > "$KEY_FILE"
  chmod 600 "$KEY_FILE"
  SSH_OPTS+=(-i "$KEY_FILE")
fi
REMOTE="$STAGING_USER@$STAGING_HOST"

# Server-side logic. Args: $1 = DEPLOY_DIR, $2 = BACKUP_KEEP_DAYS.
REMOTE_BACKUP_SCRIPT="$(cat <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail

# cron runs with a minimal PATH; make sure docker is found regardless of install location
export PATH="/usr/local/bin:/usr/local/sbin:/usr/bin:/usr/sbin:/bin:/sbin:$PATH"

DEPLOY_DIR="${1:-pullwise}"
KEEP="${2:-7}"
BACKUP_DIR="$HOME/$DEPLOY_DIR/backups"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker not available on server" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
cd "$HOME/$DEPLOY_DIR"

if [[ -z "$(docker compose ps -q postgres 2>/dev/null)" ]]; then
  echo "ERROR: postgres service is not running (or compose file missing)." >&2
  exit 1
fi

STAMP="$(date +%F-%H%M%S)"
if docker compose exec -T postgres pg_dump -U pullwise pullwise | gzip > "$BACKUP_DIR/pullwise-$STAMP.sql.gz"; then
  echo "backup created: $BACKUP_DIR/pullwise-$STAMP.sql.gz"
else
  echo "ERROR: pg_dump failed" >&2
  rm -f "$BACKUP_DIR/pullwise-$STAMP.sql.gz"
  exit 1
fi

find "$BACKUP_DIR" -name 'pullwise-*.sql.gz' -mtime "+${KEEP}" -delete
echo "retention: keeping backups newer than ${KEEP} day(s)"
SCRIPT
)"

case "$COMMAND" in
  install)
    echo ">>> Uploading backup script to server"
    ssh "${SSH_OPTS[@]}" "$REMOTE" \
      "mkdir -p ~/$DEPLOY_DIR/backups && cat > ~/$DEPLOY_DIR/backup.sh && chmod +x ~/$DEPLOY_DIR/backup.sh" <<SCRIPT
$REMOTE_BACKUP_SCRIPT
SCRIPT

    REMOTE_HOME="$(ssh "${SSH_OPTS[@]}" "$REMOTE" "echo ~")"
    CRON_LINE="0 3 * * * /bin/bash $REMOTE_HOME/$DEPLOY_DIR/backup.sh $DEPLOY_DIR $BACKUP_KEEP_DAYS >> $REMOTE_HOME/$DEPLOY_DIR/backups/backup.log 2>&1"
    ssh "${SSH_OPTS[@]}" "$REMOTE" \
      "( crontab -l 2>/dev/null | grep -v -F '$REMOTE_HOME/$DEPLOY_DIR/backup.sh' ; echo '$CRON_LINE' ) | crontab -"
    echo ">>> Installed daily Postgres backup (03:00 server time, retention ${BACKUP_KEEP_DAYS}d)."
    ;;

  run)
    ssh "${SSH_OPTS[@]}" "$REMOTE" "bash -s -- '$DEPLOY_DIR' '$BACKUP_KEEP_DAYS'" <<SCRIPT
$REMOTE_BACKUP_SCRIPT
SCRIPT
    ;;

  *)
    echo "Usage: $0 [install|run]" >&2
    exit 1
    ;;
esac
