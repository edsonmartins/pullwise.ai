# Frontend - Pullwise (SaaS de Code Review com IA)

## 🦉 Pullwise - Wise Reviews for Every Pull

**App Principal:** https://pullwise.ai  
**Documentação Dev:** https://pullwise.dev

---

## Visão Geral

**Stack Tecnológica:**
```
React 18+
TypeScript
Vite (build tool)
TailwindCSS (styling)
Shadcn/ui (component library)
React Query / TanStack Query (data fetching)
Zustand (state management)
React Router v6 (routing)
Recharts (analytics)
React Hook Form + Zod (forms)
Axios (HTTP client)
```

---

## Arquitetura Frontend

```
┌─────────────────────────────────────────────────────────────┐
│                    ARQUITETURA FRONTEND                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Public Routes                          │    │
│  │  ├─ Landing Page                                    │    │
│  │  ├─ Pricing                                         │    │
│  │  ├─ Login/Signup                                    │    │
│  │  └─ OAuth Callback                                  │    │
│  └────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Protected Routes (Dashboard)                │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │  Navigation (Sidebar + Header)                │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                                                     │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │  Pages:                                       │  │    │
│  │  │  ├─ Dashboard/Overview                        │  │    │
│  │  │  ├─ Projects                                  │  │    │
│  │  │  │   ├─ List                                  │  │    │
│  │  │  │   ├─ Detail                                │  │    │
│  │  │  │   └─ Configuration                         │  │    │
│  │  │  ├─ Reviews                                   │  │    │
│  │  │  │   ├─ List                                  │  │    │
│  │  │  │   └─ Detail                                │  │    │
│  │  │  ├─ Analytics                                 │  │    │
│  │  │  ├─ Settings                                  │  │    │
│  │  │  │   ├─ Organization                          │  │    │
│  │  │  │   ├─ Team Members                          │  │    │
│  │  │  │   ├─ Integrations                          │  │    │
│  │  │  │   └─ Billing                               │  │    │
│  │  │  └─ Documentation                             │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │           API Layer (Axios + React Query)           │    │
│  │  ├─ /api/auth                                       │    │
│  │  ├─ /api/projects                                   │    │
│  │  ├─ /api/reviews                                    │    │
│  │  ├─ /api/analytics                                  │    │
│  │  └─ /api/billing                                    │    │
│  └────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          ▼                                   │
│                    Backend API                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Estrutura de Diretórios

```
frontend/
├── public/
│   ├── favicon.ico
│   └── logo.svg
│
├── src/
│   ├── components/
│   │   ├── ui/              # Shadcn/ui components
│   │   │   ├── button.tsx
│   │   │   ├── card.tsx
│   │   │   ├── dialog.tsx
│   │   │   ├── dropdown-menu.tsx
│   │   │   ├── input.tsx
│   │   │   ├── select.tsx
│   │   │   ├── table.tsx
│   │   │   ├── tabs.tsx
│   │   │   └── toast.tsx
│   │   │
│   │   ├── layout/
│   │   │   ├── AppLayout.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Header.tsx
│   │   │   └── Footer.tsx
│   │   │
│   │   ├── auth/
│   │   │   ├── LoginForm.tsx
│   │   │   ├── SignupForm.tsx
│   │   │   └── OAuthButtons.tsx
│   │   │
│   │   ├── projects/
│   │   │   ├── ProjectCard.tsx
│   │   │   ├── ProjectList.tsx
│   │   │   ├── CreateProjectDialog.tsx
│   │   │   ├── ProjectSettings.tsx
│   │   │   └── ConfigurationEditor.tsx
│   │   │
│   │   ├── reviews/
│   │   │   ├── ReviewCard.tsx
│   │   │   ├── ReviewList.tsx
│   │   │   ├── ReviewDetail.tsx
│   │   │   ├── IssuesList.tsx
│   │   │   ├── IssueCard.tsx
│   │   │   └── CodeViewer.tsx
│   │   │
│   │   ├── analytics/
│   │   │   ├── MetricsCard.tsx
│   │   │   ├── QualityTrendChart.tsx
│   │   │   ├── IssuesBreakdownChart.tsx
│   │   │   └── UsageChart.tsx
│   │   │
│   │   └── billing/
│   │       ├── PricingCards.tsx
│   │       ├── SubscriptionStatus.tsx
│   │       └── UsageProgress.tsx
│   │
│   ├── pages/
│   │   ├── public/
│   │   │   ├── LandingPage.tsx
│   │   │   ├── PricingPage.tsx
│   │   │   ├── LoginPage.tsx
│   │   │   └── SignupPage.tsx
│   │   │
│   │   └── dashboard/
│   │       ├── DashboardPage.tsx
│   │       ├── ProjectsPage.tsx
│   │       ├── ProjectDetailPage.tsx
│   │       ├── ReviewsPage.tsx
│   │       ├── ReviewDetailPage.tsx
│   │       ├── AnalyticsPage.tsx
│   │       └── SettingsPage.tsx
│   │
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts           # Axios instance
│   │   │   ├── auth.ts
│   │   │   ├── projects.ts
│   │   │   ├── reviews.ts
│   │   │   ├── analytics.ts
│   │   │   └── billing.ts
│   │   │
│   │   ├── hooks/
│   │   │   ├── useAuth.ts
│   │   │   ├── useProjects.ts
│   │   │   ├── useReviews.ts
│   │   │   ├── useAnalytics.ts
│   │   │   └── useBilling.ts
│   │   │
│   │   └── utils/
│   │       ├── format.ts
│   │       ├── date.ts
│   │       └── validation.ts
│   │
│   ├── store/
│   │   ├── authStore.ts
│   │   ├── organizationStore.ts
│   │   └── uiStore.ts
│   │
│   ├── types/
│   │   ├── api.ts
│   │   ├── models.ts
│   │   └── enums.ts
│   │
│   ├── App.tsx
│   ├── main.tsx
│   └── routes.tsx
│
├── .env.example
├── .eslintrc.json
├── .prettierrc
├── index.html
├── package.json
├── postcss.config.js
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

