import { lazy, Suspense } from 'react'
import { MantineProvider, createTheme, Loader, Stack, Text } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { ModalsProvider } from '@mantine/modals'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from '@/components/theme-provider'
import { LanguageProvider } from '@/components/language-provider'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import { WebSocketProvider } from '@/contexts/WebSocketContext'
import { LandingPage } from '@/pages/LandingPage'
import { LoginPage } from '@/pages/LoginPage'
import { OAuthCallbackPage } from '@/pages/OAuthCallbackPage'
import DownloadPage from '@/pages/DownloadPage'
import DemoPage from '@/pages/DemoPage'
import { AppLayout } from '@/components/layout/AppLayout'
import { DashboardPage } from '@/pages/DashboardPage'
import { OrganizationsPage } from '@/pages/OrganizationsPage'
import { ProjectsPage } from '@/pages/ProjectsPage'
import { PullRequestsPage } from '@/pages/PullRequestsPage'
import { ReviewsPage } from '@/pages/ReviewsPage'
import { SettingsPage } from '@/pages/SettingsPage'

// V2 Pages - lazy loaded para code splitting
const AnalyticsDashboardPage = lazy(() => import('@/pages/v2/AnalyticsDashboardPage').then(m => ({ default: m.AnalyticsDashboardPage })))
const CodeGraphPage = lazy(() => import('@/pages/v2/CodeGraphPage').then(m => ({ default: m.CodeGraphPage })))
const AutoFixPage = lazy(() => import('@/pages/v2/AutoFixPage').then(m => ({ default: m.AutoFixPage })))
const PluginMarketplacePage = lazy(() => import('@/pages/v2/PluginMarketplacePage').then(m => ({ default: m.PluginMarketplacePage })))
const TeamAnalyticsPage = lazy(() => import('@/pages/v2/TeamAnalyticsPage').then(m => ({ default: m.TeamAnalyticsPage })))

// Loading component para Suspense
function PageLoader() {
  return (
    <Stack align="center" justify="center" style={{ height: '50vh' }}>
      <Loader size="lg" />
      <Text size="sm" c="dimmed">Carregando...</Text>
    </Stack>
  )
}

// Wrapper para Suspense
function LazyPage({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageLoader />}>{children}</Suspense>
}

const theme = createTheme({
  primaryColor: 'indigo',
  fontFamily: 'Inter, sans-serif',
})

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        Carregando...
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

export default function App() {
  return (
    <ThemeProvider defaultTheme="system">
      <LanguageProvider>
        <MantineProvider theme={theme}>
          <Notifications />
          <ModalsProvider>
            <WebSocketProvider>
              <AuthProvider>
                <BrowserRouter>
                <Routes>
              {/* Public routes */}
              <Route path="/landing" element={<LandingPage />} />
              <Route path="/" element={<Navigate to="/landing" replace />} />
              <Route path="/download" element={<DownloadPage />} />
              <Route path="/demo" element={<DemoPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

              {/* Protected routes */}
              <Route
                path="/*"
                element={
                  <ProtectedRoute>
                    <AppLayout>
                      <Routes>
                        <Route path="/" element={<DashboardPage />} />
                        <Route path="/organizations" element={<OrganizationsPage />} />
                        <Route path="/projects" element={<ProjectsPage />} />
                        <Route path="/projects/:id/pull-requests" element={<PullRequestsPage />} />
                        <Route path="/reviews" element={<ReviewsPage />} />
                        <Route path="/settings" element={<SettingsPage />} />

                        {/* V2 Routes - lazy loaded */}
                        <Route path="/v2/analytics" element={<LazyPage><AnalyticsDashboardPage /></LazyPage>} />
                        <Route path="/v2/code-graph/:projectId?" element={<LazyPage><CodeGraphPage /></LazyPage>} />
                        <Route path="/v2/autofix/:reviewId?/:issueId?" element={<LazyPage><AutoFixPage /></LazyPage>} />
                        <Route path="/v2/plugins" element={<LazyPage><PluginMarketplacePage /></LazyPage>} />
                        <Route path="/v2/team" element={<LazyPage><TeamAnalyticsPage /></LazyPage>} />
                      </Routes>
                    </AppLayout>
                  </ProtectedRoute>
                }
              />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </WebSocketProvider>
      </ModalsProvider>
      </MantineProvider>
    </LanguageProvider>
    </ThemeProvider>
  )
}
