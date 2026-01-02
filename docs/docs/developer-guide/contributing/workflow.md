# Git Workflow

Workflow for contributing to Pullwise.

## Overview

Pullwise uses a simplified Git workflow based on GitHub Flow:

```mermaid
gitGraph
    commit id: "Initial"
    branch main
    checkout main
    commit id: "v1.0.0"

    checkout main
    branch feature/new-plugin
    commit id: "Add plugin"
    commit id: "Write tests"

    checkout main
    branch feature/add-api
    commit id: "Add endpoint"

    checkout feature/new-plugin
    merge feature/add-api
    commit id: "Fix conflict"

    checkout main
    merge feature/new-plugin tag: "v1.1.0" type: HIGHLIGHT
```

## Branch Strategy

### Main Branch

- **Name:** `main`
- **Protection:** Required
- **Status Checks:** Required before merge
- **Always:** Deployable

### Feature Branches

- **Pattern:** `feature/<description>`
- **From:** `main`
- **To:** `main` (via PR)

```bash
# Create feature branch
git checkout main
git pull origin main
git checkout -b feature/add-jira-integration
```

### Bugfix Branches

- **Pattern:** `bugfix/<description>`
- **From:** `main`
- **To:** `main` (via PR)

```bash
# Create bugfix branch
git checkout -b bugfix/fix-webhook-timeout
```

### Hotfix Branches

- **Pattern:** `hotfix/<version>`
- **From:** `main` or release tag
- **To:** `main` (via PR)

```bash
# Create hotfix branch
git checkout -b hotfix/v1.0.1
```

## Development Workflow

### 1. Create Branch

```bash
# Start from clean main
git checkout main
git pull origin main

# Create your branch
git checkout -b feature/your-feature-name
```

### 2. Make Changes

```bash
# Work on your feature
# Make commits with clear messages
git add .
git commit -m "feat: add Jira integration"

# More commits
git add .
git commit -m "test: add integration tests for Jira"
```

### 3. Sync with Main

```bash
# Keep your branch up to date
git fetch origin main
git rebase origin/main

# If conflicts occur
# Resolve them, then:
git add .
git rebase --continue
```

### 4. Create Pull Request

```bash
# Push your branch
git push origin feature/your-feature-name

# Create PR via GitHub CLI
gh pr create \
  --title "feat: Add Jira integration" \
  --body "Description of changes" \
  --base main \
  --head feature/your-feature-name
```

### 5. Review and Merge

- Wait for code review
- Address feedback
- Ensure all checks pass
- Squash and merge

## Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `test` | Add or update tests |
| `refactor` | Code refactoring |
| `perf` | Performance improvement |
| `ci` | CI/CD changes |
| `chore` | Maintenance tasks |

### Examples

```bash
# Feature
git commit -m "feat(plugin): add support for custom LLM endpoints"

# Bug fix
git commit -m "fix(webhook): handle timeout for GitHub webhooks"

# Documentation
git commit -m "docs(api): update authentication examples"

# Breaking change
git commit -m "feat(api)!: remove deprecated endpoints

BREAKING CHANGE: The /v1/ endpoints have been removed"
```

## Pull Request Guidelines

### PR Title

Follow conventional commits format:

```
feat: add Jira integration
fix: resolve webhook timeout issue
docs: update deployment guide
```

### PR Description Template

```markdown
## Summary
Brief description of changes.

## Changes
- Added Jira integration
- Updated tests
- Modified configuration

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guide
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests pass locally

## Screenshots (if applicable)
Screenshots of UI changes.

## Related Issues
Closes #123
Related to #456
```

## Code Review Process

### For Reviewers

1. **Review within 24 hours**
2. **Be constructive** - Focus on code, not person
3. **Explain why** - Don't just say "change this"
4. **Approve or request changes** - Be clear

### For Authors

1. **Address all feedback** - Or explain why not
2. **Mark as resolved** - When feedback is addressed
3. **Keep discussions focused** - Move to chat if needed

## Handling Conflicts

### During Rebase

```bash
# Start rebase
git rebase origin/main

# Conflict detected
# Edit files to resolve conflicts
# Mark as resolved:
git add <resolved-files>
git rebase --continue

# If stuck
git rebase --abort
```

### During Merge

```bash
# Merge main into your branch
git checkout feature/your-feature
git merge main

# Resolve conflicts
# Commit merge
git commit
```

## Release Process

### Version Bump

```bash
# Backend
cd backend
./mvnw versions:set -DnewVersion=1.1.0

# Frontend
cd frontend
npm version 1.1.0
```

### Tag Release

```bash
# Create tag
git tag -a v1.1.0 -m "Release v1.1.0"

# Push tag
git push origin v1.1.0
```

### Changelog

Maintain `CHANGELOG.md`:

```markdown
## [1.1.0] - 2024-01-15

### Added
- Jira integration
- Custom LLM plugin support

### Fixed
- Webhook timeout issues
- Memory leak in review pipeline

### Changed
- Improved error messages
- Updated dependencies
```

## Troubleshooting

### Force Push (Use Carefully)

```bash
# Only on your own branch
git push --force-with-lease origin feature/your-feature
```

### Undo Local Changes

```bash
# Unstaged changes
git checkout -- <file>

# Staged changes
git restore --staged <file>

# All changes
git reset --hard HEAD
```

### Undo Commit

```bash
# Soft reset (keep changes)
git reset --soft HEAD~1

# Mixed reset (unstage changes)
git reset HEAD~1

# Hard reset (lose changes)
git reset --hard HEAD~1
```

## Best Practices

### 1. Small Commits

```bash
# Good: Small, focused commits
git commit -m "feat: add authentication"
git commit -m "test: add auth tests"
git commit -m "docs: update auth guide"

# Avoid: Large commits
git commit -m "add everything at once"
```

### 2. Descriptive Messages

```bash
# Good
git commit -m "fix(webhook): increase timeout to 30s for GitHub webhooks"

# Bad
git commit -m "fix stuff"
git commit -m "update"
```

### 3. Review Before Push

```bash
# Review your changes
git diff origin/main

# Review commits
git log origin/main..HEAD
```

### 4. Keep Branches Updated

```bash
# Regularly sync with main
git fetch origin
git rebase origin/main
```

## Next Steps

- [Code Style](/docs/developer-guide/contributing/code-style) - Code style guidelines
- [Testing](/docs/developer-guide/contributing/testing) - Testing guidelines
- [Pull Requests](/docs/developer-guide/contributing/pull-requests) - PR guide
