import { Command } from 'commander'
import inquirer from 'inquirer'
import open from 'open'
import { authApi } from '../../lib/api/auth'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const loginCommand = new Command('login')
  .description('Login to Pullwise')
  .option('-e, --email <email>', 'Email address')
  .option('-p, --password <password>', 'Password')
  .option('--github', 'Login with GitHub')
  .action(async (options) => {
    try {
      if (options.github) {
        await loginWithGitHub()
      } else {
        await loginWithCredentials(options.email, options.password)
      }
    } catch (error: any) {
      logger.error('Login failed', error.message)
      process.exit(1)
    }
  })

async function loginWithCredentials(email?: string, password?: string) {
  if (!email || !password) {
    const answers = await inquirer.prompt([
      {
        type: 'input',
        name: 'email',
        message: 'Email:',
        validate: (input: string) => (input ? true : 'Email is required'),
      },
      {
        type: 'password',
        name: 'password',
        message: 'Password:',
        mask: '*',
        validate: (input: string) => (input ? true : 'Password is required'),
      },
    ])

    email = answers.email
    password = answers.password
  }

  const spinner = new Spinner('Logging in...')

  const response = await authApi.login(email!, password!)

  await configManager.set('token', response.token)

  spinner.succeed(`Logged in as ${response.user.email}`)
  logger.newLine()
  logger.info('Run `pullwise projects list` to get started')
}

async function loginWithGitHub() {
  const spinner = new Spinner('Initiating GitHub OAuth...')

  const { authUrl } = await authApi.initiateOAuth('github')

  spinner.info('Opening browser for GitHub authentication...')
  await open(authUrl)

  logger.newLine()
  logger.info('After authorizing, paste the token here:')

  const { token } = await inquirer.prompt([
    {
      type: 'input',
      name: 'token',
      message: 'Token:',
      validate: (input: string) => (input ? true : 'Token is required'),
    },
  ])

  await configManager.set('token', token)

  logger.success('Successfully logged in with GitHub!')
}
