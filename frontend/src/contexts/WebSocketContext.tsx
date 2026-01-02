import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react'
import { notifications } from '@mantine/notifications'
import { IconCheck, IconAlertTriangle, IconInfoCircle } from '@tabler/icons-react'

interface WebSocketMessage {
  type: 'REVIEW_STARTED' | 'REVIEW_COMPLETED' | 'REVIEW_FAILED' | 'ISSUE_FOUND' | 'PING'
  data: any
}

interface WebSocketContextType {
  connected: boolean
  sendMessage: (message: any) => void
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined)

const WS_URL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected] = useState(false)
  const [ws, setWs] = useState<WebSocket | null>(null)
  const [reconnectAttempts, setReconnectAttempts] = useState(0)
  const maxReconnectAttempts = 5

  const connect = useCallback(() => {
    if (ws?.readyState === WebSocket.OPEN) return

    const token = localStorage.getItem('token')
    if (!token) return

    try {
      const websocket = new WebSocket(`${WS_URL}?token=${token}`)

      websocket.onopen = () => {
        console.log('WebSocket connected')
        setConnected(true)
        setReconnectAttempts(0)
      }

      websocket.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          handleWebSocketMessage(message)
        } catch (error) {
          console.error('Error parsing WebSocket message:', error)
        }
      }

      websocket.onclose = (event) => {
        console.log('WebSocket disconnected:', event.code)
        setConnected(false)
        setWs(null)

        // Attempt to reconnect
        if (reconnectAttempts < maxReconnectAttempts) {
          setTimeout(() => {
            setReconnectAttempts((prev) => prev + 1)
            connect()
          }, Math.min(1000 * Math.pow(2, reconnectAttempts), 30000))
        }
      }

      websocket.onerror = (error) => {
        console.error('WebSocket error:', error)
      }

      setWs(websocket)
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
    }
  }, [ws, reconnectAttempts])

  useEffect(() => {
    // Connect when user is authenticated
    const token = localStorage.getItem('token')
    if (token) {
      connect()
    }

    return () => {
      if (ws) {
        ws.close()
      }
    }
  }, [])

  useEffect(() => {
    // Reconnect when token changes
    const handleStorageChange = () => {
      const token = localStorage.getItem('token')
      if (token && !connected) {
        connect()
      } else if (!token && ws) {
        ws.close()
      }
    }

    window.addEventListener('storage', handleStorageChange)
    return () => window.removeEventListener('storage', handleStorageChange)
  }, [connected, ws])

  const sendMessage = useCallback((message: any) => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(message))
    }
  }, [ws])

  return (
    <WebSocketContext.Provider value={{ connected, sendMessage }}>
      {children}
    </WebSocketContext.Provider>
  )
}

export function useWebSocket() {
  const context = useContext(WebSocketContext)
  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider')
  }
  return context
}

function handleWebSocketMessage(message: WebSocketMessage) {
  switch (message.type) {
    case 'REVIEW_STARTED':
      notifications.show({
        title: 'Review iniciado',
        message: `O review do PR #${message.data.prNumber} foi iniciado`,
        color: 'blue',
        icon: <IconInfoCircle size={16} />,
      })
      break

    case 'REVIEW_COMPLETED':
      notifications.show({
        title: 'Review concluído',
        message: `O review do PR #${message.data.prNumber} foi concluído com ${message.data.issueCount} issues encontradas`,
        color: 'green',
        icon: <IconCheck size={16} />,
      })
      break

    case 'REVIEW_FAILED':
      notifications.show({
        title: 'Review falhou',
        message: `O review do PR #${message.data.prNumber} falhou: ${message.data.error}`,
        color: 'red',
        icon: <IconAlertTriangle size={16} />,
      })
      break

    case 'ISSUE_FOUND':
      // Don't show notification for each issue, too noisy
      break

    case 'PING':
      // Respond to ping with pong
      break
  }
}
