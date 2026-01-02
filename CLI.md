# CLI - Pullwise (Command Line Interface)

## 🦉 Pullwise CLI - Wise Reviews from Your Terminal

**Package:** `@pullwise/cli`  
**Aliases:** `pullwise`, `pw`

---

## Visão Geral

CLI para interagir com o Pullwise diretamente do terminal. Permite configurar projetos, triggerar reviews manualmente, visualizar resultados e gerenciar configurações.

**Stack Tecnológica:**
```
TypeScript
Node.js 18+
Commander.js (CLI framework)
Inquirer.js (interactive prompts)
Axios (HTTP client)
Chalk (colors)
Ora (spinners)
Boxen (boxes)
Table (tables)
Cosmiconfig (config management)
```

---

## Estrutura do Projeto

```
cli/
├── src/
│   ├── commands/
│   │   ├── auth/
│   │   │   ├── login.ts
│   │   │   ├── logout.ts
│   │   │   └── whoami.ts
│   │   │
│   │   ├── projects/
│   │   │   ├── list.ts
│   │   │   ├── add.ts
│   │   │   ├── remove.ts
│   │   │   ├── sync.ts
│   │   │   └── config.ts
│   │   │
│   │   ├── reviews/
│   │   │   ├── list.ts
│   │   │   ├── show.ts
│   │   │   ├── trigger.ts
│   │   │   └── watch.ts
│   │   │
│   │   └── init.ts
│   │
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts
│   │   │   ├── auth.ts
│   │   │   ├── projects.ts
│   │   │   └── reviews.ts
│   │   │
│   │   ├── config/
│   │   │   ├── manager.ts
│   │   │   └── types.ts
│   │   │
│   │   └── utils/
│   │       ├── logger.ts
│   │       ├── spinner.ts
│   │       └── table.ts
│   │
│   ├── types/
│   │   └── index.ts
│   │
│   └── index.ts
│
├── .eslintrc.json
├── .prettierrc
├── package.json
├── tsconfig.json
└── README.md
```

---

## Setup Inicial

### package.json

```json
{
  "name": "@pullwise/cli",
  "version": "1.0.0",
  "description": "CLI for Pullwise - AI-powered code reviews",
  "main": "dist/index.js",
  "bin": {
    "pullwise": "./dist/index.js",
    "pw": "./dist/index.js"
  },
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsup src/index.ts --format cjs,esm --dts --clean",
    "lint": "eslint src --ext .ts",
    "format": "prettier --write \"src/**/*.ts\"",
    "prepublishOnly": "npm run build"
  },
  "keywords": ["code-review", "ai", "cli"],
  "author": "IntegrAllTech",
  "license": "MIT",
  "dependencies": {
    "commander": "^11.1.0",
    "inquirer": "^9.2.12",
    "axios": "^1.6.2",
    "chalk": "^5.3.0",
    "ora": "^7.0.1",
    "boxen": "^7.1.1",
    "cli-table3": "^0.6.3",
    "cosmiconfig": "^9.0.0",
    "dotenv": "^16.3.1",
    "open": "^10.0.0",
    "update-notifier": "^7.0.0"
  },
  "devDependencies": {
    "@types/inquirer": "^9.0.7",
    "@types/node": "^20.10.6",
    "@types/update-notifier": "^6.0.8",
    "@typescript-eslint/eslint-plugin": "^6.15.0",
    "@typescript-eslint/parser": "^6.15.0",
    "eslint": "^8.56.0",
    "prettier": "^3.1.1",
    "tsup": "^8.0.1",
    "tsx": "^4.7.0",
    "typescript": "^5.3.3"
  }
}
```

### tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "moduleResolution": "node",
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

---

## Types

### src/types/index.ts

```typescript
export interface Config {
  apiUrl: string
  token?: string
  currentOrg?: string
  defaultProject?: string
}

export interface Project {
  id: string
  name: string
  repositoryUrl: string
  platform: 'github' | 'bitbucket'
  isActive: boolean
}

export interface Review {
  id: string
  pullRequestId: string
  prNumber: number
  prTitle: string
  status: 'pending' | 'in_progress' | 'completed' | 'failed'
  sastIssuesCount: number
  llmIssuesCount: number
  qualityScore?: number
  createdAt: string
  completedAt?: string
}

export interface Issue {
  id: string
  filePath: string
  lineNumber?: number
  source: string
  type: string
  severity: 'critical' | 'high' | 'medium' | 'low'
  message: string
  suggestedFix?: string
}

export interface ApiError {
  message: string
  status?: number
  errors?: Record<string, string[]>
}
```

