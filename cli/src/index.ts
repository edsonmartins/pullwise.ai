#!/usr/bin/env node

import { Command } from 'commander'
import { loginCommand } from './commands/auth/login'
import { logoutCommand } from './commands/auth/logout'
import { whoamiCommand } from './commands/auth/whoami'
import { listProjectsCommand } from './commands/projects/list'
import { addProjectCommand } from './commands/projects/add'
import { listReviewsCommand } from './commands/reviews/list'
import { showReviewCommand } from './commands/reviews/show'
import { triggerReviewCommand } from './commands/reviews/trigger'
import { initCommand } from './commands/init'

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

// Reviews commands
const reviews = program.command('reviews').alias('r').description('Review management')
reviews.addCommand(listReviewsCommand)
reviews.addCommand(showReviewCommand)
reviews.addCommand(triggerReviewCommand)

// Init command
program.addCommand(initCommand)

program.parse()
