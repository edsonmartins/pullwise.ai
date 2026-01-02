# Upgrading Editions

How to upgrade Pullwise editions.

## Overview

Upgrading between editions is straightforward:

1. Purchase license for new edition
2. Apply license key
3. Restart application
4. Configure new features

## License Keys

### Obtain License

```bash
# Via API (Professional)
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/license/purchase-professional \
  -d '{
    "organizationId": 123,
    "userCount": 10,
    "billingPeriod": "annual"
  }'

# Response
{
  "licenseKey": "PLW-PRO-XXXXXXXXXXXXXXXXX",
  "edition": "PROFESSIONAL",
  "expiresAt": "2025-01-15"
}
```

### Apply License

```yaml
# application.yml
pullwise:
  license:
    key: "PLW-PRO-XXXXXXXXXXXXXXXXX"
    offline: false
```

### Verify License

```bash
# Check license status
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/status

# Response
{
  "edition": "PROFESSIONAL",
  "valid": true,
  "expiresAt": "2025-01-15T00:00:00Z",
  "features": [
    "jira",
    "slack",
    "saml",
    "unlimited_projects",
    "year_history"
  ]
}
```

## Community to Professional

### Pre-Upgrade Checklist

- [ ] Backup database
- [ ] Review feature differences
- [ ] Plan user addition
- [ ] Configure integrations
- [ ] Schedule training

### Upgrade Steps

#### 1. Purchase License

```bash
# Purchase via UI
Navigate to Settings → Edition → Upgrade to Professional

# Or via API
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/purchase-professional \
  -d '{"organizationId": 123, "userCount": 10}'
```

#### 2. Apply License

```yaml
# Update application.yml
pullwise:
  license:
    key: "your-professional-license-key"
```

#### 3. Restart Application

```bash
# Kubernetes
kubectl rollout restart deployment/pullwise -n pullwise

# Docker
docker-compose pull
docker-compose up -d

# RPM/DEB
systemctl restart pullwise
```

#### 4. Verify Upgrade

```bash
# Check features unlocked
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/features

# Create test project (should work without limit)
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/projects \
  -d '{"name": "Test Project", "organizationId": 123}'
```

#### 5. Configure New Features

**Enable Jira:**

```yaml
pullwise:
  integrations:
    jira:
      enabled: true
      url: https://your-domain.atlassian.net
      username: pullwise-bot
      apiToken: your-api-token
      projectKey: PROJ
```

**Enable Slack:**

```yaml
pullwise:
  integrations:
    slack:
      enabled: true
      botToken: xoxb-your-token
      defaultChannel: "#code-reviews"
```

#### 6. Add Users

```bash
# Add more users (up to 50)
for email in user1@example.com user2@example.com user3@example.com; do
  curl -X POST \
    -H "Authorization: Bearer $TOKEN" \
    https://api.pullwise.api/api/organizations/123/users \
    -d "{\"email\": \"$email\", \"name\": \"User\", \"role\": \"MEMBER\"}"
done
```

## Professional to Enterprise

### Pre-Upgrade Checklist

- [ ] Backup database
- [ ] Review feature differences
- [ ] Plan SSO integration
- [ ] Plan SCIM provisioning
- [ ] Schedule training
- [ ] Review compliance requirements

### Upgrade Steps

#### 1. Contact Sales

```
Email: enterprise@pullwise.ai
Phone: +1-555-PULLWISE
Web: https://pullwise.ai/enterprise
```

#### 2. Discovery Phase

- Requirements gathering
- Architecture review
- Integration planning
- Migration timeline

#### 3. License Delivery

```bash
# Enterprise license delivered securely
{
  "licenseKey": "PLW-ENT-XXXXXXXXXXXXXXXXX",
  "edition": "ENTERPRISE",
  "expiresAt": "2026-01-15",
  "organization": "Your Company",
  "features": [
    "unlimited_users",
    "saml_sso",
    "scim",
    "sla_premium",
    "dedicated_support"
  ]
}
```

#### 4. Apply License

