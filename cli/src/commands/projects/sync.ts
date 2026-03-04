import { Command } from 'commander'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'
import { configManager } from '../../lib/config/manager'

export const syncProjectCommand = new Command('sync')
  .description('Sync project knowledge base')
  .argument('[projectId]', 'Project ID (uses default project if not specified)')
  .action(async (projectId) => {
    try {
      const resolvedId = projectId || await configManager.get('defaultProject')
      if (!resolvedId) {
        logger.error('No project specified', 'Provide a project ID or set a default project with "pullwise projects config --default <id>"')
        process.exit(1)
      }

      const spinner = new Spinner('Syncing knowledge base...')
      await projectsApi.syncKnowledge(resolvedId)
      spinner.succeed('Knowledge base synced successfully')
      logger.info('The project knowledge base has been updated with the latest repository data.')
    } catch (error: any) {
      logger.error('Failed to sync knowledge base', error.message)
      process.exit(1)
    }
  })
