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
      logger.info(`Run \`pullwise reviews show ${review.id}\` to see results`)
    } catch (error: any) {
      logger.error('Failed to trigger review', error.message)
      process.exit(1)
    }
  })
