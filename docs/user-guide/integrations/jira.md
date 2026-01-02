# Jira Integration

Link Pullwise reviews to Jira issues.

## Overview

The Jira integration allows you to:
- Create Jira tickets for review issues
- Update Jira issue status based on review results
- Link PRs to Jira tickets
- Track code quality in Jira

## Setup

### 1. Configure Integration

Navigate to **Settings** → **Integrations** → **Jira**

```yaml
# Jira configuration
jira:
  url: https://your-domain.atlassian.net
  username: pullwise-bot@your-domain.com
  apiToken: your-api-token-here
  projectKey: PROJ
  defaultIssueType: Bug
```

### 2. Generate API Token

1. Go to [id.atlassian.com](https://id.atlassian.com)
2. Navigate to **Security** → **API Tokens**
3. Click **Create API Token**
4. Label it "Pullwise Integration"
5. Copy the token

### 3. Test Connection

```bash
# Test Jira connection
pullwise test-integration jira

# Verify project access
pullwise test-integration jira --project=PROJ
```

## Issue Creation

### Automatic Issue Creation

Configure automatic Jira ticket creation for critical issues:

```yaml
# application.yml
pullwise:
  integrations:
    jira:
      autoCreateIssues: true
      minSeverity: CRITICAL
      issueType: Bug
      assignee: developer@company.com
```

### Manual Issue Creation

Via Web UI:

1. Navigate to **Review** → **Issues**
2. Select issue(s) to create tickets for
3. Click **Create Jira Issue**
4. Fill in details:
   - **Issue Type**: Bug / Task
   - **Priority**: Based on severity
   - **Summary**: Auto-filled
   - **Description**: Includes code snippet
5. Click **Create**

Via CLI:

```bash
# Create Jira issue for specific review issue
pullwise jira create-issue \
  --review-id=123 \
  --issue-id=456 \
  --issue-type=Bug \
  --priority=High \
  --assignee=john.doe
```

Via API:

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/integrations/jira/issues \
  -d '{
    "reviewId": 123,
    "issueId": 456,
    "issueType": "Bug",
    "priority": "High",
    "assignee": "john.doe",
    "labels": ["security", "sql-injection"]
  }'
```

## Issue Mapping

### Severity to Priority

```yaml
jira:
  priorityMapping:
    CRITICAL: Highest
    HIGH: High
    MEDIUM: Medium
    LOW: Low
```

### Issue Type Mapping

```yaml
jira:
  issueTypeMapping:
    VULNERABILITY: Bug
    BUG: Bug
    CODE_SMELL: Task
    STYLE: Task
    PERFORMANCE: Improvement
    DOCUMENTATION: Task
```

## Status Updates

### Update Jira on Review Complete

```yaml
# Update Jira issue status when review passes
pullwise:
  integrations:
    jira:
      updateOnReviewComplete: true
      transitionWhenPassed: "Ready for QA"
      transitionWhenFailed: "In Development"
```

### Custom Transitions

```yaml
jira:
  transitions:
    onCreated: "To Do"
    onInProgress: "In Progress"
    onFixed: "Ready for Review"
    onVerified: "Done"
```

## Pull Request Integration

### Link PR to Jira

```bash
# PR with Jira key in branch
git checkout -c feature/PROJ-123-new-feature

# Pullwise automatically links to PROJ-123
pullwise review
```

### Update Jira from PR

```yaml
# Update Jira when PR is created/merged
jira:
  pullRequest:
    updateOnCreated: true
    updateOnMerged: true
    commentFormat: |
      Pull request: {prUrl}
      Status: {status}
      Review: {reviewUrl}
```

## Webhook Configuration

### Jira → Pullwise

Configure Jira webhook to notify Pullwise:

```bash
# Create webhook in Jira
curl -X POST \
  -u "email:api-token" \
  -H "Content-Type: application/json" \
  https://your-domain.atlassian.net/rest/api/3/webhook \
  -d '{
    "name": "Pullwise Integration",
    "url": "https://pullwise.your-domain.com/webhooks/jira",
    "events": ["jira:issue_created", "jira:issue_updated"]
  }'
```

### Pullwise → Jira

Pullwise sends updates to Jira on:

| Event | Jira Action |
|-------|-------------|
| Review created | Add comment |
| Issue found | Create issue (if configured) |
| Issue fixed | Transition status |
| Review passed | Update status |

## Custom Templates

### Issue Description Template

```yaml
jira:
  templates:
    description: |
      {summary}

      *Pullwise Review:* {reviewUrl}
      *Severity:* {severity}
      *File:* {filePath}:{lineNumber}

      h4. Code
      {codeSnippet}

      h4. Suggested Fix
      {suggestion}

      h4. Documentation
      {documentationUrls}
```

### Comment Template

```yaml
jira:
  templates:
    comment: |
      {action} by {author}

      *Review:* {reviewUrl}
      *Status:* {status}
      *Issues Found:* {issueCount}
      *Issues Fixed:* {fixedCount}
```

## Advanced Configuration

### Custom Fields

```yaml
jira:
  customFields:
    - name: "Pullwise Review ID"
      fieldId: "customfield_10000"
      value: "{reviewId}"
    - name: "Severity"
      fieldId: "customfield_10001"
      value: "{severity}"
```

### Component Mapping

```yaml
jira:
  componentMapping:
    src/main/java/auth/*: Authentication
    src/main/java/api/*: API
    src/main/java/db/*: Database
    src/main/java/ui/*: Frontend
```

### Sprint Integration

```yaml
jira:
  sprint:
    autoAssignToActive: true
    createInCurrentSprint: true
```

## Troubleshooting

### Authentication Failed

```bash
# Check credentials
pullwise test-integration jira

# Verify API token
curl -u "email:api-token" \
  https://your-domain.atlassian.net/rest/api/3/myself
```

### Issue Type Not Found

```bash
# List available issue types
pullwise jira list-issue-types --project=PROJ

# Update configuration with correct type
```

### Transitions Not Working

```bash
# Check available transitions
pullwise jira list-transitions --issue=PROJ-123

# Update transition names in config
```

## Best Practices

### 1. Use Issue Keys in Branches

```bash
# Good: Includes Jira key
git checkout -b feature/PROJ-123-auth

# Bad: No Jira key
git checkout -b feature/add-auth
```

### 2. Link Related Issues

```yaml
# Auto-link related Jira issues
jira:
  linkRelatedIssues: true
  linkType: "relates to"
```

### 3. Limit Auto-Creation

```yaml
# Only create for critical issues
jira:
  autoCreateIssues: true
  minSeverity: CRITICAL
  maxPerReview: 5
```

### 4. Use Labels for Filtering

```yaml
# Add labels for easier filtering
jira:
  labels:
    - "pullwise"
    - "auto-generated"
    - "{rule}"
```

## Next Steps

- [Linear Integration](/docs/user-guide/integrations/linear) - Linear integration
- [Slack Integration](/docs/user-guide/integrations/slack) - Slack notifications
- [Analytics](/docs/user-guide/analytics/) - Analytics overview
