import { apiClient } from './client'

export interface AutoFixSuggestion {
  id: string
  issueId: string
  originalCode: string
  fixedCode: string
  explanation: string
  status: string
}

export const autofixApi = {
  /**
   * Generate an auto-fix for a specific issue.
   */
  generate: async (issueId: string): Promise<AutoFixSuggestion> => {
    const client = await apiClient.getClient()
    const response = await client.post('/api/autofix/generate', { issueId })
    return response.data
  },

  /**
   * Get auto-fix suggestions for an issue.
   */
  getSuggestions: async (issueId: string): Promise<AutoFixSuggestion[]> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/api/autofix/issue/${issueId}/suggestions`)
    return response.data
  },
}
