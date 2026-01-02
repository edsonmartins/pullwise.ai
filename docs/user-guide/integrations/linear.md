# Linear Integration

Link Pullwise reviews to Linear issues.

## Overview

The Linear integration allows you to:
- Create Linear issues for review findings
- Update Linear issue status based on review results
- Link commits to Linear issues
- Track technical debt in Linear

## Setup

### 1. Configure Integration

Navigate to **Settings** → **Integrations** → **Linear**

```yaml
# Linear configuration
linear:
  apiKey: lin_api_xxxxxxxxxxxxx
  workspaceKey: YOUR-WORKSPACE
  defaultTeamId: team-uuid
  defaultState: Backlog
  defaultPriority: Normal
```

### 2. Generate API Key

1. Go to [Linear.app](https://linear.app)
2. Navigate to **Settings** → **API**
3. Click **Create API Key**
4. Select **Read & Write** access
5. Copy the key

### 3. Get Workspace and Team IDs

```bash
# List workspaces
pullwise linear list-workspaces

# List teams in workspace
pullwise linear list-teams --workspace=YOUR-WORKSPACE

# Get specific team info
pullwise linear get-team --team-id=team-uuid
```

## Issue Creation

### Automatic Issue Creation

```yaml
# application.yml
pullwise:
  integrations:
    linear:
      autoCreateIssues: true
      minSeverity: HIGH
      teamId: team-uuid
      stateId: state-uuid
      assigneeId: user-uuid
```

### Manual Issue Creation

Via Web UI:

1. Navigate to **Review** → **Issues**
2. Select issue(s)
3. Click **Create Linear Issue**
4. Configure:
   - **Team**: Select team
   - **State**: Backlog / Todo / In Progress
   - **Priority**: Urgent / High / Normal / Low
   - **Assignee**: Select user
5. Click **Create**

Via CLI:

```bash
# Create Linear issue
pullwise linear create-issue \
  --review-id=123 \
  --issue-id=456 \
  --team-id=team-uuid \
  --title="Fix SQL injection vulnerability" \
  --priority=High \
  --label="security"
```

Via API:

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/integrations/linear/issues \
  -d '{
    "reviewId": 123,
    "issueId": 456,
    "teamId": "team-uuid",
    "stateId": "state-uuid",
    "priority": "High",
    "labels": ["security", "sql-injection"]
  }'
```

## Issue Mapping

### Severity to Priority

```yaml
linear:
  priorityMapping:
    CRITICAL: Urgent
    HIGH: High
    MEDIUM: Normal
    LOW: Low
```

### Issue Type Mapping

```yaml
linear:
  labelMapping:
    VULNERABILITY: ["security"]
    BUG: ["bug"]
    CODE_SMELL: ["tech-debt"]
    PERFORMANCE: ["performance"]
    STYLE: ["style"]
    DOCUMENTATION: ["docs"]
```

## Status Updates

### Update Linear on Review Events

```yaml
# Update Linear when review completes
pullwise:
  integrations:
    linear:
      updateOnReviewComplete: true
      updateOnIssueFixed: true
      commentOnEvents: true
```

### State Transitions

```yaml
linear:
  stateTransitions:
    onCreated: "Backlog"
    onInProgress: "Todo"
    onFixed: "Done"
    onVerified: "Done"
```

## Pull Request Integration

### Link PR to Linear

```bash
# PR with Linear issue ID in branch
git checkout -c fix/PWL-123-sql-injection

# Pullwise extracts issue ID and updates Linear
pullwise review
```

### Auto-Transition on PR Merge

```yaml
# Auto-transition Linear issues when PR merges
linear:
  pullRequest:
    autoTransitionOnMerge: true
    targetState: "Done"
    addComment: true
```

## Issue Templates

### Description Template

```yaml
linear:
  templates:
    description: |
      ## Pullwise Review Alert

      **Severity:** {severity}
      **File:** `{filePath}:{lineNumber}`

      ### Issue
      {message}

      ### Code
      ```{language}
      {codeSnippet}
      ```

      ### Suggestion
      {suggestion}

      ### Links
      [View in Pullwise]({reviewUrl})
```

### Comment Template

```yaml
linear:
  templates:
    comment: |
      ### Pullwise Update

      **Event:** {eventType}
      **Author:** {author}
      **Time:** {timestamp}

      **Review:** {reviewUrl}
      **Status:** {status}

      **Issues:**
      - Found: {issueCount}
      - Fixed: {fixedCount}
