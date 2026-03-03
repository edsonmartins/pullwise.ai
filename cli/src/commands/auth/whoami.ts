import { Command } from 'commander'
import { authApi } from '../../lib/api/auth'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const whoamiCommand = new Command('whoami')
  .description('Show current user information')
  .action(async () => {
    try {
      const spinner = new Spinner('Fetching user info...')

      const user = await authApi.getCurrentUser()

      spinner.stop()

      logger.box(
        `
Email: ${user.email}
Name: ${user.name}
Organization: ${user.organization?.name || 'N/A'}
Plan: ${user.organization?.planType || 'N/A'}
      `.trim(),
        { title: 'Current User' }
      )
    } catch (error: any) {
      logger.error('Failed to fetch user info', error.message)
      process.exit(1)
    }
  })
