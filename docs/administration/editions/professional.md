# Professional Edition

Features and capabilities of Pullwise Professional.

## Overview

Professional edition is designed for growing teams and organizations needing advanced features, integrations, and support.

## Pricing

```
$49 per user/month
- Minimum 5 users ($245/month)
- Annual billing
- 30-day free trial
```

## Features

### Unlimited Projects

- Create unlimited projects
- Unlimited repositories per project
- Unlimited webhook configurations
- Project templates

### Team Size

- Up to 50 users
- Unlimited team members per project
- Role-based access control (RBAC)
- Team management

### Review History

- 1 year of review history
- Unlimited issue storage
- Full audit logs
- Export and reporting

### Advanced SAST

- 50+ security rules
- OWASP Top 10 coverage
- Language-specific rules
- Custom rule configurations

### LLM Integration

- 10+ LLM providers
- OpenAI (GPT-4, GPT-3.5)
- Anthropic (Claude 3 Opus, Sonnet, Haiku)
- Google (Gemini Pro, Ultra)
- Cohere (Command R+)
- Mistral AI
- And more...

### Auto-Fix

- LLM-generated fixes
- Multi-line fixes
- Test generation
- Apply via UI or API
- Rollback support

### Integrations

**Issue Trackers:**
- Jira (create issues, update status)
- Linear (create issues, update status)

**Communication:**
- Slack (notifications, alerts)

**Version Control:**
- GitHub (PR checks, status checks)
- GitLab (merge request checks)
- BitBucket (PR checks)

### Priority Support

- Email support (48-hour response)
- Documentation access
- Video tutorials
- Quarterly webinars
- Feature requests prioritization

### Infrastructure

- High availability deployment
- Auto-scaling support
- Database clustering
- Redis replication
- Backup automation

### Compliance

- GDPR compliant
- Data export
- Data retention policies
- Audit logging

## Getting Started

### 1. Start Trial

```bash
# Navigate to Settings → Edition → Start Trial
# Or contact sales@pullwise.ai
```

### 2. Apply License

```yaml
# application.yml
pullwise:
  license:
    key: "your-professional-license-key"
```

```bash
# Restart application
kubectl rollout restart deployment/pullwise -n pullwise
```

### 3. Configure Features

#### Enable Jira

```yaml
pullwise:
  integrations:
    jira:
      enabled: true
      url: https://your-domain.atlassian.net
      username: pullwise-bot
      apiToken: your-token
      projectKey: PROJ
```

#### Enable Slack

```yaml
pullwise:
  integrations:
    slack:
      enabled: true
      botToken: xoxb-your-token
      defaultChannel: "#code-reviews"
```

#### Enable SAML SSO

```yaml
spring:
  security:
    saml:
      enabled: true
      metadata-url: https://idp.example.com/metadata
```

### 4. Add Users

```bash
# Add users via API
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/organizations/{orgId}/users \
  -d '{
    "email": "user@example.com",
    "name": "John Doe",
    "role": "MEMBER"
  }'
```

### 5. Create Projects

```bash
# Create unlimited projects
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/projects \
  -d '{
    "name": "Backend API",
    "organizationId": 123
  }'
```

## Feature Limits

| Resource | Community | Professional |
|----------|-----------|--------------|
| Projects | 5 | Unlimited |
| Users | 5 | 50 |
| Team members | 5 | 50 |
| Review history | 30 days | 1 year |
| SAST rules | 10+ | 50+ |
| LLM providers | 2 | 10+ |
| Concurrent reviews | 2 | 10 |

## Use Cases

### Startups

Perfect for startups that:
- Have 5-50 developers
- Need code review automation
- Want to catch bugs early
- Require integrations (Jira, Slack)
- Need priority support

### Growing Teams

Ideal for teams that:
- Are scaling up
- Need unlimited projects
- Want advanced SAST rules
- Require LLM review
- Need compliance features

### Product Teams

Great for product teams that:
- Ship frequently
- Need fast review cycles
- Want auto-fix suggestions
- Require analytics
- Need team metrics

## Comparison with Enterprise

| Feature | Professional | Enterprise |
|---------|--------------|------------|
| Users | Up to 50 | Unlimited |
| Support | Email (48h) | Slack (4h) + Live |
| SAML SSO | ✅ | ✅ |
| SCIM | ❌ | ✅ |
| SLA | None | 99.95% |
| Training | Videos | Live + Videos |
| Custom contracts | ❌ | ✅ |
| On-premise help | ❌ | ✅ |

## Upgrade Paths

### Professional → Enterprise

When to upgrade:
- Team exceeds 50 users
- Need SCIM provisioning
- Require SLA guarantees
- Need on-site training
- Want custom contracts

### Community → Professional

When to upgrade:
- More than 5 users
- More than 5 projects
- Need integrations
- Want support
- Need longer history

## Support Resources

### Documentation

- Full API documentation
- Configuration guides
- Integration tutorials
- Troubleshooting guides

### Video Tutorials

- Getting started
- Feature deep-dives
- Integration setup
- Best practices

### Webinars

- Quarterly product updates
- Feature previews
- Best practices
- Q&A sessions

## Billing

### Invoices

```bash
# Download invoices
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/billing/invoices
```

### Payment Methods

- Credit card (Visa, MasterCard, Amex)
- Wire transfer (annual)
- ACH (US only)

### Refund Policy

30-day money-back guarantee for annual subscriptions.

## Next Steps

- [Community Edition](/docs/administration/editions/community-edition) - Community features
- [Enterprise Edition](/docs/administration/editions/enterprise) - Enterprise features
- [Upgrading](/docs/administration/editions/upgrading) - Upgrade guide