```yaml
# application.yml
pullwise:
  license:
    key: "your-enterprise-license-key"
```

#### 5. Configure SAML SSO

```yaml
# Enable SAML
spring:
  security:
    saml:
      enabled: true
      metadata-url: https://idp.example.com/metadata
      issuer: https://idp.example.com
      assertion-consumer-service-url: https://pullwise.example.com/saml/sso
      sso:
        login-url: https://idp.example.com/sso
      logout:
        url: https://idp.example.com/slo
      jwk:
        key-set-uri: https://idp.example.com/keys
```

#### 6. Configure SCIM

```yaml
pullwise:
  scim:
    enabled: true
    accessToken: your-scim-bearer-token
    schema: urn:ietf:params:scim:schemas:core:2.0
    endpoint: /scim/v2
```

#### 7. Enable Advanced Features

```yaml
# Enable RBAC
pullwise:
  rbac:
    enabled: true
    custom-roles: true

# Enable audit logging
pullwise:
  audit:
    enabled: true
    log-all-actions: true
    retention: 7years

# Enable advanced analytics
pullwise:
  analytics:
    advanced: true
    export: true
```

#### 8. Deploy Enterprise Features

```bash
# Scale for enterprise
kubectl scale deployment pullwise -n pullwise --replicas=10

# Enable HPA
kubectl autoscale deployment pullwise \
  --min=3 --max=20 -n pullwise

# Configure backup automation
kubectl apply -f enterprise-backup-config.yaml
```

## Downgrading

### Downgrade to Community

```bash
# Cancel subscription
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/cancel \
  -d '{"edition": "COMMUNITY"}'

# After cancellation, license reverts to Community
# Features will be limited:
# - 5 projects max
# - 5 users max
# - 30-day history
```

### Graceful Downgrade

```yaml
# Before downgrade:
# 1. Remove extra users
# 2. Archive old reviews
# 3. Remove integrations
# 4. Export data
```

## Data Migration

### Export Before Upgrade

```bash
# Export all data
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/organizations/123/export \
  --output pullwise-export.json
```

### Import After Upgrade

```bash
# Import data (if needed)
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/organizations/123/import \
  -H "Content-Type: application/json" \
  --data @pullwise-export.json
```

## Trial Extensions

### Extend Professional Trial

```bash
# Request trial extension
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/trial-extend \
  -d '{"edition": "PROFESSIONAL", "days": 30}'
```

## Billing

### Invoices

```bash
# View invoices
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/billing/invoices

# Download specific invoice
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/billing/invoices/INV-12345 \
  --output invoice.pdf
```

### Payment Methods

```bash
# Update payment method
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/billing/payment-method \
  -d '{"type": "credit_card", "token": "tok_xxxx"}'
```

## Troubleshooting

### License Invalid

```bash
# Check license status
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/status

# Common issues:
# - Expired license
# - Invalid key format
# - Organization mismatch
# - Edition downgrade
```

### Features Not Available

```bash
# Verify features
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/license/features

# If feature not listed:
# - Edition doesn't include it
# - Additional configuration needed
# - Contact support
```

### Rollback Failed

```bash
# Rollback to previous version
kubectl rollout undo deployment/pullwise -n pullwise

# Remove license
kubectl set env deployment/pullwise \
  PULLWISE_LICENSE_KEY="" \
  -n pullwise
```

## Best Practices

### 1. Test Before Production

```bash
# Always test in staging
# Verify all features
# Plan rollback
```

### 2. Backup First

```bash
# Backup before major changes
./backup-script.sh --tag=pre-upgrade
```

### 3. Communicate with Users

```
# Notify team of upgrade
# Document new features
# Schedule training
# Provide support
```

### 4. Monitor After Upgrade

```bash
# Watch for issues
# Check error rates
# Monitor performance
# Review logs
```

## Next Steps

- [Community Edition](/docs/administration/editions/community-edition) - Community features
- [Professional Edition](/docs/administration/editions/professional) - Professional features
- [Enterprise Edition](/docs/administration/editions/enterprise) - Enterprise features
