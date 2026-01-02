# Pull Request Guide

Best practices for creating pull requests.

## Overview

Pull requests (PRs) are how code gets into Pullwise. A good PR makes review easier and faster.

## Before Creating a PR

### 1. Self-Review

```bash
# Review your changes
git diff origin/main

# Review commit messages
git log origin/main..HEAD

# Check for sensitive data
git log -p origin/main..HEAD | grep -i "password\|secret\|token"
```

### 2. Ensure Tests Pass

```bash
# Backend
cd backend
./mvnw clean test
./mvnw verify -Pintegration-test

# Frontend
cd frontend
npm test
npm run type-check
npm run lint
```

### 3. Update Documentation

- Update relevant docs
- Add API notes if needed
- Update CHANGELOG.md

### 4. Clean Up

```bash
# Remove debug code
git grep "console.log" -- "*.ts" "*.tsx"
git grep "System.out.println" -- "*.java"

# Remove commented code
# (Consider doing this manually)

# Check for TODOs you added
git log -p origin/main..HEAD | grep "TODO"
```

## Creating a PR

### Using GitHub CLI

```bash
# Create PR with description
gh pr create \
  --title "feat: add Jira integration" \
  --body "Description of changes" \
  --base main \
  --head feature/jira-integration

# Create as draft
gh pr create --draft

# Open PR in browser
gh pr view --web
```

### Using GitHub Web UI

