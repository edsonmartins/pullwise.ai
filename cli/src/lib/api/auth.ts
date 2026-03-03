import { apiClient } from './client'

export interface LoginResponse {
  token: string
  user: {
    id: string
    email: string
    name: string
  }
}

export const authApi = {
  login: async (email: string, password: string): Promise<LoginResponse> => {
    const client = await apiClient.getClient()
    const response = await client.post('/auth/login', { email, password })
    return response.data
  },

  getCurrentUser: async () => {
    const client = await apiClient.getClient()
    const response = await client.get('/auth/me')
    return response.data
  },

  initiateOAuth: async (provider: 'github' | 'gitlab'): Promise<{ authUrl: string }> => {
    const client = await apiClient.getClient()
    const response = await client.post(`/auth/oauth/${provider}/initiate`)
    return response.data
  },
}
