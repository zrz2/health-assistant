import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getSessions,
  createSession,
  deleteSession,
  getMessages,
  submitFeedback,
  getSuggestedQuestions,
  sendClarification,
  type ChatSession,
  type ChatMessage,
} from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  const messages = ref<ChatMessage[]>([])
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const suggestedQuestions = ref<string[]>([])

  let abortController: AbortController | null = null
  let streamingSessionId: string | null = null

  const currentSession = computed(() =>
    sessions.value.find((s) => s.sessionId === currentSessionId.value)
  )

  async function fetchSessions() {
    try {
      const res = await getSessions()
      sessions.value = res.data?.content || []
    } catch {
      // ignore
    }
  }

  async function createNewSession(firstMessage: string): Promise<string | null> {
    try {
      const res = await createSession(firstMessage)
      sessions.value.unshift(res.data)
      return res.data.sessionId
    } catch {
      return null
    }
  }

  async function removeSession(sessionId: string) {
    try {
      await deleteSession(sessionId)
      sessions.value = sessions.value.filter((s) => s.sessionId !== sessionId)
      if (currentSessionId.value === sessionId) {
        cancelStreaming()
        currentSessionId.value = null
        messages.value = []
      }
    } catch {
      // ignore
    }
  }

  async function loadMessages(sessionId: string) {
    try {
      const res = await getMessages(sessionId)
      messages.value = res.data || []
    } catch {
      messages.value = []
    }
  }

  function setSession(sessionId: string) {
    cancelStreaming()
    currentSessionId.value = sessionId
    loadMessages(sessionId)
  }

  function newSession() {
    cancelStreaming()
    currentSessionId.value = null
    messages.value = []
  }

  function cancelStreaming() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isStreaming.value = false
    streamingContent.value = ''
    streamingSessionId = null
  }

  function addUserMessage(content: string) {
    messages.value.push({
      id: Date.now(),
      messageId: `temp_${Date.now()}`,
      messageType: 1,
      content,
      createdAt: new Date().toISOString(),
    } as ChatMessage)
  }

  function addAssistantPlaceholder() {
    const placeholder: ChatMessage = {
      id: Date.now() + 1,
      messageId: `streaming_${Date.now()}`,
      messageType: 2,
      content: '正在分析您的问题...',
      createdAt: new Date().toISOString(),
    } as ChatMessage
    messages.value.push(placeholder)
    return placeholder.messageId
  }

  function updateStreamingContent(content: string) {
    streamingContent.value = content
    const last = messages.value[messages.value.length - 1]
    if (last && last.messageType === 2) {
      last.content = content
    }
  }

  function finalizeMessage(messageId: string, data: Partial<ChatMessage>) {
    const msg = messages.value.find((m) => m.messageId === messageId)
    if (msg) {
      Object.assign(msg, data)
    }
    isStreaming.value = false
    streamingContent.value = ''
  }

  function findPendingClarification(): ChatMessage | undefined {
    return [...messages.value].reverse().find(
      (m) => m.messageType === 4 && m.clarificationData?.clarificationId && !m.clarificationData.answered
    )
  }

  async function sendMessage(content: string) {
    // If there's a pending clarification, route the answer through the clarify endpoint
    const pendingClarification = findPendingClarification()
    if (pendingClarification) {
      addUserMessage(content)
      await answerClarification(pendingClarification.clarificationData!.clarificationId, content)
      return
    }

    addUserMessage(content)
    const placeholderId = addAssistantPlaceholder()
    isStreaming.value = true
    streamingContent.value = ''

    const sessionId = currentSessionId.value
    streamingSessionId = sessionId
    abortController = new AbortController()

    function isWrongSession(): boolean {
      return currentSessionId.value !== streamingSessionId
    }

    try {
      const response = await fetch('/api/v1/chat/messages', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({ sessionId, content }),
        signal: abortController.signal,
      })

      if (!response.ok) {
        let errMsg = `HTTP ${response.status}`
        try {
          const body = await response.json()
          if (body.message) errMsg = body.message
        } catch { /* ignore */ }
        throw new Error(errMsg)
      }

      const reader = response.body?.getReader()
      if (!reader) throw new Error('No reader')

      const decoder = new TextDecoder()
      let buffer = ''
      let fullContent = ''
      let finalMessageId = placeholderId

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          try {
            const data = JSON.parse(line.slice(5).trim())
            // Guard: discard events that arrived after switching sessions
            if (data.type === 'message' || data.type === 'processing' || data.type === 'done' || data.type === 'error' || data.type === 'clarification') {
              if (isWrongSession()) continue
            }
            if (data.type === 'processing') {
              updateStreamingContent(data.content || '正在分析您的问题...')
            } else if (data.type === 'message') {
              fullContent += data.content || ''
              updateStreamingContent(fullContent)
            } else if (data.type === 'clarification') {
              updateStreamingContent(data.content || '')
              finalizeMessage(finalMessageId, {
                messageId: finalMessageId,
                messageType: 4,
                clarificationData: {
                  clarificationId: data.clarificationId,
                  clarificationType: data.clarificationType,
                  question: data.content,
                  options: data.options,
                },
              })
            } else if (data.type === 'done') {
              finalizeMessage(finalMessageId, {
                messageId: finalMessageId,
                sources: data.sources,
                evidenceLevel: data.evidenceLevel,
                tokensUsed: data.usage?.totalTokens,
              })
            } else if (data.type === 'error') {
              const last = messages.value[messages.value.length - 1]
              if (last && last.messageType === 2) {
                last.content = data.content || '抱歉，处理您的问题时出错了，请重试。'
              }
              isStreaming.value = false
              streamingContent.value = ''
            }
          } catch {
            // skip unparseable lines
          }
        }
      }

      // Ensure streaming state is cleared when the stream ends naturally
      if (!isWrongSession()) {
        isStreaming.value = false
        streamingContent.value = ''
      }
    } catch (e: any) {
      if (e?.name === 'AbortError') return // session switch, silently ignore
      if (isWrongSession()) return // already on another session
      const last = messages.value[messages.value.length - 1]
      if (last && last.messageType === 2) {
        last.content = e?.message || '抱歉，回复生成失败，请重试。'
      }
      isStreaming.value = false
      streamingContent.value = ''
    } finally {
      abortController = null
      streamingSessionId = null
    }

    fetchSessions()
  }

  async function fetchSuggested() {
    try {
      const res = await getSuggestedQuestions()
      suggestedQuestions.value = res.data || []
    } catch {
      // ignore
    }
  }

  async function answerClarification(clarificationId: string, answer: string) {
    try {
      const res = await sendClarification(currentSessionId.value || '', clarificationId, answer)
      const rewritten = res.data
      if (rewritten && typeof rewritten === 'string') {
        const lastClarification = messages.value.find(
          (m) => m.messageType === 4 && m.clarificationData?.clarificationId === clarificationId
        )
        if (lastClarification) {
          lastClarification.clarificationData.answered = true
        }
        // Send the rewritten query as a new message
        await sendMessage(rewritten)
      }
    } catch {
      // ignore
    }
  }

  async function sendFeedback(messageId: string, feedbackType: number) {
    try {
      await submitFeedback(messageId, feedbackType)
      const msg = messages.value.find((m) => m.messageId === messageId)
      if (msg) msg.feedbackType = feedbackType
    } catch {
      // ignore
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    streamingContent,
    suggestedQuestions,
    currentSession,
    fetchSessions,
    createNewSession,
    removeSession,
    loadMessages,
    setSession,
    newSession,
    sendMessage,
    updateStreamingContent,
    fetchSuggested,
    sendFeedback,
    answerClarification,
  }
})
