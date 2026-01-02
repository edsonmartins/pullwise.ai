# Project Webhooks

Configure webhooks for specific projects.

## Overview

Project webhooks allow fine-grained control over when reviews are triggered for each project.

## Webhook Configuration

### Per-Project Settings

Navigate to **Project** → **Settings** → **Webhooks**

```yaml
# Webhook settings
enabled: true
trigger_on_pr_created: true
trigger_on_pr_updated: true
trigger_on_commit: false

# Branch filters
include_branches:
  - main
  - develop
  - release/*
exclude_branches:
  - feature/*
  - hotfix/*

# File filters
include_patterns:
  - "src/**/*.java"
  - "src/**/*.ts"
exclude_patterns:
  - "src/test/**"
  - "**/*.test.ts"
```

## Branch Filters

Control which branches trigger reviews:

### Include Branches

```yaml
include_branches:
  # Exact match
  - main
  - develop

  # Wildcard patterns
  - release/*
  - hotfix/*
```

### Exclude Branches

```yaml
exclude_branches:
  # Exclude feature branches
  - feature/*

  # Exclude documentation
  - docs/*

  # Exclude dependencies
  - dependabot/*
```

## File Filters

Control which files are analyzed:

### Include Patterns

```yaml
include_patterns:
  # All source files
  - "src/**/*.{java,ts,tsx}"

  # Specific directories
  - "app/**/*.js"
  - "lib/**/*.py"
```

### Exclude Patterns

```yaml
exclude_patterns:
  # Test files
  - "**/*.test.{ts,js}"
  - "**/*_test.go"

  # Generated files
  - "**/generated/**"
  - "**/dist/**"

  # Dependencies
  - "node_modules/**"
  - "vendor/**"
```

## Advanced Settings

### Diff Size Limits

```yaml
# Maximum diff size to analyze
max_diff_size: 1048576  # 1MB

# Maximum files to analyze
max_files: 100
```

### Throttling

```yaml
# Delay before starting review (seconds)
initial_delay: 30

# Minimum time between reviews (seconds)
min_interval: 60
```

### Auto-Cancel

```yaml
# Auto-cancel reviews on new commits
auto_cancel: true

# Only if previous review is older than (minutes)
auto_cancel_after: 60
```

## Multiple Webhooks

Configure multiple webhooks per project:

```yaml
webhooks:
  - name: production
    url: https://pullwise.example.com/webhooks/github
    branches:
      - main
      - release/*

  - name: development
    url: https://pullwise-staging.example.com/webhooks/github
    branches:
      - develop
      - feature/*
```

## Webhook Status

Monitor webhook delivery:

### Check Status

Navigate to **Project** → **Webhooks** → **Status**

Shows:
- Last delivery timestamp
- Delivery status (success/failure)
- Response time
- Error messages

### Recent Deliveries

```bash
# Get recent webhook deliveries via API
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/projects/{projectId}/webhooks/deliveries
```

## Testing

### Test Webhook

Send a test webhook from the UI:

1. Navigate to **Project** → **Settings** → **Webhooks**
2. Click **Test Webhook**
3. Select event type
4. Click **Send Test**

### Test Result

```json
{
  "success": true,
  "message": "Webhook delivered successfully",
  "statusCode": 202,
  "responseTime": "123ms"
}
```

## Troubleshooting

### Webhook Not Reaching Server

**Problem**: Webhook delivery fails

**Check**:

1. Server is accessible from GitHub
2. Firewall allows incoming traffic
3. SSL certificate is valid
4. Webhook URL is correct

```bash
# Test accessibility
curl -v https://your-server.com/webhooks/github
```

### Wrong Branch Triggered

**Problem**: Review triggered for excluded branch

**Check**:

1. Branch filter patterns are correct
2. Wildcards are properly escaped
3. Include/exclude order matters (exclude takes precedence)

### Too Many Reviews

**Problem**: Reviews triggered too frequently

**Solution**:

```yaml
# Enable throttling
min_interval: 300  # 5 minutes

# Exclude draft PRs
skip_draft_prs: true

# Only trigger on specific labels
required_labels:
  - ready-for-review
```

## Best Practices

### 1. Use Branch Filters

```yaml
# Only review main branch and release branches
include_branches:
  - main
  - release/*
```

### 2. Exclude Test Files

```yaml
# Don't review test files
exclude_patterns:
  - "**/*.test.*"
  - "**/test/**"
  - "**/tests/**"
```

### 3. Skip Large PRs

```yaml
# Skip PRs with more than 50 files
max_files: 50
skip_large_prs: true
```

### 4. Require Labels

```yaml
# Only review PRs with specific label
required_labels:
  - ready-for-review
```

## Next Steps

- [Triggering Reviews](/docs/user-guide/reviews/triggering-reviews) - Manual triggers
- [Repositories](/docs/user-guide/projects/repositories) - Connect repositories
- [API Reference](/docs/api/webhooks) - Webhook API