---

## Setup Inicial

### package.json

```json
{
  "name": "codereview-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "format": "prettier --write \"src/**/*.{ts,tsx}\""
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@tanstack/react-query": "^5.17.0",
    "zustand": "^4.4.7",
    "axios": "^1.6.2",
    "react-hook-form": "^7.49.0",
    "zod": "^3.22.4",
    "@hookform/resolvers": "^3.3.3",
    "recharts": "^2.10.3",
    "date-fns": "^3.0.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.2.0",
    "lucide-react": "^0.300.0",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-tabs": "^1.0.4",
    "@radix-ui/react-toast": "^1.1.5",
    "class-variance-authority": "^0.7.0",
    "prism-react-renderer": "^2.3.1"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@typescript-eslint/eslint-plugin": "^6.14.0",
    "@typescript-eslint/parser": "^6.14.0",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.16",
    "eslint": "^8.55.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "postcss": "^8.4.32",
    "prettier": "^3.1.1",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.2.2",
    "vite": "^5.0.8"
  }
}
```

### vite.config.ts

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### tailwind.config.js

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
```

---

## Types & Models

### src/types/models.ts

```typescript
export interface Organization {
  id: string
  name: string
  slug: string
  planType: 'free' | 'pro' | 'enterprise'
  settings: Record<string, any>
  createdAt: string
  updatedAt: string
}

export interface User {
  id: string
  email: string
  name: string
  avatarUrl?: string
  role: 'admin' | 'member' | 'viewer'
}

export interface Project {
  id: string
  organizationId: string
  name: string
  repositoryUrl: string
  platform: 'github' | 'bitbucket'
  repositoryId: string
  defaultBranch: string
  isActive: boolean
  settings: ProjectSettings
  createdAt: string
  updatedAt: string
}

export interface ProjectSettings {
  autoReview: boolean
  reviewOnDraft: boolean
  llmProvider?: 'openrouter' | 'ollama'
  pathInstructions: PathInstruction[]
  customRules: CustomRule[]
}

export interface PathInstruction {
  path: string
  instructions: string
}

export interface Review {
  id: string
  pullRequestId: string
  pullRequest: PullRequest
  status: 'pending' | 'in_progress' | 'completed' | 'failed'
  startedAt?: string
  completedAt?: string
  sastIssuesCount: number
  llmIssuesCount: number
  qualityScore?: number
  tokensUsed?: number
  llmProvider?: string
  costCents?: number
  summary?: string
  issues: Issue[]
  createdAt: string
}

