#!/usr/bin/env node

import { Command } from 'commander'
import { loginCommand } from './commands/auth/login'
import { logoutCommand } from './commands/auth/logout'
import { whoamiCommand } from './commands/auth/whoami'
import { listProjectsCommand } from './commands/projects/list'
import { addProjectCommand } from './commands/projects/add'
import { removeProjectCommand } from './commands/projects/remove'
import { syncProjectCommand } from './commands/projects/sync'
import { configProjectCommand } from './commands/projects/config'
import { listReviewsCommand } from './commands/reviews/list'
import { showReviewCommand } from './commands/reviews/show'
import { triggerReviewCommand } from './commands/reviews/trigger'
import { verifyReviewCommand } from './commands/reviews/verify'
import { interactReviewCommand } from './commands/reviews/interact'
import { watchReviewCommand } from './commands/reviews/watch'
import { initCommand } from './commands/init'
import { updateCommand } from './commands/update'
import { reviewCommand } from './commands/review'
import { installHooksCommand } from './commands/hooks/install'
import { uninstallHooksCommand } from './commands/hooks/uninstall'

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../package.json')

const program = new Command()

program
  .name('pullwise')
  .description('CLI for Pullwise - AI-powered code reviews')
  .version(pkg.version)

// Auth commands
const auth = program.command('auth').description('Authentication commands')
auth.addCommand(loginCommand)
auth.addCommand(logoutCommand)
auth.addCommand(whoamiCommand)

// Projects commands
const projects = program.command('projects').alias('p').description('Project management')
projects.addCommand(listProjectsCommand)
projects.addCommand(addProjectCommand)
projects.addCommand(removeProjectCommand)
projects.addCommand(syncProjectCommand)
projects.addCommand(configProjectCommand)

// Reviews commands
const reviews = program.command('reviews').alias('r').description('Review management')
reviews.addCommand(listReviewsCommand)
reviews.addCommand(showReviewCommand)
reviews.addCommand(triggerReviewCommand)
reviews.addCommand(verifyReviewCommand)
reviews.addCommand(interactReviewCommand)
reviews.addCommand(watchReviewCommand)

// Hooks commands
const hooks = program.command('hooks').alias('h').description('Git hooks management')
hooks.addCommand(installHooksCommand)
hooks.addCommand(uninstallHooksCommand)

// Review command (staged review)
program.addCommand(reviewCommand)

// Init command
program.addCommand(initCommand)

// Update command
program.addCommand(updateCommand)

program.parse()
