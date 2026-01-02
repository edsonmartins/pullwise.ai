// V2 Types for Pullwise Frontend

export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
export type IssueType = 'BUG' | 'VULNERABILITY' | 'CODE_SMELL' | 'LOGIC' | 'PERFORMANCE' | 'SECURITY' | 'STYLE' | 'SUGGESTION' | 'TEST' | 'DOCUMENTATION'
export type ReviewStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
export type FixStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'APPLIED' | 'FAILED' | 'EXPIRED'
export type FixConfidence = 'HIGH' | 'MEDIUM' | 'LOW'

export interface Issue {
  id: number
  reviewId: number
  severity: Severity
  type: IssueType
  title: string
  description?: string
  filePath?: string
  lineStart?: number
  lineEnd?: number
  ruleId?: string
  suggestion?: string
  codeSnippet?: string
  fixedCode?: string
  isFalsePositive: boolean
  createdAt: string
}

export interface FixSuggestion {
  id: number
  reviewId: number
  issueId: number
  status: FixStatus
  confidence: FixConfidence
  fixedCode: string
  originalCode: string
  explanation: string
  filePath?: string
  startLine?: number
  endLine?: number
  branchName?: string
  commitHash?: string
  reviewedBy?: string
  reviewedAt?: string
  appliedAt?: string
  errorMessage?: string
  modelUsed?: string
  inputTokens?: number
  outputTokens?: number
  estimatedCost?: number
  createdAt: string
  updatedAt: string
}

export interface Review {
  id: number
  pullRequestId: number
  status: ReviewStatus
  startedAt?: string
  completedAt?: string
  durationMs?: number
  filesAnalyzed: number
  linesAddedAnalyzed: number
  linesRemovedAnalyzed: number
  sastEnabled: boolean
  llmEnabled: boolean
  ragEnabled: boolean
  errorMessage?: string
  createdAt: string
  updatedAt: string
  stats: ReviewStats
  issues: Issue[]
}

export interface ReviewStats {
  total: number
  critical: number
  high: number
  medium: number
  low: number
  info: number
}

export interface PullRequest {
  id: number
  projectId: number
  platform: string
  prId: number
  prNumber: number
  title: string
  description?: string
  sourceBranch: string
  targetBranch: string
  authorName?: string
  reviewUrl: string
  createdAt: string
  status: string
}

export interface Project {
  id: number
  name: string
  repositoryUrl: string
  platform: string
  repositoryId: string
  autoReviewEnabled: boolean
  createdAt: string
}

export interface CodeGraphNode {
  id: string
  name: string
  type: 'class' | 'interface' | 'enum' | 'function' | 'variable'
  filePath: string
  startLine?: number
  endLine?: number
  complexity?: number
  dependencies: string[]
  dependents: string[]
  metadata?: Record<string, unknown>
}

export interface CodeGraphEdge {
  id: string
  source: string
  target: string
  type: 'depends_on' | 'used_by' | 'implements' | 'extends'
  label?: string
  weight?: number
}

export interface CodeGraphData {
  nodes: CodeGraphNode[]
  edges: CodeGraphEdge[]
  metrics: CodeGraphMetrics
}

export interface CodeGraphMetrics {
  totalNodes: number
  totalEdges: number
  maxComplexity: number
  avgComplexity: number
  hubClasses: string[]
  circularDependencies: string[][]
}

export interface AnalyticsData {
  overview: OverviewMetrics
  trends: TrendData[]
  topIssues: TopIssueData[]
  teamStats: TeamStats[]
}

export interface OverviewMetrics {
  totalReviews: number
  totalIssues: number
  criticalIssues: number
  highIssues: number
  avgReviewTime: number
  autoFixRate: number
  fixableIssues: number
}

export interface TrendData {
  date: string
  reviews: number
  issues: number
  critical: number
  fixed: number
  avgDuration: number
}

export interface TopIssueData {
  type: IssueType
  count: number
  severity: Severity
  avgFixRate: number
}

export interface TeamStats {
  teamId: string
  teamName: string
  reviews: number
  issues: number
  avgReviewTime: number
  avgQuality: number
}

export interface Plugin {
  id: string
  name: string
  version: string
  author: string
  description: string
  type: string
  supportedLanguages: string[]
  installed: boolean
  enabled: boolean
  config?: Record<string, unknown>
}

export interface SandboxResult {
  success: boolean
  output: string
  error: string
  exitCode: number
  durationMs: number
}

export interface IntegrationStatus {
  jiraEnabled: boolean
  linearEnabled: boolean
  autoCreateTickets: boolean
  jiraProjectKey?: string
  linearTeamId?: string
}
