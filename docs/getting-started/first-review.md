# Your First Review

Learn how to create and interpret your first Pullwise code review.

## Prerequisites

Before starting, ensure you have:

- [x] Pullwise installed and running
- [x] GitHub OAuth configured (or demo user)
- [x] A project created
- [x] Webhook configured

## Step 1: Create a Project

1. Navigate to **Projects** → **New Project**
2. Fill in the project details:

   ```yaml
   Name: my-first-project
   Description: My first Pullwise project
   Repository: https://github.com/your-org/your-repo
   Default Branch: main
   ```

3. Click **Create Project**

## Step 2: Configure Webhook

### GitHub Webhook

1. Go to your repository **Settings** → **Webhooks**
2. Click **Add webhook**
3. Configure:

   ```yaml
   Payload URL: https://your-server.com/webhooks/github
   Content type: application/json
   Secret: (optional) your-webhook-secret
   Events:
     - Pull requests
     - Pull request reviews
   ```

4. Click **Add webhook**

### Verify Webhook

```bash
# Test webhook delivery
curl -X POST https://your-server.com/webhooks/github \
  -H "Content-Type: application/json" \
  -d '{"test": true}'
```

## Step 3: Create a Pull Request

Make a change to your repository and create a PR:

```bash
# Make a change
echo "test" > test.txt
git add test.txt
git commit -m "Add test file"

# Push and create PR
git push origin feature/test-branch
# Then create PR via GitHub UI
```

## Step 4: Monitor Review Progress

Pullwise will automatically start reviewing when the webhook is received.

### Check Status

Navigate to **Reviews** → select your review

The review goes through these stages:

1. **Queued** - Waiting to start
2. **Cloning** - Fetching repository
3. **Analyzing** - Running SAST + LLM
4. **Consolidating** - Merging results
5. **Completed** - Ready to view

### Real-time Updates

Pullwise provides real-time updates via WebSocket:

```bash
# WebSocket connection
wss://your-server.com/ws/reviews/{review-id}
```

## Step 5: Interpret Results

Once complete, you'll see:

### Summary Cards

```yaml
Total Issues: 12
  Critical: 1  🔴
  High:      3  🟠
  Medium:    5  🟡
  Low:       3  🔵
```

### Issue Details

Each issue includes:

| Field | Description |
|-------|-------------|
| **Severity** | Critical, High, Medium, Low |
| **Type** | Bug, Vulnerability, Code Smell, Style |
| **Rule** | SAST rule or LLM analysis |
| **File** | Affected file path |
| **Line** | Line number |
| **Message** | Human-readable description |
| **Suggestion** | Proposed fix (if available) |

### Example Issue

```yaml
Severity: High 🔴
Type: Security Vulnerability
Rule: SONARSECURITY:SQL_INJECTION
File: src/main/java/com/example/UserRepository.java
Line: 42

Message:
  Potential SQL injection vulnerability. User input is directly
  concatenated into SQL query without sanitization.

Code:
  String query = "SELECT * FROM users WHERE id = " + userId;

Suggestion:
  Use parameterized queries:
  String query = "SELECT * FROM users WHERE id = ?";

[Auto-Fix Available] ✅
```

## Step 6: Take Action

### View in Context

Click any issue to view it in context:

1. **File View** - See the code with syntax highlighting
2. **Diff View** - Compare suggested fix
3. **Blame View** - See who wrote the code

### Mark False Positive

If an issue is not applicable:

1. Click the issue
2. Select **Mark as False Positive**
3. (Optional) Add a reason
4. Click **Confirm**

Pullwise learns from false positives and reduces similar issues in future reviews.

### Apply Auto-Fix

For issues with auto-fix available:

1. Click **Apply Fix**
2. Review the changes in diff view
3. Click **Confirm** to apply

The fix is automatically committed to a new branch:

```bash
# Auto-fix branch naming
pullwise/auto-fix/{review-id}/{issue-id}
```

## Step 7: Review in Pull Request

Pullwise also posts comments directly to your GitHub PR:

### Summary Comment

```markdown
## Pullwise Review Results

**Total Issues**: 12 (1 critical, 3 high, 5 medium, 3 low)

### Critical Issues 🔴
- [SQL Injection](https://pullwise.example.com/reviews/123#issue-456)

### High Issues 🟠
- [Null Pointer Dereference](https://pullwise.example.com/reviews/123#issue-457)
- [Resource Leak](https://pullwise.example.com/reviews/123#issue-458)
- [Unused Import](https://pullwise.example.com/reviews/123#issue-459)

---
[View Full Review](https://pullwise.example.com/reviews/123)
```

### Inline Comments

For specific code issues:

```markdown
⚠️ **Security Issue**: Potential SQL injection
Consider using parameterized queries instead of string concatenation.
[Suggested Fix](https://pullwise.example.com/autofix/123/456)
```

## Understanding Review Quality

### Coverage

Pullwise analyzes:

- **New code** - Lines added in the PR
- **Modified code** - Lines changed in the PR
- **Impact analysis** - Files affected by changes

### Accuracy

- **SAST**: ~95% precision, ~80% recall
- **LLM**: ~85% precision, ~70% recall
- **Combined**: ~90% precision, ~85% recall

### Performance

| Repository Size | Review Time |
|----------------|-------------|
| < 1,000 LOC | 1-2 minutes |
| 1,000-5,000 LOC | 2-5 minutes |
| 5,000-10,000 LOC | 5-10 minutes |
| > 10,000 LOC | 10+ minutes |

## Troubleshooting

### Review Not Starting

**Possible causes:**
1. Webhook not configured correctly
2. Server not accessible from GitHub
3. Authentication issue

**Solutions:**
```bash
# Check webhook delivery in GitHub
# Repository → Settings → Webhooks → Recent Deliveries

# Check backend logs
docker-compose logs -f backend

# Test webhook manually
curl -X POST http://localhost:8080/webhooks/github \
  -H "Content-Type: application/json" \
  -d '{"action":"test"}'
```

### Review Stuck

**Possible causes:**
1. Repository too large
2. Rate limiting from LLM provider
3. Database connection issue

**Solutions:**
```bash
# Check review status
curl http://localhost:8080/api/reviews/{id}

# Cancel stuck review
curl -X POST http://localhost:8080/api/reviews/{id}/cancel
```

### No Issues Found

This is normal for small PRs or well-written code. To test, try:

1. Intentional bug (e.g., null pointer)
2. Security issue (e.g., hardcoded password)
3. Code smell (e.g., unused variable)

## Next Steps

- [Configuration](/docs/getting-started/configuration) - Customize review settings
- [User Guide](/docs/category/user-guide) - Learn more features
- [Auto-Fix](/docs/category/autofix) - Automatic fix generation
