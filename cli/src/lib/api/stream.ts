import { configManager } from '../config/manager'

export interface ReviewStreamEvent {
  type: string
  data: any
}

export type StreamEventHandler = (event: ReviewStreamEvent) => void

/**
 * SSE client for streaming review progress.
 * Connects to GET /api/reviews/{id}/stream and emits events.
 */
export async function streamReviewProgress(
  reviewId: string,
  onEvent: StreamEventHandler,
  onComplete: () => void,
  onError: (error: Error) => void
): Promise<AbortController> {
  const config = await configManager.load()
  const token = await configManager.get('token')
  const baseUrl = config.apiUrl

  const url = `${baseUrl}/api/reviews/${reviewId}/stream`
  const abortController = new AbortController()

  // Use fetch with ReadableStream for SSE (no extra dependencies needed)
  try {
    const headers: Record<string, string> = {
      Accept: 'text/event-stream',
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(url, {
      headers,
      signal: abortController.signal,
    })

    if (!response.ok) {
      throw new Error(`Stream failed: ${response.status} ${response.statusText}`)
    }

    if (!response.body) {
      throw new Error('No response body for SSE stream')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const processStream = async () => {
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          onComplete()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // Keep incomplete line in buffer

        let currentEvent = ''
        let currentData = ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            currentData = line.substring(5).trim()
          } else if (line === '' && currentEvent && currentData) {
            // Empty line = end of event
            try {
              const data = JSON.parse(currentData)
              onEvent({ type: currentEvent, data })

              if (currentEvent === 'review.completed' || currentEvent === 'review.failed' || currentEvent === 'error') {
                onComplete()
                reader.cancel()
                return
              }
            } catch {
              // Ignore malformed events
            }
            currentEvent = ''
            currentData = ''
          }
        }
      }
    }

    processStream().catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err)
      }
    })
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      onError(err)
    }
  }

  return abortController
}
