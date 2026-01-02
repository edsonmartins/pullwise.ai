# Pullwise - The Open Code Review Platform

<div align="center">

  ![Pullwise Logo](https://pullwise.ai/owl-icon.svg)

  **The Open Code Review Platform**

  [![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
  [![Docker](https://img.shields.io/badge/docker-pull-blue.svg)](https://hub.docker.com/r/pullwise/pullwise-ce)
  [![GitHub Stars](https://img.shields.io/github/stars/integralltech/pullwise-ai?style=social)](https://github.com/integralltech/pullwise-ai)

  [Website](https://pullwise.ai) • [Docs](https://docs.pullwise.ai) • [Demo](https://pullwise.ai/demo) • [Discord](https://discord.gg/pullwise)

  **"Start free, scale enterprise."**

  Production-Grade. Free Forever. MIT Licensed.

</div>

---

## 🦉 What is Pullwise?

Pullwise is an **open-source, self-hosted AI code review platform** that combines static analysis (SAST) with large language models (LLMs) to provide intelligent, automated code reviews.

### 🎯 The Problem

Code reviews are essential for software quality, but they're:
- **Time-consuming** - Senior developers spend hours reviewing PRs
- **Inconsistent** - Different reviewers catch different issues
- **Expensive** - Enterprise tools cost thousands per month
- **Vendor lock-in** - Proprietary solutions trap your data

### ✨ The Solution

**Pullwise Community Edition (MIT Licensed):**
- **Free forever** - No credit card, no time limits
- **Self-hosted** - Your code never leaves your infrastructure
- **AI-Powered** - Multi-model LLM support (GPT-4, Claude, local models)
- **SAST Integration** - SonarQube, ESLint, Checkstyle, PMD, SpotBugs
- **Auto-Fix** - One-click apply suggestions
- **200+ Plugins** - Community-driven extensions

---

## 🚀 Quick Start

### 5-Minute Setup with Docker

```bash
# Clone the repository
git clone https://github.com/integralltech/pullwise-ai.git
cd pullwise-ai/frontend

# Download the docker-compose.yml
wget https://pullwise.ai/docker-compose.yml

# Start all services
docker-compose up -d

# Access Pullwise
open http://localhost:8080
```

That's it! Pullwise is now running with:
- PostgreSQL database
- Redis cache
- Ollama (local LLM)
- Pullwise CE

### System Requirements

- **Docker** 20.10+ and Docker Compose 2.0+
- **2 GB RAM** minimum (4 GB recommended)
- **10 GB** disk space
- **Linux**, **macOS**, or **Windows** with WSL2

---

## 💎 Editions

Pullwise follows the **GitLab open-core model**:

| Feature | Community Edition | Professional | Enterprise | Enterprise Plus |
|---------|------------------|-------------|------------|------------------|
| **Price** | **FREE** | $49/dev/mo | $99/dev/mo | $149/dev/mo |
| **License** | MIT | Proprietary | Proprietary | Proprietary |
| **Users** | 5 | 50 | Unlimited | Unlimited |
| **Organizations** | 1 | 3 | Unlimited | Unlimited |
| **Pipeline** | 2-pass | 4-pass | 4-pass | 4-pass |
| **Code Graph** | ❌ | ✅ | ✅ | ✅ |
| **SSO/SAML** | ❌ | ✅ | ✅ | ✅ |
| **RBAC** | ❌ | Basic | Advanced | Advanced |
| **Audit Logs** | ❌ | 30 days | 1 year | Custom |
| **Air-Gapped** | ❌ | ❌ | ✅ | ✅ |
| **SLA** | Community | 48h | 4h | 1h |
| **Source Access** | Core | ❌ | ❌ | ✅ |

[→ Compare all editions](https://pullwise.ai/#comparison)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      PULLWISE PLATFORM                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │   Frontend   │    │   Backend    │    │  PostgreSQL  │ │
│  │   (React)    │◀──▶│ (Spring Boot)│◀──▶│  + pgvector  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         │                    ├──▶ Redis (cache)   │         │
│         │                    ├──▶ RabbitMQ (jobs) │         │
│         │                    └──▶ LLM Providers   │         │
│         │                         ├─ OpenRouter   │         │
│         │                         ├─ Ollama (local)│         │
│         │                         └─ Custom       │         │
│         │                                                    │
│         ▼                                                    │
│  GitHub / GitLab / BitBucket (Webhooks + API)               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Features

### 🔍 Hybrid SAST + AI Reviews

Pullwise combines the best of both worlds:

1. **Static Analysis** (parallel execution):
   - SonarQube (bugs, vulnerabilities, code smells)
   - ESLint (JavaScript/TypeScript)
   - Checkstyle (Java)
   - PMD (anti-patterns)
   - SpotBugs (bug patterns)

2. **AI Review** (with full context):
   - SAST results as baseline
   - Code graph analysis
   - Historical PR data
   - Custom team instructions

3. **Smart Consolidation**:
   - Deduplicates similar issues
   - Prioritizes by severity
   - Formats actionable comments

### 🧠 Multi-Model LLM Router

- **Cloud models**: GPT-4, Claude Sonnet, Gemini Pro via OpenRouter
- **Local models**: Llama 3, Mistral, Gemma via Ollama
- **Cost optimization**: Auto-routes to cheapest model for task
- **Fallback**: Graceful degradation when models fail

### 🔌 Plugin System

200+ community plugins extending:
- Language linters (Rust, Go, Python, PHP)
- Framework-specific rules (Laravel, Django, Spring)
- Custom checks for your codebase
- [→ Plugin Marketplace](https://pullwise.ai/plugins)

### ⚡ Auto-Fix

- One-click apply for AI suggestions
- Safe preview before applying
- Rollback support
- Batch operations

---

## 🌍 Community

Join **10,000+ developers** using Pullwise:

- **5,000+** GitHub Stars
- **10,000+** Docker Pulls
- **200+** Community Plugins
- **1,000+** Discord Members

[→ Join Discord](https://discord.gg/pullwise)

---

## 📖 Documentation

- [Getting Started Guide](https://docs.pullwise.ai/getting-started)
- [Installation Guide](https://docs.pullwise.ai/installation)
- [Configuration](https://docs.pullwise.ai/configuration)
- [Plugin Development](https://docs.pullwise.ai/plugins)
- [API Reference](https://docs.pullwise.ai/api)

---

## 🚢 Deployment

### Docker (Recommended)

```bash
docker-compose up -d
```

### Kubernetes

```bash
kubectl apply -f k8s/community-edition/
```

### Helm Chart

```bash
helm repo add pullwise https://charts.pullwise.ai
helm install pullwise pullwise/pullwise-ce
```

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md).

**Areas where we need help:**
- Language integrations (Rust, Go, Python, PHP)
- Plugin development
- Documentation improvements
- Bug reports and testing
- [→ Good First Issues](https://github.com/integralltech/pullwise-ai/issues?q=label%3A%22good+first+issue%22+is%3Aopen+is%3Aissue)

---

## 📜 License

**Community Edition** - [MIT License](LICENSE)

Free to use, modify, and distribute. Forever.

---

## 🙏 Acknowledgments

Built with inspiration from:
- **GitLab** - Open-core business model
- **SonarQube** - SAST foundation
- **CodeRabbit** - AI review patterns
- **Open Source Community** - Tools and libraries

---

<div align="center">

  **[⬆ Back to Top](#pullwise---the-open-code-review-platform)**

  Made with ❤️ by the Pullwise community

  **pullwise.ai** • [@pullwise](https://twitter.com/pullwise) • [hello@pullwise.ai](mailto:hello@pullwise.ai)

</div>