```

## Labels and Workflow

### Auto-Assign Labels

```yaml
linear:
  labels:
    autoAssign:
      - "pullwise"
      - "{rule}"
      - "{severity}"
    bySeverity:
      CRITICAL: ["critical", "security"]
      HIGH: ["high-priority"]
      MEDIUM: ["medium-priority"]
      LOW: ["low-priority"]
```

### Custom Workflow States

```yaml
# Map to your Linear workflow
linear:
  workflow:
    states:
      Backlog: "state-uuid-1"
      Todo: "state-uuid-2"
      In Progress: "state-uuid-3"
      In Review: "state-uuid-4"
      Done: "state-uuid-5"
      Canceled: "state-uuid-6"
```

## Advanced Configuration

### Parent Issue Linking

```yaml
# Link child issues to parent
linear:
  parentIssue:
    enabled: true
    parentIdentifier: "PWL-123"  # Parent issue ID
    linkType: "subtask"
```

### Project Assignment

```yaml
# Assign issues to projects
linear:
  projects:
    enabled: true
    projectMapping:
      security/*: "SEC-123"
      performance/*: "PERF-456"
      "*/auth/*": "AUTH-789"
```

### Cycle Assignment

```yaml
# Assign to active cycle
linear:
  cycle:
    autoAssignToActive: true
    fallbackToBacklog: true
```

## Bulk Operations

### Bulk Create Issues

```bash
# Create Linear issues for all HIGH+ severity issues
pullwise linear bulk-create \
  --review-id=123 \
  --min-severity=HIGH \
  --team-id=team-uuid \
  --dry-run
```

### Bulk Update Status

```bash
# Update all related Linear issues
pullwise linear bulk-update \
  --review-id=123 \
  --state=Done \
  --comment="All issues fixed in review"
```

## Troubleshooting

### Authentication Issues

```bash
# Test API key
pullwise test-integration linear

# Verify workspace access
pullwise linear get-workspace
```

### Team Not Found

```bash
# List available teams
pullwise linear list-teams

# Get team UUID
pullwise linear get-team --name="Engineering"
```

### State Transition Failed

```bash
# Check available states for team
pullwise linear list-states --team-id=team-uuid

# Verify state configuration
```

## Best Practices

### 1. Use Issue Identifiers in Branches

```bash
# Good: Includes Linear issue ID
git checkout -c fix/PWL-123-auth-bug

# Bad: No issue reference
git checkout -c fix/auth-bug
```

### 2. Group Related Issues

```yaml
# Create parent issue for review
linear:
  parentIssue:
    createForReview: true
    title: "Fix issues from review {reviewId}"
    state: "In Progress"
```

### 3. Use Labels Effectively

```yaml
# Organize by type and severity
linear:
  labels:
    byType:
      VULNERABILITY: "security"
      BUG: "bug"
      CODE_SMELL: "tech-debt"
    byRule:
      SQL_INJECTION: "sql"
      NULL_POINTER: "npe"
```

### 4. Set Appropriate Priorities

```yaml
# Priority based on severity
linear:
  priorityMapping:
    CRITICAL: Urgent
    HIGH: High
    MEDIUM: Normal
    LOW: Low
```

## API Reference

### Create Issue

```bash
curl -X POST \
  -H "Authorization: Bearer $LINEAR_API_KEY" \
  -H "Content-Type: application/json" \
  https://api.linear.app/graphql \
  -d '{
    "query": "mutation ($title: String!, $teamId: String!) { issueCreate(input: {title: $title, teamId: $teamId}) { issue { id } } }",
    "variables": {
      "title": "Fix SQL injection",
      "teamId": "team-uuid"
    }
  }'
```

### Update Issue

```bash
curl -X POST \
  -H "Authorization: Bearer $LINEAR_API_KEY" \
  -H "Content-Type: application/json" \
  https://api.linear.app/graphql \
  -d '{
    "query": "mutation ($id: String!, $stateId: String!) { issueUpdate(input: {id: $id, stateId: $stateId}) { issue { id } } }",
    "variables": {
      "id": "PWL-123",
      "stateId": "state-uuid"
    }
  }'
```

## Next Steps

- [Slack Integration](/docs/user-guide/integrations/slack) - Slack notifications
- [Jira Integration](/docs/user-guide/integrations/jira) - Jira integration
- [Analytics](/docs/user-guide/analytics/) - Analytics overview
