# Documentation Guide

How to write and maintain documentation.

## Overview

Good documentation is crucial for project success. This guide covers how to contribute to Pullwise documentation.

## Documentation Structure

```
docs/
├── docs/                    # Main content
│   ├── getting-started/
│   ├── user-guide/
│   ├── developer-guide/
│   ├── plugin-development/
│   ├── api/
│   ├── deployment/
│   └── administration/
├── i18n/                    # Translations
│   ├── pt/
│   └── es/
├── src/                     # Customization
│   ├── css/
│   └── theme/
├── docusaurus.config.ts     # Configuration
└── sidebars.ts              # Navigation
```

## Writing Documentation

### Markdown Best Practices

```markdown
# Headings

Use ATX-style headings (#) for consistency.

## Sections

Organize content into logical sections.

### Subsections

Break down complex topics.

---

### Code Blocks

Specify language for syntax highlighting:

\```java
public class Example {
    private String name;
}
\```

\```bash
npm install
\```

### Admonitions

Use for important information:

:::note
Useful information that readers should know.
:::

:::tip
Helpful suggestions for better results.
:::

:::warning
Important warnings about potential issues.
:::

:::danger
Critical warnings that could cause data loss.
:::

### Tables

Use for structured data:

| Feature | Community | Pro | Enterprise |
|---------|-----------|-----|------------|
| SAST    | ✅        | ✅  | ✅         |
| LLM     | ✅        | ✅  | ✅         |
| SSO     | ❌        | ✅  | ✅         |
```

### Frontmatter

Every documentation file should have frontmatter:

```markdown
---
title: Page Title
description: Brief description for SEO and search
sidebar_label: Short Label
sidebar_position: 1
---

# Page Title

Content here...
```

## Docusaurus Features

### Mermaid Diagrams

```markdown
\```mermaid
graph LR
    A[Start] --> B[Process]
    B --> C[End]
\```
```

### Tabs

```markdown
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs>
  <TabItem value="docker" label="Docker" default>
    Docker instructions here...
  </TabItem>
  <TabItem value="kubernetes" label="Kubernetes">
    Kubernetes instructions here...
  </TabItem>
</Tabs>
```

### Code Tabs

```markdown
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs groupId="language">
  <TabItem value="java" label="Java">
    \```java
    System.out.println("Hello");
    \```
  </TabItem>
  <TabItem value="typescript" label="TypeScript">
    \```typescript
    console.log("Hello");
    \```
  </TabItem>
</Tabs>
```

### Inline Code with Variables

```markdown
Use the `--token` flag with your `TOKEN` variable.

export const TOKEN = 'your-api-token';
```

## Content Guidelines

### Target Audience

Different sections serve different audiences:

| Section | Audience | Tone |
|---------|----------|------|
| Getting Started | New users | Friendly, step-by-step |
| User Guide | End users | Practical, task-oriented |
| Developer Guide | Contributors | Technical, detailed |
| Plugin Development | Plugin devs | Reference-style |
| API Reference | API users | Precise, structured |
| Deployment | DevOps | Operational |
| Administration | Admins | Security-focused |

### Style Guidelines

1. **Be concise** - Get to the point quickly
2. **Be accurate** - Test code examples
3. **Be consistent** - Use same terminology
4. **Be helpful** - Anticipate questions

### Writing Steps

```markdown
## Task Name

Brief description of what we're doing.

### Prerequisites

- Requirement 1
- Requirement 2

### Steps

1. **First step** - What we're doing and why
   ```bash
   command here
   ```

2. **Second step** - Continue the process
   ```bash
   another command
   ```

### Verification

How to verify it worked:

```bash
# Expected output
Success!
```

### Troubleshooting

Common issues and solutions.
```

## API Documentation

### OpenAPI Specification

API docs use Scalar for OpenAPI rendering:

```yaml
# docs/api/openapi.yaml
openapi: 3.0.3
info:
  title: Pullwise API
  version: 1.0.0
paths:
  /api/reviews:
    get:
      summary: List reviews
      responses:
        '200':
          description: Success
```

### API Page Format

```markdown
---
title: Reviews API
description: API for managing code reviews
---

# Reviews API

Operations for creating and managing code reviews.

## List Reviews

\`GET /api/reviews\`

Returns all reviews for a project.

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| projectId | integer | Yes | Project ID |
| status | string | No | Filter by status |

### Response

\```json
{
  "data": [...],
  "total": 10
}
\```
```

## Code Comments

### JavaDoc

