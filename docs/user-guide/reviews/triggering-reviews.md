# Triggering Reviews

Learn how to trigger code reviews in Pullwise.

## Overview

Pullwise reviews can be triggered:

- **Automatically** via webhooks
- **Manually** via UI or API
- **On schedule** via cron jobs

## Automatic Triggers

### Webhook Triggers

Configure webhooks to automatically trigger reviews:

```yaml
# Triggers when:
- Pull request is opened
- Pull request is updated (new commits)
- Commits are pushed to protected branches
```

See [Webhooks](/docs/user-guide/projects/webhooks) for setup.

### Trigger Conditions

| Event | Default | Configurable |
|-------|---------|--------------|
| PR Opened | ✅ Yes | ✅ Yes |
| PR Updated | ✅ Yes | ✅ Yes |
| Draft PR | ❌ No | ✅ Yes |
| Push to main | ❌ No | ✅ Yes |

### Configure Auto-Trigger

```yaml
# Project settings
reviews:
  auto_trigger: true
  trigger_on_pr_created: true
  trigger_on_pr_updated: true
  trigger_on_draft: false
  trigger_on_push: false
```

## Manual Triggers

### Via UI

1. Navigate to **Projects** → Select project
2. Click **Trigger Review**
3. Fill in details:

   ```yaml
   Branch: feature/new-feature
   Commit SHA: abc123...
   Run SAST: true
   Run LLM: true
   ```

4. Click **Start Review**

### Via API

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews \
  -d '{
    "projectId": 123,
    "branch": "feature/new-feature",
    "commitSha": "abc123def456",
    "runSast": true,
    "runLlm": true
  }'
```

**Response:**

```json
{
  "id": 456,
  "status": "QUEUED",
  "projectId": 123,
  "branch": "feature/new-feature",
  "commitSha": "abc123def456",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

### Via Git CLI

Create a Git alias for easy triggering:

```bash
# Add to ~/.gitconfig
[alias]
    review = "!f() \
    curl -X POST \
      -H \"Authorization: Bearer $PULLWISE_TOKEN\" \
      -H \"Content-Type: application/json\" \
      https://api.pullwise.ai/api/reviews \
      -d \"{\
        \\\"projectId\\\": $PULLWISE_PROJECT_ID,\
        \\\"branch\\\": $(git rev-parse --abbrev-ref HEAD),\
        \\\"commitSha\\\": $(git rev-parse HEAD)\
      }\"; \
    f"
```

Use:

```bash
git review
```

## Scheduled Reviews

### Cron Jobs

Schedule regular reviews using cron:

```bash
# Review main branch daily
0 2 * * * curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews \
  -d '{
    "projectId": 123,
    "branch": "main",
    "commitSha": "HEAD"
  }'
```

### Systemd Timer

Create `/etc/systemd/system/pullwise-review.service`:

```ini
[Unit]
Description=Pullwise Scheduled Review

[Service]
Type=oneshot
ExecStart=/usr/bin/curl -X POST \
  -H "Authorization: Bearer $PULLWISE_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews \
  -d '{"projectId": 123, "branch": "main"}'
```

Create `/etc/systemd/system/pullwise-review.timer`:

```ini
[Unit]
Description=Daily Pullwise Review
Requires=pullwise-review.service

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

Enable:

```bash
sudo systemctl enable pullwise-review.timer
sudo systemctl start pullwise-review.timer
```

## Review Options

### SAST Analysis

```yaml
runSast: true  # Enable static analysis
```

Tools included:
- SonarQube
- ESLint
- Checkstyle
- PMD
- SpotBugs

### LLM Analysis

```yaml
runLlm: true  # Enable AI review
llmModel: "anthropic/claude-3.5-sonnet"  # Override default
```

### Code Graph

```yaml
# Requires Professional Edition or higher
runCodeGraph: true
codeGraphDepth: 2  # Analysis depth
```

### Custom Scope

```yaml
# Analyze specific paths
includePaths:
  - "src/**/*.java"
  - "lib/**/*.ts"

# Exclude paths
excludePaths:
  - "src/test/**"
  - "**/*.test.ts"
```

## Review Priority

Control when reviews are processed:

```yaml
priority: 1  # 1=highest, 5=lowest

# Priority rules:
# - Security reviews: priority 1
# - Main branch: priority 2
# - Feature branches: priority 3
# - Draft PRs: priority 5
```

## Canceling Reviews

### Via UI

1. Navigate to **Reviews** → Select review
2. Click **Cancel Review**
3. Confirm cancellation

### Via API

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{id}/cancel
```

### Auto-Cancel on Update

```yaml
# Automatically cancel previous review when PR is updated
auto_cancel_on_update: true
```

## Re-triggering

### After Code Changes

Re-trigger a review after making changes:

1. Push new commits to PR
2. Webhook automatically triggers new review
3. Previous review is auto-cancelled

### Manual Re-trigger

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{id}/retrigger
```

## Batch Reviews

Trigger multiple reviews at once:

```bash
for branch in feature/a feature/b feature/c; do
  curl -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    https://api.pullwise.ai/api/reviews \
    -d "{
      \"projectId\": 123,
      \"branch\": \"$branch\"
    }"
done
```

## Monitoring Triggers

### Trigger History

View recent triggers:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/projects/{projectId}/reviews?size=20
```

### Failed Triggers

Check for failed triggers:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews?status=FAILED
```

## Best Practices

### 1. Use Webhooks for Automation

```yaml
# Automatic triggers ensure every PR is reviewed
auto_trigger: true
```

### 2. Skip Draft PRs

```yaml
# Don't review draft PRs
skip_draft_prs: true
```

### 3. Require Labels

```yaml
# Only review when ready
required_labels:
  - ready-for-review
```

### 4. Limit Concurrent Reviews

```yaml
# Avoid overwhelming the system
max_concurrent_reviews: 5
```

## Troubleshooting

### Review Not Triggering

**Problem**: PR created but no review starts

**Check**:

1. Webhook is configured correctly
2. Project settings enable auto-trigger
3. Branch filters don't exclude the branch
4. Pullwise server is running

```bash
# Check webhook logs
docker-compose logs backend | grep webhook

# Check project settings
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/projects/{id}
```

### Review Stuck Queued

**Problem**: Review status stays "QUEUED"

**Solution**:

```bash
# Check worker status
curl http://localhost:8080/actuator/health

# Restart backend if needed
docker-compose restart backend
```

## Next Steps

- [Understanding Results](/docs/user-guide/reviews/understanding-results) - Interpret results
- [Severity Levels](/docs/user-guide/reviews/severity-levels) - Severity guide
- [Auto-Fix](/docs/category/autofix) - Apply fixes automatically