1. Go to [Pull Requests](https://github.com/integralltech/pullwise-ai/pulls)
2. Click "New Pull Request"
3. Select your branch
4. Click "Create Pull Request"
5. Fill in template

## PR Description Template

```markdown
## Summary
Brief one-line description of what this PR does.

## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Breaking change
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Code refactoring

## Related Issue
Fixes #123
Relates to #456

## Changes Made
- Added Jira integration service
- Created API endpoints for Jira sync
- Added tests for new functionality
- Updated deployment docs with Jira config

## Screenshots (if applicable)
<!-- Drag and drop screenshots here -->

## Testing
### Manual Testing
- [ ] Tested locally
- [ ] Tested in staging environment
- [ ] Tested with real data

### Automated Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added
- [ ] E2E tests added
- [ ] All tests passing

## Checklist
- [ ] Code follows project style guide
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new compiler warnings
- [ ] Added tests for changes
- [ ] All tests pass locally
- [ ] Ready for review

## Breaking Changes
<!-- List any breaking changes -->
None

## Additional Notes
Any additional context or considerations.
```

## PR Titles

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add Jira integration
fix: resolve webhook timeout
docs: update deployment guide
refactor: simplify review pipeline
perf: optimize issue consolidation
test: add integration tests for auth
chore: upgrade Spring Boot to 3.2.0
```

### Breaking Changes

Add `!` after type:

```
feat!: remove deprecated v1 API endpoints
```

## PR Sizes

Keep PRs focused and manageable:

| Size | Files Changed | Lines Changed | Review Time |
|------|---------------|---------------|-------------|
| Small | 1-5 | < 200 | 15-30 min |
| Medium | 5-15 | 200-500 | 30-60 min |
| Large | 15-30 | 500-1000 | 1-2 hours |
| XL | 30+ | 1000+ | 2+ hours |

### Splitting Large PRs

```bash
# Instead of one large PR:
feature/add-jira-integration (5000 lines)

# Split into smaller PRs:
feat/jira: add authentication
feat/jira: add issue sync
feat/jira: add webhooks
feat/jira: add configuration UI
```

## Review Process

### What Reviewers Look For

1. **Correctness** - Does it work as intended?
2. **Design** - Is it well-architected?
3. **Style** - Does it follow conventions?
4. **Tests** - Are tests adequate?
5. **Documentation** - Is it documented?
6. **Performance** - Any performance impact?

### Requesting Changes

```markdown
# Example review comment

## Changes Requested

### 1. Error Handling
The error handling in `JiraService.sync()` could be improved.

```java
// Current
try {
    sync();
} catch (Exception e) {
    log.error("Failed", e);
}

// Suggested
try {
    sync();
} catch (JiraApiException e) {
    log.error("API error: {}", e.getMessage());
    throw new SyncException("Jira sync failed", e);
} catch (IOException e) {
    log.error("Network error", e);
    throw new SyncException("Network failure", e);
}
```

### 2. Missing Test
Please add a test for the case where Jira returns 401.

### 3. Documentation
The README needs updating with Jira setup instructions.

---

Everything else looks good! Great work on this feature.
```

### Approving

After all requested changes are addressed:

```markdown
## Approved

All feedback addressed. LGTM! 👍

**One minor suggestion** (optional):
- Consider caching the Jira project list to reduce API calls.
```

## Handling Feedback

### Responding to Comments

```markdown
# Acknowledging feedback

@reviewer Thanks for catching that! I'll fix it.

# Explaining decisions

@reviewer I chose this approach because:
1. It's simpler than the alternative
2. Performance impact is negligible
3. Matches our existing patterns

Let me know if you'd like me to reconsider.

# Implementing changes

@reviewer Fixed! Please take another look.
```

### Making Changes

```bash
# Make the fix
git checkout feature/your-feature
# ... make changes ...

# Commit directly to branch
git add .
git commit -m "fix: add error handling for Jira sync"
git push

# Or amend if not yet reviewed
git add .
git commit --amend
git push --force-with-lease
```

### Resolving Conversations

After addressing feedback:

1. Click "Resolve conversation"
2. Leave a comment: "Fixed as suggested"
3. Request re-review if needed

## Common Issues

### CI Failures

```bash
# Check what failed
gh run view --log-failed

# Reproduce locally
./mvnw clean verify
npm test

# Fix and push
git add .
git commit -m "fix: resolve failing test"
git push
```

### Merge Conflicts

```bash
# Rebase on main
git checkout feature/your-feature
git fetch origin main
git rebase origin/main

# Resolve conflicts
# Edit files
git add <resolved-files>
git rebase --continue

# Push
git push --force-with-lease
```

### Reviewer Unavailable

After 3 business days without response:

```markdown
@reviewer Friendly ping! 🏓

Would you have time to review this PR? No rush, just wanted to make sure it didn't get lost.
```

After 5 days, request review from another maintainer.

## Merging

### Before Merging

- [ ] All reviewers approved
- [ ] All CI checks pass
- [ ] All conversations resolved
- [ ] Branch is up to date with main

### Merge Method

**Squash and Merge** (default):

```bash
# Creates single clean commit on main
# Maintains clean history
# Preserves original commits in PR branch
```

**Rebase and Merge** (for linear history):

```bash
# Preserves original commits
# Creates linear history
# Use for small, focused PRs
```

### Post-Merge

```bash
# Delete branch
git branch -d feature/your-feature
git push origin --delete feature/your-feature

# Or keep for reference
git push origin --no-verify feature/your-feature
```

## Emergency PRs

For critical fixes:

```markdown
## 🚨 Emergency PR

This is an emergency fix for a production issue.

### Issue
Production is down due to XYZ.

### Fix
Reverts problematic commit / Adds hotfix

### Impact
- Deploying immediately
- Full review to follow

### Review
Reviewed by @maintainer in Slack
Approved for fast-track merge
```

## Release Notes

Update `CHANGELOG.md` in your PR:

```markdown
## [Unreleased]

### Added
- Jira integration for issue sync (#123)

### Fixed
- Webhook timeout for large repositories (#124)

### Changed
- Improved error messages for plugin failures (#125)
```

## Best Practices

### 1. Keep PRs Focused

```bash
# Good: Single feature
feat: add Jira integration

# Bad: Multiple unrelated changes
chore: add Jira, fix bug, update deps, refactor config
```

### 2. Link Issues

```markdown
Fixes #123
Related to #456
Part of #789
```

### 3. Add Context

```markdown
## Context
We need Jira integration to track issues found in code reviews.

This PR implements the initial sync functionality.
Future PRs will add bi-directional sync and custom field mapping.
```

### 4. Be Responsive

- Respond to reviews within 24 hours
- Address feedback promptly
- Keep PR updated

## Next Steps

- [Workflow](/docs/developer-guide/contributing/workflow) - Git workflow
- [Code Style](/docs/developer-guide/contributing/code-style) - Style guide
- [Testing](/docs/developer-guide/contributing/testing) - Testing guide
