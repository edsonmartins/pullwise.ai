import { cosmiconfig } from 'cosmiconfig'
import fs from 'fs/promises'
import path from 'path'
import os from 'os'
import { Config } from '../../types'

const CONFIG_DIR = path.join(os.homedir(), '.pullwise')
const CONFIG_FILE = path.join(CONFIG_DIR, 'config.json')

export class ConfigManager {
  private config: Config | null = null

  async load(): Promise<Config> {
    if (this.config) {
      return this.config
    }

    try {
      const content = await fs.readFile(CONFIG_FILE, 'utf-8')
      this.config = JSON.parse(content)
      return this.config!
    } catch {
      this.config = {
        apiUrl: process.env.PULLWISE_API_URL || 'https://api.pullwise.ai',
      }
      return this.config
    }
  }

  async save(config: Partial<Config>): Promise<void> {
    const current = await this.load()
    this.config = { ...current, ...config }

    await fs.mkdir(CONFIG_DIR, { recursive: true })
    await fs.writeFile(CONFIG_FILE, JSON.stringify(this.config, null, 2))
  }

  async get<K extends keyof Config>(key: K): Promise<Config[K] | undefined> {
    const config = await this.load()
    return config[key]
  }

  async set<K extends keyof Config>(key: K, value: Config[K]): Promise<void> {
    await this.save({ [key]: value })
  }

  async clear(): Promise<void> {
    try {
      await fs.unlink(CONFIG_FILE)
      this.config = null
    } catch {
      // File doesn't exist, ignore
    }
  }

  async loadProjectConfig(): Promise<Record<string, unknown> | null> {
    const explorer = cosmiconfig('pullwise')
    const result = await explorer.search()
    return result?.config || null
  }
}

export const configManager = new ConfigManager()
