import { Command } from 'commander'
import { configManager } from '../../lib/config/manager'
import { logger } from '../../lib/utils/logger'

export const logoutCommand = new Command('logout')
  .description('Logout from Pullwise')
  .action(async () => {
    try {
      await configManager.clear()
      logger.success('Successfully logged out')
    } catch (error: any) {
      logger.error('Logout failed', error.message)
      process.exit(1)
    }
  })
