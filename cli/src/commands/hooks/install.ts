import { Command } from 'commander'
import fs from 'fs/promises'
import path from 'path'
import { execSync } from 'child_process'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

const PRE_COMMIT_HOOK = `#!/bin/sh
# Pullwise pre-commit hook — AI code review on staged changes
# Installed by: pullwise hooks install

# Skip if PULLWISE_SKIP is set (e.g., during rebases)
if [ -n "$PULLWISE_SKIP" ]; then
  exit 0
fi

# Run staged review
pullwise review --staged --fail-on-blocking
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo ""
  echo "Pullwise found blocking issues. Commit aborted."
  echo "Use PULLWISE_SKIP=1 git commit to bypass."
  exit 1
fi
`

const PREPARE_COMMIT_MSG_HOOK = `#!/bin/sh
# Pullwise prepare-commit-msg hook — annotate commit with review summary
# Installed by: pullwise hooks install

if [ -n "$PULLWISE_SKIP" ]; then
  exit 0
fi

# Only annotate if review ran successfully
if command -v pullwise &> /dev/null; then
  REVIEW_SUMMARY=$(pullwise review --staged --summary-only 2>/dev/null)
  if [ -n "$REVIEW_SUMMARY" ] && [ "$REVIEW_SUMMARY" != "[]" ]; then
    echo "" >> "$1"
    echo "# Pullwise Review: $REVIEW_SUMMARY" >> "$1"
  fi
fi
`

export const installHooksCommand = new Command('install')
  .description('Install Pullwise git hooks for pre-commit review')
  .option('--pre-commit', 'Install pre-commit hook (blocks on critical/high issues)', true)
  .option('--prepare-commit-msg', 'Install prepare-commit-msg hook (annotates commit message)')
  .option('--force', 'Overwrite existing hooks')
  .action(async (options) => {
    try {
      // Find git root
      let gitRoot: string
      try {
        gitRoot = execSync('git rev-parse --show-toplevel', { encoding: 'utf-8' }).trim()
      } catch {
        logger.error('Not a git repository', 'Run this command from within a git repository')
        process.exit(1)
        return
      }

      const hooksDir = path.join(gitRoot, '.git', 'hooks')
      const spinner = new Spinner('Installing Pullwise git hooks...')

      let installed = 0

      // Install pre-commit hook
      if (options.preCommit !== false) {
        const hookPath = path.join(hooksDir, 'pre-commit')
        const exists = await fileExists(hookPath)

        if (exists && !options.force) {
          const content = await fs.readFile(hookPath, 'utf-8')
          if (!content.includes('pullwise')) {
            spinner.warn('pre-commit hook already exists. Use --force to overwrite.')
          } else {
            spinner.info('Pullwise pre-commit hook already installed')
          }
        } else {
          await fs.writeFile(hookPath, PRE_COMMIT_HOOK, { mode: 0o755 })
          installed++
        }
      }

      // Install prepare-commit-msg hook
      if (options.prepareCommitMsg) {
        const hookPath = path.join(hooksDir, 'prepare-commit-msg')
        const exists = await fileExists(hookPath)

        if (exists && !options.force) {
          const content = await fs.readFile(hookPath, 'utf-8')
          if (!content.includes('pullwise')) {
            spinner.warn('prepare-commit-msg hook already exists. Use --force to overwrite.')
          } else {
            spinner.info('Pullwise prepare-commit-msg hook already installed')
          }
        } else {
          await fs.writeFile(hookPath, PREPARE_COMMIT_MSG_HOOK, { mode: 0o755 })
          installed++
        }
      }

      if (installed > 0) {
        spinner.succeed(`Installed ${installed} Pullwise hook(s)`)
      } else {
        spinner.info('No hooks installed (already present or skipped)')
      }

      logger.newLine()
      logger.info('Pullwise will review staged changes before each commit')
      logger.info('Set PULLWISE_SKIP=1 to bypass: PULLWISE_SKIP=1 git commit')
    } catch (error: any) {
      logger.error('Failed to install hooks', error.message)
      process.exit(1)
    }
  })

async function fileExists(filepath: string): Promise<boolean> {
  try {
    await fs.access(filepath)
    return true
  } catch {
    return false
  }
}
