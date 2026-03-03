import axios, { AxiosInstance, AxiosError } from 'axios'
import { configManager } from '../config/manager'
import { ApiError } from '../../types'

class ApiClient {
  private client: AxiosInstance | null = null

  async getClient(): Promise<AxiosInstance> {
    if (this.client) {
      return this.client
    }

    const config = await configManager.load()

    this.client = axios.create({
      baseURL: config.apiUrl,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor — attach token
    this.client.interceptors.request.use(
      async (reqConfig) => {
        const token = await configManager.get('token')
        if (token) {
          reqConfig.headers.Authorization = `Bearer ${token}`
        }
        return reqConfig
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor — normalize errors
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        const apiError: ApiError = {
          message: error.message,
          status: error.response?.status,
        }

        if (error.response?.data) {
          const data = error.response.data as Record<string, unknown>
          apiError.message = (data.message as string) || error.message
          apiError.errors = data.errors as Record<string, string[]>
        }

        return Promise.reject(apiError)
      }
    )

    return this.client
  }
}

export const apiClient = new ApiClient()
