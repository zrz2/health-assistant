<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <div class="logo-icon">
          <el-icon :size="16" color="#fff"><Monitor /></el-icon>
        </div>
        <span>管理后台</span>
      </div>

      <nav class="admin-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeMenu === item.path }"
        >
          <el-icon :size="16">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/chat" class="back-link">
          <el-icon :size="15"><Back /></el-icon>
          <span>返回对话</span>
        </router-link>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-button text size="small" @click="$router.push('/chat')">
          <el-icon><Back /></el-icon>
          返回对话
        </el-button>
      </header>
      <main class="admin-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, User, Collection, TrendCharts, WarningFilled, Back, Monitor } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)

const navItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'DataAnalysis' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/knowledge', label: '知识库管理', icon: 'Collection' },
  { path: '/admin/stats', label: '咨询统计', icon: 'TrendCharts' },
  { path: '/admin/sensitive-words', label: '敏感词管理', icon: 'WarningFilled' },
]

const pageTitles: Record<string, string> = {
  '/admin/dashboard': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/knowledge': '知识库管理',
  '/admin/stats': '咨询统计',
  '/admin/sensitive-words': '敏感词管理',
}

const currentPageTitle = computed(() => pageTitles[route.path] || '')
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.admin-sidebar {
  width: var(--admin-sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.04);
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid var(--sidebar-border);
}

.logo-icon {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.admin-logo span {
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
}

.admin-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: #64748b;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s;
}

.nav-item:hover {
  background: var(--sidebar-hover);
  color: #94a3b8;
}

.nav-item.active {
  background: var(--sidebar-active);
  color: #93c5fd;
}

.sidebar-footer {
  padding: 12px 10px 16px;
  border-top: 1px solid var(--sidebar-border);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  color: #475569;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.15s;
}

.back-link:hover {
  background: var(--sidebar-hover);
  color: #94a3b8;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-page);
}

.admin-header {
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

.admin-body {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}
</style>
