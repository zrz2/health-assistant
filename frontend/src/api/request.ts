import axios from 'axios'
import type { AxiosInstance, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const instance: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

instance.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          const res = await axios.post('/api/v1/auth/refresh', { refreshToken })
          const newToken = res.data.data.accessToken
          localStorage.setItem('token', newToken)
          pendingRequests.forEach((cb) => cb(newToken))
          pendingRequests = []
          if (error.config) {
            error.config.headers.Authorization = `Bearer ${newToken}`
            return instance(error.config)
          }
        } catch {
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('userInfo')
          router.push('/login')
        } finally {
          isRefreshing = false
        }
      } else if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((token: string) => {
            if (error.config) {
              error.config.headers.Authorization = `Bearer ${token}`
              resolve(instance(error.config))
            }
          })
        })
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default instance
