# Role-Based Access Control (RBAC)

Configure detailed permissions for Pullwise.

## Overview

RBAC controls:

- Who can access which resources
- What actions users can perform
- Which data is visible to whom

## Permission Model

```
Organization
├── Owners
│   └── Full control
├── Admins
│   ├── Manage users
│   ├── Manage projects
│   └── View all data
├── Members
│   ├── Access assigned projects
│   ├── Trigger reviews
│   └── View own data
└── Viewers
    └── Read-only access
```

## Permissions

### Organization Permissions

| Permission | Owner | Admin | Member | Viewer |
|------------|-------|-------|--------|--------|
| View organization | ✅ | ✅ | ✅ | ✅ |
| Edit organization | ✅ | ✅ | ❌ | ❌ |
| Manage users | ✅ | ✅ | ❌ | ❌ |
| Manage billing | ✅ | ❌ | ❌ | ❌ |
| Delete organization | ✅ | ❌ | ❌ | ❌ |

### Project Permissions

| Permission | Owner | Admin | Member | Viewer |
|------------|-------|-------|--------|--------|
| View project | ✅ | ✅ | ✅ | ✅ |
| Edit project | ✅ | ✅ | ❌ | ❌ |
| Trigger review | ✅ | ✅ | ✅ | ❌ |
| Cancel review | ✅ | ✅ | ✅ | ❌ |
| Delete project | ✅ | ✅ | ❌ | ❌ |
| Configure review | ✅ | ✅ | ❌ | ❌ |

## Role Definitions

### Owner

```yaml
permissions:
  organization:
    - "*"
  projects:
    - "*"
  billing:
    - "*"
  users:
    - "*"
```

### Admin

```yaml
permissions:
  organization:
    - read
    - update
    - manage_users
    - manage_teams
  projects:
    - "*"
  billing:
    - read
  users:
    - read
    - create
    - update
    - deactivate
```

### Member

```yaml
permissions:
  projects:
    - read
    - trigger_review
    - cancel_own_review
  reviews:
    - read
    - false_positive
    - apply_fix
```

### Viewer

```yaml
permissions:
  projects:
    - read
  reviews:
    - read
```

## Custom Roles

### Create Custom Role

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.api/api/organizations/{orgId}/roles \
  -d '{
    "name": "Reviewer",
    "description": "Can review but not modify",
    "permissions": [
      "projects:read",
      "reviews:read",
      "reviews:false_positive",
      "reviews:apply_fix"
    ]
  }'
```

## Role Assignment

### Assign Role

```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.api/api/users/{userId}/role \
  -d '{
    "organizationId": 123,
    "role": "ADMIN"
  }'
```

### Project-Level Role

```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.api/api/projects/{projectId}/members/{userId} \
  -d '{
    "role": "MEMBER"
  }'
```

## Resource-Based Access

### Row-Level Security

```sql
-- Enable RLS
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Policy: Users see own org's reviews
CREATE POLICY reviews_org_policy ON reviews
FOR SELECT
TO pullwise_app
USING (
  organization_id = current_setting('app.current_org')::BIGINT
);

-- Set org context
SET app.current_org = '123';
```

### Project Filtering

```java
// Filter projects by user access
public List<Project> findAccessibleProjects(User user) {
    return projectRepository.findAllByUser(user)
        .stream()
        .filter(project -> user.canAccess(project))
        .toList();
}
```

## Permission Checks

### Backend

```java
@PreAuthorize("hasPermission(#projectId, 'READ', 'PROJECT')")
public Project getProject(Long projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> new NotFoundException());
}

@PreAuthorize("hasRole('ADMIN')")
public void deleteOrganization(Long orgId) {
    organizationService.delete(orgId);
}
```

### Frontend

```typescript
// Permission check hook
function usePermission(resource: string, action: string) {
  const { user } = useAuth();
  return user?.permissions?.some(
    p => p.resource === resource && p.actions.includes(action)
  );
}

// Usage
function DeleteButton({ projectId }) {
  const canDelete = usePermission('project', 'delete');

  if (!canDelete) return null;

  return <Button onClick={() => deleteProject(projectId)}>
    Delete
  </Button>;
}
```

## Best Practices

### 1. Use Roles Over Permissions

```yaml
# Good: Assign role
user.role = ADMIN

# Avoid: Assign individual permissions
user.permissions = [p1, p2, p3, ...]
```

### 2. Apply Least Privilege

```yaml
# Start with minimum access
default_role = VIEWER

# Grant more as needed
role = getRoleForTask(task)
```

### 3. Audit Regularly

```bash
# Review role assignments
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/organizations/{orgId}/roles/audit
```

### 4. Document Roles

```yaml
# Document role definitions
docs:
  roles:
    - name: Owner
      description: "Full control"
      responsibilities: "Responsible for all decisions"
    - name: Admin
      description: "Manager access"
      responsibilities: "Day-to-day operations"
    - name: Member
      description: "Contributor access"
      responsibilities: "Code reviews and fixes"
```

## Troubleshooting

### Access Denied

**Problem**: User gets 403 Forbidden

**Check**:

```bash
# Verify user permissions
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/users/{userId}/permissions

# Check role assignment
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.api/api/users/{userId}/role
```

### Permission Inheritance

```yaml
# Permissions cascade:
# Organization > Team > Project

# User with Admin role:
# - All organization projects
# - All team projects

# User with Member role:
# - Assigned team projects
# - Directly assigned projects
```

## Next Steps

- [Managing Users](/docs/administration/users/managing-users) - User management
- [Teams](/docs/administration/users/teams) - Team management
- [SAML SSO](/docs/administration/security/saml-sso) - SSO integration
