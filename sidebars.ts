import type { SidebarsConfig } from '@docusaurus/plugin-content-docs'

const sidebars: SidebarsConfig = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      collapsible: true,
      collapsed: false,
      items: [
        'getting-started/intro',
        'getting-started/quick-start',
        {
          type: 'category',
          label: 'Installation',
          collapsible: true,
          collapsed: true,
          items: [
            'getting-started/installation/docker',
            'getting-started/installation/kubernetes',
            'getting-started/installation/manual',
            'getting-started/installation/requirements',
            'getting-started/installation/webhooks',
          ],
        },
        'getting-started/first-review',
        'getting-started/configuration',
        'getting-started/troubleshooting',
      ],
    },
    {
      type: 'category',
      label: 'User Guide',
      collapsible: true,
      collapsed: true,
      items: [
        {
          type: 'category',
          label: 'Projects',
          collapsible: true,
          collapsed: true,
          items: [
            'user-guide/projects/creating-projects',
            'user-guide/projects/repositories',
            'user-guide/projects/webhooks',
          ],
        },
        {
          type: 'category',
          label: 'Reviews',
          collapsible: true,
          collapsed: true,
          items: [
            'user-guide/reviews/triggering-reviews',
            'user-guide/reviews/understanding-results',
            'user-guide/reviews/severity-levels',
            'user-guide/reviews/false-positives',
          ],
        },
        {
          type: 'category',
          label: 'Auto-Fix',
          collapsible: true,
          collapsed: true,
          items: [
            'user-guide/autofix/overview',
            'user-guide/autofix/applying-fixes',
            'user-guide/autofix/rollback',
          ],
        },
        {
          type: 'category',
          label: 'Integrations',
          collapsible: true,
          collapsed: true,
          items: [
            'user-guide/integrations/jira',
            'user-guide/integrations/linear',
            'user-guide/integrations/slack',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Developer Guide',
      collapsible: true,
      collapsed: true,
      items: [
        'developer-guide/overview',
        {
          type: 'category',
          label: 'Setup',
          collapsible: true,
          collapsed: true,
          items: [
            'developer-guide/setup/backend',
            'developer-guide/setup/frontend',
          ],
        },
        {
          type: 'category',
          label: 'Architecture',
          collapsible: true,
          collapsed: true,
          items: [
            'developer-guide/architecture/backend-architecture',
          ],
        },
        {
          type: 'category',
          label: 'Contributing',
          collapsible: true,
          collapsed: true,
          items: [
            'developer-guide/contributing/workflow',
            'developer-guide/contributing/code-style',
            'developer-guide/contributing/testing',
            'developer-guide/contributing/documentation',
            'developer-guide/contributing/pull-requests',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Plugin Development',
      collapsible: true,
      collapsed: true,
      items: [
        'plugin-development/overview',
        'plugin-development/getting-started',
        {
          type: 'category',
          label: 'Plugin Types',
          collapsible: true,
          collapsed: true,
          items: [
            'plugin-development/plugin-types/sast',
          ],
        },
        {
          type: 'category',
          label: 'API Reference',
          collapsible: true,
          collapsed: true,
          items: [
            'plugin-development/api-reference/code-review-plugin',
            'plugin-development/api-reference/abstract-plugin',
            'plugin-development/api-reference/plugin-context',
            'plugin-development/api-reference/analysis-request',
            'plugin-development/api-reference/analysis-result',
          ],
        },
        {
          type: 'category',
          label: 'Examples',
          collapsible: true,
          collapsed: true,
          items: [
            'plugin-development/examples/simple-linter',
            'plugin-development/examples/rust-tool',
            'plugin-development/examples/config-plugin',
            'plugin-development/examples/external-tool',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      collapsible: true,
      collapsed: true,
      items: ['api/overview', 'api/authentication', 'api/webhooks'],
    },
    {
      type: 'category',
      label: 'Deployment',
      collapsible: true,
      collapsed: true,
      items: [
        {
          type: 'category',
          label: 'Docker',
          collapsible: true,
          collapsed: true,
          items: [
            'deployment/docker/production',
            'deployment/docker/swarm',
          ],
        },
        {
          type: 'category',
          label: 'Kubernetes',
          collapsible: true,
          collapsed: true,
          items: [
            'deployment/kubernetes/helm',
            'deployment/kubernetes/manifests',
            'deployment/kubernetes/scaling',
          ],
        },
        {
          type: 'category',
          label: 'Environments',
          collapsible: true,
          collapsed: true,
          items: [
            'deployment/environments/application-yml',
          ],
        },
        {
          type: 'category',
          label: 'Security',
          collapsible: true,
          collapsed: true,
          items: [
            'deployment/security/ssl-https',
            'deployment/security/secrets',
            'deployment/security/firewall',
          ],
        },
        {
          type: 'category',
          label: 'Monitoring',
          collapsible: true,
          collapsed: true,
          items: [
            'deployment/monitoring/prometheus',
            'deployment/monitoring/grafana',
            'deployment/monitoring/jaeger',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Administration',
      collapsible: true,
      collapsed: true,
      items: [
        {
          type: 'category',
          label: 'Users',
          collapsible: true,
          collapsed: true,
          items: [
            'administration/users/managing-users',
          ],
        },
        {
          type: 'category',
          label: 'Security',
          collapsible: true,
          collapsed: true,
          items: [
            'administration/security/rbac',
          ],
        },
        {
          type: 'category',
          label: 'Maintenance',
          collapsible: true,
          collapsed: true,
          items: [
            'administration/maintenance/backups',
            'administration/maintenance/migrations',
            'administration/maintenance/updates',
            'administration/maintenance/monitoring',
          ],
        },
        {
          type: 'category',
          label: 'Editions',
          collapsible: true,
          collapsed: true,
          items: [
            'administration/editions/community-edition',
            'administration/editions/professional',
            'administration/editions/enterprise',
            'administration/editions/upgrading',
          ],
        },
      ],
    },
  ],
}

export default sidebars
