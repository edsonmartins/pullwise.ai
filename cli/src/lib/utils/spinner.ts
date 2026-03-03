import ora, { Ora } from 'ora'

export class Spinner {
  private spinner: Ora

  constructor(text: string) {
    this.spinner = ora(text).start()
  }

  succeed(text?: string): void {
    this.spinner.succeed(text)
  }

  fail(text?: string): void {
    this.spinner.fail(text)
  }

  warn(text?: string): void {
    this.spinner.warn(text)
  }

  info(text?: string): void {
    this.spinner.info(text)
  }

  update(text: string): void {
    this.spinner.text = text
  }

  stop(): void {
    this.spinner.stop()
  }
}
