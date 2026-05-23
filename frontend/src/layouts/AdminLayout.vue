<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
        <span>管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1d1e2b"
        text-color="#c0c4cc"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><TrendCharts /></el-icon>
          <span>咨询统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Operation /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/sensitive-words">
          <el-icon><WarningFilled /></el-icon>
          <span>敏感词管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Tickets /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-bottom-link">
        <router-link to="/chat" class="back-link">
          <el-icon><Back /></el-icon>
          返回对话
        </router-link>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/admin' }">管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-button text @click="$router.push('/chat')">
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

const route = useRoute()

const activeMenu = computed(() => route.path)

const pageTitles: Record<string, string> = {
  '/admin/dashboard': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/knowledge': '知识库管理',
  '/admin/stats': '咨询统计',
  '/admin/config': '系统配置',
  '/admin/sensitive-words': '敏感词管理',
  '/admin/logs': '操作日志',
}

const currentPageTitle = computed(() => pageTitles[route.path] || '')
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
}

.admin-sidebar {
  width: var(--admin-sidebar-width);
  background: #1d1e2b;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  border-bottom: 1px solid #2a2b3d;
}

.el-menu {
  flex: 1;
  border-right: none;
}

.sidebar-bottom-link {
  padding: 12px 16px;
  border-top: 1px solid #2a2b3d;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #c0c4cc;
  text-decoration: none;
  font-size: 14px;
  padding: 8px;
  border-radius: 6px;
}

.back-link:hover {
  background: #2a2b3d;
  color: #fff;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  height: var(--header-height);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.admin-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f5f7fa;
}
</style>
