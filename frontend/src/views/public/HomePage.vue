<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-content">
        <h1>医疗健康智能助手</h1>
        <p class="hero-desc">
          基于 AI 大模型与循证医学知识库，为您提供科学、可靠的健康咨询服务
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/login')">立即体验</el-button>
          <el-button size="large" @click="$router.push('/register')">注册账号</el-button>
        </div>
      </div>
    </section>

    <section class="features">
      <div class="section-title">核心功能</div>
      <el-row :gutter="32">
        <el-col :span="8">
          <div class="feature-card">
            <el-icon :size="48" color="#409EFF"><ChatDotRound /></el-icon>
            <h3>AI 智能问答</h3>
            <p>基于通义千问大模型，提供流式对话体验，实时生成专业的健康建议</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <el-icon :size="48" color="#67C23A"><Search /></el-icon>
            <h3>科学循证</h3>
            <p>整合 WHO、权威医疗机构知识，所有回答附带证据等级和引用来源</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <el-icon :size="48" color="#E6A23C"><Document /></el-icon>
            <h3>健康档案</h3>
            <p>建立个人健康档案，获得更具个性化的健康建议和管理方案</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="highlights">
      <div class="section-title">产品亮点</div>
      <el-row :gutter="24">
        <el-col :span="6">
          <div class="highlight-item">
            <span class="highlight-num">40+</span>
            <span class="highlight-label">权威知识来源</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="highlight-item">
            <span class="highlight-num">毫秒级</span>
            <span class="highlight-label">智能检索速度</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="highlight-item">
            <span class="highlight-num">4级</span>
            <span class="highlight-label">证据等级评估</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="highlight-item">
            <span class="highlight-num">流式</span>
            <span class="highlight-label">实时对话体验</span>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="disclaimer">
      <el-alert
        title="医学免责声明"
        type="warning"
        :closable="false"
        description="本系统提供的健康信息仅供参考，不构成医疗诊断或治疗建议。如有身体不适，请及时前往正规医疗机构就诊。"
        show-icon
      />
    </section>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-page {
  flex: 1;
}

.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 100px 24px 80px;
  text-align: center;
}

.hero-content {
  max-width: 700px;
  margin: 0 auto;
}

.hero h1 {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 20px;
}

.hero-desc {
  font-size: 18px;
  line-height: 1.8;
  opacity: 0.9;
  margin-bottom: 40px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.features, .highlights {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
}

.section-title {
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 48px;
}

.feature-card {
  text-align: center;
  padding: 40px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.feature-card h3 {
  margin: 20px 0 12px;
  font-size: 18px;
  color: #303133;
}

.feature-card p {
  color: #606266;
  line-height: 1.6;
}

.highlights {
  background: #f0f2f5;
  max-width: 100%;
  padding: 80px 24px;
}

.highlights .section-title {
  max-width: 1200px;
  margin: 0 auto 48px;
}

.highlights .el-row {
  max-width: 1200px;
  margin: 0 auto;
}

.highlight-item {
  text-align: center;
}

.highlight-num {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: #409EFF;
  margin-bottom: 8px;
}

.highlight-label {
  color: #606266;
  font-size: 15px;
}

.disclaimer {
  max-width: 800px;
  margin: 0 auto 60px;
  padding: 0 24px;
}
</style>
