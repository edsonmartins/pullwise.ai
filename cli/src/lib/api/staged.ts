import { apiClient } from './client'

export interface StagedIssue {
  filePath: string
  severity: string
  type: string
  title: string
  description: string
  lineNumber: number | null
  suggestion: string | null
}

export interface StagedReviewResponse {
  totalIssues: number
  hasBlockingIssues: boolean
  issues: StagedIssue[]
}

export const stagedApi = {
  /**
   * Submit staged diff for synchronous review.
   */
  review: async (
    diff: string,
    filePaths: string[],
    commitMessage?: string,
    projectId?: string
  ): Promise<StagedReviewResponse> => {
    const client = await apiClient.getClient()
    const response = await client.post('/api/reviews/staged', {
      diff,
      filePaths,
      commitMessage: commitMessage || null,
      projectId: projectId || null,
    }, {
      timeout: 60000, // 60s timeout for staged reviews
    })
    return response.data
  },
}
