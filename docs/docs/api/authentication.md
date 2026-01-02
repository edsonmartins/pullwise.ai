# Authentication

Learn how to authenticate with the Pullwise API.

## Overview

Pullwise uses **GitHub OAuth2** for authentication and **JWT (JSON Web Tokens)** for API access.

## Authentication Flow

### 1. Initiate OAuth

```bash
# Redirect user to GitHub authorization URL
GET https://github.com/login/oauth/authorize?
  client_id={GITHUB_CLIENT_ID}&
  redirect_uri={CALLBACK_URL}&
  scope=read:user,user:email
```

### 2. Handle Callback

After user authorizes, GitHub redirects to your callback URL with a code:

```bash
{CALLBACK_URL}?code={AUTHORIZATION_CODE}
```

### 3. Exchange Code for Token

```bash
curl -X POST \
  https://api.pullwise.ai/api/auth/callback/github?code={AUTHORIZATION_CODE}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "avatarUrl": "https://avatars.githubusercontent.com/u/123"
  }
}
```

## Using JWT Tokens

Include the access token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  https://api.pullwise.ai/api/reviews
```

## Token Lifetime

| Token Type | Lifetime |
|------------|----------|
| Access Token | 24 hours |
| Refresh Token | 30 days |

## Refresh Token

Before your access token expires, refresh it:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  https://api.pullwise.ai/api/auth/refresh \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

## Logout

Invalidate the current session:

```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.pullwise.ai/api/auth/logout
```

:::note
Logout is client-side only - tokens remain valid until expiration.
:::

## Token Structure

JWT tokens contain three parts separated by dots:

```
HEADER.PAYLOAD.SIGNATURE
```

### Payload

```json
{
  "sub": "123",
  "email": "john@example.com",
  "name": "John Doe",
  "iat": 1704067200,
  "exp": 1704153600
}
```

| Field | Description |
|-------|-------------|
| `sub` | User ID |
| `email` | User email |
| `name` | User name |
| `iat` | Issued at (Unix timestamp) |
| `exp` | Expiration (Unix timestamp) |

## Error Handling

### Invalid Token

```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer error="invalid_token"
```

### Expired Token

```json
{
  "error": "Unauthorized",
  "message": "Token expired"
}
```

**Solution:** Refresh your token using the refresh endpoint.

## Security Best Practices

### 1. Store Tokens Securely

```javascript
// Browser: Use httpOnly cookies or session storage
// Never store in localStorage for production

// Server: Use secure environment variables
process.env.JWT_SECRET
```

### 2. Use HTTPS

Always use HTTPS in production:

```bash
# Good
https://api.pullwise.ai/api/reviews

# Bad
http://api.pullwise.ai/api/reviews
```

### 3. Validate Tokens

Always validate tokens on the server:

```java
// Spring Boot example
SecurityContextHolder.getContext().getAuthentication();
```

```javascript
// Node.js example
const jwt = require('jsonwebtoken');
const decoded = jwt.verify(token, SECRET);
```

### 4. Implement Token Refresh

Refresh tokens before expiration:

```javascript
// Check if token expires in less than 5 minutes
if (tokenExp - now < 300000) {
  await refreshToken();
}
```

### 5. Handle Logout Properly

Clear tokens from storage on logout:

```javascript
function logout() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  window.location.href = '/login';
}
```

## Demo User (Development Only)

For development without OAuth:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/auth/login \
  -d '{
    "email": "demo@pullwise.ai",
    "password": "demo123"
  }'
```

:::warning
Never use demo credentials in production!
:::

## Configuration

### GitHub OAuth App

1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Create a new OAuth App:
   - **Application name**: Pullwise
   - **Homepage URL**: `https://pullwise.example.com`
   - **Authorization callback URL**: `https://api.pullwise.example.com/api/auth/callback/github`
3. Copy the **Client ID** and generate a **Client Secret**

### Environment Variables

```bash
# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# JWT Secret (min 256 bits)
JWT_SECRET=your_very_long_random_secret_key_here
```

### Spring Security Configuration

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user,user:email
```

## SDK Examples

### JavaScript

```javascript
import { PullwiseClient } from '@pullwise/javascript-sdk';

const client = new PullwiseClient({
  baseURL: 'https://api.pullwise.ai',
  accessToken: 'your_jwt_token'
});

// Auto-refresh on 401
client.on('tokenExpired', async () => {
  await client.refreshToken();
});
```

### Python

```python
from pullwise import PullwiseClient

client = PullwiseClient(
    base_url='https://api.pullwise.ai',
    access_token='your_jwt_token'
)

# Auto-refresh
client.auto_refresh = True
```

## Troubleshooting

### Token Not Working

1. Check token expiration:
```bash
# Decode JWT to check exp claim
echo "YOUR_TOKEN" | cut -d. -f2 | base64 -d
```

2. Verify token format:
```bash
# Should be: Bearer TOKEN
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.pullwise.ai/api/auth/me
```

### Refresh Token Fails

If refresh token is expired, user must re-authenticate:

```javascript
try {
  await refreshToken();
} catch (error) {
  // Redirect to login
  window.location.href = '/login';
}
```

## Next Steps

- [API Overview](/docs/api/overview) - API reference
- [Webhooks](/docs/api/webhooks) - Webhook configuration
- [Configuration](/docs/getting-started/configuration) - App configuration
