# Understanding Results

Learn how to interpret and act on Pullwise review results.

## Review Summary

When a review completes, you'll see a summary:

```yaml
Total Issues: 24
├── Critical: 2  🔴
├── High:      5  🟠
├── Medium:    12 🟡
└── Low:       5  🔵

Files Analyzed: 8
Lines Changed: 542
Review Time: 3m 24s
```

## Severity Levels

### 🔴 Critical

**Immediate action required**

Security vulnerabilities that could lead to:
- Data breaches
- Injection attacks
- Authentication bypass
- Sensitive data exposure

**Examples:**
- SQL injection
- Hardcoded credentials
- Missing authentication
- XXE/XSS vulnerabilities

**Action Required:**
- Block merge until fixed
- Escalate to security team
- Document fix in issue tracker

### 🟠 High

**Should fix before merge**

Bugs and major issues that could cause:
- Runtime errors
- Data corruption
- Incorrect behavior
- Performance degradation

**Examples:**
- Null pointer dereference
- Resource leaks
- Race conditions
- Logic errors

**Action Required:**
- Fix before merge
- Add unit test for fix
- Code review recommended

### 🟡 Medium

**Consider fixing**

Code quality and maintainability issues:
- Complex code
- Duplicate logic
- Missing documentation
- Non-critical bugs

**Examples:**
- Long methods
- High cyclomatic complexity
- Missing error handling
- Code duplicates

**Action Required:**
- Fix if time permits
- Create tech debt ticket
- Consider for refactoring

### 🔵 Low

**Nice to have**

Style and minor improvements:
- Naming conventions
- Formatting issues
- Comment quality
- Minor optimizations

**Examples:**
- Inconsistent naming
- Missing JSDoc
- Long lines
- Unused imports

**Action Required:**
- Fix at your convenience
- Can be batched
- Optional fixes

## Issue Details

Each issue includes:

### Header

```yaml
Rule: SONARSECURITY:SQL_INJECTION
Severity: Critical 🔴
File: src/main/java/UserRepository.java
Lines: 42-45
```

### Message

Human-readable description:

```
Potential SQL injection vulnerability. User input is directly
concatenated into SQL query without sanitization.
```

### Code Snippet

The problematic code:

```java
String query = "SELECT * FROM users WHERE id = " + userId;
```

### Suggestion

Recommended fix:

```java
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement ps = connection.prepareStatement(query);
ps.setInt(1, Integer.parseInt(userId));
```

### Additional Information

- **CWE**: CWE-89 (SQL Injection)
- **OWASP**: A1:2021 – Broken Access Control
- **References**: Links to resources
- **False Positive**: Button to mark as false positive

## Interpreting Results

### Review Status

| Status | Meaning | Action |
|--------|---------|--------|
| **QUEUED** | Waiting to start | Wait |
| **CLONING** | Fetching repository | Wait |
| **ANALYZING** | Running analysis | Wait |
| **COMPLETED** | Review finished | Review results |
| **FAILED** | Review failed | Check logs |
| **CANCELLED** | Review cancelled | None |

### Health Score

Pullwise calculates a health score (0-100):

```
Health Score = 100 - (Critical×20 + High×10 + Medium×5 + Low×1)
```

| Score | Color | Meaning |
|-------|-------|---------|
| 90-100 | 🟢 | Excellent |
| 70-89 | 🟢 | Good |
| 50-69 | 🟡 | Fair |
| 30-49 | 🟠 | Poor |
| 0-29 | 🔴 | Critical |

### Trend Indicators

Compare with previous reviews:

```yaml
vs Previous Review:
  Critical: +1  ⬆️
  High:      -3  ⬇️
  Medium:    +2  ⬆️
  Low:       -1  ⬇️

Trend: 🟡 Improving (from last review)
```

## Taking Action

### View in Context

Click any issue to view:

1. **File Viewer** - Full file with syntax highlighting
2. **Diff View** - Before/after comparison
3. **Blame View** - Who wrote the code
4. **History** - Related commits

### Mark False Positive

If an issue is not applicable:

1. Click the issue
2. Click **Mark as False Positive**
3. Add reason (optional)
4. Click **Confirm**

The issue is hidden and won't appear in future reviews.

### Apply Auto-Fix

For issues with auto-fix available:

1. Click **Apply Fix**
2. Review the diff
3. Click **Confirm** to apply

Fix is applied to a new branch:

```bash
pullwise/auto-fix/{review-id}/{issue-id}
```

### Create Issue

Create a tracker issue for the problem:

1. Click **Create Issue**
2. Select tracker (Jira, Linear, GitHub)
3. Modify issue details
4. Click **Create**

## Export Results

### Download Report

Export review as PDF or JSON:

```bash
# Download JSON
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{id}/export/json

# Download PDF
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{id}/export/pdf
```

### Generate SARIF

Export in SARIF format for integrations:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/reviews/{id}/export/sarif
```

## Filtering and Sorting

### Filter by Severity

Show only critical issues:

```yaml
filters:
  severity: [CRITICAL]
```

### Filter by Type

Show only security issues:

```yaml
filters:
  type: [VULNERABILITY]
```

### Filter by File

Show issues in specific file:

```yaml
filters:
  filePath: "src/main/java/**"
```

### Sort Options

- **Severity** (highest first)
- **File** (alphabetical)
- **Line** (ascending)
- **Type** (grouped)

## Analytics

### Per-Review Metrics

```yaml
Analysis Time: 3m 24s
Files Scanned: 8
Lines Analyzed: 542
Issues Found: 24
False Positives: 2
Fix Applied: 5
```

### Project Trends

```yaml
Last 30 Days:
  Reviews: 45
  Total Issues: 543
  Critical: 23
  High: 87
  Medium: 234
  Low: 199
  Fixed: 312
  Trend: 📈 Improving
```

## Best Practices

### 1. Start with Critical

Always address critical issues first:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.pullwise.ai/api/reviews/{id}/issues?severity=CRITICAL"
```

### 2. Batch Similar Issues

Group low/medium issues:

```yaml
# Create single task for all low-priority issues
batch:
  - All naming convention issues
  - All formatting issues
```

### 3. Learn from Patterns

Identify recurring issues:

```yaml
# Most common issues this month:
1. Missing error handling (45 occurrences)
2. Console.log statements (32 occurrences)
3. Missing JSDoc (28 occurrences)
```

### 4. Track Improvement

Monitor metrics over time:

```yaml
# Set quality goals
goals:
  critical_issues: 0
  health_score: 80+
  false_positive_rate: <10%
```

## Next Steps

- [Severity Levels](/docs/user-guide/reviews/severity-levels) - Severity guide
- [False Positives](/docs/user-guide/reviews/false-positives) - Handling false positives
- [Auto-Fix](/docs/category/autofix) - Automatic fixes