---

## Configuration Manager

### src/lib/config/manager.ts

```typescript
import { cosmiconfig } from 'cosmiconfig'
import fs from 'fs/promises'
import path from 'path'
import os from 'os'
import { Config } from '../../types'

const CONFIG_DIR = path.join(os.homedir(), '.pullwise')
const CONFIG_FILE = path.join(CONFIG_DIR, 'config.json')

export class ConfigManager {
  private config: Config | null = null

  async load(): Promise<Config> {
    if (this.config) {
      return this.config
    }

    try {
      const content = await fs.readFile(CONFIG_FILE, 'utf-8')
      this.config = JSON.parse(content)
      return this.config!
    } catch (error) {
      // Config doesn't exist, return defaults
      this.config = {
        apiUrl: process.env.PULLWISE_API_URL || 'https://api.pullwise.ai',
      }
      return this.config
    }
  }

  async save(config: Partial<Config>): Promise<void> {
    const current = await this.load()
    this.config = { ...current, ...config }

    await fs.mkdir(CONFIG_DIR, { recursive: true })
    await fs.writeFile(CONFIG_FILE, JSON.stringify(this.config, null, 2))
  }

  async get<K extends keyof Config>(key: K): Promise<Config[K] | undefined> {
    const config = await this.load()
    return config[key]
  }

  async set<K extends keyof Config>(key: K, value: Config[K]): Promise<void> {
    await this.save({ [key]: value })
  }

  async clear(): Promise<void> {
    try {
      await fs.unlink(CONFIG_FILE)
      this.config = null
    } catch (error) {
      // File doesn't exist, ignore
    }
  }

  async loadProjectConfig(): Promise<Record<string, any> | null> {
    const explorer = cosmiconfig('pullwise')
    const result = await explorer.search()
    return result?.config || null
  }
}

export const configManager = new ConfigManager()
```

---

## API Client

### src/lib/api/client.ts

```typescript
import axios, { AxiosInstance, AxiosError } from 'axios'
import { configManager } from '../config/manager'
import { ApiError } from '../../types'

class ApiClient {
  private client: AxiosInstance | null = null

  async getClient(): Promise<AxiosInstance> {
    if (this.client) {
      return this.client
    }

    const config = await configManager.load()

    this.client = axios.create({
      baseURL: config.apiUrl,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor
    this.client.interceptors.request.use(
      async (config) => {
        const token = await configManager.get('token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        const apiError: ApiError = {
          message: error.message,
          status: error.response?.status,
        }

        if (error.response?.data) {
          const data = error.response.data as any
          apiError.message = data.message || error.message
          apiError.errors = data.errors
        }

        return Promise.reject(apiError)
      }
    )

    return this.client
  }
}

export const apiClient = new ApiClient()
```

### src/lib/api/auth.ts

```typescript
import { apiClient } from './client'

export interface LoginResponse {
  token: string
  user: {
    id: string
    email: string
    name: string
  }
}

export const authApi = {
  login: async (email: string, password: string): Promise<LoginResponse> => {
    const client = await apiClient.getClient()
    const response = await client.post('/auth/login', { email, password })
    return response.data
  },

  getCurrentUser: async () => {
    const client = await apiClient.getClient()
    const response = await client.get('/auth/me')
    return response.data
  },

  initiateOAuth: async (provider: 'github' | 'bitbucket'): Promise<{ authUrl: string }> => {
    const client = await apiClient.getClient()
    const response = await client.post(`/auth/oauth/${provider}/initiate`)
    return response.data
  },
}
```

### src/lib/api/projects.ts