export interface PullRequest {
  id: string
  projectId: string
  prNumber: number
  title: string
  author: string
  sourceBranch: string
  targetBranch: string
  commitSha: string
  state: 'open' | 'closed' | 'merged'
}

export interface Issue {
  id: string
  reviewId: string
  filePath: string
  lineNumber?: number
  source: 'sonarqube' | 'checkstyle' | 'pmd' | 'spotbugs' | 'llm'
  type: string
  severity: 'critical' | 'high' | 'medium' | 'low'
  message: string
  description?: string
  reasoning?: string
  suggestedFix?: string
  isResolved: boolean
  resolutionType?: 'accepted' | 'rejected' | 'wontfix'
}

export interface Subscription {
  id: string
  organizationId: string
  planType: 'free' | 'pro' | 'enterprise'
  status: 'active' | 'trialing' | 'canceled' | 'past_due'
  currentPeriodStart: string
  currentPeriodEnd: string
  cancelAtPeriodEnd: boolean
}

export interface UsageRecord {
  periodMonth: string
  reviewsCount: number
  tokensUsed: number
  costCents: number
}
```

---

## API Client

### src/lib/api/client.ts

```typescript
import axios, { AxiosError } from 'axios'
import { useAuthStore } from '@/store/authStore'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor para adicionar token
apiClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor para tratar erros
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
```

### src/lib/api/projects.ts

```typescript
import apiClient from './client'
import { Project, ProjectSettings } from '@/types/models'

export interface CreateProjectRequest {
  name: string
  repositoryUrl: string
  platform: 'github' | 'bitbucket'
  settings?: Partial<ProjectSettings>
}

export const projectsApi = {
  list: async (page = 0, size = 20) => {
    const response = await apiClient.get<{ content: Project[]; totalPages: number }>(
      '/projects',
      { params: { page, size } }
    )
    return response.data
  },

  get: async (projectId: string) => {
    const response = await apiClient.get<Project>(`/projects/${projectId}`)
    return response.data
  },

  create: async (data: CreateProjectRequest) => {
    const response = await apiClient.post<Project>('/projects', data)
    return response.data
  },

  update: async (projectId: string, data: Partial<Project>) => {
    const response = await apiClient.put<Project>(`/projects/${projectId}`, data)
    return response.data
  },

  delete: async (projectId: string) => {
    await apiClient.delete(`/projects/${projectId}`)
  },

  syncKnowledge: async (projectId: string) => {
    await apiClient.post(`/projects/${projectId}/sync-knowledge`)
  },
}
```

### src/lib/api/reviews.ts

```typescript
import apiClient from './client'
import { Review } from '@/types/models'

export const reviewsApi = {
  listByProject: async (projectId: string, page = 0, size = 20) => {
    const response = await apiClient.get<{ content: Review[]; totalPages: number }>(
      `/projects/${projectId}/reviews`,
      { params: { page, size } }
    )
    return response.data
  },

  get: async (reviewId: string) => {
    const response = await apiClient.get<Review>(`/reviews/${reviewId}`)
    return response.data
  },

  triggerManual: async (projectId: string, prNumber: number) => {
    const response = await apiClient.post<Review>('/reviews/trigger', {
      projectId,
      prNumber,
    })
    return response.data
  },

  provideFeedback: async (
    issueId: string,
    feedback: 'accept' | 'reject' | 'helpful' | 'not_helpful',
    reason?: string
  ) => {
    await apiClient.post(`/reviews/issues/${issueId}/feedback`, {
      feedbackType: feedback,
      reason,
    })
  },
}
```

---

## React Query Hooks

### src/lib/hooks/useProjects.ts

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectsApi, CreateProjectRequest } from '@/lib/api/projects'
import { useToast } from '@/components/ui/use-toast'

export const useProjects = (page = 0, size = 20) => {
  return useQuery({
    queryKey: ['projects', page, size],
    queryFn: () => projectsApi.list(page, size),
  })
}

export const useProject = (projectId: string) => {
  return useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.get(projectId),
    enabled: !!projectId,
  })
}

export const useCreateProject = () => {
  const queryClient = useQueryClient()
  const { toast } = useToast()

  return useMutation({
    mutationFn: (data: CreateProjectRequest) => projectsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      toast({
        title: 'Project created',
        description: 'Your project has been successfully created.',
      })
    },
    onError: () => {
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to create project. Please try again.',
      })
    },
  })
}

export const useSyncKnowledge = () => {
  const { toast } = useToast()

  return useMutation({
    mutationFn: (projectId: string) => projectsApi.syncKnowledge(projectId),
    onSuccess: () => {
      toast({
        title: 'Syncing knowledge base',
        description: 'Documentation and guidelines are being indexed.',
      })
    },
  })
}
```

