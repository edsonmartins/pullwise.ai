# Webhooks

Configure webhooks for real-time notifications from Pullwise.

## Overview

Pullwise can send webhook notifications when events occur in your projects:

- **Review completed** - Review finished successfully
- **Review failed** - Review encountered an error
- **Issue detected** - New issue found during review

## GitHub Integration

Pullwise integrates directly with GitHub via webhooks.

### Configure GitHub Webhook

1. Navigate to your repository **Settings** → **Webhooks** → **Add webhook**
2. Configure:

   ```yaml
   Payload URL: https://your-server.com/webhooks/github
   Content type: application/json
   Secret: (optional) your_webhook_secret
   Events:
     - Pull requests
     - Pull request reviews
   ```

3. Click **Add webhook**

### Webhook Payload

Pullwise posts to GitHub when review is complete:

```json
{
  "review_id": 123,
  "summary": {
    "total_issues": 12,
    "critical_issues": 1,
    "high_issues": 3,
    "medium_issues": 5,
    "low_issues": 3
  },
  "url": "https://pullwise.example.com/reviews/123"
}
```

## GitLab Integration

Configure GitLab webhook:

1. Navigate to **Settings** → **Webhooks**
2. Configure:

   ```yaml
   URL: https://your-server.com/webhooks/gitlab
   Secret token: (optional) your_webhook_secret
   Trigger: Merge request events
   ```

## BitBucket Integration

Configure BitBucket webhook:

1. Navigate to **Repository settings** → **Webhooks** → **Add webhook**
2. Configure:

   ```yaml
   Title: Pullwise
   URL: https://your-server.com/webhooks/bitbucket
   Triggers: Pull request created/updated
   ```

## Custom Webhooks

Configure custom webhooks for notifications to your systems.

### Create Webhook

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/webhooks \
  -d '{
    "url": "https://your-system.com/webhooks/pullwise",
    "secret": "your_webhook_secret",
    "events": ["review.completed", "review.failed"]
  }'
```

### Webhook Events

| Event | Description | Payload |
|-------|-------------|---------|
| `review.completed` | Review finished successfully | ReviewDTO |
| `review.failed` | Review failed | ReviewDTO + error |
| `issue.detected` | New issue found | IssueDTO |

### Payload Format

All webhook payloads follow this structure:

```json
{
  "eventId": "evt_abc123",
  "eventType": "review.completed",
  "timestamp": "2024-01-01T12:00:00Z",
  "signature": "sha256=...",
  "data": {
    "reviewId": 123,
    "projectId": 456,
    "pullRequestId": "pr-789",
    "status": "COMPLETED",
    "totalIssues": 12,
    "criticalIssues": 1,
    "highIssues": 3,
    "mediumIssues": 5,
    "lowIssues": 3
  }
}
```

## Security

### Verify Signature

Pullwise signs webhook payloads using HMAC-SHA256:

```bash
signature = hmac_sha256(webhook_secret, payload)
```

### Verify Signature (Node.js)

```javascript
const crypto = require('crypto');

function verifySignature(payload, signature, secret) {
  const hmac = crypto.createHmac('sha256', secret);
  hmac.update(payload);
  const digest = hmac.digest('hex');
  const expectedSignature = `sha256=${digest}`;

  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(expectedSignature)
  );
}

// Express example
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-pullwise-signature'];
  const payload = JSON.stringify(req.body);

  if (!verifySignature(payload, signature, WEBHOOK_SECRET)) {
    return res.status(401).send('Invalid signature');
  }

  // Process webhook
  console.log('Webhook received:', req.body);
  res.sendStatus(200);
});
```

### Verify Signature (Python)

```python
import hmac
import hashlib

def verify_signature(payload, signature, secret):
    expected_signature = 'sha256=' + hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()

    return hmac.compare_digest(signature, expected_signature)

# Flask example
@app.route('/webhook', methods=['POST'])
def webhook():
    signature = request.headers.get('X-Pullwise-Signature')
    payload = request.get_data()

    if not verify_signature(payload, signature, WEBHOOK_SECRET):
        return 'Invalid signature', 401

    # Process webhook
    print(f"Webhook received: {request.json}")
    return 'OK', 200
```

### Verify Signature (Java)

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class WebhookVerifier {
    public static boolean verifySignature(
            String payload,
            String signature,
            String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes());
            String expectedSignature = "sha256=" + Hex.encodeHexString(digest);

            return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            return false;
        }
    }
}
```

## Responding to Webhooks

Always respond quickly:

```bash
# Acknowledge receipt
HTTP 200 OK

# Or
HTTP 202 Accepted
```

Process webhooks asynchronously:

```javascript
// Express example
app.post('/webhook', async (req, res) => {
  // Acknowledge immediately
  res.sendStatus(202);

  // Process asynchronously
  processWebhook(req.body).catch(console.error);
});

async function processWebhook(payload) {
  switch (payload.eventType) {
    case 'review.completed':
      await handleReviewCompleted(payload.data);
      break;
    case 'review.failed':
      await handleReviewFailed(payload.data);
      break;
  }
}
```

## Retry Policy

If your webhook endpoint returns a non-2xx status:

| Attempt | Delay |
|---------|-------|
| 1 | Immediate |
| 2 | 1 minute |
| 3 | 5 minutes |
| 4 | 30 minutes |
| 5 | 1 hour |

After 5 failed attempts, the webhook is disabled.

## Best Practices

### 1. Use HTTPS

Always use HTTPS for webhook URLs:

```bash
# Good
https://your-server.com/webhooks/pullwise

# Bad
http://your-server.com/webhooks/pullwise
```

### 2. Verify Signatures

Always verify webhook signatures before processing.

### 3. Respond Quickly

Acknowledge webhook within 5 seconds. Process asynchronously.

### 4. Handle Duplicates

Webhooks may be delivered multiple times. Use `eventId` for deduplication:

```javascript
const processedEvents = new Set();

app.post('/webhook', (req, res) => {
  const { eventId } = req.body;

  if (processedEvents.has(eventId)) {
    return res.sendStatus(200); // Already processed
  }

  processedEvents.add(eventId);
  // Process webhook...
});
```

### 5. Monitor Webhooks

Track webhook delivery status:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.pullwise.ai/api/webhooks/{id}/deliveries
```

## Testing

### Test with ngrok

For local testing:

```bash
# Start ngrok
ngrok http 3000

# Use ngrok URL as webhook URL
https://abc123.ngrok.io/webhooks/pullwise
```

### Test Payload

Send test webhook:

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/webhooks/{id}/test \
  -d '{
    "eventType": "review.completed",
    "test": true
  }'
```

## Troubleshooting

### Webhook Not Received

1. Check webhook URL is accessible
2. Verify server allows inbound traffic
3. Check firewall rules
4. View delivery logs in Pullwise dashboard

### Signature Verification Fails

1. Ensure you're using raw request body
2. Check secret matches configuration
3. Verify timestamp is within tolerance (5 minutes)

### Slow Processing

1. Respond immediately with 202
2. Process asynchronously
3. Use background jobs

## Next Steps

- [API Overview](/docs/api/overview) - API reference
- [Authentication](/docs/api/authentication) - Authentication guide
- [Projects](/docs/user-guide/projects/webhooks) - Project webhook setup
