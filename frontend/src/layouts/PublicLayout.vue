<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <el-icon :size="20" color="#fff"><FirstAidKit /></el-icon>
          </div>
          <span class="logo-text">医疗健康智能助手</span>
        </div>
        <nav class="header-nav">
          <el-button text class="nav-btn" @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" class="nav-register-btn" @click="$router.push('/register')">免费注册</el-button>
        </nav>
      </div>
    </header>
    <main class="public-main">
      <router-view />
    </main>
    <footer class="public-footer">
      <div class="footer-inner">
        <div class="footer-logo">
          <div class="logo-icon-sm">
            <el-icon :size="14" color="#fff"><FirstAidKit /></el-icon>
          </div>
          <span>医疗健康智能助手</span>
        </div>
        <p class="footer-disclaimer">AI 辅助健康咨询，不构成医疗诊断建议 &copy; 2026</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { FirstAidKit } from '@element-plus/icons-vue'
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.public-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-decoration: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-btn {
  color: var(--text-secondary) !important;
  font-size: 14px;
}

.nav-register-btn {
  padding: 8px 20px !important;
  font-weight: 500;
}

.public-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.public-footer {
  background: var(--text-primary);
  padding: 24px 32px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
}

.logo-icon-sm {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-disclaimer {
  color: #64748b;
  font-size: 13px;
}
</style>
