# Slack Integration

Receive notifications in Slack about code reviews.

## Overview

The Slack integration sends notifications for:
- New reviews started
- Issues found
- Review completion
- Auto-fix applied
- Review failures

## Setup

### 1. Create Slack App

1. Go to [api.slack.com/apps](https://api.slack.com/apps)
2. Click **Create New App**
3. Choose **From scratch**
4. Enter app name and workspace

### 2. Configure Permissions

Add OAuth scopes:

| Scope | Purpose |
|-------|---------|
| `chat:write` | Send messages |
| `chat:write.public` | Post to public channels |
| `chat:write.customize` | Customize messages |
| `files:write` | Upload files |

### 3. Install App

1. Click **Install to Workspace**
2. Copy **Bot User OAuth Token** (starts with `xoxb-`)
3. Add to Pullwise configuration

```yaml
# application.yml
pullwise:
  integrations:
    slack:
      botToken: xoxb-your-token-here
      signingSecret: your-signing-secret
      defaultChannel: "#code-reviews"
```

### 4. Enable Events

Subscribe to events:

```
app_mention
message.channels
```

Set **Request URL** to:
```
https://pullwise.your-domain.com/webhooks/slack/events
```

## Notification Types

### Review Started

```yaml
notifications:
  reviewStarted:
    enabled: true
    channels:
      - "#code-reviews"
    template: |
      🚀 *New Review Started*

      *Project:* {projectName}
      *Branch:* {branch}
      *Commit:* {commitSha}
      *Author:* {author}

      <{reviewUrl}|View Review>
```

### Issues Found

```yaml
notifications:
  issuesFound:
    enabled: true
    channels:
      - "#code-reviews"
    threshold: HIGH  # Only notify for HIGH+ severity
    template: |
      🚨 *Issues Found*

      *Review:* {projectName} - {branch}
      *Critical:* {criticalCount}
      *High:* {highCount}
      *Medium:* {mediumCount}
      *Low:* {lowCount}

      <{reviewUrl}|View Details>
```

### Review Completed

```yaml
notifications:
  reviewCompleted:
    enabled: true
    channels:
      - "#code-reviews"
    template: |
      ✅ *Review Completed*

      *Project:* {projectName}
      *Status:* {status}
      *Issues:* {issueCount}
      *Duration:* {duration}

      <{reviewUrl}|View Results>
```

### Auto-Fix Applied

```yaml
notifications:
  autofixApplied:
    enabled: true
    channels:
      - "#code-reviews"
    template: |
      🔧 *Auto-Fix Applied*

      *Issue:* {rule}
      *File:* {filePath}:{lineNumber}
      *Confidence:* {confidence}

      <{diffUrl}|View Diff>
```

## Channel Configuration

### Per-Project Channels

```yaml
slack:
  channelMapping:
    project-backend: "#backend-reviews"
    project-frontend: "#frontend-reviews"
    project-mobile: "#mobile-reviews"
    default: "#code-reviews"
```

### Per-Severity Channels

```yaml
slack:
  severityChannels:
    CRITICAL: "#security-alerts"
    HIGH: "#bugs"
    MEDIUM: "#code-quality"
    LOW: "#minor-issues"
```

## Message Formatting

### Block Kit Messages

```yaml
slack:
  useBlockKit: true
  messageFormat: |
    {
      "blocks": [
        {
          "type": "header",
          "text": {
            "type": "plain_text",
            "text": "🚀 Review Started"
          }
        },
        {
          "type": "section",
          "fields": [
            {
              "type": "mrkdwn",
              "text": "*Project:*\n{projectName}"
            },
            {
              "type": "mrkdwn",
              "text": "*Branch:*\n{branch}"
            }
          ]
        },
        {
          "type": "actions",
          "elements": [
            {
              "type": "button",
              "text": {
                "type": "plain_text",
                "text": "View Review"
              },
              "url": "{reviewUrl}"
            }
          ]
        }
      ]
    }
```

### Attachment Format

```yaml
slack:
  useAttachments: true
  attachmentFormat: |
    {
      "color": "{color}",
      "title": "{title}",
      "text": "{text}",
      "fields": [
        {"title": "Project", "value": "{projectName}"},
        {"title": "Issues", "value": "{issueCount}"}
      ],
      "actions": [
        {
          "type": "button",
          "text": "View Review",
          "url": "{reviewUrl}"
        }
      ]
    }
```

## Color Coding

```yaml
slack:
  colors:
    CRITICAL: "#FF0000"
    HIGH: "#FF6600"
    MEDIUM: "#FFCC00"
    LOW: "#00CCFF"
    SUCCESS: "#00CC00"
    INFO: "#0066CC"
```

## Interactive Features

### Slash Commands

```bash
# /pullwise-status
# Show recent reviews

# /pullwise-review [branch]
# Trigger new review

# /pullwise-fix [issue-id]
# Apply auto-fix

# /pullwise-config
# Show configuration
```

### Quick Actions

Add action buttons to messages:

```yaml
slack:
  quickActions:
    reviewStarted:
      - type: button
        text: "View Review"
        url: "{reviewUrl}"
      - type: button
        text: "Cancel"
        action_id: "cancel_review"
    issuesFound:
      - type: button
        text: "View Details"
        url: "{reviewUrl}"
      - type: button
        text: "Apply Fixes"
        action_id: "apply_fixes"
```

## Webhook Configuration

### Incoming Webhook

```bash
# Create incoming webhook in Slack
# https://api.slack.com/messaging/webhooks

curl -X POST \
  -H "Content-Type: application/json" \
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL \
  -d '{
    "text": "Review completed!",
    "blocks": [...]
  }'
```

### Configure in Pullwise

```yaml
slack:
  webhookUrl: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

## Advanced Configuration

### Threaded Replies

```yaml
slack:
  threading:
    enabled: true
    threadNotifications: true
    broadcastToChannel: false
```

### User Mentions

```yaml
slack:
  mentions:
    onIssueFound:
      - "@{author}"
    onReviewComplete:
      - "@team-lead"
    onCritical:
      - "@security-team"
```

### Conditional Notifications

```yaml
slack:
  conditions:
    notifyOnFailure: true
    notifyOnNewIssues: true
    notifyOnFixedIssues: false
    minSeverity: MEDIUM
    maxNotificationsPerHour: 10
```

## Troubleshooting

### Messages Not Appearing

```bash
# Test webhook
curl -X POST \
  -H "Content-Type: application/json" \
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL \
  -d '{"text": "Test message"}'

# Check Pullwise logs
pullwise logs --component=slack
```

### Invalid Token

```bash
# Verify token
curl -X GET \
  -H "Authorization: Bearer xoxb-your-token" \
  https://slack.com/api/auth.test
```

### Rate Limiting

```yaml
# Configure rate limiting
slack:
  rateLimit:
    enabled: true
    maxPerMinute: 20
    queueMessages: true
```

## Best Practices

### 1. Use Dedicated Channels

```yaml
# Good: Separate channels
slack:
  channels:
    security: "#security-alerts"
    reviews: "#code-reviews"
    bugs: "#bug-reports"

# Bad: Everything in one channel
defaultChannel: "#general"
```

### 2. Include Context

```yaml
# Good: Detailed context
template: |
  🚨 *Critical Issue Found*

  *File:* `src/auth/Login.java:42`
  *Rule:* SQL_INJECTION
  *Severity:* CRITICAL

  ```java
  String query = "SELECT * FROM users WHERE id = '" + id + "'";
  ```

  *Suggested Fix:* Use parameterized queries

  ```text
  <{reviewUrl}|View Review>
  ```

### Bad: Minimal information
template: "Critical issue found in {projectName}"
```

### 3. Link Everything

```yaml
# Always include review URL
template: |
  Review: {reviewUrl}
  Diff: {diffUrl}
  Issue: {issueUrl}
```

### 4. Use Appropriate Emojis

```yaml
# Consistent emoji usage
emojis:
  reviewStarted: "🚀"
  issueFound: "🚨"
  reviewCompleted: "✅"
  fixApplied: "🔧"
  error: "❌"
  warning: "⚠️"
```

## Next Steps

- [Jira Integration](/docs/user-guide/integrations/jira) - Jira integration
- [Linear Integration](/docs/user-guide/integrations/linear) - Linear integration
- [Analytics](/docs/user-guide/analytics/) - Analytics overview
