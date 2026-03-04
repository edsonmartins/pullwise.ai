import { Command } from 'commander'
import chalk from 'chalk'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'
import { streamReviewProgress } from '../../lib/api/stream'

const severityColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return chalk.red
    case 'HIGH': return chalk.yellow
    case 'MEDIUM': return chalk.cyan
    case 'LOW': return chalk.gray
    default: return chalk.white
  }
}

export const watchReviewCommand = new Command('watch')
  .description('Watch a review in real-time')
  .argument('<reviewId>', 'Review ID to watch')
  .action(async (reviewId) => {
    try {
      const spinner = new Spinner('Connecting to review stream...')

      const summary = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 } as Record<string, number>

      await new Promise<void>((resolve) => {
        streamReviewProgress(
          reviewId,
          (event) => {
            if (event.type === 'review.started') {
              spinner.succeed('Review started')
              logger.newLine()
            } else if (event.type === 'review.progress') {
              const percent = event.data.percent || 0
              const step = event.data.step || 'Processing'
              spinner.update(`${step} — ${percent}%`)
            } else if (event.type === 'issue.found') {
              spinner.stop()
              const issue = event.data
              const severity = issue.severity || 'MEDIUM'
              const colorFn = severityColor(severity)
              summary[severity] = (summary[severity] || 0) + 1
              logger.log(
                colorFn(`  [${severity}]`) +
                ` ${issue.file}${issue.line ? ':' + issue.line : ''} — ${issue.message}`
              )
              spinner.update('Watching for issues...')
            } else if (event.type === 'review.completed') {
              spinner.succeed('Review completed')
              logger.newLine()

              const total = Object.values(summary).reduce((a, b) => a + b, 0)
              if (total > 0) {
                logger.info(`Summary: ${total} issue(s) found`)
                if (summary.CRITICAL > 0) logger.log(chalk.red(`  CRITICAL: ${summary.CRITICAL}`))
                if (summary.HIGH > 0) logger.log(chalk.yellow(`  HIGH:     ${summary.HIGH}`))
                if (summary.MEDIUM > 0) logger.log(chalk.cyan(`  MEDIUM:   ${summary.MEDIUM}`))
                if (summary.LOW > 0) logger.log(chalk.gray(`  LOW:      ${summary.LOW}`))
              } else {
                logger.success('No issues found')
              }

              resolve()
            } else if (event.type === 'review.failed') {
              spinner.fail('Review failed')
              const reason = event.data.error || event.data.message || 'Unknown error'
              logger.error('Review failed', reason)
              process.exit(1)
            }
          },
          () => resolve(),
          (error) => {
            spinner.fail(`Connection error: ${error.message}`)
            process.exit(1)
          }
        )
      })
    } catch (error: any) {
      logger.error('Failed to watch review', error.message)
      process.exit(1)
    }
  })
