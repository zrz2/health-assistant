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

  // Cache messages per session so background streams can write without affecting display
  const messageCache = new Map<string, ChatMessage[]>()

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
      messageCache.delete(sessionId)
      if (currentSessionId.value === sessionId) {
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
      const list = res.data || []
      messageCache.set(sessionId, list)
      if (currentSessionId.value === sessionId) {
        messages.value = list
      }
    } catch {
      if (currentSessionId.value === sessionId) {
        messages.value = []
      }
    }
  }

  function setSession(sessionId: string) {
    if (currentSessionId.value === sessionId) return

    // Save current messages to cache
    if (currentSessionId.value) {
      messageCache.set(currentSessionId.value, [...messages.value])
    }

    currentSessionId.value = sessionId
    isStreaming.value = false
    streamingContent.value = ''

    // Restore from cache or load from server
    const cached = messageCache.get(sessionId)
    if (cached) {
      messages.value = cached
    } else {
      messages.value = []
      loadMessages(sessionId)
    }
  }

  function newSession() {
    if (currentSessionId.value) {
      messageCache.set(currentSessionId.value, [...messages.value])
    }
    currentSessionId.value = null
    messages.value = []
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

  function updateStreamingContent(content: string, sessionId?: string) {
    if (!sessionId || sessionId === currentSessionId.value) {
      streamingContent.value = content
    }
    const target = targetMessages(sessionId)
    const last = target[target.length - 1]
    if (last && last.messageType === 2) {
      last.content = content
    }
  }

  function finalizeMessage(messageId: string, data: Partial<ChatMessage>, sessionId?: string) {
    const target = targetMessages(sessionId)
    const msg = target.find((m) => m.messageId === messageId)
    if (msg) {
      Object.assign(msg, data)
    }
    if (!sessionId || sessionId === currentSessionId.value) {
      isStreaming.value = false
      streamingContent.value = ''
    }
  }

  function findPendingClarification(): ChatMessage | undefined {
    return [...messages.value].reverse().find(
      (m) => m.messageType === 4 && m.clarificationData?.clarificationId && !m.clarificationData.answered
    )
  }

  /**
   * Returns the messages array that SSE events should write to.
   * If the user is viewing the streaming session, writes to messages.value (visible).
   * Otherwise writes to the messageCache (background).
   */
  function targetMessages(sessionId?: string): ChatMessage[] {
    const sid = sessionId ?? currentSessionId.value
    if (!sid) return messages.value
    if (sid === currentSessionId.value) return messages.value
    // Background session: return cache entry
    let cached = messageCache.get(sid)
    if (!cached) {
      cached = []
      messageCache.set(sid, cached)
    }
    return cached
  }

  async function sendMessage(content: string, skipClarification = false) {
    const pendingClarification = findPendingClarification()
    if (pendingClarification) {
      // Don't show raw answer — the rewritten query will be shown instead
      await answerClarification(pendingClarification.clarificationData!.clarificationId, content)
      return
    }

    addUserMessage(content)
    const placeholderId = addAssistantPlaceholder()
    isStreaming.value = true
    streamingContent.value = ''

    const sessionId = currentSessionId.value!

    try {
      const response = await fetch('/api/v1/chat/messages', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({ sessionId, content, skipClarification }),
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
            if (data.type === 'processing') {
              updateStreamingContent(data.content || '正在分析您的问题...', sessionId)
            } else if (data.type === 'message') {
              fullContent += data.content || ''
              updateStreamingContent(fullContent, sessionId)
            } else if (data.type === 'clarification') {
              updateStreamingContent(data.content || '', sessionId)
              finalizeMessage(finalMessageId, {
                messageId: finalMessageId,
                messageType: 4,
                clarificationData: {
                  clarificationId: data.clarificationId,
                  clarificationType: data.clarificationType,
                  question: data.content,
                  options: data.options,
                },
              }, sessionId)
            } else if (data.type === 'done') {
              finalizeMessage(finalMessageId, {
                messageId: finalMessageId,
                sources: data.sources,
                evidenceLevel: data.evidenceLevel,
                tokensUsed: data.usage?.totalTokens,
              }, sessionId)
            } else if (data.type === 'error') {
              const target = targetMessages(sessionId)
              const last = target[target.length - 1]
              if (last && last.messageType === 2) {
                last.content = data.content || '抱歉，处理您的问题时出错了，请重试。'
              }
              if (sessionId === currentSessionId.value) {
                isStreaming.value = false
                streamingContent.value = ''
              }
            }
          } catch {
            // skip unparseable lines
          }
        }
      }

      if (sessionId === currentSessionId.value) {
        isStreaming.value = false
        streamingContent.value = ''
      }
    } catch (e: any) {
      if (sessionId !== currentSessionId.value) return // switched away, silently discard
      const last = messages.value[messages.value.length - 1]
      if (last && last.messageType === 2) {
        last.content = e?.message || '抱歉，回复生成失败，请重试。'
      }
      isStreaming.value = false
      streamingContent.value = ''
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
        await sendMessage(rewritten, true)
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
