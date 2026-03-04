import { Command } from 'commander'
import { execSync } from 'child_process'
import { logger } from '../lib/utils/logger'
import { Spinner } from '../lib/utils/spinner'

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../../package.json')

export const updateCommand = new Command('update')
  .description('Update Pullwise CLI to the latest version')
  .option('--check', 'Only check for updates, do not install')
  .action(async (options) => {
    try {
      const currentVersion = pkg.version
      logger.info(`Current version: ${currentVersion}`)

      const spinner = new Spinner('Checking for updates...')

      // Check npm registry for latest version
      let latestVersion: string
      try {
        latestVersion = execSync('npm view @pullwise/cli version 2>/dev/null', {
          encoding: 'utf-8',
        }).trim()
      } catch {
        spinner.fail('Failed to check for updates')
        logger.info('Could not reach npm registry. Check your internet connection.')
        process.exit(1)
        return
      }

      if (currentVersion === latestVersion) {
        spinner.succeed(`Already on the latest version (${currentVersion})`)
        return
      }

      spinner.info(`New version available: ${latestVersion} (current: ${currentVersion})`)

      if (options.check) {
        logger.info(`Run \`pullwise update\` to install`)
        return
      }

      // Install update
      const installSpinner = new Spinner(`Installing @pullwise/cli@${latestVersion}...`)

      try {
        execSync(`npm install -g @pullwise/cli@${latestVersion}`, {
          encoding: 'utf-8',
          stdio: 'pipe',
        })
        installSpinner.succeed(`Updated to ${latestVersion}`)
      } catch (err: any) {
        installSpinner.fail('Update failed')
        logger.error('Installation error', err.message)
        logger.info('Try running with sudo: sudo pullwise update')
      }
    } catch (error: any) {
      logger.error('Update check failed', error.message)
      process.exit(1)
    }
  })
