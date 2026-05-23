import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: {
    id: number
    username: string
    nickname: string
    avatarUrl: string | null
    userType: number
  }
}

export function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post('/auth/register', data)
}

export function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh', { refreshToken })
}

export function logout() {
  return request.post('/auth/logout')
}
