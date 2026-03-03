import { apiClient } from './client'
import { Review, Issue } from '../../types'

export const reviewsApi = {
  list: async (projectId: string): Promise<Review[]> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/api/projects/${projectId}/reviews`)
    return response.data.content
  },

  get: async (reviewId: string): Promise<Review & { issues: Issue[] }> => {
    const client = await apiClient.getClient()
    const response = await client.get(`/api/reviews/${reviewId}`)
    return response.data
  },

  trigger: async (projectId: string, prNumber: number): Promise<Review> => {
    const client = await apiClient.getClient()
    const response = await client.post('/api/reviews/trigger', {
      projectId,
      prNumber,
    })
    return response.data
  },
}
