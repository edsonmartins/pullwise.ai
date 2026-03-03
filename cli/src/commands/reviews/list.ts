import { Command } from 'commander'
import chalk from 'chalk'
import { reviewsApi } from '../../lib/api/reviews'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { createTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'

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
            ? chalk.green('\u2713')
            : review.status === 'failed'
              ? chalk.red('\u2717')
              : chalk.yellow('\u23F3')

        const qualityScore = review.qualityScore
          ? `${review.qualityScore}/10`
          : '-'

        return [
          `PR #${review.prNumber}`,
          review.prTitle.substring(0, 40),
          `${statusIcon} ${review.status}`,
          qualityScore,
          `${review.sastIssuesCount + review.llmIssuesCount}`,
          review.createdAt,
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
