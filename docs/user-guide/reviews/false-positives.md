# False Positives

Handle and reduce false positives in Pullwise reviews.

## What is a False Positive?

A false positive occurs when Pullwise flags an issue that isn't actually a problem.

### Examples

| Issue | Why It's False Positive |
|-------|------------------------|
| "TODO comment found" | TODO is for documentation purposes |
| "Missing error handling" | Handled by framework |
| "Unused variable" | Used via reflection |
| "Complex method" | Necessary complexity |

## Marking False Positives

### Via UI

1. Navigate to **Reviews** → Select review
2. Click on the issue
3. Click **Mark as False Positive**
4. (Optional) Add a reason
5. Click **Confirm**

### Via API

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews/issues/{issueId}/false-positive \
  -d '{
    "reason": "TODO is for documentation"
  }'
```

### Via PR Comment

Reply to the Pullwise comment:

```
@pullwise false positive

Reason: TODO comment is for future reference
```

## False Positive Reasons

### Common Reasons

```yaml
reasons:
  - "Intentional usage"
  - "Handled by framework"
  - "Test code"
  - "Generated code"
  - "External dependency"
  - "Legacy code"
  - "Temporary workaround"
  - "Not applicable to this context"
```

### Best Practices for Reasons

1. **Be specific** - Explain why it's not an issue
2. **Provide context** - Add relevant information
3. **Suggest improvement** - Help us improve detection

## Bulk Actions

### Mark Multiple Issues

1. Navigate to review
2. Select multiple issues
3. Click **Mark Selected as False Positive**
4. Add reason for all
5. Click **Confirm**

### Bulk via API

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/reviews/{reviewId}/false-positive/bulk \
  -d '{
    "issueIds": [1, 2, 3, 4, 5],
    "reason": "All are generated code"
  }'
```

## Learning from False Positives

### Automatic Learning

Pullwise learns from false positives:

```yaml
learning:
  enabled: true
  threshold: 5  # After 5 FPs, suppress similar issues
```

### Pattern Matching

Pullwise identifies patterns:

```java
// After marking false positive 5 times:
// TODO: [A-Z]+
// This pattern is now suppressed in future reviews
```

## Per-File False Positives

### Ignore Files

```yaml
# application.yml
pullwise:
  ignore:
    files:
      - "src/main/generated/**"  # Generated code
      - "**/test/**"              # Test code
      - "**/*.config.js"          # Config files
```

### File Annotations

Add comments to ignore specific lines:

```java
// pullwise-disable-next-line
String password = "hardcoded";  // Won't be flagged

// pullwise-disable-line RULE_NAME
complexMethod();  // Specific rule disabled
```

### Block Ignore

```java
// pullwise-disable
public void legacyCode() {
    // Lots of legacy code issues
}
// pullwise-enable
```

## Per-Rule False Positives

### Disable Rules

```yaml
# application.yml
pullwise:
  rules:
    disabled:
      - TOO_MANY_PARAMS      # Too many false positives
      - FUNCTION_LENGTH      # Not applicable
      - MISSING_JAVADOC      # Optional for us
```

### Rule Configuration

```yaml
pullwise:
  rules:
    TOO_MANY_PARAMS:
      enabled: true
      severity: LOW
      threshold: 10  # More lenient
```

## False Positive Rate

### Track Rate

```bash
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/analytics/projects/{projectId}/false-positive-rate
```

### Target Rate

Industry standard: **< 10%** false positive rate

```yaml
if fp_rate > 0.10:
  # Review rule configuration
  adjust_rules()
```

## Reducing False Positives

### 1. Refine Configuration

```yaml
# Make rules more specific
rules:
  SQL_INJECTION:
    pattern: "SELECT.*WHERE.*\\+.*user"  # More specific
    exclude:
      - "WHERE id = \\d+"  # Allow numeric IDs
```

### 2. Exclude Patterns

```yaml
exclude_patterns:
  # Test files
  - "**/*Test.java"
  - "**/*Tests.ts"

  # Generated files
  - "**/generated/**"
  - "**/build/**"
```

### 3. Adjust Severity

```yaml
# Downgrade to reduce noise
severity:
  TOO_MANY_PARAMS:
    default: MEDIUM
    files:
      "controllers/**": LOW  # Controllers need many params
```

## Analyzing False Positives

### Review Periodically

```bash
# Weekly false positive report
curl -H "Authorization: Bearer $TOKEN" \
  https://api.pullwise.ai/api/analytics/reports/false-positives?days=7
```

### Top False Positive Rules

```yaml
# Most common false positives
top_fp_rules:
  1. MISSING_JAVADOC (45%)
  2. TOO_MANY_PARAMS (23%)
  3. LONG_METHOD (15%)
  4. UNUSED_IMPORT (10%)
```

### Action Items

For high false positive rules:

1. Review rule configuration
2. Adjust threshold
3. Update pattern
4. Consider disabling

## Reporting False Positives

### Help Us Improve

Report persistent false positives:

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/feedback/false-positive \
  -d '{
    "rule": "TOO_MANY_PARAMS",
    "code": "public User create(String name, String email, String password, boolean active, int age)",
    "reason": "Constructor requires all fields",
    "suggestedImprovement": "Exclude constructors from this rule"
  }'
```

## Best Practices

### 1. Review Before Marking

Make sure it's truly a false positive:

1. Read the issue description
2. Look at the code context
3. Understand the rule intent
4. Mark only if confident

### 2. Add Clear Reasons

Help others understand:

```yaml
reason: "This is intentionally simple for demo purposes"
```

### 3. Review Periodically

False positives may become real issues:

```yaml
# Review false positives every 6 months
review_fp_every: 180 days
```

### 4. Use Bulk Actions

For similar issues, mark in bulk:

```yaml
# Select all "missing javadoc" in test files
bulk_mark:
  rule: MISSING_JAVADOC
  files: "**/test/**"
  reason: "Test files don't require javadoc"
```

## Troubleshooting

### Can't Mark False Positive

**Problem**: Button is disabled

**Solutions**:

1. Check if review is completed
2. Verify you have permission
3. Ensure issue isn't already marked

### False Positive Reappears

**Problem**: Same issue appears again

**Solutions**:

1. Check if pattern changed
2. Verify rule wasn't reset
3. Ensure learning threshold was met

## Next Steps

- [Severity Levels](/docs/user-guide/reviews/severity-levels) - Understand severity
- [Understanding Results](/docs/user-guide/reviews/understanding-results) - Interpret results
- [Configuration](/docs/getting-started/configuration) - Customize rules
