<template>
  <div class="main-layout">
    <aside class="chat-sidebar">
      <div class="sidebar-top">
        <el-button type="primary" size="large" class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会话..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="search-input"
        />
      </div>

      <el-scrollbar class="session-list">
        <div
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: chatStore.currentSessionId === session.sessionId }"
          @click="handleSessionClick(session.sessionId)"
        >
          <div class="session-title">{{ session.title || '新对话' }}</div>
          <div class="session-meta">
            <span>{{ session.messageCount || 0 }} 条消息</span>
            <el-popconfirm
              title="确定删除此会话？"
              @confirm="chatStore.removeSession(session.sessionId)"
            >
              <template #reference>
                <el-icon class="delete-icon" :size="14" @click.stop><Delete /></el-icon>
              </template>
            </el-popconfirm>
          </div>
        </div>
        <el-empty v-if="!filteredSessions.length" description="暂无会话" :image-size="60" />
      </el-scrollbar>

      <div class="sidebar-bottom">
        <router-link to="/health-record" class="sidebar-link">
          <el-icon><Document /></el-icon>
          健康档案
        </router-link>
        <router-link to="/profile" class="sidebar-link">
          <el-icon><Setting /></el-icon>
          个人设置
        </router-link>
        <router-link v-if="authStore.isAdmin" to="/admin" class="sidebar-link admin-link">
          <el-icon><Monitor /></el-icon>
          管理后台
        </router-link>
        <div class="sidebar-link logout-btn" @click="authStore.logout()">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </div>
      </div>
    </aside>

    <div class="main-content">
      <header class="main-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/chat' }">对话</el-breadcrumb-item>
          <el-breadcrumb-item v-if="chatStore.currentSession">
            {{ chatStore.currentSession.title || '新对话' }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown trigger="click">
          <div class="user-avatar">
            <el-avatar :size="32" :src="authStore.userInfo?.avatarUrl">
              {{ authStore.userInfo?.nickname?.[0] || 'U' }}
            </el-avatar>
            <span class="username">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人设置</el-dropdown-item>
              <el-dropdown-item divided @click="authStore.logout()">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
import { Search } from '@element-plus/icons-vue'
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
}

.chat-sidebar {
  width: var(--chat-sidebar-width);
  background: #1d1e2b;
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-top {
  padding: 16px;
}

.new-chat-btn {
  width: 100%;
  margin-bottom: 12px;
}

.search-input {
  --el-input-bg-color: #2a2b3d;
  --el-input-border-color: #3a3b4d;
  --el-input-text-color: #e0e0e0;
}

.session-list {
  flex: 1;
  padding: 0 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.session-item:hover {
  background: #2a2b3d;
}

.session-item.active {
  background: #2a2b3d;
  border: 1px solid #409EFF;
}

.session-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.delete-icon {
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .delete-icon {
  opacity: 1;
}

.sidebar-bottom {
  padding: 12px 16px;
  border-top: 1px solid #2a2b3d;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  color: #c0c4cc;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.sidebar-link:hover {
  background: #2a2b3d;
  color: #fff;
}

.logout-btn:hover {
  color: #F56C6C;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fa;
}

.main-header {
  height: var(--header-height);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #303133;
}

.content-body {
  flex: 1;
  overflow: hidden;
}
</style>
