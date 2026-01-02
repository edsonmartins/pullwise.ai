# Rolling Back Fixes

How to revert Auto-Fix changes.

## Overview

Sometimes an Auto-Fix doesn't work as expected. This guide shows how to rollback changes.

## Quick Rollback

### Single Fix Rollback

```bash
# Revert last commit (single fix)
git revert HEAD --no-edit
git push
```

### Multiple Fixes Rollback

```bash
# Revert last N commits
git revert HEAD~N..HEAD

# Example: Revert last 3 fix commits
git revert HEAD~3..HEAD
```

## Rollback via Pullwise CLI

### Rollback Specific Fix

```bash
# Rollback fix by issue ID
pullwise rollback-fix \
  --review-id=123 \
  --issue-id=456 \
  --reason="Fix introduced regression"

# Rollback and report
pullwise rollback-fix \
  --review-id=123 \
  --issue-id=456 \
  --report-issue
```

### Rollback Review Fixes

```bash
# Rollback all fixes from a review
pullwise rollback-fix \
  --review-id=123 \
  --all

# Dry run first
pullwise rollback-fix \
  --review-id=123 \
  --all \
  --dry-run
```

## Rollback via API

### Revert Fix

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews/{reviewId}/issues/{issueId}/fix/revert \
  -d '{
    "reason": "Fix broke existing functionality",
    "createIssue": true
  }'
```

**Response:**

```json
{
  "success": true,
  "revertedAt": "2024-01-15T10:30:00Z",
  "issueCreated": true,
  "issueUrl": "https://github.com/repo/issues/789"
}
```

## Rollback Strategies

### Strategy 1: Git Revert

```bash
# Safe: Creates new commit reverting changes
git revert <commit-hash>

# Example: Revert fix commit
git revert abc123def
```

### Strategy 2: Git Reset

```bash
# Dangerous: Removes commits entirely
git reset --hard HEAD~1

# Only use if commits haven't been pushed!
git push --force-with-lease
```

### Strategy 3: Manual Revert

```bash
# View what changed
git show <commit-hash>

# Manually edit files to revert
# ...

# Commit manual revert
git commit -am "revert: manually rollback fix for issue 456"
```

## Selective Rollback

### Rollback Part of a Fix

If a fix touched multiple files and you only want to revert some:

```bash
# Checkout specific file from before fix
git checkout <commit-before-fix> -- path/to/file.java

# Commit partial revert
git add path/to/file.java
git commit -m "revert: partial rollback of fix for issue 456"
```

### Rollback Specific Changes in File

For changes within a single file:

```bash
# Interactive rebase
git rebase -i HEAD~3

# Mark commits to edit
# When prompted, reset the file partially
git reset HEAD~1 -- path/to/file.java
git checkout path/to/file.java
git commit --amend
git rebase --continue
```

## Tracking Rollbacks

### Mark Fix as Failed

```bash
# Mark fix as failed in Pullwise
pullwise mark-fix-failed \
  --review-id=123 \
  --issue-id=456 \
  --reason="Introduced regression in test UserServiceTest"
```

This helps improve future fix suggestions.

### Create Issue

```bash
# Create GitHub issue for failed fix
pullwise rollback-fix \
  --review-id=123 \
  --issue-id=456 \
  --create-issue \
  --issue-title="Fix for issue 456 introduced regression" \
  --issue-body="The auto-fix for SQL injection at UserService.java:42
broke the existing test suite. Need to revisit this fix."
```

## Rollback Workflow

### Safe Rollback Process

```bash
# 1. Identify problematic commit
git log --oneline -10

# 2. Verify what changed
git show <commit-hash>

# 3. Run tests to confirm failure
mvn test

# 4. Revert the commit
git revert <commit-hash> --no-edit

# 5. Verify tests pass again
mvn test

# 6. Push revert
git push

# 7. Report to Pullwise
pullwise mark-fix-failed \
  --review-id=123 \
  --issue-id=456 \
  --commit=<commit-hash> \
  --reason="Tests failed after applying fix"
```

## Emergency Rollback

### Hotfix Rollback

If a deployed fix causes production issues:

```bash
# 1. Identify bad commit
git log --oneline --deployed

# 2. Create hotfix branch
git checkout -b hotfix/rollback-fix-456

# 3. Revert bad commit
git revert <bad-commit>

# 4. Tag and push
git tag hotfix/v1.0.1
git push origin hotfix/rollback-fix-456 --tags

# 5. Deploy hotfix
kubectl rollout undo deployment/pullwise
```

### Database Migration Rollback

If fix included database changes:

```bash
# 1. Check migration status
mvn flyway:status

# 2. Rollback migration
mvn flyway:undo -Dflyway.target=1.2.3

# 3. Revert code changes
git revert <commit-hash>

# 4. Verify
mvn test
```

## Preventing Future Issues

### Require Approval

```yaml
# application.yml
pullwise:
  autofix:
    requireApproval: true
    confidenceThreshold: HIGH
    requireTestsPass: true
```

### Staged Rollout

```bash
# Apply to staging first
pullwise apply-fix \
  --review-id=123 \
  --branch="staging"

# Verify on staging
# Run tests, manual checks

# If good, apply to main
git checkout main
git merge staging
```

### Fix Hooks

```bash
# Pre-apply hook
# .pullwise/hooks/pre-apply
#!/bin/bash
mvn test
if [ $? -ne 0 ]; then
  echo "Tests failed, aborting fix"
  exit 1
fi
```

## Troubleshooting

### Revert Causes Conflicts

```bash
# Resolve revert conflicts
git revert <commit-hash>

# When conflicts occur:
# 1. Edit files to resolve
# 2. git add <resolved-files>
# 3. git revert --continue

# Or abort if needed
git revert --abort
```

### Cannot Identify Bad Commit

```bash
# Binary search for bad commit
git bisect start
git bisect bad HEAD  # Current broken state
git bisect good <known-good-commit>

# Git will checkout commits, test each
mvn test
git bisect good  # or git bisect bad

# Once found, revert that commit
git revert <bad-commit-hash>
git bisect reset
```

### Rollback Causes New Issues

```bash
# Sometimes reverting a fix reveals the original issue
# May need to manually fix

# 1. View both versions
git show <commit-before-fix>:file.java
git show <commit-after-fix>:file.java

# 2. Create manual fix combining both
# ...

# 3. Commit as manual fix
git commit -am "fix: manually resolve issue 456"
```

## Best Practices

### 1. Always Test Before Pushing

```bash
# Run full test suite
mvn test

# Run integration tests
mvn verify

# Manual smoke test
# ...
```

### 2. Document Rollbacks

```bash
# Good commit message
git revert <commit-hash> -m "revert: rollback fix for SQL injection

Original fix broke user authentication.
Issue tracked in pullwise#456.
Will revisit with alternative approach."
```

### 3. Keep Fix History

```bash
# Don't delete fix commits
# Revert creates new commit preserving history

# Bad: git reset --hard (loses history)
# Good: git revert (preserves history)
```

### 4. Use Feature Flags

```java
// Feature flag for new fixes
@FeatureToggle("pullwise-fix-456")
public User getUser(String id) {
    // New fix code
    return jdbcTemplate.query(...);
}
```

## Next Steps

- [Applying Fixes](/docs/user-guide/autofix/applying-fixes) - Applying fixes
- [Overview](/docs/user-guide/autofix/overview) - Auto-Fix overview
- [Reviews](/docs/user-guide/reviews/) - Review documentation
