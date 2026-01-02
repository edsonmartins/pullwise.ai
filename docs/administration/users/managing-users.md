# User Management

Manage users and teams in Pullwise.

## Overview

Pullwise supports multi-tenant user management:

- **Organizations** - Top-level containers
- **Teams** - Sub-groups within organizations
- **Users** - Individual accounts
- **Roles** - Permission levels

## User Types

| User Type | Description | Permissions |
|-----------|-------------|--------------|
| **Owner** | Organization owner | Full access + billing |
| **Admin** | Organization admin | Full access |
| **Member** | Regular user | Limited access |
| **Viewer** | Read-only access | View only |

## Creating Users

### Via UI

1. Navigate to **Settings** → **Users** → **Invite User**
2. Enter user details:

   ```yaml
   Email: user@example.com
   Name: John Doe
   Role: Member
   Teams:
     - Engineering
     - Backend
   ```

3. Click **Send Invitation**

### Via API

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/organizations/{orgId}/users \
  -d '{
    "email": "user@example.com",
    "name": "John Doe",
    "role": "MEMBER",
    "teamIds": [1, 2]
  }'
```

### Bulk Import

```bash
# CSV import format
email,name,role,teams
user1@example.com,Alice Smith,Member,"Backend,DevOps"
user2@example.com,Bob Jones,Admin,Frontend

curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "users=@users.csv" \
  https://api.pullwise.ai/api/organizations/{orgId}/users/bulk
```

## User Roles

### Owner

- Full access to organization
- Can manage subscriptions
- Can delete organization
- Can assign all roles

### Admin

- Full access to organization resources
- Can manage users and teams
- Can configure projects
- Cannot manage billing

### Member

- Access to assigned projects
- Can trigger reviews
- Can view assigned reports
- Cannot manage organization

### Viewer

- Read-only access to assigned projects
- Cannot trigger reviews
- Cannot make changes

## Teams

### Create Team

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/organizations/{orgId}/teams \
  -d '{
    "name": "Backend Team",
    "description": "Backend developers",
    "defaultRole": "MEMBER"
  }'
```

### Assign Projects to Team

```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/teams/{teamId}/projects \
  -d '{
    "projectIds": [1, 2, 3],
    "permission": "WRITE"
  }'
```

### Add User to Team

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/teams/{teamId}/members/{userId}
```

## SSO Integration

### SAML SSO

Configure SAML for enterprise:

```yaml
# application.yml
spring:
  security:
    saml:
      enabled: true
      metadata-url: https://idp.example.com/metadata
      issuer: https://idp.example.com
      assertion-consumer-service-url: https://pullwise.example.com/saml/sso
```

### SCIM Provisioning

Automate user provisioning:

```bash
# SCIM endpoint
https://api.pullwise.api/scim/v2/Users

# Create user via SCIM
curl -X POST \
  -H "Authorization: Bearer $SCIM_TOKEN" \
  -H "Content-Type: application/scim+json" \
  https://api.pullwise.api/scim/v2/Users \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "user@example.com",
    "name": {
      "givenName": "John",
      "familyName": "Doe"
    },
    "emails": [{
      "primary": true,
      "value": "user@example.com"
    }],
    "active": true
  }'
```

## User Deactivation

### Deactivate User

```bash
curl -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/users/{userId}
```

### Deactivate vs Delete

| Action | Data | Access |
|--------|------|--------|
| **Deactivate** | Preserved | Blocked |
| **Delete** | Anonymized | Removed |

## User Activity

### View Activity

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/users/{userId}/activity
```

### Recent Logins

```yaml
last_login: 2024-01-01T12:00:00Z
login_count: 45
last_seen: 2024-01-01T14:30:00Z
active_projects: 3
```

## Best Practices

### 1. Principle of Least Privilege

```yaml
# Assign minimum required permissions
role = role_for_task(task)
user.role = role
```

### 2. Regular Audits

```bash
# Quarterly user access review
0 0 1 */3 curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/organizations/{orgId}/audit/users
```

### 3. Use Teams

```yaml
# Organize users by team
teams:
  - Backend: [user1, user2, user3]
  - Frontend: [user4, user5]
  - DevOps: [user6, user7]
```

### 4. Monitor Inactive Users

```bash
# Find inactive users
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.pullwise.api/api/organizations/{orgId}/users?inactive=90"
```

## Troubleshooting

### User Cannot Access

**Problem**: User can't login or see projects

**Check**:

1. User is active
2. User is assigned to organization
3. User has team assignments
4. Projects are shared with user's teams

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/users/{userId}/permissions
```

### Too Many Users

**Problem**: Exceeding plan limits

**Solution**:

1. Review active users
2. Deactivate inactive users
3. Upgrade plan if needed

## Next Steps

- [Teams](/docs/administration/users/teams) - Team management
- [Permissions](/docs/administration/users/permissions) - Permission details
- [RBAC](/docs/administration/security/rbac) - Advanced permissions
