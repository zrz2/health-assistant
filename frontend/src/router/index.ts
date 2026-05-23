import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getValidToken } from '@/utils/jwt'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/public/HomePage.vue') },
      { path: 'login', name: 'Login', component: () => import('@/views/public/LoginPage.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/public/RegisterPage.vue') },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'chat', name: 'Chat', component: () => import('@/views/chat/ChatPage.vue') },
      { path: 'chat/:sessionId', name: 'ChatSession', component: () => import('@/views/chat/ChatPage.vue') },
      { path: 'health-record', name: 'HealthRecord', component: () => import('@/views/user/HealthRecordPage.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/user/ProfilePage.vue') },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/DashboardPage.vue') },
      { path: 'users', name: 'UserManagement', component: () => import('@/views/admin/UserManagementPage.vue') },
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/admin/KnowledgePage.vue') },
      { path: 'stats', name: 'Statistics', component: () => import('@/views/admin/StatisticsPage.vue') },
      { path: 'config', name: 'SystemConfig', component: () => import('@/views/admin/SystemConfigPage.vue') },
      { path: 'sensitive-words', name: 'SensitiveWords', component: () => import('@/views/admin/SensitiveWordsPage.vue') },
      { path: 'logs', name: 'OperationLogs', component: () => import('@/views/admin/OperationLogsPage.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = getValidToken()

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin) {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.userType !== 3) {
          next({ name: 'Chat' })
          return
        }
      } catch {
        next({ name: 'Login' })
        return
      }
    } else {
      next({ name: 'Login' })
      return
    }
  }

  // 已登录用户访问登录/注册页 → 重定向到对话页
  if (token && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Chat' })
    return
  }

  next()
})

export default router
