import { themes } from 'prism-react-renderer'
import type { Config } from '@docusaurus/types'

const config: Config = {
  title: 'Pullwise Documentation',
  tagline: 'The Open Code Review Platform',
  favicon: 'img/favicon.svg',

  url: 'https://docs.pullwise.ai',
  baseUrl: '/',

  organizationName: 'integralltech',
  projectName: 'pullwise-ai',

  themes: ['@docusaurus/theme-mermaid'],

  markdown: {
    mermaid: true,
  },

  // i18n: {
  //   defaultLocale: 'en',
  //   locales: ['en', 'pt', 'es'],
  //   localeConfigs: {
  //     en: {
  //       label: 'English',
  //     },
  //     pt: {
  //       label: 'Português',
  //     },
  //     es: {
  //       label: 'Español',
  //     },
  //   },
  // },

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  onDuplicateRoutes: 'warn',

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/integralltech/pullwise-ai/tree/main/docs/',
          versions: {
            current: {
              label: 'v1.0',
            },
          },
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
        sitemap: {
          changefreq: 'weekly',
          priority: 0.5,
        },
      }),
    ],
  ],

  // themes: [
  //   [
  //     '@scalar/docusaurus',
  //     {
  //       url: '/api/openapi.yaml',
  //     },
  //   ],
  // ],

  themeConfig: {
    navbar: {
      title: 'Pullwise Docs',
      logo: {
        alt: 'Pullwise Logo',
        src: 'img/logo.svg',
        srcDark: 'img/logo-dark.svg',
        width: 32,
        height: 32,
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          type: 'doc',
          docId: 'api/overview',
          position: 'left',
          label: 'API Reference',
        },
        {
          href: 'https://github.com/integralltech/pullwise-ai',
          position: 'right',
          className: 'header-github-link',
          'aria-label': 'GitHub repository',
        },
        // {
        //   type: 'localeDropdown',
        //   position: 'right',
        // },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/category/getting-started',
            },
            {
              label: 'User Guide',
              to: '/docs/category/user-guide',
            },
            {
              label: 'Developer Guide',
              to: '/docs/category/developer-guide',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Discord',
              href: 'https://discord.gg/pullwise',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/integralltech/pullwise-ai',
            },
            {
              label: 'Twitter',
              href: 'https://twitter.com/pullwise',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Homepage',
              href: 'https://pullwise.ai',
            },
            {
              label: 'Blog',
              href: 'https://pullwise.ai/blog',
            },
          ],
        },
      ],
      logo: {
        alt: 'Pullwise Owl',
        src: 'img/owl-icon.svg',
        width: 48,
        height: 48,
      },
      copyright: `Copyright © ${new Date().getFullYear()} Pullwise. MIT Licensed.`,
    },
    prism: {
      theme: themes.oneLight,
      darkTheme: themes.oneDark,
      additionalLanguages: [
        'java',
        'typescript',
        'javascript',
        'bash',
        'yaml',
        'json',
        'kotlin',
        'python',
        'go',
        'rust',
      ],
      magicComments: [
        {
          className: 'theme-code-block-highlighted-line',
          line: 'highlight-next-line',
          block: { start: 'highlight-start', end: 'highlight-end' },
        },
      ],
    },
    algolia: {
      appId: 'YOUR_APP_ID',
      apiKey: 'YOUR_API_KEY',
      indexName: 'pullwise',
      contextualSearch: true,
      searchParameters: {},
      searchPagePath: 'search',
    },
    docs: {
      sidebar: {
        hideable: true,
        autoCollapseCategories: true,
      },
    },
  } satisfies Preset.ThemeConfig,

  plugins: [],
}

export default config
