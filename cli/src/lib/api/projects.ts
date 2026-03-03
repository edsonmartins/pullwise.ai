import { apiClient } from './client'
import { Project } from '../../types'

export const projectsApi = {
  list: async (): Promise<Project[]> => {
    const client = await apiClient.getClient()
    const response = await client.get('/api/projects')
    return response.data.content
  },

  get: async (projectId: string): Promise<Project> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/api/projects/${projectId}`)
    return response.data
  },

  create: async (data: {
    name: string
    repositoryUrl: string
    platform: 'github' | 'bitbucket' | 'gitlab'
  }): Promise<Project> => {
    const client = await apiClient.getClient()
    const response = await client.post('/api/projects', data)
    return response.data
  },

  delete: async (projectId: string): Promise<void> => {
    const client = await apiClient.getClient()
    await client.delete(`/api/projects/${projectId}`)
  },

  syncKnowledge: async (projectId: string): Promise<void> => {
    const client = await apiClient.getClient()
    await client.post(`/api/projects/${projectId}/sync-knowledge`)
  },
}
