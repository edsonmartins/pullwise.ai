export interface Config {
  apiUrl: string
  token?: string
  currentOrg?: string
  defaultProject?: string
}

export interface Project {
  id: string
  name: string
  repositoryUrl: string
  platform: 'github' | 'bitbucket' | 'gitlab'
  isActive: boolean
}

export interface Review {
  id: string
  pullRequestId: string
  prNumber: number
  prTitle: string
  status: 'pending' | 'in_progress' | 'completed' | 'failed'
  sastIssuesCount: number
  llmIssuesCount: number
  qualityScore?: number
  tokensUsed?: number
  createdAt: string
  completedAt?: string
}

export interface Issue {
  id: string
  filePath: string
  lineNumber?: number
  source: string
  type: string
  severity: 'critical' | 'high' | 'medium' | 'low'
  message: string
  suggestedFix?: string
}

export interface ApiError {
  message: string
  status?: number
  errors?: Record<string, string[]>
}
