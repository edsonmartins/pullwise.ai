# Applying Fixes

How to apply Auto-Fix suggestions.

## Overview

Auto-Fix suggestions can be applied via the web UI, CLI, or API. Always review fixes before applying.

## Web UI

### Individual Fix

1. Navigate to **Reviews** → **Review Details**
2. Find the issue with a suggested fix
3. Click **View Fix** to see the suggested change
4. Review the diff:
   ```diff
   - String query = "SELECT * FROM users WHERE id = '" + userId + "'";
   + String query = "SELECT * FROM users WHERE id = ?";
   ```
5. Click **Apply Fix** if acceptable

### Bulk Apply

1. Navigate to **Reviews** → **Review Details**
2. Filter issues by **Has Fix** = `true`
3. Select issues to fix
4. Click **Apply Selected**
5. Review combined changes
6. Confirm to apply

## CLI

### Apply Single Fix

```bash
# Apply specific fix
pullwise apply-fix \
  --review-id=123 \
  --issue-id=456 \
  --commit-message="fix: apply auto-fix for SQL injection"
```

### Apply Multiple Fixes

```bash
# Apply all fixes with HIGH confidence
pullwise apply-fix \
  --review-id=123 \
  --confidence=HIGH \
  --commit-message="fix: apply auto-fixes"

# Apply fixes for specific rules
pullwise apply-fix \
  --review-id=123 \
  --rules="SQL_INJECTION,NULL_POINTER" \
  --commit-message="fix: apply security fixes"

# Interactive mode
pullwise apply-fix \
  --review-id=123 \
  --interactive
```

### Dry Run

```bash
# Preview changes without applying
pullwise apply-fix \
  --review-id=123 \
  --dry-run \
  --output=fixes.patch
```

## API

### Get Fix Suggestion

```bash
curl -X GET \
  -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{reviewId}/issues/{issueId}/fix
```

**Response:**

```json
{
  "issueId": 456,
  "fix": {
    "originalCode": "Statement.execute(query + id)",
    "fixedCode": "jdbcTemplate.query(query, id)",
    "confidence": "HIGH",
    "explanation": "Use parameterized query to prevent SQL injection"
  },
  "diff": "@@ -42,1 +42,1 @@\n-Statement.execute(query + id)\n+jdbcTemplate.query(query, id)"
}
```

### Apply Fix

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews/{reviewId}/issues/{issueId}/fix/apply \
  -d '{
    "createCommit": true,
    "commitMessage": "fix: apply auto-fix for SQL injection",
    "branch": "fix/issue-456"
  }'
```

## Applying via Pull Request

### Create Fix PR

```bash
# Apply fixes and create PR
pullwise apply-fix \
  --review-id=123 \
  --create-pr \
  --pr-title="fix: apply auto-fix suggestions" \
  --pr-body="Applying auto-fix suggestions from review #123"
```

### GitHub Integration

Auto-Fix can create a PR with all suggested fixes:

```yaml
# .github/workflows/pullwise-autofix.yml
name: Pullwise Auto-Fix

on:
  pull_request:
    types: [review_requested]

jobs:
  autofix:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Apply Auto-Fix
        run: |
          npx pullwise-cli apply-fix \
            --review-id=${{ steps.review.outputs.id }} \
            --create-pr
```

## Fix Workflows

### Interactive Workflow

```bash
# Interactive fix application
$ pullwise apply-fix --review-id=123 --interactive

Found 5 issues with fixes:

1. [HIGH] SQL_INJECTION at UserService.java:42
   > View diff? y
   > Apply? y

2. [MEDIUM] NULL_POINTER at Repository.java:18
   > View diff? y
   > Apply? n  # Skip this one

3. [HIGH] MISSING_NULL_CHECK at Controller.java:55
   > View diff? n
   > Apply? y  # Apply without viewing

...
Applied 2 of 5 fixes
```

### Automated Workflow

```bash
# CI/CD pipeline
- name: Run Pullwise
  run: npx pullwise-cli review

