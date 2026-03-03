import { Command } from 'commander'
import chalk from 'chalk'
import { reviewsApi } from '../../lib/api/reviews'
import { logger } from '../../lib/utils/logger'
import { formatIssueTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'

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
          logger.log(chalk.red.bold(`\nCritical (${criticalIssues.length})`))
          console.log(formatIssueTable(criticalIssues))
        }

        if (highIssues.length > 0) {
          logger.log(chalk.hex('#FFA500').bold(`\nHigh (${highIssues.length})`))
          console.log(formatIssueTable(highIssues))
        }

        if (mediumIssues.length > 0) {
          logger.log(chalk.yellow.bold(`\nMedium (${mediumIssues.length})`))
          console.log(formatIssueTable(mediumIssues))
        }

        if (lowIssues.length > 0) {
          logger.log(chalk.blue.bold(`\nLow (${lowIssues.length})`))
          console.log(formatIssueTable(lowIssues))
        }
      }
    } catch (error: any) {
      logger.error('Failed to fetch review', error.message)
      process.exit(1)
    }
  })