### src/lib/hooks/useReviews.ts

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { reviewsApi } from '@/lib/api/reviews'
import { useToast } from '@/components/ui/use-toast'

export const useReviews = (projectId: string, page = 0, size = 20) => {
  return useQuery({
    queryKey: ['reviews', projectId, page, size],
    queryFn: () => reviewsApi.listByProject(projectId, page, size),
    enabled: !!projectId,
  })
}

export const useReview = (reviewId: string) => {
  return useQuery({
    queryKey: ['review', reviewId],
    queryFn: () => reviewsApi.get(reviewId),
    enabled: !!reviewId,
    refetchInterval: (data) => {
      // Poll while in progress
      return data?.status === 'in_progress' ? 3000 : false
    },
  })
}

export const useTriggerReview = () => {
  const queryClient = useQueryClient()
  const { toast } = useToast()

  return useMutation({
    mutationFn: ({ projectId, prNumber }: { projectId: string; prNumber: number }) =>
      reviewsApi.triggerManual(projectId, prNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] })
      toast({
        title: 'Review triggered',
        description: 'Code review is being processed.',
      })
    },
  })
}

export const useIssueFeedback = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      issueId,
      feedback,
      reason,
    }: {
      issueId: string
      feedback: 'accept' | 'reject' | 'helpful' | 'not_helpful'
      reason?: string
    }) => reviewsApi.provideFeedback(issueId, feedback, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['review'] })
    },
  })
}
```

---

## Zustand Stores

### src/store/authStore.ts

```typescript
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User } from '@/types/models'

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (token: string, user: User) => void
  logout: () => void
  updateUser: (user: Partial<User>) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,

      login: (token, user) =>
        set({
          token,
          user,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          token: null,
          user: null,
          isAuthenticated: false,
        }),

      updateUser: (userData) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : null,
        })),
    }),
    {
      name: 'auth-storage',
    }
  )
)
```

### src/store/organizationStore.ts

```typescript
import { create } from 'zustand'
import { Organization } from '@/types/models'

interface OrganizationState {
  currentOrganization: Organization | null
  setCurrentOrganization: (org: Organization) => void
}

export const useOrganizationStore = create<OrganizationState>((set) => ({
  currentOrganization: null,
  setCurrentOrganization: (org) => set({ currentOrganization: org }),
}))
```

---

## Componentes Principais

### src/components/layout/AppLayout.tsx

```typescript
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

export default function AppLayout() {
  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
```

### src/components/layout/Sidebar.tsx

```typescript
import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FolderKanban,
  FileCheck2,
  BarChart3,
  Settings,
  BookOpen,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Projects', href: '/projects', icon: FolderKanban },
  { name: 'Reviews', href: '/reviews', icon: FileCheck2 },
  { name: 'Analytics', href: '/analytics', icon: BarChart3 },
  { name: 'Settings', href: '/settings', icon: Settings },
  { name: 'Docs', href: '/docs', icon: BookOpen },
]

export default function Sidebar() {
  const location = useLocation()

  return (
    <div className="flex w-64 flex-col bg-white border-r">
      <div className="flex h-16 items-center px-6 border-b">
        <h1 className="text-xl font-bold">🦉 Pullwise</h1>
      </div>

      <nav className="flex-1 space-y-1 p-4">
        {navigation.map((item) => {
          const isActive = location.pathname.startsWith(item.href)
          return (
            <Link
              key={item.name}
              to={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          )
        })}
      </nav>
    </div>
  )
}
```

### src/components/projects/ProjectCard.tsx

```typescript
import { Project } from '@/types/models'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ExternalLink, Settings } from 'lucide-react'
import { Link } from 'react-router-dom'

