<template>
  <div class="main-layout">
    <aside class="chat-sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <el-icon :size="18" color="#fff"><FirstAidKit /></el-icon>
        </div>
        <span class="brand-name">健康助手</span>
      </div>

      <div class="sidebar-top">
        <button class="new-chat-btn" @click="handleNewChat">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新对话</span>
        </button>
        <div class="search-wrap">
          <el-icon :size="14" class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索会话..."
          />
        </div>
      </div>

      <div class="session-section-label">最近对话</div>

      <el-scrollbar class="session-list">
        <div
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: chatStore.currentSessionId === session.sessionId }"
          @click="handleSessionClick(session.sessionId)"
        >
          <el-icon :size="14" class="session-icon"><ChatDotRound /></el-icon>
          <div class="session-info">
            <div class="session-title">{{ session.title || '新对话' }}</div>
            <div class="session-count">{{ session.messageCount || 0 }} 条消息</div>
          </div>
          <el-popconfirm
            title="确定删除此会话？"
            @confirm="chatStore.removeSession(session.sessionId)"
          >
            <template #reference>
              <button class="session-delete" @click.stop>
                <el-icon :size="12"><Delete /></el-icon>
              </button>
            </template>
          </el-popconfirm>
        </div>
        <div v-if="!filteredSessions.length" class="session-empty">
          <el-icon :size="28" color="#334155"><ChatDotRound /></el-icon>
          <span>暂无会话</span>
        </div>
      </el-scrollbar>

      <div class="sidebar-bottom">
        <router-link to="/health-record" class="sidebar-link">
          <el-icon :size="16"><Document /></el-icon>
          <span>健康档案</span>
        </router-link>
        <router-link to="/profile" class="sidebar-link">
          <el-icon :size="16"><Setting /></el-icon>
          <span>个人设置</span>
        </router-link>
        <router-link v-if="authStore.isAdmin" to="/admin" class="sidebar-link admin-link">
          <el-icon :size="16"><Monitor /></el-icon>
          <span>管理后台</span>
        </router-link>
        <div class="sidebar-divider"></div>
        <div class="user-info-row">
          <el-avatar :size="32" :src="authStore.userInfo?.avatarUrl" class="user-avatar-sm">
            {{ authStore.userInfo?.nickname?.[0] || 'U' }}
          </el-avatar>
          <div class="user-meta">
            <div class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</div>
            <div class="user-role">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</div>
          </div>
          <button class="logout-btn" @click="authStore.logout()" title="退出登录">
            <el-icon :size="16"><SwitchButton /></el-icon>
          </button>
        </div>
      </div>
    </aside>

    <div class="main-content">
      <header class="main-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/chat' }">对话</el-breadcrumb-item>
            <el-breadcrumb-item v-if="chatStore.currentSession">
              {{ chatStore.currentSession.title || '新对话' }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag v-if="chatStore.isStreaming" type="success" size="small" class="streaming-tag">
            <span class="streaming-dot"></span>
            AI 正在回复
          </el-tag>
          <el-dropdown trigger="click">
            <div class="user-avatar-wrap">
              <el-avatar :size="34" :src="authStore.userInfo?.avatarUrl">
                {{ authStore.userInfo?.nickname?.[0] || 'U' }}
              </el-avatar>
              <span class="username">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
              <el-icon :size="12" color="#9ca3af"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><Setting /></el-icon>个人设置
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/health-record')">
                  <el-icon><Document /></el-icon>健康档案
                </el-dropdown-item>
                <el-dropdown-item divided @click="authStore.logout()">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Search, FirstAidKit, Plus, ChatDotRound, Delete,
  Document, Setting, Monitor, SwitchButton, ArrowDown
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const authStore = useAuthStore()
const chatStore = useChatStore()
const router = useRouter()
const route = useRoute()
const searchKeyword = ref('')

const filteredSessions = computed(() => {
  if (!searchKeyword.value) return chatStore.sessions
  const kw = searchKeyword.value.toLowerCase()
  return chatStore.sessions.filter(
    (s) => (s.title || '').toLowerCase().includes(kw)
  )
})

function handleSessionClick(sessionId: string) {
  chatStore.setSession(sessionId)
  if (route.path !== '/chat') {
    router.push('/chat')
  }
}

async function handleNewChat() {
  chatStore.newSession()
  router.push('/chat')
}

onMounted(() => {
  chatStore.fetchSessions()
  chatStore.fetchSuggested()
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

/* Sidebar */
.chat-sidebar {
  width: var(--chat-sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.04);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid var(--sidebar-border);
}

.brand-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.2px;
}

.sidebar-top {
  padding: 12px 12px 8px;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(37, 99, 235, 0.15);
  border: 1px solid rgba(37, 99, 235, 0.3);
  border-radius: 10px;
  color: #93c5fd;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}

.new-chat-btn:hover {
  background: rgba(37, 99, 235, 0.25);
  border-color: rgba(37, 99, 235, 0.5);
  color: #bfdbfe;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  padding: 7px 12px;
}

.search-icon {
  color: #475569;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 13px;
  color: #94a3b8;
  min-width: 0;
}

.search-input::placeholder {
  color: #475569;
}

.session-section-label {
  padding: 8px 16px 4px;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.session-list {
  flex: 1;
  padding: 0 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.15s;
  position: relative;
}

.session-item:hover {
  background: var(--sidebar-hover);
}

.session-item.active {
  background: var(--sidebar-active);
}

.session-icon {
  color: #475569;
  flex-shrink: 0;
}

.session-item.active .session-icon {
  color: #93c5fd;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 13px;
  color: var(--sidebar-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.session-item.active .session-title {
  color: var(--sidebar-text-active);
}

.session-count {
  font-size: 11px;
  color: #475569;
  margin-top: 1px;
}

.session-delete {
  opacity: 0;
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 3px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-delete:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 16px;
  color: #334155;
  font-size: 13px;
}

.sidebar-bottom {
  padding: 8px 12px 16px;
  border-top: 1px solid var(--sidebar-border);
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  color: #64748b;
  text-decoration: none;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
}

.sidebar-link:hover {
  background: var(--sidebar-hover);
  color: #94a3b8;
}

.admin-link {
  color: #a78bfa;
}

.admin-link:hover {
  color: #c4b5fd;
}

.sidebar-divider {
  height: 1px;
  background: var(--sidebar-border);
  margin: 8px 0;
}

.user-info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
}

.user-avatar-sm {
  flex-shrink: 0;
  font-size: 13px;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #cbd5e1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: #475569;
}

.logout-btn {
  background: transparent;
  border: none;
  color: #475569;
  cursor: pointer;
  padding: 5px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

/* Main content */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-page);
}

.main-header {
  height: var(--header-height);
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.streaming-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}

.streaming-dot {
  width: 6px;
  height: 6px;
  background: #16a34a;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.user-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}

.user-avatar-wrap:hover {
  background: var(--bg-hover);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.content-body {
  flex: 1;
  overflow: hidden;
}
</style>
