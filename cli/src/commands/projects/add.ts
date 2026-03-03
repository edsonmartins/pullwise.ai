import { Command } from 'commander'
import inquirer from 'inquirer'
import { projectsApi } from '../../lib/api/projects'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const addProjectCommand = new Command('add')
  .description('Add a new project')
  .option('-n, --name <name>', 'Project name')
  .option('-r, --repo <url>', 'Repository URL')
  .option('-p, --platform <platform>', 'Platform (github, bitbucket, or gitlab)')
  .action(async (options) => {
    try {
      let { name, repo, platform } = options

      if (!name || !repo || !platform) {
        const answers = await inquirer.prompt([
          {
            type: 'input',
            name: 'name',
            message: 'Project name:',
            when: !name,
            validate: (input: string) => (input ? true : 'Name is required'),
          },
          {
            type: 'input',
            name: 'repo',
            message: 'Repository URL:',
            when: !repo,
            validate: (input: string) => (input ? true : 'Repository URL is required'),
          },
          {
            type: 'list',
            name: 'platform',
            message: 'Platform:',
            when: !platform,
            choices: ['github', 'bitbucket', 'gitlab'],
          },
        ])

        name = name || answers.name
        repo = repo || answers.repo
        platform = platform || answers.platform
      }

      const spinner = new Spinner('Creating project...')

      const project = await projectsApi.create({
        name,
        repositoryUrl: repo,
        platform,
      })

      spinner.succeed(`Project "${project.name}" created successfully!`)
      logger.newLine()
      logger.info(`Project ID: ${project.id}`)
      logger.info('Configure webhooks in your repository to enable automatic reviews')
    } catch (error: any) {
      logger.error('Failed to create project', error.message)
      process.exit(1)
    }
  })
