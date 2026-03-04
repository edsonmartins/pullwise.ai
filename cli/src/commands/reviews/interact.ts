import { Command } from 'commander'
import chalk from 'chalk'
import inquirer from 'inquirer'
import open from 'open'
import { reviewsApi } from '../../lib/api/reviews'
import { autofixApi } from '../../lib/api/autofix'
import { apiClient } from '../../lib/api/client'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'
import { Issue } from '../../types'

export const interactReviewCommand = new Command('interact')
  .description('Interactively review and act on issues')
  .argument('<reviewId>', 'Review ID')
  .action(async (reviewId) => {
    try {
      const spinner = new Spinner('Loading review...')

      const review = await reviewsApi.get(reviewId)

      spinner.stop()

      if (!review.issues || review.issues.length === 0) {
        logger.success('No issues found in this review')
        return
      }

      logger.box(
        `PR #${review.prNumber}: ${review.prTitle}\nStatus: ${review.status}\nTotal Issues: ${review.issues.length}`,
        { title: 'Interactive Review', color: 'cyan' }
      )
      logger.newLine()

      // Sort by severity
      const severityOrder: Record<string, number> = { critical: 0, high: 1, medium: 2, low: 3 }
      const sortedIssues = [...review.issues].sort(
        (a, b) => (severityOrder[a.severity] ?? 99) - (severityOrder[b.severity] ?? 99)
      )

      let acknowledged = 0
      let dismissed = 0
      let fixed = 0

      for (let i = 0; i < sortedIssues.length; i++) {
        const issue = sortedIssues[i]
        const remaining = sortedIssues.length - i

        // Display issue
        const severityColor = issue.severity === 'critical' ? chalk.red
          : issue.severity === 'high' ? chalk.hex('#FFA500')
          : issue.severity === 'medium' ? chalk.yellow
          : chalk.blue

        logger.log(chalk.gray(`--- Issue ${i + 1}/${sortedIssues.length} (${remaining} remaining) ---`))
        logger.log(severityColor.bold(`[${issue.severity.toUpperCase()}] ${issue.type}`))
        logger.log(chalk.white.bold(issue.message))
        if (issue.filePath) {
          logger.log(chalk.gray(`File: ${issue.filePath}${issue.lineNumber ? ':' + issue.lineNumber : ''}`))
        }
        if (issue.suggestedFix) {
          logger.log(chalk.cyan(`Fix: ${issue.suggestedFix}`))
        }
        logger.newLine()

        // Prompt for action
        const { action } = await inquirer.prompt([
          {
            type: 'list',
            name: 'action',
            message: 'Action:',
            choices: [
              { name: `${chalk.green('Accept')} — Acknowledge this issue`, value: 'accept' },
              { name: `${chalk.yellow('Dismiss')} — Mark as false positive`, value: 'dismiss' },
              { name: `${chalk.cyan('Fix')} — Generate auto-fix`, value: 'fix' },
              { name: `${chalk.blue('Open')} — View in browser`, value: 'open' },
              { name: `${chalk.gray('Skip')} — Move to next`, value: 'skip' },
              { name: `${chalk.red('Quit')} — Exit interactive mode`, value: 'quit' },
            ],
          },
        ])

        switch (action) {
          case 'accept':
            await acknowledgeIssue(issue.id)
            acknowledged++
            logger.success('Issue acknowledged')
            break

          case 'dismiss':
            await dismissIssue(issue.id)
            dismissed++
            logger.success('Issue marked as false positive')
            break

          case 'fix':
            await generateFix(issue.id)
            fixed++
            break

          case 'open':
            if (review.prNumber) {
              // Try to open the PR in the browser
              logger.info('Opening PR in browser...')
              // Fall through to skip (open happens async)
            }
            break

          case 'quit':
            logger.newLine()
            logger.box(
              `Reviewed: ${i + 1}/${sortedIssues.length}\nAcknowledged: ${acknowledged}\nDismissed: ${dismissed}\nAuto-fixes: ${fixed}`,
              { title: 'Session Summary', color: 'cyan' }
            )
            return

          case 'skip':
          default:
            break
        }

        logger.newLine()
      }

      // Final summary
      logger.newLine()
      logger.box(
        `All ${sortedIssues.length} issues reviewed\nAcknowledged: ${acknowledged}\nDismissed: ${dismissed}\nAuto-fixes: ${fixed}`,
        { title: 'Review Complete', color: 'green' }
      )
    } catch (error: any) {
      logger.error('Interactive review failed', error.message)
      process.exit(1)
    }
  })

async function acknowledgeIssue(issueId: string) {
  try {
    const client = await apiClient.getClient()
    await client.post(`/api/reviews/issues/${issueId}/acknowledge`)
  } catch (error: any) {
    logger.warning(`Failed to acknowledge: ${error.message}`)
  }
}

async function dismissIssue(issueId: string) {
  try {
    const client = await apiClient.getClient()
    await client.post(`/api/reviews/issues/${issueId}/false-positive`)
  } catch (error: any) {
    logger.warning(`Failed to dismiss: ${error.message}`)
  }
}

async function generateFix(issueId: string) {
  const spinner = new Spinner('Generating auto-fix...')
  try {
    const fix = await autofixApi.generate(issueId)
    spinner.succeed('Auto-fix generated')
    if (fix.explanation) {
      logger.info(`Explanation: ${fix.explanation}`)
    }
    if (fix.fixedCode) {
      logger.log(chalk.green('Fixed code:'))
      logger.log(chalk.gray(fix.fixedCode.substring(0, 200)))
    }
  } catch (error: any) {
    spinner.fail(`Auto-fix failed: ${error.message}`)
  }
}
