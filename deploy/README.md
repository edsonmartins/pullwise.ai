# Staging deployment

Deploy pullwise to a staging VPS over SSH using prebuilt images from `ghcr.io`.

## How it works

1. Push to `develop` triggers the `Docker Build & Push` job, which builds and
   pushes `ghcr.io/<repo>/backend` and `ghcr.io/<repo>/frontend` with the
   `develop` and `develop-<sha>` tags.
2. The `Deploy to Staging` job SSHes into the staging server and runs
   `deploy/deploy-staging.sh`, which:
   - uploads `docker-compose.staging.yml`
   - creates a `.env` on the server on first run (stable DB/RabbitMQ passwords,
     JWT secret, API keys from GitHub secrets)
   - runs `docker compose pull && docker compose up -d`

The `.env` is only created once; later deploys keep it, so data and secrets
persist across releases.

## One-time server setup

On the VPS (Debian/Ubuntu):

```bash
# Docker + compose plugin
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
newgrp docker

# cron (required for the daily backup installed by deploy-staging.sh)
sudo apt-get update && sudo apt-get install -y cron

# Test
docker run --rm hello-world
```

The deploy runs with the SSH user, so that user needs `docker` group access
and passwordless login via the SSH key you register in GitHub.

## Required GitHub secrets

Configure in **Settings → Secrets and variables → Actions** of the repo:

| Secret | Required | Notes |
| ------ | -------- | ----- |
| `STAGING_HOST` | yes | IP or hostname of the staging server |
| `STAGING_USER` | yes | SSH user with docker access |
| `STAGING_SSH_KEY` | yes | PEM private key (no passphrase) authorized on the server |
| `STAGING_URL` | no | Public URL for the smoke test (defaults to `http://<STAGING_HOST>`) |
| `JWT_SECRET` | no | Auto-generated on first deploy if empty |
| `OPENROUTER_API_KEY` | no | Enables LLM reviews |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | no | OAuth GitHub login on staging |
| `GITHUB_WEBHOOK_SECRET` | no | Webhook signature validation |

> Note: `docker-build` also requires the `packages` permission (already set in
> the workflow) and a `GHCR_REPOSITORY` default derived from the repo name.

## Exposed ports / TLS

The compose exposes the frontend on port `80` and the API on port `8080`. TLS
is intentionally left to your reverse proxy (host nginx + certbot, Caddy, or
Cloudflare Tunnel). Point `staging.pullwise.ai` at the server and terminate
TLS there; the frontend nginx proxies `/api` and `/ws` to the backend over the
internal Docker network.

## Smoke test

After a deploy (or manually), verify the environment is healthy:

```bash
STAGING_URL=https://staging.pullwise.ai ./deploy/smoke-test-staging.sh
```

Checks (frontend `GET /` → 200, backend health via `/actuator/health`
→ `"status":"UP"`, and the `/ws` WebSocket endpoint). Pass `STAGING_TOKEN=<jwt>`
to also validate a full WebSocket handshake (expects `101`); without it, the
script only asserts the endpoint answers and auth is enforced (`4xx`).

## Backups

`deploy-staging.sh` automatically installs a daily Postgres backup on the server
(cron at 03:00 server time, via `deploy/backup-staging.sh install`). Backups are
`pg_dump | gzip` snapshots kept in `~/$DEPLOY_DIR/backups/` and pruned after
`BACKUP_KEEP_DAYS` (default 7).

On-demand backup:

```bash
./deploy/backup-staging.sh run
```

Restore from a backup (on the server):

```bash
cd ~/pullwise
gzip -dc backups/pullwise-<timestamp>.sql.gz | docker compose exec -T postgres \
  psql -U pullwise -d pullwise
```