```java
/**
 * Service for managing code reviews.
 *
 * <p>This service handles the creation, execution, and
 * management of code reviews including SAST and LLM
 * analysis passes.
 *
 * @author Pullwise Team
 * @since 1.0.0
 */
@Service
public class ReviewService {

    /**
     * Creates a new code review.
     *
     * @param request the review request, must not be {@code null}
     * @return the created review with generated ID
     * @throws ValidationException if request is invalid
     * @throws PluginException if plugin initialization fails
     */
    public Review create(CreateReviewRequest request) {
        // Implementation
    }
}
```

### JSDoc

```typescript
/**
 * Hook for fetching reviews.
 *
 * @param options - Configuration options
 * @param options.projectId - The project ID to fetch reviews for
 * @param options.status - Optional status filter
 * @returns Reviews state with data, loading, and error properties
 *
 * @example
 * ```ts
 * const { reviews, loading } = useReviews({ projectId: 1 });
 * ```
 */
export function useReviews(
  options: UseReviewsOptions
): UseReviewsResult {
  // Implementation
}
```

## Localization

### Adding Translations

1. Create translated file in `i18n/<locale>/`:

```bash
docs/i18n/pt/docusaurus-plugin-content-docs/current/
├── getting-started/
│   └── intro.md
```

2. Translate content maintaining structure:

```markdown
---
title: Introdução
---

# Bem-vindo ao Pullwise

Tradução do conteúdo aqui...
```

3. Update locale in `docusaurus.config.ts` if needed:

```typescript
i18n: {
  defaultLocale: 'en',
  locales: ['en', 'pt', 'es'],
}
```

### Translation Guidelines

- Maintain markdown structure
- Keep code examples unchanged
- Translate frontmatter fields
- Preserve formatting and links

## Previewing Changes

### Local Development

```bash
# Install dependencies
npm install

# Start dev server
npm start

# Build for production
npm build

# Serve built files
npm serve
```

### Check Links

```bash
# Check for broken links
npm run docs:check-links
```

### Format Markdown

```bash
# Format markdown files
npm run docs:format
```

## Versioning Documentation

### Versioned Docs

When releasing a new version:

```bash
# Tag new version
npm run docusaurus docs:version 1.1.0
```

This creates a versioned copy of documentation at `/docs/1.1.0/`

### Current Version

Current documentation is always at `/docs/next/` or `/docs/`

### Version Badge

Add version badge to indicate compatibility:

```markdown
:::info
This feature is available in version 1.1.0 and later.
:::
```

## Diagram Guidelines

### Architecture Diagrams

Use Mermaid for architecture diagrams:

```markdown
\```mermaid
graph TB
    subgraph "Frontend"
        UI[React App]
    end

    subgraph "Backend"
        API[REST API]
        SVC[Review Service]
    end

    UI --> API
    API --> SVC
\```
```

### Sequence Diagrams

For showing interactions:

```markdown
\```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend

    User->>Frontend: Click "Trigger Review"
    Frontend->>Backend: POST /api/reviews
    Backend-->>Frontend: 201 Created
\```
```

## Search Optimization

### Keywords

Add keywords to frontmatter:

```markdown
---
title: Creating Projects
description: How to create and configure projects in Pullwise
keywords:
  - project setup
  - configuration
  - repository
---
```

### Internal Links

Use descriptive link text:

```markdown
[See User Guide](/docs/user-guide/overview) for more details.

Learn about [authentication](/docs/api/authentication) in the API docs.
```

### External References

```markdown
For more on Spring Boot, see the [official documentation](https://spring.io/projects/spring-boot).
```

## Review Process

### Documentation PR Checklist

- [ ] Links work correctly
- [ ] Code examples tested
- [ ] Spelling and grammar checked
- [ ] Frontmatter complete
- [ ] Screenshots included (if applicable)
- [ ] Translation files updated (if needed)

### Peer Review

All documentation changes should be reviewed:

1. Technical accuracy - Is the information correct?
2. Clarity - Is it easy to understand?
3. Completeness - Is anything missing?
4. Formatting - Is it consistent?

## Common Tasks

### Adding New Page

1. Create markdown file in appropriate directory
2. Add frontmatter
3. Write content
4. Add to `sidebars.ts` if needed
5. Test locally

### Adding Code Example

1. Write and test code
2. Add syntax highlighting
3. Include output/result
4. Add comments for clarity

### Updating Diagram

1. Edit Mermaid code
2. Verify syntax
3. Test in dev server
4. Check mobile rendering

## Next Steps

- [Pull Requests](/docs/developer-guide/contributing/pull-requests) - PR guide
- [Workflow](/docs/developer-guide/contributing/workflow) - Git workflow
- [Testing](/docs/developer-guide/contributing/testing) - Testing guidelines
