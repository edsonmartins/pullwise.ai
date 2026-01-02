# Community Edition

Everything about Pullwise Community Edition (MIT License).

## Overview

Pullwise Community Edition is **free forever** under the MIT license. It provides core code review functionality for small teams.

## Features

| Feature | Community Edition |
|---------|-------------------|
| **Price** | **Free** |
| **License** | MIT |
| **Users** | 5 |
| **Organizations** | 1 |
| **Reviews/month** | 500 |
| **Projects** | 3 |
| **Analysis Passes** | 2 (SAST + LLM) |
| **Code Graph** | ❌ |
| **SSO/SAML** | ❌ |
| **RBAC** | ❌ |
| **Priority Support** | ❌ |
| **Source Access** | Core only |

## Included Functionality

### SAST Tools

- **SonarQube** - Bugs, vulnerabilities, code smells
- **ESLint** - JavaScript/TypeScript linting
- **Checkstyle** - Java code style
- **PMD** - Anti-patterns
- **SpotBugs** - Bug patterns

### LLM Integration

- **OpenRouter** - GPT-4, Claude, Gemini
- **Ollama** - Local models (Llama, Mistral)
- **Model Routing** - Cost optimization
- **Fallback** - Graceful degradation

### Core Features

- ✅ Webhook integration (GitHub, GitLab, BitBucket)
- ✅ Pull request comments
- ✅ Issue tracking
- ✅ False positive marking
- ✅ Basic auto-fix
- ✅ REST API access
- ✅ Docker deployment
- ✅ Community support

## Limitations

### User Limits

- **5 users** per organization
- **1 organization** maximum
- **3 projects** per organization

### Review Limits

- **500 reviews/month** soft limit
- **2-pass analysis** (no code graph context)
- **Priority queue** behind paid tiers

### Feature Limitations

- ❌ Code graph analysis
- ❌ Custom LLM fine-tuning
- ❌ SAML SSO
- ❌ Advanced RBAC
- ❌ Priority support
- ❌ Enterprise SLA

## Installation

### Docker (Recommended)

```bash
# Download docker-compose.yml
curl -LO https://pullwise.ai/docker-compose.yml

# Start Pullwise
docker-compose up -d

# Access at http://localhost:3000
```

### Manual Installation

See [Installation Guide](/docs/category/installation) for:
- [Docker Installation](/docs/getting-started/installation/docker)
- [Kubernetes Installation](/docs/getting-started/installation/kubernetes)
- [Manual Installation](/docs/getting-started/installation/manual)

## Configuration

### Minimal Configuration

```yaml
# application.yml
spring:
  profiles:
    active: ce
  datasource:
    url: jdbc:postgresql://localhost:5432/pullwise
    username: pullwise
    password: your_password

pullwise:
  edition: COMMUNITY
  llm:
    openrouter:
      api-key: your-openrouter-key
    default-model: openai/gpt-3.5-turbo
```

### Disable CE Watermark

The CE edition displays a small watermark. To remove:

```yaml
pullwise:
  edition: COMMUNITY
  watermark: false  # Requires valid MIT attribution
```

:::caution
MIT license requires attribution when removing水印.
:::

## Upgrading

### to Professional

See [Upgrading Editions](/docs/administration/editions/upgrading).

Benefits:
- 50 users
- 3 organizations
- Unlimited projects
- 4-pass analysis with code graph
- Basic SSO
- Email support

### to Enterprise

See [Upgrading Editions](/docs/administration/editions/upgrading).

Benefits:
- Unlimited users
- Unlimited organizations
- SAML SSO
- Advanced RBAC
- Priority support
- 4-hour SLA

## MIT License

This edition is licensed under the MIT License:

```
MIT License

Copyright (c) 2024 Pullwise

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Attribution

If you modify the CE edition, you must:

1. Retain the copyright notice
2. Include the MIT license
3. Clearly indicate changes made

## Support

### Community Support

- **Discord**: [Join Community](https://discord.gg/pullwise)
- **GitHub Issues**: [Report Issues](https://github.com/integralltech/pullwise-ai/issues)
- **Documentation**: [docs.pullwise.ai](https://docs.pullwise.ai)

### Commercial Support

For priority support, consider upgrading to:
- **Professional**: Email support during business hours
- **Enterprise**: 24/7 support with 4-hour SLA

## Plugin Ecosystem

All 200+ community plugins work with CE:

- Language linters (Rust, Go, Python, PHP)
- Framework-specific rules
- Custom checks
- [View Plugins](https://pullwise.ai/plugins)

## Frequently Asked Questions

**Is CE really free forever?**

Yes! The MIT license grants you the right to use CE indefinitely.

**Are there any hidden costs?**

No. You only pay for:
- Your own infrastructure (servers, cloud hosting)
- LLM API usage (OpenRouter, etc.)

**Can I use CE commercially?**

Yes! The MIT license permits commercial use.

**What happens if I exceed the limits?**

CE will continue to work but may display warnings. Consider upgrading for higher limits.

**Can I remove the attribution?**

Yes, if you comply with the MIT license terms.

## Related Docs

- [Professional Edition](/docs/administration/editions/professional) - Professional features
- [Enterprise Edition](/docs/administration/editions/enterprise) - Enterprise features
- [Upgrading](/docs/administration/editions/upgrading) - How to upgrade