```typescript
import { apiClient } from './client'
import { Project } from '../../types'

export const projectsApi = {
  list: async (): Promise<Project[]> => {
    const client = await apiClient.getClient()
    const response = await client.get('/projects')
    return response.data.content
  },

  get: async (projectId: string): Promise<Project> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/projects/${projectId}`)
    return response.data
  },

  create: async (data: {
    name: string
    repositoryUrl: string
    platform: 'github' | 'bitbucket'
  }): Promise<Project> => {
    const client = await apiClient.getClient()
    const response = await client.post('/projects', data)
    return response.data
  },

  delete: async (projectId: string): Promise<void> => {
    const client = await apiClient.getClient()
    await client.delete(`/projects/${projectId}`)
  },

  syncKnowledge: async (projectId: string): Promise<void> => {
    const client = await apiClient.getClient()
    await client.post(`/projects/${projectId}/sync-knowledge`)
  },
}
```

### src/lib/api/reviews.ts

```typescript
import { apiClient } from './client'
import { Review, Issue } from '../../types'

export const reviewsApi = {
  list: async (projectId: string): Promise<Review[]> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/projects/${projectId}/reviews`)
    return response.data.content
  },

  get: async (reviewId: string): Promise<Review & { issues: Issue[] }> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/reviews/${reviewId}`)
    return response.data
  },

  trigger: async (projectId: string, prNumber: number): Promise<Review> => {
    const client = await apiClient.getClient()
    const response = await client.post('/reviews/trigger', {
      projectId,
      prNumber,
    })
    return response.data
  },
}
```

---

## Utilities

### src/lib/utils/logger.ts

```typescript
import chalk from 'chalk'
import boxen from 'boxen'

export const logger = {
  success: (message: string) => {
    console.log(chalk.green('✓'), message)
  },

  error: (message: string, details?: string) => {
    console.log(chalk.red('✗'), chalk.red(message))
    if (details) {
      console.log(chalk.gray(details))
    }
  },

  warning: (message: string) => {
    console.log(chalk.yellow('⚠'), chalk.yellow(message))
  },

  info: (message: string) => {
    console.log(chalk.blue('ℹ'), message)
  },

  box: (message: string, options?: { title?: string; color?: string }) => {
    console.log(
      boxen(message, {
        padding: 1,
        margin: 1,
        borderStyle: 'round',
        borderColor: options?.color || 'cyan',
        title: options?.title,
      })
    )
  },

  log: (message: string) => {
    console.log(message)
  },

  newLine: () => {
    console.log()
  },
}
```

### src/lib/utils/spinner.ts

```typescript
import ora, { Ora } from 'ora'

export class Spinner {
  private spinner: Ora

  constructor(text: string) {
    this.spinner = ora(text).start()
  }

  succeed(text?: string): void {
    this.spinner.succeed(text)
  }

  fail(text?: string): void {
    this.spinner.fail(text)
  }

  warn(text?: string): void {
    this.spinner.warn(text)
  }

  info(text?: string): void {
    this.spinner.info(text)
  }

  update(text: string): void {
    this.spinner.text = text
  }

  stop(): void {
    this.spinner.stop()
  }
}
```

### src/lib/utils/table.ts

```typescript
import Table from 'cli-table3'
import chalk from 'chalk'

export function createTable(head: string[], rows: string[][]): string {
  const table = new Table({
    head: head.map((h) => chalk.cyan(h)),
    style: {
      head: [],
      border: ['gray'],
    },
  })

  rows.forEach((row) => table.push(row))

  return table.toString()
}

export function formatIssueTable(issues: any[]): string {
  const table = new Table({
    head: [
      chalk.cyan('Severity'),
      chalk.cyan('File'),
      chalk.cyan('Line'),
      chalk.cyan('Message'),
    ],
    colWidths: [12, 40, 8, 60],
    wordWrap: true,
  })

  issues.forEach((issue) => {
    const severityColor =
      issue.severity === 'critical'
        ? chalk.red
        : issue.severity === 'high'
          ? chalk.orange
          : issue.severity === 'medium'
            ? chalk.yellow
            : chalk.blue

    table.push([
      severityColor(issue.severity.toUpperCase()),
      issue.filePath,
      issue.lineNumber?.toString() || '-',
      issue.message,
    ])
  })

  return table.toString()
}
```

---

## Commands

### src/commands/auth/login.ts

```typescript
import { Command } from 'commander'
import inquirer from 'inquirer'
import open from 'open'
import { authApi } from '../../lib/api/auth'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const loginCommand = new Command('login')
  .description('Login to Pullwise')
  .option('-e, --email <email>', 'Email address')
  .option('-p, --password <password>', 'Password')
  .option('--github', 'Login with GitHub')
  .action(async (options) => {
    try {
      if (options.github) {
        await loginWithGitHub()
      } else {
        await loginWithCredentials(options.email, options.password)
      }
    } catch (error: any) {
      logger.error('Login failed', error.message)
      process.exit(1)
    }
  })

