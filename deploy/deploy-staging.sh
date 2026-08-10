#!/usr/bin/env bash
#
# Deploy Pullwise to a staging server over SSH using Docker Compose.
#
# Requirements on the server: docker + docker compose plugin.
# Requirements on the runner: openssh-client, openssl, scp.
#
# Env vars (GitHub Actions secrets / local):
#   STAGING_HOST        SSH host of the staging server (required)
#   STAGING_USER        SSH user (required)
#   STAGING_SSH_KEY     PEM private key (optional if ssh-agent is preloaded)
#   DEPLOY_DIR          Remote directory, default "pullwise"
#   IMAGE_TAG           Image tag to deploy, default "develop"
#   GHCR_REPOSITORY     ghcr repo, default "$GITHUB_REPOSITORY"
#   JWT_SECRET          Generated on first bootstrap if empty
#   OPENROUTER_API_KEY  Optional (LLM reviews disabled without it)
#   GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET   Optional OAuth
#   GITHUB_WEBHOOK_SECRET                      Optional webhook validation

set -euo pipefail

STAGING_HOST="${STAGING_HOST:-}"
STAGING_USER="${STAGING_USER:-}"
STAGING_SSH_KEY="${STAGING_SSH_KEY:-}"
DEPLOY_DIR="${DEPLOY_DIR:-pullwise}"
IMAGE_TAG="${IMAGE_TAG:-develop}"
GHCR_REPOSITORY="${GHCR_REPOSITORY:-${GITHUB_REPOSITORY:-edsonmartins/pullwise.ai}}"

JWT_SECRET="${JWT_SECRET:-}"
OPENROUTER_API_KEY="${OPENROUTER_API_KEY:-}"
GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-}"
GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-}"
GITHUB_WEBHOOK_SECRET="${GITHUB_WEBHOOK_SECRET:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "$STAGING_HOST" || -z "$STAGING_USER" ]]; then
  echo "::error::STAGING_HOST and STAGING_USER must be set (GitHub secrets)." >&2
  exit 1
fi

# ---- SSH setup --------------------------------------------------------------
SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null)
if [[ -n "$STAGING_SSH_KEY" ]]; then
  KEY_FILE="$(mktemp)"
  printf '%s\n' "$STAGING_SSH_KEY" > "$KEY_FILE"
  chmod 600 "$KEY_FILE"
  SSH_OPTS+=(-i "$KEY_FILE")
fi
REMOTE="$STAGING_USER@$STAGING_HOST"

echo ">>> Ensuring remote directory: ~/$DEPLOY_DIR"
ssh "${SSH_OPTS[@]}" "$REMOTE" "mkdir -p ~/$DEPLOY_DIR"

echo ">>> Uploading compose file"
scp "${SSH_OPTS[@]}" \
  "$SCRIPT_DIR/docker-compose.staging.yml" \
  "$REMOTE:~/$DEPLOY_DIR/docker-compose.yml"

# ---- Create .env on first bootstrap (kept stable across deploys) ------------
if ssh "${SSH_OPTS[@]}" "$REMOTE" "test -f ~/$DEPLOY_DIR/.env"; then
  echo ">>> .env already exists on server, keeping it"
else
  echo ">>> Creating .env on server (first bootstrap)"
  ssh "${SSH_OPTS[@]}" "$REMOTE" "cat > ~/$DEPLOY_DIR/.env" <<ENVEOF
POSTGRES_PASSWORD=$(openssl rand -hex 16)
RABBITMQ_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=${JWT_SECRET:-$(openssl rand -hex 32)}
OPENROUTER_API_KEY=${OPENROUTER_API_KEY}
GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
GITHUB_WEBHOOK_SECRET=${GITHUB_WEBHOOK_SECRET}
IMAGE_TAG=${IMAGE_TAG}
GHCR_REPOSITORY=${GHCR_REPOSITORY}
ENVEOF
fi

# ---- Deploy ----------------------------------------------------------------
echo ">>> Pulling images and starting services (tag: $IMAGE_TAG)"
ssh "${SSH_OPTS[@]}" "$REMOTE" \
  "cd ~/$DEPLOY_DIR && docker compose pull && docker compose up -d"

echo ">>> Service status"
ssh "${SSH_OPTS[@]}" "$REMOTE" "cd ~/$DEPLOY_DIR && docker compose ps"

echo ">>> Installing daily Postgres backup (cron)"
"$SCRIPT_DIR/backup-staging.sh" install

echo ">>> Pullwise staging deploy completed successfully."