- name: Apply High-Confidence Fixes
  if: steps.review.outputs.exit_code == 1
  run: npx pullwise-cli apply-fix --confidence=HIGH

- name: Run Tests
  run: mvn test

- name: Commit Fixes
  if: success()
  run: |
    git config user.name "Pullwise Bot"
    git commit -am "fix: apply auto-fix suggestions"
```

## Reviewing Fixes

### Fix Diff Format

```diff
--- a/src/main/java/UserService.java
+++ b/src/main/java/UserService.java
@@ -40,7 +40,10 @@ public class UserService {
     public User getUser(String id) {
-        String query = "SELECT * FROM users WHERE id = '" + id + "'";
-        return statement.execute(query);
+        String query = "SELECT * FROM users WHERE id = ?";
+        return jdbcTemplate.queryForObject(
+            query,
+            User.class,
+            id
+        );
     }
```

### What to Check

- [ ] **Logic preserved** - Original behavior maintained
- [ ] **No new issues** - Fix doesn't introduce problems
- [ ] **Tests pass** - All tests still pass
- [ ] **Style matches** - Follows project conventions
- [ ] **Imports added** - Required imports included

## Fix Options

### Create New Branch

```bash
# Create fix branch
pullwise apply-fix \
  --review-id=123 \
  --branch="fix/auto-fix-123" \
  --create-pr
```

### Commit Options

```bash
# Auto-commit with message
pullwise apply-fix \
  --review-id=123 \
  --commit \
  --commit-message="fix: apply auto-fix from review #123"

# Stage but don't commit
pullwise apply-fix \
  --review-id=123 \
  --stage
```

### Format Options

```bash
# Apply fix and format
pullwise apply-fix \
  --review-id=123 \
  --format

# Use specific formatter
pullwise apply-fix \
  --review-id=123 \
  --format-command="npx prettier write"
```

## Confidence Levels

### High Confidence

Apply automatically (with tests):

```bash
pullwise apply-fix --confidence=HIGH --verify-tests
```

### Medium Confidence

Review before applying:

```bash
pullwise apply-fix --confidence=MEDIUM --interactive
```

### Low Confidence

Manual review required:

```bash
pullwise apply-fix --confidence=LOW --dry-run
# Review output, apply manually if desired
```

## Batch Operations

### Apply by Rule Type

```bash
# Apply all security fixes
pullwise apply-fix \
  --review-id=123 \
  --issue-types=VULNERABILITY

# Apply all style fixes
pullwise apply-fix \
  --review-id=123 \
  --issue-types=CODE_SMELL \
  --rules="LONG_LINE,MISSING_SEMICOLON"
```

### Apply by File

```bash
# Apply fixes in specific file
pullwise apply-fix \
  --review-id=123 \
  --file="src/main/java/UserService.java"

# Apply fixes excluding test files
pullwise apply-fix \
  --review-id=123 \
  --exclude="**/test/**"
```

## Troubleshooting

### Fix Cannot Be Applied

**Problem:** "Cannot apply fix - file has changed"

**Solution:**

```bash
# Rebase to latest
git pull --rebase

# Try again
pullwise apply-fix --review-id=123
```

### Conflicting Fixes

**Problem:** Multiple fixes affect same code

**Solution:**

```bash
# Apply one at a time
pullwise apply-fix --review-id=123 --issue-id=1
git commit -am "fix: apply fix 1"

pullwise apply-fix --review-id=123 --issue-id=2
# Resolve conflicts if any
git commit -am "fix: apply fix 2"
```

### Tests Fail After Fix

**Problem:** Tests fail after applying fix

**Solution:**

```bash
# Rollback the fix
git revert HEAD

# Report issue
pullwise report-fix-issue \
  --review-id=123 \
  --issue-id=456 \
  --message="Fix breaks test UserServiceTest#testGetUser"
```

## Next Steps

- [Rollback](/docs/user-guide/autofix/rollback) - Reverting fixes
- [Overview](/docs/user-guide/autofix/overview) - Auto-Fix overview
- [Reviews](/docs/user-guide/reviews/) - Review documentation
