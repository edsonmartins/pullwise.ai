import { Command } from 'commander'
import inquirer from 'inquirer'
import fs from 'fs/promises'
import { logger } from '../lib/utils/logger'
import { Spinner } from '../lib/utils/spinner'

const DEFAULT_CONFIG = `# Pullwise Configuration
# https://pullwise.ai/docs/configuration

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

# Tools
tools:
  checkstyle:
    enabled: true
  pmd:
    enabled: true
  sonarqube:
    enabled: false
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
