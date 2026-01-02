# Enterprise Edition

Features and capabilities of Pullwise Enterprise.

## Overview

Enterprise edition is designed for large organizations with advanced security, compliance, and support requirements.

## Pricing

Custom pricing based on:
- Team size (100+ developers)
- Deployment model (cloud, on-premise, hybrid)
- Support requirements
- Compliance needs
- Custom integrations

Contact: enterprise@pullwise.ai

## Features

### Everything in Professional, plus:

- Unlimited users
- Forever review history
- 100+ SAST rules
- 20+ LLM providers
- SAML SSO
- SCIM provisioning
- Advanced RBAC
- Multi-region deployment
- SLA guarantees (99.95%)
- Dedicated support
- Custom training
- On-site assistance

## Security

### SAML SSO

```yaml
# Enable SAML
spring:
  security:
    saml:
      enabled: true
      metadata-url: https://idp.example.com/metadata
      issuer: https://idp.example.com
      assertion-consumer-service-url: https://pullwise.example.com/saml/sso
```

**Supported IdPs:**
- Okta
- Azure AD
- OneLogin
- Auth0
- Ping Identity
- Google Workspace
- Custom SAML 2.0 providers

### SCIM Provisioning

```yaml
# Enable SCIM
pullwise:
  scim:
    enabled: true
    accessToken: your-scim-token
```

**Supported:**
- Automatic user provisioning
- User deactivation
- Group synchronization
- Just-in-time provisioning

### Advanced RBAC

```yaml
# Custom roles
pullwise:
  rbac:
    custom-roles:
      - name: SecurityReviewer
        permissions:
          - reviews:read
          - issues:false_positive
      - name: ComplianceOfficer
        permissions:
          - "*:read"
          - reports:read
          - audit:read
```

### Row-Level Security

```sql
-- Enable RLS per organization
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

CREATE POLICY reviews_org_policy ON reviews
FOR ALL
TO pullwise_app
USING (
  organization_id = current_setting('app.current_org')::BIGINT
);
```

## Compliance

### Certifications

- **SOC 2 Type II** - Annual audit
- **ISO 27001** - Information security
- **GDPR** - Data protection
- **HIPAA** - Healthcare data (add-on)

### Data Residency

- Choose data center region
- Data at rest encryption
- Data in transit encryption
- Customer-managed encryption keys

### Audit Logging

```yaml
# Comprehensive audit logs
pullwise:
  audit:
    enabled: true
    log-all-actions: true
    retention: 7 years
```

### Data Export

```bash
# Export all data
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/organizations/{orgId}/export

# Export format: JSON, CSV, or SQL
```

## Deployment

### Multi-Region

```
Region: US East
├── Pullwise Application (HA)
├── PostgreSQL (Primary)
├── PostgreSQL (Standby)
└── Redis Cluster

Region: EU West
├── Pullwise Application (HA)
├── PostgreSQL (Standby)
└── Redis Cluster
```

### On-Premise

```
Requirements:
- Kubernetes 1.24+
- PostgreSQL 16
- Redis 7
- RabbitMQ 3
- MinIO (S3-compatible storage)

Support:
- Installation assistance
- Configuration guidance
- Troubleshooting help
```

### Hybrid Cloud

```
- Cloud control plane
- On-premise data nodes
- Edge caching
- WAN optimization
```

## Support

### Dedicated Support

- **Slack channel**: 4-hour response
- **Email**: 24-hour response
- **Phone**: Critical incidents
- **Account Manager**: Dedicated
- **Technical Account Manager**: Available

### SLA

```
Availability: 99.95% monthly uptime
- Excludes maintenance windows
- Credit for downtime: 10x service credit

Response times:
- Critical: 4 hours
- High: 8 hours
- Medium: 24 hours
- Low: 48 hours
```

### Training

### On-Site Training

- 2-day on-site workshop
- Customized to your team
- Up to 20 participants
- Includes hands-on labs

### Custom Training Programs

- Code review best practices
- SAST rule customization
- Plugin development
- Operations and monitoring

### Office Hours

- Monthly office hours
- Q&A with engineering team
- Product roadmap discussions
- Feature prioritization

## Custom Development

### Custom Features

```
Available for Enterprise:
- Custom SAST rules
- Custom integrations
- Custom reports
- Custom workflows
- SLA-based routing
```

### Professional Services

```
Available:
- Architecture review
- Security audit
- Performance optimization
- Plugin development
- Custom training
```

## Architecture

### High Availability

```
Application:
├── 3+ replicas per zone
├── Zone redundancy
├── Health checks
└── Auto-scaling

Database:
├── Primary-replica setup
├── Automated failover
├── Point-in-time recovery
└── Cross-region replication

Cache:
├── Redis cluster mode
├── Automatic failover
└── Cross-region replication
```

### Disaster Recovery

```
RPO: 5 minutes
RTO: 1 hour
- Automated backups
- Off-site storage
- Regular restore testing
```

## Migration

### From Professional to Enterprise

1. **Planning** (2-4 weeks)
   - Requirements gathering
   - Architecture review
   - Migration planning

2. **Preparation** (1-2 weeks)
   - License configuration
   - Feature enablement
   - User training

3. **Migration** (1 week)
   - Data migration (if needed)
   - Feature rollout
   - User onboarding

4. **Post-Migration** (2 weeks)
   - Monitoring
   - Support
   - Optimization

### From Community to Enterprise

Contact sales for:
- Assessment
- Migration plan
- Training program
- Go-live support

## Pricing Examples

### Small Enterprise

```
100 users
Cloud deployment
Standard support
Price: ~$12,000/month
```

### Medium Enterprise

```
500 users
Hybrid deployment
Priority support + training
Price: ~$40,000/month
```

### Large Enterprise

```
1000+ users
On-premise deployment
Dedicated support + training
Price: Custom
```

## Contract Terms

### Minimum Commitment

- 12-month minimum
- Multi-year discounts available
- Payment terms: Annual, quarterly

### Termination

- 30-day notice
- Pro-rated refund for annual
- Data export assistance

### Custom Clauses

- Data processing addendum
- SLA customization
- Support scope
- Acceptance criteria

## Success Stories

### Use Case 1: Fintech Company

```
Challenges:
- 200+ developers
- SOC 2 compliance
- Code security reviews

Solution:
- Pullwise Enterprise
- SOC 2 reporting
- Custom SAST rules
- Jira + Slack integration

Results:
- 50% faster reviews
- 40% fewer vulnerabilities
- Compliance automation
```

### Use Case 2: Healthcare Platform

```
Challenges:
- 150+ developers
- HIPAA compliance
- Legacy code refactoring

Solution:
- Pullwise Enterprise
- HIPAA add-on
- Custom migration
- Training program

Results:
- Improved code quality
- Compliance verification
- Team training
```

## Getting Started

### 1. Contact Sales

```
Email: enterprise@pullwise.ai
Phone: +1-555-PULLWISE
Form: https://pullwise.ai/enterprise
```

### 2. Discovery Call

- Requirements discussion
- Technical assessment
- Pricing options
- Timeline planning

### 3. Proof of Concept

- 30-day POC environment
- Custom configuration
- Support from team
- Evaluation criteria

### 4. Contract Negotiation

- Scope definition
- SLA customization
- Payment terms
- Support level

### 5. Deployment

- Onboarding assistance
- Installation support
- Configuration
- Training delivery

## Next Steps

- [Community Edition](/docs/administration/editions/community-edition) - Community features
- [Professional Edition](/docs/administration/editions/professional) - Professional features
- [Upgrading](/docs/administration/editions/upgrading) - Upgrade guide
