import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/api/auth'
import router from '@/router'
import { getValidToken } from '@/utils/jwt'

interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  userType: number
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getValidToken() || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.userType === 3)

  function setAuth(accessToken: string, rt: string, info: UserInfo) {
    token.value = accessToken
    refreshToken.value = rt
    userInfo.value = info
    localStorage.setItem('token', accessToken)
    localStorage.setItem('refreshToken', rt)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function clearAuth() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    setAuth(res.data.accessToken, res.data.refreshToken, res.data.userInfo)
    router.push('/chat')
  }

  async function register(data: RegisterRequest) {
    await registerApi(data)
    router.push('/login')
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuth()
      router.push('/')
    }
  }

  // restore userInfo from localStorage
  const stored = localStorage.getItem('userInfo')
  if (stored) {
    try {
      userInfo.value = JSON.parse(stored)
    } catch {
      // ignore
    }
  }

  return { token, refreshToken, userInfo, isLoggedIn, isAdmin, setAuth, clearAuth, login, register, logout }
})
