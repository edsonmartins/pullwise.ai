import { Command } from 'commander'
import chalk from 'chalk'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { createTable } from '../../lib/utils/table'
import { Spinner } from '../../lib/utils/spinner'

export const listProjectsCommand = new Command('list')
  .alias('ls')
  .description('List all projects')
  .option('-a, --all', 'Show inactive projects')
  .action(async (options) => {
    try {
      const spinner = new Spinner('Fetching projects...')

      const projects = await projectsApi.list()

      spinner.stop()

      if (projects.length === 0) {
        logger.warning('No projects found')
        logger.info('Run `pullwise projects add` to add a project')
        return
      }

      const filtered = options.all ? projects : projects.filter((p) => p.isActive)

      const rows = filtered.map((project) => [
        project.name,
        project.platform,
        project.repositoryUrl,
        project.isActive ? chalk.green('\u2713') : chalk.red('\u2717'),
        project.id,
      ])

      const table = createTable(['Name', 'Platform', 'Repository', 'Active', 'ID'], rows)

      console.log(table)
      logger.newLine()
      logger.info(`Total: ${filtered.length} project(s)`)
    } catch (error: any) {
      logger.error('Failed to list projects', error.message)
      process.exit(1)
    }
  })
