import request from './request'

export interface ChatSession {
  id: number
  sessionId: string
  title: string
  messageCount: number
  lastMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  messageId: string
  messageType: number
  content: string
  evidenceLevel?: number
  sources?: Array<{
    title: string
    url: string
    sourceName: string
  }>
  parentMessageId?: string
  clarificationData?: Record<string, any>
  feedbackType?: number
  tokensUsed?: number
  createdAt: string
}

export function getSessions() {
  return request.get<{ content: ChatSession[] }>('/chat/sessions')
}

export function getSession(sessionId: string) {
  return request.get<ChatSession>(`/chat/sessions/${sessionId}`)
}

export function createSession(firstMessage: string) {
  return request.post<ChatSession>('/chat/sessions', { firstMessage })
}

export function deleteSession(sessionId: string) {
  return request.delete(`/chat/sessions/${sessionId}`)
}

export function getMessages(sessionId: string) {
  return request.get<ChatMessage[]>(`/chat/messages/${sessionId}`)
}

export function sendClarification(sessionId: string, clarificationId: string, answer: string) {
  return request.post('/chat/clarify', { sessionId, clarificationId, answer })
}

export function submitFeedback(messageId: string, feedbackType: number) {
  return request.post('/chat/feedback', { messageId, feedbackType })
}

export function getSuggestedQuestions() {
  return request.get<string[]>('/chat/suggested-questions')
}
