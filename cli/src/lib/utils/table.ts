import Table from 'cli-table3'
import chalk from 'chalk'

export function createTable(head: string[], rows: string[][]): string {
  const table = new Table({
    head: head.map((h) => chalk.cyan(h)),
    style: {
      head: [],
      border: ['gray'],
    },
  })

  rows.forEach((row) => table.push(row))

  return table.toString()
}

export function formatIssueTable(issues: Array<{
  severity: string
  filePath: string
  lineNumber?: number
  message: string
}>): string {
  const table = new Table({
    head: [
      chalk.cyan('Severity'),
      chalk.cyan('File'),
      chalk.cyan('Line'),
      chalk.cyan('Message'),
    ],
    colWidths: [12, 40, 8, 60],
    wordWrap: true,
  })

  issues.forEach((issue) => {
    const severityColor =
      issue.severity === 'critical'
        ? chalk.red
        : issue.severity === 'high'
          ? chalk.hex('#FFA500')
          : issue.severity === 'medium'
            ? chalk.yellow
            : chalk.blue

    table.push([
      severityColor(issue.severity.toUpperCase()),
      issue.filePath,
      issue.lineNumber?.toString() || '-',
      issue.message,
    ])
  })

  return table.toString()
}
