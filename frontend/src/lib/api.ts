import axios, { AxiosError } from 'axios'

const API_BASE = '/api'

export const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Interceptor para adicionar token JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Interceptor para tratar erros
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Types
export interface User {
  id: number
  email: string
  username: string
  displayName: string
  avatarUrl?: string
  createdAt: string
}

export interface Organization {
  id: number
  name: string
  slug: string
  planType: 'FREE' | 'PRO' | 'ENTERPRISE'
  repoCount?: number
  reviewCount?: number
  memberCount?: number
  createdAt?: string
}

export interface Project {
  id: number
  name: string
  description?: string
  platform: 'GITHUB' | 'BITBUCKET'
  repositoryUrl?: string
  repositoryId?: string
  githubInstallationId?: number
  autoReviewEnabled: boolean
  isActive: boolean
  createdAt: string
  organization?: Organization
}

export interface PullRequest {
  id: number
  projectId: number
  platform: 'GITHUB' | 'BITBUCKET'
  prId: number
  prNumber: number
  title: string
  description?: string
  sourceBranch: string
  targetBranch: string
  authorName: string
  authorEmail?: string
  reviewUrl?: string
  isClosed: boolean
  isMerged: boolean
  createdAt: string
  updatedAt: string
}

export interface Issue {
  id: number
  reviewId: number
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
  type: string
  source: string
  title: string
  description?: string
  filePath?: string
  lineStart?: number
  lineEnd?: number
  ruleId?: string
  suggestion?: string
  isFalsePositive: boolean
}

export interface Review {
  id: number
  pullRequestId: number
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  sastEnabled: boolean
  llmEnabled: boolean
  ragEnabled: boolean
  filesAnalyzed?: number
  linesAddedAnalyzed?: number
  linesRemovedAnalyzed?: number
  reviewCommentId?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
  stats?: {
    total: number
    critical: number
    high: number
    medium: number
    low: number
    info: number
  }
}

export interface AuthResponse {
  token: string
  expiresAt: string
  user: User
  organizations: Organization[]
}

// API Functions
export const authApi = {
  async login(token: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/callback', {}, {
      headers: { Authorization: `Bearer ${token}` },
    })
    localStorage.setItem('token', response.data.token)
    return response.data
  },

  async me(): Promise<User> {
    const response = await api.get<User>('/auth/me')
    return response.data
  },

  logout() {
    localStorage.removeItem('token')
  },
}

export const organizationsApi = {
  async list(): Promise<Organization[]> {
    const response = await api.get<Organization[]>('/organizations')
    return response.data
  },

  async get(id: number): Promise<Organization> {
    const response = await api.get<Organization>(`/organizations/${id}`)
    return response.data
  },

  async create(data: {
    name: string
    slug: string
    planType: string
  }): Promise<Organization> {
    const response = await api.post<Organization>('/organizations', data)
    return response.data
  },

  async update(id: number, data: Partial<Organization>): Promise<Organization> {
    const response = await api.put<Organization>(`/organizations/${id}`, data)
    return response.data
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/organizations/${id}`)
  },

  async getProjects(id: number): Promise<Project[]> {
    const response = await api.get<Project[]>(`/organizations/${id}/projects`)
    return response.data
  },

  async getSubscription(id: number): Promise<SubscriptionResponse> {
    const response = await api.get<SubscriptionResponse>(`/billing/organizations/${id}/subscription`)
    return response.data
  },

  async startCheckout(id: number, plan: string): Promise<{ checkoutUrl: string }> {
    const response = await api.post<{ checkoutUrl: string }>(
      `/billing/organizations/${id}/checkout`,
      null,
      { params: { plan } }
    )
    return response.data
  },

  async getPortalUrl(id: number): Promise<{ portalUrl: string }> {
    const response = await api.post<{ portalUrl: string }>(
      `/billing/organizations/${id}/portal`,
      null
    )
    return response.data
  },

  async cancelSubscription(id: number): Promise<void> {
    await api.post(`/billing/organizations/${id}/cancel`)
  },
}

export interface SubscriptionResponse {
  id: number
  organizationId: number
  organizationName: string
  planType: string
  status: string
  stripeSubscriptionId?: string
  stripeCustomerId?: string
  currentPeriodStart?: string
  currentPeriodEnd?: string
  cancelAtPeriodEnd?: boolean
  trialStart?: string
  trialEnd?: string
  createdAt?: string
  updatedAt?: string
}

export const projectsApi = {
  async list(): Promise<Project[]> {
    const response = await api.get<Project[]>('/projects')
    return response.data
  },

  async get(id: number): Promise<Project> {
    const response = await api.get<Project>(`/projects/${id}`)
    return response.data
  },

  async create(data: {
    name: string
    description?: string
    platform: string
    repositoryUrl?: string
    repositoryId?: string
    githubInstallationId?: number
    autoReviewEnabled?: boolean
  }): Promise<Project> {
    const response = await api.post<Project>('/projects', data)
    return response.data
  },

  async update(id: number, data: Partial<Project>): Promise<Project> {
    const response = await api.put<Project>(`/projects/${id}`, data)
    return response.data
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/projects/${id}`)
  },
}

export const pullRequestsApi = {
  async list(projectId: number): Promise<PullRequest[]> {
    const response = await api.get<PullRequest[]>(`/projects/${projectId}/pull-requests`)
    return response.data
  },

  async get(id: number): Promise<PullRequest> {
    const response = await api.get<PullRequest>(`/pull-requests/${id}`)
    return response.data
  },
}

export const reviewsApi = {
  async list(projectId?: number): Promise<Review[]> {
    const response = await api.get<Review[]>('/reviews', {
      params: projectId ? { projectId } : undefined,
    })
    return response.data
  },

  async get(id: number): Promise<Review> {
    const response = await api.get<Review>(`/reviews/${id}`)
    return response.data
  },

  async getIssues(id: number): Promise<Issue[]> {
    const response = await api.get<Issue[]>(`/reviews/${id}/issues`)
    return response.data
  },

  async create(data: {
    pullRequestId: number
    sastEnabled: boolean
    llmEnabled: boolean
    ragEnabled: boolean
  }): Promise<Review> {
    const response = await api.post<Review>('/reviews', data)
    return response.data
  },

  async cancel(id: number): Promise<void> {
    await api.post(`/reviews/${id}/cancel`)
  },

  async markFalsePositive(issueId: number): Promise<void> {
    await api.post(`/reviews/issues/${issueId}/false-positive`)
  },
}
