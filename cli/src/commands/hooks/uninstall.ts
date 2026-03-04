import { Command } from 'commander'
import fs from 'fs/promises'
import path from 'path'
import { execSync } from 'child_process'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

export const uninstallHooksCommand = new Command('uninstall')
  .description('Remove Pullwise git hooks')
  .action(async () => {
    try {
      let gitRoot: string
      try {
        gitRoot = execSync('git rev-parse --show-toplevel', { encoding: 'utf-8' }).trim()
      } catch {
        logger.error('Not a git repository', 'Run this command from within a git repository')
        process.exit(1)
        return
      }

      const hooksDir = path.join(gitRoot, '.git', 'hooks')
      const spinner = new Spinner('Removing Pullwise git hooks...')

      let removed = 0
      const hookNames = ['pre-commit', 'prepare-commit-msg']

      for (const hookName of hookNames) {
        const hookPath = path.join(hooksDir, hookName)
        try {
          const content = await fs.readFile(hookPath, 'utf-8')
          if (content.includes('pullwise')) {
            await fs.unlink(hookPath)
            removed++
          }
        } catch {
          // Hook doesn't exist, skip
        }
      }

      if (removed > 0) {
        spinner.succeed(`Removed ${removed} Pullwise hook(s)`)
      } else {
        spinner.info('No Pullwise hooks found')
      }
    } catch (error: any) {
      logger.error('Failed to remove hooks', error.message)
      process.exit(1)
    }
  })
