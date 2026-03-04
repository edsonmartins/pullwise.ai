import { Command } from 'commander'
import inquirer from 'inquirer'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const removeProjectCommand = new Command('remove')
  .alias('rm')
  .description('Remove a project')
  .argument('<projectId>', 'Project ID to remove')
  .option('-f, --force', 'Skip confirmation')
  .action(async (projectId, options) => {
    try {
      if (!options.force) {
        const { confirm } = await inquirer.prompt([{
          type: 'confirm',
          name: 'confirm',
          message: `Are you sure you want to remove project ${projectId}? This cannot be undone.`,
          default: false,
        }])
        if (!confirm) {
          logger.info('Operation cancelled')
          return
        }
      }

      const spinner = new Spinner('Removing project...')
      await projectsApi.delete(projectId)
      spinner.succeed('Project removed successfully')
    } catch (error: any) {
      logger.error('Failed to remove project', error.message)
      process.exit(1)
    }
  })
