export interface Review {
  id: string;
  pullRequestId: string;
  prNumber: number;
  prTitle: string;
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'cancelled';
  sastIssuesCount: number;
  llmIssuesCount: number;
  qualityScore?: number;
  tokensUsed?: number;
  createdAt: string;
  completedAt?: string;
  stats?: ReviewStats;
}

export interface ReviewStats {
  totalIssues: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  infoCount: number;
}

export interface Issue {
  id: string;
  reviewId: string;
  filePath: string;
  lineStart?: number;
  lineEnd?: number;
  source: string;
  type: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  title: string;
  description?: string;
  ruleId?: string;
  suggestion?: string;
  codeSnippet?: string;
  fixedCode?: string;
  isFalsePositive: boolean;
  createdAt: string;
}

export interface Project {
  id: string;
  name: string;
  repositoryUrl: string;
  platform: 'github' | 'bitbucket' | 'gitlab' | 'azure_devops';
  isActive: boolean;
}

export interface ApiError {
  message: string;
  status?: number;
  errors?: Record<string, string[]>;
}

export type SeverityLevel = 'critical' | 'high' | 'medium' | 'low' | 'info';