interface ProjectCardProps {
  project: Project
}

export default function ProjectCard({ project }: ProjectCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-lg">{project.name}</CardTitle>
            <CardDescription className="mt-1 flex items-center gap-2">
              <Badge variant="outline">{project.platform}</Badge>
              <span className="text-xs">{project.defaultBranch}</span>
            </CardDescription>
          </div>
          <Badge variant={project.isActive ? 'default' : 'secondary'}>
            {project.isActive ? 'Active' : 'Inactive'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent>
        <div className="flex gap-2">
          <Button asChild variant="outline" size="sm">
            <Link to={`/projects/${project.id}`}>
              View Details
            </Link>
          </Button>
          <Button asChild variant="ghost" size="sm">
            <a href={project.repositoryUrl} target="_blank" rel="noopener noreferrer">
              <ExternalLink className="h-4 w-4" />
            </a>
          </Button>
          <Button asChild variant="ghost" size="sm">
            <Link to={`/projects/${project.id}/settings`}>
              <Settings className="h-4 w-4" />
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
```

### src/components/reviews/ReviewCard.tsx

```typescript
import { Review } from '@/types/models'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { formatDistanceToNow } from 'date-fns'
import { Link } from 'react-router-dom'
import { AlertCircle, CheckCircle2, Clock, XCircle } from 'lucide-react'

interface ReviewCardProps {
  review: Review
}

const statusConfig = {
  pending: { icon: Clock, color: 'text-gray-500', label: 'Pending' },
  in_progress: { icon: Clock, color: 'text-blue-500', label: 'In Progress' },
  completed: { icon: CheckCircle2, color: 'text-green-500', label: 'Completed' },
  failed: { icon: XCircle, color: 'text-red-500', label: 'Failed' },
}

export default function ReviewCard({ review }: ReviewCardProps) {
  const status = statusConfig[review.status]
  const StatusIcon = status.icon

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-base">
              PR #{review.pullRequest.prNumber} - {review.pullRequest.title}
            </CardTitle>
            <CardDescription className="mt-1">
              by {review.pullRequest.author} •{' '}
              {formatDistanceToNow(new Date(review.createdAt), { addSuffix: true })}
            </CardDescription>
          </div>
          <Badge variant="outline" className="gap-1">
            <StatusIcon className={`h-3 w-3 ${status.color}`} />
            {status.label}
          </Badge>
        </div>
      </CardHeader>

      {review.status === 'completed' && (
        <CardContent>
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Quality Score</p>
              <p className="text-2xl font-bold">{review.qualityScore}/10</p>
            </div>
            <div>
              <p className="text-muted-foreground">SAST Issues</p>
              <p className="text-2xl font-bold">{review.sastIssuesCount}</p>
            </div>
            <div>
              <p className="text-muted-foreground">AI Issues</p>
              <p className="text-2xl font-bold">{review.llmIssuesCount}</p>
            </div>
          </div>

          <Button asChild className="mt-4 w-full" variant="outline">
            <Link to={`/reviews/${review.id}`}>View Full Review</Link>
          </Button>
        </CardContent>
      )}
    </Card>
  )
}
```

### src/components/reviews/IssueCard.tsx

```typescript
import { Issue } from '@/types/models'
import { Card, CardHeader, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ThumbsUp, ThumbsDown } from 'lucide-react'
import { useIssueFeedback } from '@/lib/hooks/useReviews'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism'

interface IssueCardProps {
  issue: Issue
}

const severityColors = {
  critical: 'bg-red-100 text-red-800 border-red-200',
  high: 'bg-orange-100 text-orange-800 border-orange-200',
  medium: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  low: 'bg-blue-100 text-blue-800 border-blue-200',
}

export default function IssueCard({ issue }: IssueCardProps) {
  const feedback = useIssueFeedback()

  const handleFeedback = (type: 'accept' | 'reject') => {
    feedback.mutate({ issueId: issue.id, feedback: type })
  }

  return (
    <Card className={severityColors[issue.severity]}>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <Badge variant="outline">{issue.severity.toUpperCase()}</Badge>
              <Badge variant="secondary">{issue.source}</Badge>
              <Badge variant="outline">{issue.type}</Badge>
            </div>
            <p className="font-medium">{issue.message}</p>
            <p className="text-sm text-muted-foreground mt-1">
              {issue.filePath}:{issue.lineNumber}
            </p>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {issue.description && (
          <div className="mb-4">
            <p className="text-sm">{issue.description}</p>
          </div>
        )}

        {issue.reasoning && (
          <div className="mb-4 p-3 bg-white/50 rounded-md">
            <p className="text-xs font-medium mb-1">AI Reasoning:</p>
            <p className="text-sm">{issue.reasoning}</p>
          </div>
        )}

        {issue.suggestedFix && (
          <div className="mb-4">
            <p className="text-xs font-medium mb-2">Suggested Fix:</p>
            <SyntaxHighlighter
              language="java"
              style={vscDarkPlus}
              customStyle={{ fontSize: '12px', borderRadius: '6px' }}
            >
              {issue.suggestedFix}
            </SyntaxHighlighter>
          </div>
        )}

        {!issue.isResolved && (
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleFeedback('accept')}
              disabled={feedback.isPending}
            >
              <ThumbsUp className="h-4 w-4 mr-1" />
              Helpful
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleFeedback('reject')}
              disabled={feedback.isPending}
            >
              <ThumbsDown className="h-4 w-4 mr-1" />
              Not Helpful
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
```

### src/pages/dashboard/ProjectsPage.tsx

```typescript
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'
import { useProjects } from '@/lib/hooks/useProjects'
import ProjectCard from '@/components/projects/ProjectCard'
import CreateProjectDialog from '@/components/projects/CreateProjectDialog'

export default function ProjectsPage() {
  const [page, setPage] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const { data, isLoading } = useProjects(page)

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">Projects</h1>
          <p className="text-muted-foreground mt-1">
            Manage your code repositories and review settings
          </p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New Project
        </Button>
      </div>

      {isLoading ? (
        <div>Loading...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {data?.content.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}

      <CreateProjectDialog open={showCreate} onOpenChange={setShowCreate} />
    </div>
  )
}
```

---

## Rotas

### src/routes.tsx

```typescript
import { createBrowserRouter, Navigate } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout'
import LandingPage from './pages/public/LandingPage'
import LoginPage from './pages/public/LoginPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import ProjectsPage from './pages/dashboard/ProjectsPage'
import ProjectDetailPage from './pages/dashboard/ProjectDetailPage'
import ReviewsPage from './pages/dashboard/ReviewsPage'
import ReviewDetailPage from './pages/dashboard/ReviewDetailPage'
import AnalyticsPage from './pages/dashboard/AnalyticsPage'
import SettingsPage from './pages/dashboard/SettingsPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <LandingPage />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/dashboard',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'projects',
        element: <ProjectsPage />,
      },
      {
        path: 'projects/:projectId',
        element: <ProjectDetailPage />,
      },
      {
        path: 'reviews',
        element: <ReviewsPage />,
      },
      {
        path: 'reviews/:reviewId',
        element: <ReviewDetailPage />,
      },
      {
        path: 'analytics',
        element: <AnalyticsPage />,
      },
      {
        path: 'settings',
        element: <SettingsPage />,
      },
    ],
  },
])
```

---

## Environment Variables

### .env.example

```bash
VITE_API_URL=http://localhost:8080/api
VITE_GITHUB_CLIENT_ID=your_github_client_id
VITE_STRIPE_PUBLIC_KEY=pk_test_your_stripe_key
```

---

## Main Entry

### src/main.tsx

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { router } from './routes'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
)
```

---

## Deploy

### Vercel

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "framework": "vite",
  "env": {
    "VITE_API_URL": "@api_url"
  }
}
```

### Netlify

```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

---

## Próximos Passos

1. ✅ Setup inicial com Vite + React + TypeScript
2. ✅ Configurar TailwindCSS + Shadcn/ui
3. ✅ Implementar autenticação (OAuth GitHub)
4. ✅ Criar páginas públicas (Landing, Pricing)
5. ✅ Implementar dashboard e layouts
6. ✅ Criar componentes de Projects
7. ✅ Criar componentes de Reviews
8. ✅ Implementar Analytics e gráficos
9. ✅ Integrar Stripe para billing
10. ✅ Testes E2E com Playwright
11. ✅ Deploy em Vercel ou Netlify
