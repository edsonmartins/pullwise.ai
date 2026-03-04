import { Command } from 'commander'
import chalk from 'chalk'
import { reviewsApi } from '../../lib/api/reviews'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'
import { streamReviewProgress } from '../../lib/api/stream'

export const triggerReviewCommand = new Command('trigger')
  .description('Manually trigger a review')
  .argument('<prNumber>', 'Pull request number')
  .option('-p, --project <id>', 'Project ID')
  .option('--stream', 'Stream results in real-time as issues are found')
  .option('--no-stream', 'Disable streaming (just trigger and return)')
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
      logger.info(`Review ID: ${review.id}`)

      // If streaming enabled, connect to SSE
      if (options.stream !== false) {
        logger.newLine()
        const streamSpinner = new Spinner('Waiting for review results...')
        let issueCount = 0

        await new Promise<void>((resolve) => {
          streamReviewProgress(
            review.id,
            (event) => {
              if (event.type === 'review.progress') {
                const status = event.data.status || 'ANALYZING'
                const files = event.data.filesAnalyzed || 0
                streamSpinner.update(`${status} — ${files} files analyzed...`)
              } else if (event.type === 'issue.detected') {
                issueCount++
                streamSpinner.stop()
                const issue = event.data
                const severityColor = issue.severity === 'CRITICAL' ? chalk.red
                  : issue.severity === 'HIGH' ? chalk.hex('#FFA500')
                  : issue.severity === 'MEDIUM' ? chalk.yellow
                  : chalk.blue
                logger.log(
                  severityColor(`  [${issue.severity}]`) +
                  ` ${issue.filePath}${issue.lineStart ? ':' + issue.lineStart : ''} — ${issue.title}`
                )
                streamSpinner.update(`Analyzing... (${issueCount} issues found)`)
              } else if (event.type === 'review.completed') {
                streamSpinner.succeed(`Review completed — ${issueCount} issue(s) found`)
                resolve()
              } else if (event.type === 'review.failed') {
                streamSpinner.fail('Review failed')
                resolve()
              }
            },
            () => resolve(),
            (error) => {
              streamSpinner.fail(`Stream error: ${error.message}`)
              resolve()
            }
          )
        })

        logger.newLine()
        logger.info(`Run \`pullwise reviews show ${review.id} --issues\` for full details`)
      } else {
        logger.newLine()
        logger.info(`Status: ${review.status}`)
        logger.info(`Run \`pullwise reviews show ${review.id}\` to see results`)
      }
    } catch (error: any) {
      logger.error('Failed to trigger review', error.message)
      process.exit(1)
    }
  })
