<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <el-icon :size="28" color="#409EFF"><FirstAidKit /></el-icon>
          <span class="logo-text">医疗健康智能助手</span>
        </div>
        <div class="header-actions">
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </div>
      </div>
    </header>
    <main class="public-main">
      <router-view />
    </main>
    <footer class="public-footer">
      <p>医疗健康智能助手 &copy; 2026 - AI 辅助健康咨询，不构成医疗诊断建议</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.public-header {
  height: 64px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
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
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.public-main {
  flex: 1;
  display: flex;
}

.public-footer {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
}
</style>
