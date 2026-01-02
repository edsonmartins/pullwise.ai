# Webhooks Setup

Configure webhooks for automatic code reviews.

## Overview

Webhooks allow Pullwise to automatically start reviews when:

- A pull request is created
- A pull request is updated
- Commits are pushed

## GitHub Webhooks

### Create Webhook

1. Navigate to your repository **Settings** → **Webhooks** → **Add webhook**
2. Configure:

   ```yaml
   Payload URL: https://your-server.com/webhooks/github
   Content type: application/json
   Secret: (optional) generate random secret
   SSL verification: Enabled
   ```

3. Select events:
   - ✅ **Pull requests**
   - ✅ **Pull request reviews**
   - ✅ **Pushes** (optional)

4. Click **Add webhook**

### Webhook URL

| Deployment | Webhook URL |
|------------|-------------|
| **Local** | Use [ngrok](https://ngrok.com) for testing |
| **Docker** | `http://YOUR_SERVER_IP:8080/webhooks/github` |
| **Kubernetes** | `https://pullwise.example.com/webhooks/github` |
| **Cloud** | Use your load balancer URL |

### Verify Webhook

1. Click the webhook → **Recent Deliveries**
2. Find a recent delivery
3. Check response status: **200 OK**

## GitLab Webhooks

### Create Webhook

1. Navigate to **Settings** → **Webhooks**
2. Configure:

   ```yaml
   URL: https://your-server.com/webhooks/gitlab
   Secret token: (optional) your secret
   Trigger:
     ✅ Merge request events
     ✅ Merge request comments
   ```

3. Click **Add webhook**

### Push Events

For review on push (without PR):

```yaml
Trigger:
  ✅ Push events
```

## BitBucket Webhooks

### Create Webhook

1. Navigate to **Repository settings** → **Webhooks** → **Add webhook**
2. Configure:

   ```yaml
   Title: Pullwise
   URL: https://your-server.com/webhooks/bitbucket
   Triggers:
     ✅ Pull request created
     ✅ Pull request updated
   ```

3. Click **Save**

## Webhook Security

### Secret Verification

Configure a secret to verify webhook authenticity:

```bash
# Generate random secret
openssl rand -hex 32
```

Add to both:
1. Webhook configuration (GitHub/GitLab/BitBucket)
2. Pullwise configuration:

```yaml
# application.yml
pullwise:
  github:
    webhook-secret: your-generated-secret
```

### Signature Verification

Pullwise verifies signatures using HMAC-SHA256:

```
signature = hmac_sha256(secret, payload)
```

## Testing Webhooks

### Using ngrok (Local Testing)

```bash
# Install ngrok
brew install ngrok  # macOS
# or download from https://ngrok.com

# Start ngrok
ngrok http 8080

# Use the ngrok URL as webhook URL
# Example: https://abc123.ngrok.io/webhooks/github
```

### Test Payload

Send a test payload:

```bash
curl -X POST https://your-server.com/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -d @test-payload.json
```

### test-payload.json

```json
{
  "action": "opened",
  "number": 1,
  "pull_request": {
    "id": 123,
    "number": 1,
    "state": "open",
    "title": "Add new feature",
    "body": "This PR adds a new feature",
    "user": {
      "login": "username"
    },
    "head": {
      "sha": "abc123",
      "ref": "feature-branch"
    },
    "base": {
      "ref": "main"
    },
    "html_url": "https://github.com/org/repo/pull/1"
  },
  "repository": {
    "id": 456,
    "name": "repo",
    "full_name": "org/repo",
    "clone_url": "https://github.com/org/repo.git"
  }
}
```

## Webhook Events

### GitHub Events

| Event | Triggered By |
|-------|--------------|
| `pull_request` | PR opened, synchronized, reopened |
| `pull_request_review` | Review submitted |
| `push` | Commit pushed |

### GitLab Events

| Event | Triggered By |
|-------|--------------|
| `Merge Request Hook` | MR opened, updated |
| `Push Hook` | Commit pushed |

### BitBucket Events

| Event | Triggered By |
|-------|--------------|
| `pullrequest:created` | PR created |
| `pullrequest:updated` | PR updated |

## Troubleshooting

### Webhook Not Triggering

**Problem**: PR doesn't start review

**Solutions**:

1. Check webhook URL is correct
2. Verify server is accessible from GitHub
3. Check webhook delivery logs
4. Verify Pullwise backend is running

```bash
# Check backend logs
docker-compose logs -f backend | grep webhook
```

### Signature Verification Failed

**Problem**: Webhook rejected with signature error

**Solutions**:

1. Verify secret matches on both sides
2. Check raw payload is being verified (not parsed)
3. Ensure encoding is correct (UTF-8)

### Time Synchronization

Webhook signatures may fail if system time is incorrect:

```bash
# Check system time
timedatectl

# Sync time
sudo ntpdate pool.ntp.org
```

## Next Steps

- [First Review](/docs/getting-started/first-review) - Run your first review
- [Configuration](/docs/getting-started/configuration) - Configure settings
- [Projects](/docs/user-guide/projects/webhooks) - Project-specific webhooks
