import request from './request'

export interface AdminUser {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  userType: number
  lastLoginTime: string
  createdAt: string
}

export interface DashboardStats {
  todaySessions: number
  activeUsers: number
  knowledgeCount: number
  avgResponseTime: number
}

export interface KnowledgeItem {
  docId: string
  title: string
  content: string
  sourceName: string
  sourceType: number
  evidenceLevel: number
  category: string
  status: number
  createdAt: string
}

export interface SensitiveWord {
  id: number
  word: string
  category: string
  level: number
  enabled: number
}

// Dashboard
export function getDashboardStats() {
  return request.get<DashboardStats>('/admin/dashboard/stats')
}

export function getDashboardTrends() {
  return request.get('/admin/dashboard/trends')
}

export function getSourceDistribution() {
  return request.get('/admin/dashboard/source-distribution')
}

// Users
export function getUsers(params?: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: AdminUser[]; total: number }>('/admin/users', { params })
}

export function getUser(id: number) {
  return request.get<AdminUser>(`/admin/users/${id}`)
}

export function updateUserStatus(id: number, status: number) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export function updateUserRole(id: number, userType: number) {
  return request.put(`/admin/users/${id}/role`, null, { params: { userType } })
}

// Knowledge
export function getKnowledgeItems(params?: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: KnowledgeItem[]; total: number }>('/admin/knowledge/items', { params })
}

export function getKnowledgeItem(docId: string) {
  return request.get<KnowledgeItem>(`/admin/knowledge/items/${docId}`)
}

export function createKnowledgeItem(data: Partial<KnowledgeItem>) {
  return request.post('/admin/knowledge/items', data)
}

export function updateKnowledgeItem(docId: string, data: Partial<KnowledgeItem>) {
  return request.put(`/admin/knowledge/items/${docId}`, data)
}

export function deleteKnowledgeItem(docId: string) {
  return request.delete(`/admin/knowledge/items/${docId}`)
}

export function batchDeleteKnowledgeItems(docIds: string[]) {
  return request.post('/admin/knowledge/items/batch-delete', { docIds })
}

export function importSingleKnowledge(data: any) {
  return request.post('/admin/knowledge/import/single', data)
}

export function importBatchKnowledge(formData: FormData) {
  return request.post('/admin/knowledge/import/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// Sensitive Words
export function getSensitiveWords() {
  return request.get<SensitiveWord[]>('/admin/sensitive-words')
}

export function addSensitiveWord(data: { word: string; category?: string; level?: number }) {
  return request.post('/admin/sensitive-words', data)
}

export function deleteSensitiveWord(id: number) {
  return request.delete(`/admin/sensitive-words/${id}`)
}
