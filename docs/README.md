# Pullwise Documentation

Official documentation for [Pullwise](https://pullwise.ai) - The Open Code Review Platform.

[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![Docusaurus](https://img.shields.io/badge/Docusaurus-3.5-blue.svg)](https://docusaurus.io/)

## What is Pullwise?

Pullwise is an open-source, self-hosted AI code review platform that combines static analysis (SAST) with large language models (LLMs) to provide intelligent, automated code reviews.

### Key Features

- **Hybrid SAST + AI** - Combines static analysis tools with LLMs for comprehensive code reviews
- **Auto-Fix** - One-click apply AI-generated fixes with preview and rollback support
- **Multi-Model LLM** - Support for GPT-4, Claude, Gemini, and local models via Ollama
- **Plugin System** - 200+ community plugins for custom linters and security rules
- **GitHub Integration** - Seamless integration with GitHub webhooks for automatic PR reviews
- **Enterprise Ready** - SAML SSO, RBAC, audit logs, and dedicated support available

### Architecture

![Pullwise Architecture](../images/arquitetura.png)

## This Documentation Project

This is the official documentation website built with [Docusaurus](https://docusaurus.io/).

### Tech Stack

- **Docusaurus 3.5+** - Static site generator
- **React 18** - UI components
- **TypeScript** - Type-safe development
- **Mermaid** - Diagram rendering
- **Infima** - CSS styling framework

## Quick Start

### Prerequisites

- Node.js 18+ and npm/yarn/pnpm

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run start
```

Open [http://localhost:3000](http://localhost:3000) to view the documentation.

### Build for Production

```bash
# Build static files
npm run build

# Serve production build locally
npm run serve
```

## Project Structure

```
docs/
├── docusaurus.config.ts    # Main configuration
├── sidebars.ts              # Sidebar navigation
├── package.json             # Dependencies and scripts
├── src/
│   ├── css/
│   │   └── custom.css      # Custom styles
│   └── pages/
│       └── index.tsx       # Homepage
├── static/                  # Static assets (images, etc.)
└── docs/                    # Documentation content
    ├── getting-started/     # Quick start and installation
    ├── user-guide/          # User documentation
    ├── developer-guide/     # Contributor documentation
    ├── plugin-development/  # Plugin API reference
    ├── api/                 # API reference
    ├── deployment/          # Deployment guides
    └── administration/      # Admin documentation
```

## Documentation Sections

### Getting Started
- Introduction to Pullwise
- Quick start guide (5 minutes setup)
- Installation options (Docker, Kubernetes, Manual)
- First review walkthrough
- Configuration and troubleshooting

### User Guide
- Creating and managing projects
- Connecting repositories (GitHub, GitLab, BitBucket)
- Configuring webhooks
- Understanding review results
- Auto-fix feature
- Integrations (Jira, Linear, Slack)
- Analytics and metrics

### Developer Guide
- Architecture overview
- Local development setup
- Backend (Java 17, Spring Boot, Maven)
- Frontend (React, Vite, TypeScript)
- Contributing workflow
- Code style and testing

### Plugin Development
- Plugin architecture
- Plugin types (SAST, Linter, Security, Custom LLM)
- API reference
- Examples and tutorials
- Packaging and publishing

### API Reference
- REST API documentation
- Authentication (JWT)
- Webhook endpoints

### Deployment
- Docker deployment
- Kubernetes/Helm charts
- Environment configuration
- Security (SSL, secrets)
- Monitoring (Prometheus, Grafana, Jaeger)

### Administration
- User management
- Security (RBAC, SAML SSO)
- Maintenance (backups, migrations)
- Edition comparison (Community, Professional, Enterprise)

## Development

### Adding New Content

1. Create a new markdown file in the appropriate `docs/` subdirectory
2. Add the file to `sidebars.ts`
3. Run `npm run start` to preview

### Custom Styling

Edit `src/css/custom.css` to modify the site's appearance. The project uses Docusaurus's [Infima](https://infima.dev/) CSS framework with custom purple theme variables.

### Internationalization

The documentation supports English, Portuguese (pt), and Spanish (es). Translations are located in the `i18n/` directory.

## Deployment

### Docker

```bash
# Build documentation container
docker build -t pullwise-docs .

# Run documentation server
docker run -p 80:80 pullwise-docs
```

### Docker Compose

```bash
docker-compose -f docker-compose.docs.yml up -d
```

The documentation will be available at [http://localhost:8080](http://localhost:8080).

## Contributing

We welcome contributions! Please see the [Contributing Guide](https://docs.pullwise.ai/docs/developer-guide/contributing/workflow) for details.

## Links

- **Main Website**: [https://pullwise.ai](https://pullwise.ai)
- **Documentation**: [https://docs.pullwise.ai](https://docs.pullwise.ai)
- **GitHub**: [https://github.com/integralltech/pullwise-ai](https://github.com/integralltech/pullwise-ai)
- **Discord**: [https://discord.gg/pullwise](https://discord.gg/pullwise)

## License

This documentation is licensed under the MIT License - see the [LICENSE](https://github.com/integralltech/pullwise-ai/blob/main/LICENSE) file for details.

---

**Pullwise** - The Open Code Review Platform
