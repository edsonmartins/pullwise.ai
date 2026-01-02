# Creating Projects

Learn how to create and configure projects in Pullwise.

## What is a Project?

A project in Pullwise represents a code repository that you want to review. Each project:

- Connects to a Git repository (GitHub, GitLab, or BitBucket)
- Has its own review configuration
- Tracks reviews and issues over time
- Can be customized with specific rules and settings

## Create a Project

### Via UI

1. Navigate to **Projects** → **New Project**
2. Fill in the project details:

   | Field | Description | Example |
   |-------|-------------|---------|
   | **Name** | Project name | `my-api-service` |
   | **Description** | Optional description | `REST API service` |
   | **Repository** | Git repository URL | `https://github.com/org/repo` |
   | **Default Branch** | Main branch name | `main` |
   | **Organization** | Parent organization | `My Organization` |

3. Click **Create Project**

### Via API

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/projects \
  -d '{
    "name": "my-api-service",
    "description": "REST API service",
    "repositoryUrl": "https://github.com/org/repo",
    "defaultBranch": "main"
  }'
```

**Response:**

```json
{
  "id": 123,
  "name": "my-api-service",
  "description": "REST API service",
  "repositoryUrl": "https://github.com/org/repo",
  "defaultBranch": "main",
  "organizationId": 1,
  "createdAt": "2024-01-01T12:00:00Z"
}
```

## Repository Access

Pullwise needs access to your repository to:

1. **Clone the code** for analysis
2. **Read pull request diffs**
3. **Post comments** (optional)
4. **Create branches** for auto-fixes (optional)

### GitHub App Installation

1. Navigate to **Settings** → **Integrations** → **GitHub**
2. Click **Install GitHub App**
3. Select repositories to grant access
4. Authorize the installation

### Personal Access Token

Alternatively, use a personal access token:

1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token with:
   - `repo` - Full repository access
   - `pull_requests` - PR access
3. Add token to project configuration

:::warning
Use GitHub App for better security and management.
:::

## Project Configuration

After creating, configure your project:

### Review Settings

```yaml
# What to analyze
analysis:
  enabled_tools:
    - sonarqube
    - eslint
    - llm
  file_patterns:
    include:
      - "src/**/*.java"
      - "src/**/*.ts"
    exclude:
      - "src/test/**"
      - "**/*.test.ts"

# When to run
triggers:
  on_pr_created: true
  on_pr_updated: true
  on_commit_added: false
```

### Severity Thresholds

Configure which issues should block merges:

```yaml
thresholds:
  block_on_critical: true
  block_on_high: false
  block_on_medium: false
  block_on_low: false
```

### LLM Settings

Choose which LLM to use:

```yaml
llm:
  model: "anthropic/claude-3.5-sonnet"
  temperature: 0.3
  max_tokens: 4000
```

## Webhook Configuration

For automatic review triggering, configure webhooks:

### GitHub

1. Go to repository **Settings** → **Webhooks**
2. Add webhook:

   ```yaml
   Payload URL: https://your-server.com/webhooks/github
   Content type: application/json
   Events:
     - Pull requests
   ```

3. Click **Add webhook**

### Verify Webhook

Create a test PR and verify Pullwise starts reviewing.

## Branch Protection

Integrate Pullwise with branch protection rules:

### GitHub Branch Rules

1. Navigate to **Settings** → **Branches**
2. Add rule for main branch:

   ```yaml
   Require status checks:
     - Pullwise Review Passed
   Require branches to be up to date before merging: true
   ```

### Custom Status Check

Pullwise posts a status check that:

- ✅ **Passes** - No critical/high issues
- ⚠️ **Warning** - Medium issues found
- ❌ **Fails** - Critical issues found

## Project Visibility

Control who can access the project:

| Visibility | Description |
|-----------|-------------|
| **Private** | Only organization members |
| **Organization** | All organization members |
| **Public** | Anyone with link (read-only) |

## Cloning the Documentation

Clone an existing project's configuration:

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/projects/{sourceId}/clone \
  -d '{
    "name": "my-cloned-project",
    "repositoryUrl": "https://github.com/org/new-repo"
  }'
```

## Deleting a Project

:::caution
Deleting a project permanently removes all associated reviews and issues.
:::

### Via UI

1. Navigate to **Projects** → Select project
2. Click **Settings** → **Delete Project**
3. Confirm deletion

### Via API

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.pullwise.ai/api/projects/{id}
```

## Best Practices

### 1. Organize Projects by Team

Create separate projects for different teams or services:

```
my-org/
├── frontend-team/
│   ├── web-app
│   └── mobile-app
├── backend-team/
│   ├── api-service
│   └── worker-service
└── platform-team/
    ├── infra
    └── monitoring
```

### 2. Use Descriptive Names

Choose clear, descriptive names:

- ✅ `payment-api-service`
- ✅ `frontend-react-app`
- ❌ `project-1`
- ❌ `test-repo`

### 3. Configure Per-Project Rules

Tailor analysis rules per project type:

| Project Type | SAST Tools | LLM Model |
|--------------|-----------|-----------|
| Frontend | ESLint, Biome | GPT-4 |
| Backend | SonarQube, Checkstyle | Claude 3.5 |
| Mobile | Android Linter, SwiftLint | GPT-4 |

### 4. Set Appropriate Thresholds

Different projects need different thresholds:

```yaml
# Critical systems (payment, auth)
thresholds:
  block_on_critical: true
  block_on_high: true

# Internal tools
thresholds:
  block_on_critical: true
  block_on_high: false
```

## Troubleshooting

### Repository Access Denied

**Problem**: Can't clone repository

**Solutions**:
1. Verify GitHub App installation
2. Check personal access token has `repo` scope
3. Ensure repository isn't private without access

### Webhook Not Triggering

**Problem**: Creating PR doesn't start review

**Solutions**:
1. Check webhook URL is correct
2. Verify webhook events include "Pull requests"
3. Check webhook delivery logs in GitHub

### Configuration Not Applied

**Problem**: Settings not taking effect

**Solutions**:
1. Check configuration cache (may take 1-2 minutes)
2. Verify syntax is correct
3. Check for conflicting settings

## Next Steps

- [Repositories](/docs/user-guide/projects/repositories) - Connect repositories
- [Webhooks](/docs/user-guide/projects/webhooks) - Configure webhooks
- [Reviews](/docs/category/reviews) - Trigger and manage reviews