async function loginWithCredentials(email?: string, password?: string) {
  if (!email || !password) {
    const answers = await inquirer.prompt([
      {
        type: 'input',
        name: 'email',
        message: 'Email:',
        validate: (input) => (input ? true : 'Email is required'),
      },
      {
        type: 'password',
        name: 'password',
        message: 'Password:',
        mask: '*',
        validate: (input) => (input ? true : 'Password is required'),
      },
    ])

    email = answers.email
    password = answers.password
  }

  const spinner = new Spinner('Logging in...')

  const response = await authApi.login(email!, password!)

  await configManager.set('token', response.token)

  spinner.succeed(`Logged in as ${response.user.email}`)
  logger.newLine()
  logger.info('Run `codereview projects list` to get started')
}

async function loginWithGitHub() {
  const spinner = new Spinner('Initiating GitHub OAuth...')

  const { authUrl } = await authApi.initiateOAuth('github')

  spinner.info('Opening browser for GitHub authentication...')
  await open(authUrl)

  logger.newLine()
  logger.info('After authorizing, paste the token here:')

  const { token } = await inquirer.prompt([
    {
      type: 'input',
      name: 'token',
      message: 'Token:',
      validate: (input) => (input ? true : 'Token is required'),
    },
  ])

  await configManager.set('token', token)

  logger.success('Successfully logged in with GitHub!')
}
```

### src/commands/auth/logout.ts

```typescript
import { Command } from 'commander'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'

export const logoutCommand = new Command('logout')
  .description('Logout from CodeReview.ai')
  .action(async () => {
    try {
      await configManager.clear()
      logger.success('Successfully logged out')
    } catch (error: any) {
      logger.error('Logout failed', error.message)
      process.exit(1)
    }
  })
```

### src/commands/auth/whoami.ts

```typescript
import { Command } from 'commander'
import { authApi } from '../../lib/api/auth'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const whoamiCommand = new Command('whoami')
  .description('Show current user information')
  .action(async () => {
    try {
      const spinner = new Spinner('Fetching user info...')

      const user = await authApi.getCurrentUser()

      spinner.stop()

      logger.box(
        `
Email: ${user.email}
Name: ${user.name}
Organization: ${user.organization?.name || 'N/A'}
Plan: ${user.organization?.planType || 'N/A'}
      `.trim(),
        { title: 'Current User' }
      )
    } catch (error: any) {
      logger.error('Failed to fetch user info', error.message)
      process.exit(1)
    }
  })
