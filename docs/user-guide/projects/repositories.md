# Connecting Repositories

Connect your Git repositories to Pullwise for automated code reviews.

## Supported Git Providers

| Provider | OAuth | Access Token | Webhooks |
|----------|-------|-------------|----------|
| **GitHub** | ✅ | ✅ | ✅ |
| **GitLab** | ✅ | ✅ | ✅ |
| **BitBucket** | ✅ | ✅ | ✅ |

## GitHub Integration

### GitHub App (Recommended)

Using the GitHub App provides:

- Automatic installation
- Easier permission management
- Webhook configuration
- No credential management

#### Install GitHub App

1. Navigate to **Settings** → **Integrations** → **GitHub**
2. Click **Install GitHub App**
3. Select repository access:
   - **All repositories** - Access to all
   - **Only select repositories** - Choose specific repos
4. Click **Install**

#### Permissions Required

The GitHub App requires:

| Permission | Why |
|------------|-----|
| `Contents: Read` | Clone repository |
| `Pull requests: Read/Write` | Read PRs, post comments |
| `Checks: Read/Write` | Post status checks |

### Personal Access Token

Alternatively, use a personal access token:

#### Create Token

1. Go to **Settings** → **Developer settings** → **Personal access tokens**
2. Click **Generate new token**
3. Configure:

   ```
   Note: Pullwise Integration
   Expiration: 90 days
   Scopes:
   ✍️ repo (Full control of private repositories)
   ```

4. Click **Generate token**
5. Copy the token (you won't see it again)

#### Add Token to Pullwise

1. Navigate to **Projects** → **New Project**
2. Under **Repository Access**, select **Personal Access Token**
3. Paste your token
4. Click **Verify** to confirm access

## GitLab Integration

### GitLab Application

1. Navigate to **Settings** → **Integrations** → **GitLab**
2. Click **Connect GitLab**
3. Authorize the application

### Personal Access Token

#### Create Token

1. Go to **Settings** → **Access Tokens**
2. Create token:

   ```
   Token name: Pullwise
   Scopes:
   ✍️ api
   ✍️ read_repository
   ✍️ read_api
   ```

3. Click **Create personal access token**
4. Copy the token

#### Configure Project

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/projects \
  -d '{
    "name": "my-project",
    "repositoryUrl": "https://gitlab.com/org/repo.git",
    "accessToken": "GITLAB_TOKEN"
  }'
```

## BitBucket Integration

### OAuth

1. Navigate to **Settings** → **Integrations** → **BitBucket**
2. Click **Connect BitBucket**
3. Authorize the application

### App Password

#### Create App Password

1. Go to **Settings** → **App passwords**
2. Create password:

   ```
   Label: Pullwise
   Permissions:
   ✍️ Repositories: Read
   ✍️ Pull requests: Read/Write
   ```

3. Copy the generated password

## Repository URL Formats

Use the correct URL format for each provider:

| Provider | HTTP URL | SSH URL |
|----------|----------|---------|
| **GitHub** | `https://github.com/org/repo` | `git@github.com:org/repo.git` |
| **GitLab** | `https://gitlab.com/org/repo` | `git@gitlab.com:org/repo.git` |
| **BitBucket** | `https://bitbucket.org/org/repo` | `git@bitbucket.org:org/repo.git` |

:::tip
Pullwise uses HTTP URLs by default. For SSH, configure authentication separately.
:::

## Webhook Configuration

After connecting, configure webhooks for automatic reviews.

### GitHub Webhook

1. Go to repository **Settings** → **Webhooks**
2. Click **Add webhook**
3. Configure:

   ```yaml
   Payload URL: https://your-server.com/webhooks/github
   Content type: application/json
   Secret: (optional) generate a random secret
   SSL verification: Enabled
   Events:
     ✅ Pull requests
     ✅ Pull request reviews
   ```

4. Click **Add webhook**

### GitLab Webhook

1. Go to **Settings** → **Webhooks**
2. Add webhook:

   ```yaml
   URL: https://your-server.com/webhooks/gitlab
   Secret token: (optional) your secret
   Trigger:
     ✅ Merge request events
   ```

3. Click **Add webhook**

### Verify Webhook

Create a test PR and verify:

1. Review starts automatically
2. Check **Recent Deliveries** in webhook settings
3. Check Pullwise dashboard for new review

## Branch Configuration

Configure which branches to review:

### Default Branch

The default branch (usually `main` or `master`) is:

- Protected by default
- Reviewed on all PRs
- Requires passing status checks to merge

### Protected Branches

Configure additional protected branches:

```yaml
protected_branches:
  - main
  - production
  - release/*
```

### Excluded Branches

Exclude branches from automatic review:

```yaml
excluded_branches:
  - develop
  - feature/*
  - hotfix/*
```

## Repository Sync

Pullwise syncs repository information:

### Manual Sync

1. Navigate to **Projects** → Select project
2. Click **Settings** → **Sync Repository**
3. Wait for sync to complete

### Automatic Sync

Pullwise automatically syncs:

- When a PR is created/updated
- Every 24 hours for metadata
- On demand for review triggers

## Troubleshooting

### Connection Failed

**Problem**: Can't connect to repository

**Solutions**:

1. Verify repository URL is correct
2. Check access token hasn't expired
3. Ensure repository exists and is accessible
4. Verify network connectivity

```bash
# Test connection manually
git clone https://github.com/org/repo.git
```

### Webhook Not Triggering

**Problem**: PR doesn't start review

**Solutions**:

1. Check webhook is enabled
2. Verify webhook URL is correct
3. Check webhook events include "Pull requests"
4. Test webhook with sample payload

### Large Repository Timeout

**Problem**: Large repos timeout during clone

**Solutions**:

1. Increase clone timeout in configuration:

   ```yaml
   git:
     clone_timeout: 600000  # 10 minutes
   ```

2. Use shallow clone:

   ```yaml
   git:
     shallow_clone: true
     depth: 1
   ```

## Next Steps

- [Creating Projects](/docs/user-guide/projects/creating-projects) - Create projects
- [Webhooks](/docs/user-guide/projects/webhooks) - Webhook setup
- [Triggering Reviews](/docs/user-guide/reviews/triggering-reviews) - Start reviews
