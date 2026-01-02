import { create } from 'zustand'
import { subscribeWithSelector } from 'zustand/middleware'
import type {
  Review,
  FixSuggestion,
  CodeGraphData,
  AnalyticsData,
  Plugin,
  IntegrationStatus,
  SandboxResult
} from '@/types/v2'

interface V2State {
  // Reviews
  selectedReview: Review | null
  selectedIssue: Review['issues'][number] | null
  setSelectedReview: (review: Review | null) => void
  setSelectedIssue: (issue: Review['issues'][number] | null) => void

  // Code Graph
  codeGraphData: CodeGraphData | null
  setCodeGraphData: (data: CodeGraphData | null) => void
  isCodeGraphLoading: boolean
  setIsCodeGraphLoading: (loading: boolean) => void
  selectedGraphNode: string | null
  setSelectedGraphNode: (nodeId: string | null) => void

  // Auto-Fix
  fixSuggestions: FixSuggestion[]
  setFixSuggestions: (suggestions: FixSuggestion[]) => void
  selectedFixSuggestion: FixSuggestion | null
  setSelectedFixSuggestion: (suggestion: FixSuggestion | null) => void
  isGeneratingFix: boolean
  setIsGeneratingFix: (generating: boolean) => void
  isApplyingFix: boolean
  setIsApplyingFix: (applying: boolean) => void

  // Sandbox
  sandboxResult: SandboxResult | null
  setSandboxResult: (result: SandboxResult | null) => void
  isSandboxRunning: boolean
  setIsSandboxRunning: (running: boolean) => void

  // Analytics
  analyticsData: AnalyticsData | null
  setAnalyticsData: (data: AnalyticsData | null) => void
  selectedTimeRange: string
  setSelectedTimeRange: (range: string) => void

  // Plugins
  plugins: Plugin[]
  setPlugins: (plugins: Plugin[]) => void
  installedPlugins: string[]
  setInstalledPlugins: (pluginIds: string[]) => void

  // Integrations
  integrationStatus: IntegrationStatus | null
  setIntegrationStatus: (status: IntegrationStatus | null) => void

  // Real-time
  isConnected: boolean
  setIsConnected: (connected: boolean) => void
  reviewProgress: Record<number, number>
  setReviewProgress: (reviewId: number, progress: number) => void

  // Reset
  reset: () => void
}

export const useV2Store = create<V2State>()(
  subscribeWithSelector((set) => ({
    // Reviews
    selectedReview: null,
    selectedIssue: null,
    setSelectedReview: (review) => set({ selectedReview: review }),
    setSelectedIssue: (issue) => set({ selectedIssue: issue }),

    // Code Graph
    codeGraphData: null,
    isCodeGraphLoading: false,
    selectedGraphNode: null,
    setCodeGraphData: (data) => set({ codeGraphData: data }),
    setIsCodeGraphLoading: (loading) => set({ isCodeGraphLoading: loading }),
    setSelectedGraphNode: (nodeId) => set({ selectedGraphNode: nodeId }),

    // Auto-Fix
    fixSuggestions: [],
    selectedFixSuggestion: null,
    isGeneratingFix: false,
    isApplyingFix: false,
    setFixSuggestions: (suggestions) => set({ fixSuggestions: suggestions }),
    setSelectedFixSuggestion: (suggestion) => set({ selectedFixSuggestion: suggestion }),
    setIsGeneratingFix: (generating) => set({ isGeneratingFix: generating }),
    setIsApplyingFix: (applying) => set({ isApplyingFix: applying }),

    // Sandbox
    sandboxResult: null,
    isSandboxRunning: false,
    setSandboxResult: (result) => set({ sandboxResult: result }),
    setIsSandboxRunning: (running) => set({ isSandboxRunning: running }),

    // Analytics
    analyticsData: null,
    selectedTimeRange: '30d',
    setAnalyticsData: (data) => set({ analyticsData: data }),
    setSelectedTimeRange: (range) => set({ selectedTimeRange: range }),

    // Plugins
    plugins: [],
    installedPlugins: [],
    setPlugins: (plugins) => set({ plugins }),
    setInstalledPlugins: (pluginIds) => set({ installedPlugins: pluginIds }),

    // Integrations
    integrationStatus: null,
    setIntegrationStatus: (status) => set({ integrationStatus: status }),

    // Real-time
    isConnected: false,
    reviewProgress: {},
    setIsConnected: (connected) => set({ isConnected: connected }),
    setReviewProgress: (reviewId, progress) => set((state) => ({
      reviewProgress: { ...state.reviewProgress, [reviewId]: progress }
    })),

    // Reset
    reset: () => set({
      selectedReview: null,
      selectedIssue: null,
      codeGraphData: null,
      selectedGraphNode: null,
      fixSuggestions: [],
      selectedFixSuggestion: null,
      sandboxResult: null,
      analyticsData: null,
    }),
  }))
)

// Selectors
export const useFixSuggestions = () => useV2Store((state) => state.fixSuggestions)
export const useCodeGraphData = () => useV2Store((state) => state.codeGraphData)
export const useAnalyticsData = () => useV2Store((state) => state.analyticsData)
export const usePlugins = () => useV2Store((state) => state.plugins)
