import axios from 'axios'
import type {
  FixSuggestion,
  CodeGraphData,
  Plugin,
  SandboxResult,
  IntegrationStatus
} from '@/types/v2'

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Auto-Fix API
export const autofixApi = {
  generateFix: async (issueId: number, fileContent: string, branchName: string) => {
    const response = await api.post('/autofix/generate', {
      issueId,
      fileContent,
      branchName,
    })
    return response.data
  },

  applyFix: async (suggestionId: number, authToken?: string) => {
    const response = await api.post(`/autofix/apply`, {
      suggestionId,
      authToken,
    })
    return response.data
  },

  generateAndApply: async (issueId: number, fileContent: string, authToken: string) => {
    const response = await api.post(`/autofix/generate-and-apply`, {
      issueId,
      fileContent,
      authToken,
    })
    return response.data
  },

  approveSuggestion: async (suggestionId: number, reviewedBy: string) => {
    await api.post(`/autofix/${suggestionId}/approve`, { reviewedBy })
  },

  rejectSuggestion: async (suggestionId: number, reviewedBy: string, reason: string) => {
    await api.post(`/autofix/${suggestionId}/reject`, { reviewedBy, reason })
  },

  listByReview: async (reviewId: number, status?: string) => {
    const params = status ? { status } : {}
    const response = await api.get<FixSuggestion[]>(`/autofix/reviews/${reviewId}`, { params })
    return response.data
  },

  listReadyToApply: async (reviewId: number) => {
    const response = await api.get<FixSuggestion[]>(`/autofix/reviews/${reviewId}/ready`)
    return response.data
  },

  getSuggestion: async (suggestionId: number) => {
    const response = await api.get<FixSuggestion>(`/autofix/${suggestionId}`)
    return response.data
  },

  validateFix: async (originalCode: string, fixedCode: string, language: string) => {
    const response = await api.post(`/autofix/validate`, {
      originalCode,
      fixedCode,
      language,
    })
    return response.data
  },
}

// Code Graph API
export const codeGraphApi = {
  analyze: async (projectId: number, branch?: string) => {
    const response = await api.post<CodeGraphData>('/code-graph/analyze', { projectId, branch })
    return response.data
  },

  getImpact: async (projectId: number, filePaths: string[]) => {
    const response = await api.post('/code-graph/impact', { projectId, filePaths })
    return response.data
  },

  getDependencies: async (projectId: number, filePath: string) => {
    const response = await api.post('/code-graph/dependencies', {
      projectId,
      filePath,
    })
    return response.data
  },

  getDependents: async (projectId: number, filePath: string) => {
    const response = await api.post('/code-graph/dependents', {
      projectId,
      filePath,
    })
    return response.data
  },

  getBlastRadius: async (projectId: number, filePath: string, maxDepth?: number) => {
    const response = await api.post('/code-graph/blast-radius', {
      projectId,
      filePath,
      maxDepth,
    })
    return response.data
  },
}

// Analytics API
export const analyticsApi = {
  getOverview: async (timeRange = '30d') => {
    const response = await api.get(`/analytics/overview?timeRange=${timeRange}`)
    return response.data
  },

  getTrends: async (timeRange = '30d') => {
    const response = await api.get(`/analytics/trends?timeRange=${timeRange}`)
    return response.data
  },

  getTopIssues: async (limit = 10) => {
    const response = await api.get(`/analytics/top-issues?limit=${limit}`)
    return response.data
  },

  getTeamStats: async () => {
    const response = await api.get('/analytics/team')
    return response.data
  },

  getReviewMetrics: async (reviewId: number) => {
    const response = await api.get(`/analytics/reviews/${reviewId}`)
    return response.data
  },
}

// Plugin API
export const pluginApi = {
  list: async () => {
    const response = await api.get<{ plugins: Plugin[] }>('/plugins')
    return response.data.plugins
  },

  getInstalled: async () => {
    const response = await api.get<{ plugins: Plugin[] }>('/plugins/installed')
    return response.data.plugins
  },

  install: async (pluginId: string) => {
    const response = await api.post(`/plugins/${pluginId}/install`)
    return response.data
  },

  uninstall: async (pluginId: string) => {
    const response = await api.delete(`/plugins/${pluginId}`)
    return response.data
  },

  enable: async (pluginId: string) => {
    const response = await api.put(`/plugins/${pluginId}/enable`)
    return response.data
  },

  disable: async (pluginId: string) => {
    const response = await api.put(`/plugins/${pluginId}/disable`)
    return response.data
  },

  updateConfig: async (pluginId: string, config: Record<string, unknown>) => {
    const response = await api.put(`/plugins/${pluginId}/config`, config)
    return response.data
  },
}

// Sandbox API
export const sandboxApi = {
  execute: async (code: string, language: string, input?: string) => {
    const response = await api.post<SandboxResult>('/sandbox/execute', {
      code,
      language,
      input,
    })
    return response.data
  },

  validateFix: async (originalCode: string, fixedCode: string, language: string) => {
    const response = await api.post('/sandbox/validate', {
      originalCode,
      fixedCode,
      language,
    })
    return response.data
  },

  checkSecurity: async (code: string, language: string) => {
    const response = await api.post('/sandbox/security', {
      code,
      language,
    })
    return response.data
  },

  analyzeCode: async (code: string, language: string) => {
    const response = await api.post('/sandbox/analyze', {
      code,
      language,
    })
    return response.data
  },
}

// Integration API
export const integrationApi = {
  getStatus: async () => {
    const response = await api.get<IntegrationStatus>('/integrations/status')
    return response.data
  },

  // Jira
  getJiraTicket: async (ticketKey: string) => {
    const response = await api.get(`/integrations/jira/${ticketKey}`)
    return response.data
  },

  addJiraComment: async (ticketKey: string, comment: string) => {
    await api.post(`/integrations/jira/${ticketKey}/comment`, { comment })
  },

  updateJiraStatus: async (ticketKey: string, status: string) => {
    await api.put(`/integrations/jira/${ticketKey}/status`, { status })
  },

  // Linear
  getLinearIssue: async (issueId: string) => {
    const response = await api.get(`/integrations/linear/${issueId}`)
    return response.data
  },

  getLinearStates: async (teamId: string) => {
    const response = await api.get(`/integrations/linear/states?teamId=${teamId}`)
    return response.data
  },

  addLinearComment: async (issueId: string, comment: string) => {
    await api.post(`/integrations/linear/${issueId}/comment`, { comment })
  },
}
