import { Command } from 'commander'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'

export const configProjectCommand = new Command('config')
  .description('Configure project settings')
  .option('-d, --default <projectId>', 'Set default project')
  .option('-s, --show', 'Show current project configuration')
  .action(async (options) => {
    try {
      if (options.show) {
        const defaultProject = await configManager.get('defaultProject')
        logger.newLine()
        if (defaultProject) {
          logger.info(`Default project: ${defaultProject}`)
        } else {
          logger.info('No default project configured')
        }
        return
      }

      if (options.default) {
        await configManager.set('defaultProject', options.default)
        logger.success(`Default project set to: ${options.default}`)
        return
      }

      logger.info('Use --default <projectId> to set a default project, or --show to view current config')
    } catch (error: any) {
      logger.error('Failed to update configuration', error.message)
      process.exit(1)
    }
  })
