import chalk from 'chalk'
import boxen from 'boxen'

export const logger = {
  success: (message: string) => {
    console.log(chalk.green('\u2713'), message)
  },

  error: (message: string, details?: string) => {
    console.log(chalk.red('\u2717'), chalk.red(message))
    if (details) {
      console.log(chalk.gray(details))
    }
  },

  warning: (message: string) => {
    console.log(chalk.yellow('\u26A0'), chalk.yellow(message))
  },

  info: (message: string) => {
    console.log(chalk.blue('\u2139'), message)
  },

  box: (message: string, options?: { title?: string; color?: string }) => {
    console.log(
      boxen(message, {
        padding: 1,
        margin: 1,
        borderStyle: 'round',
        borderColor: (options?.color || 'cyan') as any,
        title: options?.title,
      })
    )
  },

  log: (message: string) => {
    console.log(message)
  },

  newLine: () => {
    console.log()
  },
}
