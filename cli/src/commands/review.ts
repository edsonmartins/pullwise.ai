import { Command } from 'commander'
import { execSync } from 'child_process'
import chalk from 'chalk'
import { stagedApi, StagedIssue } from '../lib/api/staged'
import { configManager } from '../lib/config/manager'
import { logger } from '../lib/utils/logger'
import { Spinner } from '../lib/utils/spinner'

export const reviewCommand = new Command('review')
  .description('Review code changes')
  .option('--staged', 'Review staged changes (for pre-commit hooks)')
  .option('--fail-on-blocking', 'Exit with code 1 if CRITICAL or HIGH issues found')
  .option('--summary-only', 'Output only a one-line summary (for commit message annotation)')
  .option('-p, --project <id>', 'Project ID')
  .action(async (options) => {
    if (options.staged) {
      await reviewStaged(options)
    } else {
      logger.error('No review mode specified', 'Use --staged for pre-commit review')
      process.exit(1)
    }
  })

async function reviewStaged(options: {
  failOnBlocking?: boolean
  summaryOnly?: boolean
  project?: string
}) {
  try {
    // Get staged diff
    let diff: string
    try {
      diff = execSync('git diff --cached', { encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 })
    } catch {
      logger.error('Failed to get staged changes', 'Are you in a git repository?')
      process.exit(1)
      return
    }

    if (!diff.trim()) {
      if (options.summaryOnly) {
        process.exit(0)
      }
      logger.info('No staged changes to review')
      process.exit(0)
      return
    }

    // Get staged file paths
    let filePaths: string[]
    try {
      const files = execSync('git diff --cached --name-only', { encoding: 'utf-8' })
      filePaths = files.trim().split('\n').filter(Boolean)
    } catch {
      filePaths = []
    }

    // Get commit message if available (from prepare-commit-msg context)
    const commitMessage = process.env.PULLWISE_COMMIT_MSG || undefined

    // Get project ID
    let projectId = options.project
    if (!projectId) {
      projectId = await configManager.get('defaultProject') as string | undefined
    }

    if (options.summaryOnly) {
      // Quiet mode for prepare-commit-msg hook
      try {
        const result = await stagedApi.review(diff, filePaths, commitMessage, projectId)
        if (result.totalIssues > 0) {
          const critical = result.issues.filter((i) => i.severity === 'CRITICAL').length
          const high = result.issues.filter((i) => i.severity === 'HIGH').length
          const medium = result.issues.filter((i) => i.severity === 'MEDIUM').length
          const low = result.issues.filter((i) => i.severity === 'LOW').length
          const parts: string[] = []
          if (critical) parts.push(`${critical} critical`)
          if (high) parts.push(`${high} high`)
          if (medium) parts.push(`${medium} medium`)
          if (low) parts.push(`${low} low`)
          console.log(parts.join(', '))
        }
      } catch {
        // Silent failure in summary mode
      }
      process.exit(0)
      return
    }

    // Full review mode
    const spinner = new Spinner(`Reviewing ${filePaths.length} staged file(s)...`)

    const result = await stagedApi.review(diff, filePaths, commitMessage, projectId)

    spinner.stop()

    if (result.totalIssues === 0) {
      logger.success('No issues found in staged changes')
      process.exit(0)
      return
    }

    // Display results
    logger.newLine()
    displayIssues(result.issues)

    // Summary
    const critical = result.issues.filter((i) => i.severity === 'CRITICAL').length
    const high = result.issues.filter((i) => i.severity === 'HIGH').length
    const medium = result.issues.filter((i) => i.severity === 'MEDIUM').length
    const low = result.issues.filter((i) => i.severity === 'LOW').length

    logger.newLine()
    logger.box(
      `
Found ${result.totalIssues} issue(s) in staged changes

${critical ? chalk.red(`  CRITICAL: ${critical}`) + '\n' : ''}${high ? chalk.hex('#FFA500')(`  HIGH:     ${high}`) + '\n' : ''}${medium ? chalk.yellow(`  MEDIUM:   ${medium}`) + '\n' : ''}${low ? chalk.blue(`  LOW:      ${low}`) + '\n' : ''}
${result.hasBlockingIssues ? chalk.red.bold('Blocking issues found!') : chalk.green('No blocking issues')}
      `.trim(),
      { title: 'Pullwise Pre-Commit Review', color: result.hasBlockingIssues ? 'red' : 'green' }
    )

    if (result.hasBlockingIssues && options.failOnBlocking) {
      process.exit(1)
    }
  } catch (error: any) {
    logger.error('Staged review failed', error.message)
    if (options.failOnBlocking) {
      // Don't block commit on review failure — only block on actual issues
      process.exit(0)
    }
  }
}

function displayIssues(issues: StagedIssue[]) {
  const severityOrder = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

  for (const severity of severityOrder) {
    const filtered = issues.filter((i) => i.severity === severity)
    if (filtered.length === 0) continue

    const color = severity === 'CRITICAL' ? chalk.red
      : severity === 'HIGH' ? chalk.hex('#FFA500')
      : severity === 'MEDIUM' ? chalk.yellow
      : chalk.blue

    const icon = severity === 'CRITICAL' ? '\u2718'
      : severity === 'HIGH' ? '\u26A0'
      : severity === 'MEDIUM' ? '\u25CF'
      : '\u2139'

    logger.log(color.bold(`\n${icon} ${severity} (${filtered.length})`))

    for (const issue of filtered) {
      const location = issue.lineNumber ? `:${issue.lineNumber}` : ''
      logger.log(color(`  ${issue.filePath}${location}`))
      logger.log(`    ${issue.title}`)
      if (issue.description) {
        logger.log(chalk.gray(`    ${issue.description}`))
      }
      if (issue.suggestion) {
        logger.log(chalk.cyan(`    Fix: ${issue.suggestion}`))
      }
      logger.newLine()
    }
  }
}