```

### src/commands/projects/list.ts

```typescript
import { Command } from 'commander'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { createTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'
import chalk from 'chalk'

export const listProjectsCommand = new Command('list')
  .alias('ls')
  .description('List all projects')
  .option('-a, --all', 'Show inactive projects')
  .action(async (options) => {
    try {
      const spinner = new Spinner('Fetching projects...')

      const projects = await projectsApi.list()

      spinner.stop()

      if (projects.length === 0) {
        logger.warning('No projects found')
        logger.info('Run `codereview projects add` to add a project')
        return
      }

      const filtered = options.all ? projects : projects.filter((p) => p.isActive)

      const rows = filtered.map((project) => [
        project.name,
        project.platform,
        project.repositoryUrl,
        project.isActive ? chalk.green('✓') : chalk.red('✗'),
        project.id,
      ])

      const table = createTable(['Name', 'Platform', 'Repository', 'Active', 'ID'], rows)

      console.log(table)
      logger.newLine()
      logger.info(`Total: ${filtered.length} project(s)`)
    } catch (error: any) {
      logger.error('Failed to list projects', error.message)
      process.exit(1)
    }
  })
```

### src/commands/projects/add.ts

```typescript
import { Command } from 'commander'
import inquirer from 'inquirer'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const addProjectCommand = new Command('add')
  .description('Add a new project')
  .option('-n, --name <name>', 'Project name')
  .option('-r, --repo <url>', 'Repository URL')
  .option('-p, --platform <platform>', 'Platform (github or bitbucket)')
  .action(async (options) => {
    try {
      let { name, repo, platform } = options

      if (!name || !repo || !platform) {
        const answers = await inquirer.prompt([
          {
            type: 'input',
            name: 'name',
            message: 'Project name:',
            when: !name,
            validate: (input) => (input ? true : 'Name is required'),
          },
          {
            type: 'input',
            name: 'repo',
            message: 'Repository URL:',
            when: !repo,
            validate: (input) => (input ? true : 'Repository URL is required'),
          },
          {
            type: 'list',
            name: 'platform',
            message: 'Platform:',
            when: !platform,
            choices: ['github', 'bitbucket'],
          },
        ])

        name = name || answers.name
        repo = repo || answers.repo
        platform = platform || answers.platform
      }

      const spinner = new Spinner('Creating project...')

      const project = await projectsApi.create({
        name,
        repositoryUrl: repo,
        platform,
      })

      spinner.succeed(`Project "${project.name}" created successfully!`)
      logger.newLine()
      logger.info(`Project ID: ${project.id}`)
      logger.info('Configure webhooks in your repository to enable automatic reviews')
    } catch (error: any) {
      logger.error('Failed to create project', error.message)
      process.exit(1)
    }
  })
```

### src/commands/reviews/list.ts

```typescript
import { Command } from 'commander'
import { reviewsApi } from '../../lib/api/reviews'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { createTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'
import { formatDistanceToNow } from 'date-fns'
import chalk from 'chalk'

export const listReviewsCommand = new Command('list')
  .alias('ls')
  .description('List reviews for a project')
  .option('-p, --project <id>', 'Project ID')
  .action(async (options) => {
    try {
      let projectId = options.project

      if (!projectId) {
        projectId = await configManager.get('defaultProject')
        if (!projectId) {
          logger.error('No project specified', 'Use --project or set a default project')
          process.exit(1)
        }
      }

      const spinner = new Spinner('Fetching reviews...')

      const reviews = await reviewsApi.list(projectId)

      spinner.stop()

      if (reviews.length === 0) {
        logger.warning('No reviews found')
        return
      }

      const rows = reviews.map((review) => {
        const statusIcon =
          review.status === 'completed'
            ? chalk.green('✓')
            : review.status === 'failed'
              ? chalk.red('✗')
              : chalk.yellow('⏳')

        const qualityScore = review.qualityScore
          ? `${review.qualityScore}/10`
          : '-'

        return [
          `PR #${review.prNumber}`,
          review.prTitle.substring(0, 40),
          `${statusIcon} ${review.status}`,
          qualityScore,
          `${review.sastIssuesCount + review.llmIssuesCount}`,
          formatDistanceToNow(new Date(review.createdAt), { addSuffix: true }),
        ]
      })

      const table = createTable(
        ['PR', 'Title', 'Status', 'Score', 'Issues', 'Created'],
        rows
      )

      console.log(table)
      logger.newLine()
      logger.info(`Total: ${reviews.length} review(s)`)
    } catch (error: any) {
      logger.error('Failed to list reviews', error.message)
      process.exit(1)
    }
  })
```

### src/commands/reviews/show.ts

```typescript
import { Command } from 'commander'
import { reviewsApi } from '../../lib/api/reviews'
import { logger } from '../../lib/utils/logger'
import { formatIssueTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'
import chalk from 'chalk'

export const showReviewCommand = new Command('show')
  .description('Show details of a review')
  .argument('<reviewId>', 'Review ID')
  .option('-i, --issues', 'Show all issues')
  .action(async (reviewId, options) => {
    try {
      const spinner = new Spinner('Fetching review details...')

      const review = await reviewsApi.get(reviewId)

      spinner.stop()

      // Summary
      logger.box(
        `
PR #${review.prNumber}: ${review.prTitle}
Status: ${review.status}
Quality Score: ${review.qualityScore || 'N/A'}/10

SAST Issues: ${review.sastIssuesCount}
AI Issues: ${review.llmIssuesCount}
Total Issues: ${review.sastIssuesCount + review.llmIssuesCount}

Tokens Used: ${review.tokensUsed || 'N/A'}
      `.trim(),
        { title: 'Review Summary', color: 'cyan' }
      )

      // Issues breakdown
      if (options.issues && review.issues.length > 0) {
        logger.newLine()
        logger.log(chalk.bold('Issues:'))
        logger.newLine()

        const criticalIssues = review.issues.filter((i) => i.severity === 'critical')
        const highIssues = review.issues.filter((i) => i.severity === 'high')
        const mediumIssues = review.issues.filter((i) => i.severity === 'medium')
        const lowIssues = review.issues.filter((i) => i.severity === 'low')

        if (criticalIssues.length > 0) {
          logger.log(chalk.red.bold(`\n🔴 Critical (${criticalIssues.length})`))
          console.log(formatIssueTable(criticalIssues))
        }

        if (highIssues.length > 0) {
          logger.log(chalk.orange.bold(`\n🟠 High (${highIssues.length})`))
          console.log(formatIssueTable(highIssues))
        }

        if (mediumIssues.length > 0) {
          logger.log(chalk.yellow.bold(`\n🟡 Medium (${mediumIssues.length})`))
          console.log(formatIssueTable(mediumIssues))
        }

        if (lowIssues.length > 0) {
          logger.log(chalk.blue.bold(`\n🟢 Low (${lowIssues.length})`))
          console.log(formatIssueTable(lowIssues))
        }
      }
    } catch (error: any) {
      logger.error('Failed to fetch review', error.message)
      process.exit(1)
    }
  })
```

### src/commands/reviews/trigger.ts

```typescript
import { Command } from 'commander'
import { reviewsApi } from '../../lib/api/reviews'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const triggerReviewCommand = new Command('trigger')
  .description('Manually trigger a review')
  .argument('<prNumber>', 'Pull request number')
  .option('-p, --project <id>', 'Project ID')
  .action(async (prNumber, options) => {
    try {
      let projectId = options.project

      if (!projectId) {
        projectId = await configManager.get('defaultProject')
        if (!projectId) {
          logger.error('No project specified', 'Use --project or set a default project')
          process.exit(1)
        }
      }

      const spinner = new Spinner(`Triggering review for PR #${prNumber}...`)

      const review = await reviewsApi.trigger(projectId, parseInt(prNumber))

      spinner.succeed(`Review triggered for PR #${prNumber}`)
      logger.newLine()
      logger.info(`Review ID: ${review.id}`)
      logger.info(`Status: ${review.status}`)
      logger.info(`Run \`codereview reviews show ${review.id}\` to see results`)
    } catch (error: any) {
      logger.error('Failed to trigger review', error.message)
      process.exit(1)
    }
  })
```

### src/commands/init.ts

```typescript
import { Command } from 'commander'
import inquirer from 'inquirer'
import fs from 'fs/promises'
import { logger } from '../lib/utils/logger'
import { Spinner } from '../lib/utils/spinner'

const DEFAULT_CONFIG = `# Pullwise Configuration
# https://pullwise.dev/docs/configuration

# Auto-review settings
auto_review:
  enabled: true
  on_draft: false

# Path-specific instructions
path_instructions:
  - path: "src/**/*.java"
    instructions: |
      - Follow Clean Architecture principles
      - Use dependency injection
      - Add comprehensive JavaDoc comments

  - path: "src/**/controller/**"
    instructions: |
      - Validate all inputs with @Valid
      - Use ResponseEntity with proper HTTP status
      - Document with OpenAPI annotations

  - path: "src/test/**"
    instructions: |
      - Use JUnit 5
      - Aim for 80% code coverage
      - Follow AAA pattern (Arrange-Act-Assert)

# Custom rules
custom_rules:
  - name: "No System.out.println"
    pattern: "System\\\\.out\\\\.println"
    severity: "high"
    message: "Use logger instead of System.out.println"
`

export const initCommand = new Command('init')
  .description('Initialize Pullwise in current directory')
  .action(async () => {
    try {
      const { confirm } = await inquirer.prompt([
        {
          type: 'confirm',
          name: 'confirm',
          message: 'Create .pullwise.yaml in current directory?',
          default: true,
        },
      ])

      if (!confirm) {
        logger.info('Cancelled')
        return
      }

      const spinner = new Spinner('Creating configuration file...')

      await fs.writeFile('.pullwise.yaml', DEFAULT_CONFIG)

      spinner.succeed('Configuration file created!')
      logger.newLine()
      logger.info('Edit .pullwise.yaml to customize review settings')
      logger.info('Commit this file to your repository')
    } catch (error: any) {
      logger.error('Failed to initialize', error.message)
      process.exit(1)
    }
  })
```

---

## Main Entry Point

### src/index.ts

```typescript
#!/usr/bin/env node

import { Command } from 'commander'
import updateNotifier from 'update-notifier'
import { loginCommand } from './commands/auth/login'
import { logoutCommand } from './commands/auth/logout'
import { whoamiCommand } from './commands/auth/whoami'
import { listProjectsCommand } from './commands/projects/list'
import { addProjectCommand } from './commands/projects/add'
import { listReviewsCommand } from './commands/reviews/list'
import { showReviewCommand } from './commands/reviews/show'
import { triggerReviewCommand } from './commands/reviews/trigger'
import { initCommand } from './commands/init'

const pkg = require('../package.json')

// Check for updates
updateNotifier({ pkg }).notify()

const program = new Command()

program
  .name('pullwise')
  .description('CLI for Pullwise - AI-powered code reviews')
  .version(pkg.version)

// Auth commands
const auth = program.command('auth').description('Authentication commands')
auth.addCommand(loginCommand)
auth.addCommand(logoutCommand)
auth.addCommand(whoamiCommand)

// Projects commands
const projects = program.command('projects').alias('p').description('Project management')
projects.addCommand(listProjectsCommand)
projects.addCommand(addProjectCommand)

// Reviews commands
const reviews = program.command('reviews').alias('r').description('Review management')
reviews.addCommand(listReviewsCommand)
reviews.addCommand(showReviewCommand)
reviews.addCommand(triggerReviewCommand)

// Init command
program.addCommand(initCommand)

program.parse()
```

---

## Usage Examples

```bash
# Installation
npm install -g @pullwise/cli

# Login
pullwise auth login
pullwise auth login --github

# Check current user
pullwise auth whoami

# Initialize project
cd my-project
pullwise init

# List projects
pullwise projects list
pullwise p ls

# Add project
pullwise projects add
pullwise p add -n "My API" -r "https://github.com/org/repo" -p github

# List reviews
pullwise reviews list -p <project-id>
pullwise r ls

# Show review details
pullwise reviews show <review-id>
pullwise r show <review-id> --issues

# Trigger manual review
pullwise reviews trigger 123 -p <project-id>
pullwise r trigger 123

# Logout
pullwise auth logout
```

---

## Build & Publish

```bash
# Build
npm run build

# Test locally
npm link

# Publish to npm
npm publish --access public
```

---

## Configuration File (.pullwise.yaml)

```yaml
# Pullwise Configuration
# Auto-review settings
auto_review:
  enabled: true
  on_draft: false
  base_branches:
    - main
    - develop

# Review profile
reviews:
  profile: assertive  # or 'chill'
  high_level_summary: true

# Path-specific instructions
path_instructions:
  - path: "**/*.java"
    instructions: |
      - Use Java 17 features
      - Follow Google Java Style Guide

  - path: "src/**/controller/**"
    instructions: |
      - REST best practices
      - OpenAPI documentation

# Knowledge base
knowledge_base:
  code_guidelines:
    enabled: true
    filePatterns:
      - "**/.cursorrules"
      - "**/CLAUDE.md"
      - "**/docs/ADR/**/*.md"

# Tools
tools:
  checkstyle:
    enabled: true
  pmd:
    enabled: true
  sonarqube:
    enabled: true
```

---

## Próximos Passos

1. ✅ Setup inicial com TypeScript + Commander
2. ✅ Implementar comandos de autenticação
3. ✅ Implementar comandos de projetos
4. ✅ Implementar comandos de reviews
5. ✅ Adicionar suporte a config file
6. ✅ Criar comando `init`
7. ✅ Adicionar testes unitários
8. ✅ Build e publicação no npm
9. ✅ Documentação completa
10. ✅ GitHub Actions para CI/CD

---

## Features Avançadas (Futuro)

- `codereview reviews watch` - Real-time monitoring
- `codereview config set` - Interactive config editor
- `codereview stats` - Usage statistics
- `codereview templates` - Configuration templates
- `codereview webhooks setup` - Automatic webhook configuration
