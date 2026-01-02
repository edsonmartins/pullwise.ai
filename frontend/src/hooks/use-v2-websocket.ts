import { useEffect, useRef, useCallback } from 'react'
import { io, Socket } from 'socket.io-client'
import { useV2Store } from '@/store/v2-store'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080'

interface WebSocketMessage {
  type: 'review.progress' | 'review.completed' | 'review.failed' | 'issue.detected' | 'fix.generated' | 'plugin.status'
  data: unknown
}

interface ReviewProgressMessage {
  reviewId: number
  progress: number
  stage: string
  message?: string
}

interface ReviewCompletedMessage {
  reviewId: number
  duration: number
  stats: {
    total: number
    critical: number
    high: number
    medium: number
    low: number
    info: number
  }
}

interface ReviewFailedMessage {
  reviewId: number
  error: string
  retryable: boolean
}

interface IssueDetectedMessage {
  reviewId: number
  issue: {
    id: number
    severity: string
    type: string
    title: string
    filePath: string
    lineStart: number
  }
}

interface FixGeneratedMessage {
  suggestionId: number
  issueId: number
  status: string
}

interface PluginStatusMessage {
  pluginId: string
  status: 'installed' | 'uninstalled' | 'enabled' | 'disabled' | 'error'
  message?: string
}

export function useV2WebSocket(autoConnect = true) {
  const socketRef = useRef<Socket | null>(null)
  const reconnectTimeoutRef = useRef<number>()
  const reconnectAttempts = useRef(0)
  const MAX_RECONNECT_ATTEMPTS = 5

  const {
    setIsConnected,
    setReviewProgress,
  } = useV2Store()

  // Connect to WebSocket
  const connect = useCallback(() => {
    if (socketRef.current?.connected) return

    const socket = io(WS_URL, {
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: MAX_RECONNECT_ATTEMPTS,
      timeout: 10000,
    })

    socket.on('connect', () => {
      console.log('[WebSocket] Connected:', socket.id)
      setIsConnected(true)
      reconnectAttempts.current = 0

      // Join room for organization-specific updates (if applicable)
      const orgId = localStorage.getItem('orgId')
      if (orgId) {
        socket.emit('join:org', { orgId })
      }
    })

    socket.on('disconnect', (reason: string) => {
      console.log('[WebSocket] Disconnected:', reason)
      setIsConnected(false)
    })

    socket.on('error', (error: Error) => {
      console.error('[WebSocket] Error:', error)
    })

    socket.on('reconnect', (attemptNumber: number) => {
      console.log('[WebSocket] Reconnected after', attemptNumber, 'attempts')
      reconnectAttempts.current = 0
    })

    socket.on('reconnect_attempt', (attemptNumber: number) => {
      reconnectAttempts.current = attemptNumber
      console.log('[WebSocket] Reconnect attempt:', attemptNumber)
    })

    socket.on('reconnect_failed', () => {
      console.error('[WebSocket] Reconnection failed after', MAX_RECONNECT_ATTEMPTS, 'attempts')
    })

    // Review progress updates
    socket.on('review:progress', (data: ReviewProgressMessage) => {
      console.log('[WebSocket] Review progress:', data)
      setReviewProgress(data.reviewId, data.progress)

      // Trigger analytics refresh if needed
      if (data.progress === 100) {
        // Analytics will be refreshed by the completed event
      }
    })

    // Review completed
    socket.on('review:completed', (data: ReviewCompletedMessage) => {
      console.log('[WebSocket] Review completed:', data)
      setReviewProgress(data.reviewId, 100)

      // Refresh analytics
      // This would trigger a query invalidation in real usage
    })

    // Review failed
    socket.on('review:failed', (data: ReviewFailedMessage) => {
      console.error('[WebSocket] Review failed:', data)
      // Show error notification
      showErrorNotification(`Review #${data.reviewId} failed: ${data.error}`)
    })

    // Issue detected (real-time)
    socket.on('issue:detected', (data: IssueDetectedMessage) => {
      console.log('[WebSocket] Issue detected:', data)
      // Could show a toast notification
      showIssueNotification(data.issue)
    })

    // Fix generated
    socket.on('fix:generated', (data: FixGeneratedMessage) => {
      console.log('[WebSocket] Fix generated:', data)
      showSuccessNotification(`Auto-fix generated for issue #${data.issueId}`)
    })

    // Plugin status changed
    socket.on('plugin:status', (data: PluginStatusMessage) => {
      console.log('[WebSocket] Plugin status:', data)
      // Refresh plugins list
      if (data.status === 'installed' || data.status === 'uninstalled') {
        // Invalidate plugins query
      }
    })

    // Code graph updated
    socket.on('codegraph:updated', (data: { projectId: number; timestamp: number }) => {
      console.log('[WebSocket] Code graph updated:', data)
      // Could trigger auto-refresh of code graph
    })

    // Organization-wide broadcasts
    socket.on('org:announcement', (data: { message: string; type: string }) => {
      console.log('[WebSocket] Org announcement:', data)
      showAnnouncement(data.message, data.type)
    })

    socketRef.current = socket
  }, [setIsConnected, setReviewProgress])

  // Disconnect from WebSocket
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
    }
    socketRef.current?.disconnect()
    socketRef.current = null
    setIsConnected(false)
  }, [setIsConnected])

  // Emit events
  const emit = useCallback((event: string, data?: unknown) => {
    socketRef.current?.emit(event, data)
  }, [])

  // Subscribe to specific review updates
  const subscribeToReview = useCallback((reviewId: number) => {
    emit('subscribe:review', { reviewId })
  }, [emit])

  // Unsubscribe from review updates
  const unsubscribeFromReview = useCallback((reviewId: number) => {
    emit('unsubscribe:review', { reviewId })
  }, [emit])

  // Subscribe to project updates
  const subscribeToProject = useCallback((projectId: number) => {
    emit('subscribe:project', { projectId })
  }, [emit])

  // Request review status
  const requestReviewStatus = useCallback((reviewId: number) => {
    emit('request:review:status', { reviewId })
  }, [emit])

  // Cancel review
  const cancelReview = useCallback((reviewId: number) => {
    emit('cancel:review', { reviewId })
  }, [emit])

  // Auto-connect on mount
  useEffect(() => {
    if (autoConnect) {
      connect()
    }

    return () => {
      disconnect()
    }
  }, [autoConnect, connect, disconnect])

  return {
    isConnected: socketRef.current?.connected ?? false,
    connect,
    disconnect,
    emit,
    subscribeToReview,
    unsubscribeFromReview,
    subscribeToProject,
    requestReviewStatus,
    cancelReview,
    socket: socketRef.current,
  }
}

// Helper functions for notifications
function showErrorNotification(message: string) {
  // In a real app, this would use a toast notification system
  console.error('[Notification]', message)
}

function showSuccessNotification(message: string) {
  console.log('[Notification]', message)
}

function showIssueNotification(issue: IssueDetectedMessage['issue']) {
  console.log(`[Issue Found - ${issue.severity}]`, issue.title, 'at', issue.filePath)
}

function showAnnouncement(message: string, type: string) {
  console.log(`[Announcement - ${type}]`, message)
}

// Types
export type { WebSocketMessage, ReviewProgressMessage, ReviewCompletedMessage, ReviewFailedMessage }
