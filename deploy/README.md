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

## Local smoke test

Render the compose without a server:

```bash
cp deploy/docker-compose.staging.yml /tmp/dc.yml
# from repo root
docker compose -f deploy/docker-compose.staging.yml --env-file deploy/.env.staging.local config
```
