<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg-grid"></div>
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          基于循证医学 · AI 驱动
        </div>
        <h1 class="hero-title">
          您的专属<br />
          <span class="gradient-text">智能健康顾问</span>
        </h1>
        <p class="hero-desc">
          整合 WHO、权威医疗机构知识库，结合通义千问大模型，<br />
          为您提供科学、可靠、个性化的健康咨询服务
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="cta-btn" @click="$router.push('/login')">
            <el-icon><ChatDotRound /></el-icon>
            开始咨询
          </el-button>
          <el-button size="large" class="secondary-btn" @click="$router.push('/register')">
            免费注册
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="stat-val">40+</span>
            <span class="stat-desc">权威知识来源</span>
          </div>
          <div class="stat-divider"></div>
          <div class="hero-stat">
            <span class="stat-val">4级</span>
            <span class="stat-desc">证据等级评估</span>
          </div>
          <div class="stat-divider"></div>
          <div class="hero-stat">
            <span class="stat-val">毫秒级</span>
            <span class="stat-desc">智能检索速度</span>
          </div>
          <div class="stat-divider"></div>
          <div class="hero-stat">
            <span class="stat-val">流式</span>
            <span class="stat-desc">实时对话体验</span>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="chat-preview">
          <div class="preview-header">
            <div class="preview-dots">
              <span></span><span></span><span></span>
            </div>
            <span class="preview-title">健康咨询</span>
          </div>
          <div class="preview-body">
            <div class="preview-msg assistant">
              <div class="preview-avatar">
                <el-icon :size="14" color="#fff"><FirstAidKit /></el-icon>
              </div>
              <div class="preview-bubble">您好！我是您的智能健康顾问，请问有什么可以帮助您的？</div>
            </div>
            <div class="preview-msg user">
              <div class="preview-bubble user-bubble">我最近总是头痛，应该怎么办？</div>
            </div>
            <div class="preview-msg assistant">
              <div class="preview-avatar">
                <el-icon :size="14" color="#fff"><FirstAidKit /></el-icon>
              </div>
              <div class="preview-bubble">
                头痛可能由多种原因引起。根据循证医学建议：
                <br />• 注意休息，保证充足睡眠
                <br />• 检查是否存在颈椎问题
                <br />• 如持续超过3天，建议就医检查
                <div class="preview-source">
                  <el-icon :size="10"><Link /></el-icon>
                  引用来源：WHO 头痛指南 · Level 1 证据
                </div>
              </div>
            </div>
            <div class="preview-typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="section-header">
        <div class="section-tag">核心功能</div>
        <h2 class="section-title">为什么选择我们</h2>
        <p class="section-desc">专业、可靠、个性化的健康咨询体验</p>
      </div>
      <el-row :gutter="24">
        <el-col :span="8" v-for="f in features" :key="f.title">
          <div class="feature-card">
            <div class="feature-icon" :style="{ background: f.iconBg }">
              <el-icon :size="24" :color="f.iconColor">
                <component :is="f.icon" />
              </el-icon>
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- How it works -->
    <section class="how-it-works">
      <div class="section-header">
        <div class="section-tag">使用流程</div>
        <h2 class="section-title">三步开始健康咨询</h2>
      </div>
      <div class="steps">
        <div class="step" v-for="(step, i) in steps" :key="i">
          <div class="step-num">{{ i + 1 }}</div>
          <div class="step-content">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
          <div class="step-arrow" v-if="i < steps.length - 1">
            <el-icon :size="20" color="#d1d5db"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <!-- Disclaimer -->
    <section class="disclaimer-section">
      <div class="disclaimer-card">
        <el-icon :size="20" color="#d97706"><Warning /></el-icon>
        <div>
          <strong>医学免责声明</strong>
          <p>本系统提供的健康信息仅供参考，不构成医疗诊断或治疗建议。如有身体不适，请及时前往正规医疗机构就诊。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Search, Document, ArrowRight, Warning, Link, FirstAidKit } from '@element-plus/icons-vue'

const features = [
  {
    icon: 'ChatDotRound',
    iconBg: '#eff6ff',
    iconColor: '#2563eb',
    title: 'AI 智能问答',
    desc: '基于通义千问大模型，提供流式对话体验，实时生成专业的健康建议，支持多轮上下文理解',
  },
  {
    icon: 'Search',
    iconBg: '#f0fdf4',
    iconColor: '#16a34a',
    title: '科学循证',
    desc: '整合 WHO、权威医疗机构知识，所有回答附带证据等级和引用来源，确保信息可靠性',
  },
  {
    icon: 'Document',
    iconBg: '#fefce8',
    iconColor: '#d97706',
    title: '健康档案',
    desc: '建立个人健康档案，记录既往病史、用药情况，获得更具个性化的健康建议',
  },
]

const steps = [
  { title: '注册账号', desc: '免费创建您的健康账号' },
  { title: '完善档案', desc: '填写个人健康信息' },
  { title: '开始咨询', desc: '获取专业健康建议' },
]
</script>

<style scoped>
.home-page {
  flex: 1;
}

/* Hero */
.hero {
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%);
  min-height: 680px;
  display: flex;
  align-items: center;
  padding: 80px 32px;
  position: relative;
  overflow: hidden;
  gap: 60px;
  max-width: 100%;
}

.hero-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.hero-content {
  flex: 1;
  max-width: 580px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(37, 99, 235, 0.15);
  border: 1px solid rgba(37, 99, 235, 0.3);
  color: #93c5fd;
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 28px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  background: #3b82f6;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  color: #fff;
  line-height: 1.15;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.gradient-text {
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 16px;
  color: #94a3b8;
  line-height: 1.8;
  margin-bottom: 36px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 48px;
}

.cta-btn {
  padding: 14px 28px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%) !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.4) !important;
}

.secondary-btn {
  padding: 14px 24px !important;
  font-size: 15px !important;
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  color: #e2e8f0 !important;
}

.secondary-btn:hover {
  background: rgba(255,255,255,0.12) !important;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-val {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.stat-desc {
  font-size: 12px;
  color: #64748b;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255,255,255,0.1);
}

/* Chat preview */
.hero-visual {
  flex: 1;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

.chat-preview {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}

.preview-header {
  background: rgba(255,255,255,0.05);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.preview-dots {
  display: flex;
  gap: 5px;
}

.preview-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
}

.preview-dots span:nth-child(1) { background: #ef4444; }
.preview-dots span:nth-child(2) { background: #f59e0b; }
.preview-dots span:nth-child(3) { background: #22c55e; }

.preview-title {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

.preview-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.preview-msg.user {
  flex-direction: row-reverse;
}

.preview-avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.preview-bubble {
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 12px;
  color: #cbd5e1;
  line-height: 1.6;
  max-width: 280px;
}

.user-bubble {
  background: rgba(37, 99, 235, 0.25);
  border-color: rgba(37, 99, 235, 0.3);
  color: #bfdbfe;
}

.preview-source {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255,255,255,0.08);
  font-size: 11px;
  color: #64748b;
}

.preview-typing {
  display: flex;
  gap: 4px;
  padding: 4px 8px;
}

.preview-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #475569;
  animation: typing 1.4s infinite;
}

.preview-typing span:nth-child(2) { animation-delay: 0.2s; }
.preview-typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-3px); }
}

/* Features */
.features {
  max-width: 1200px;
  margin: 0 auto;
  padding: 96px 32px;
}

.section-header {
  text-align: center;
  margin-bottom: 56px;
}

.section-tag {
  display: inline-block;
  background: var(--primary-bg);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 100px;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}

.section-title {
  font-size: 36px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.section-desc {
  font-size: 16px;
  color: var(--text-muted);
}

.feature-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 32px 28px;
  transition: all 0.25s ease;
  height: 100%;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-border);
}

.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.feature-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.7;
}

/* How it works */
.how-it-works {
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  padding: 96px 32px;
}

.how-it-works .section-header {
  max-width: 1200px;
  margin: 0 auto 56px;
}

.steps {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 0;
}

.step {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.step-num {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.step-content p {
  font-size: 13px;
  color: var(--text-muted);
}

.step-arrow {
  flex-shrink: 0;
  padding: 0 16px;
}

/* Disclaimer */
.disclaimer-section {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

.disclaimer-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 20px 24px;
}

.disclaimer-card strong {
  display: block;
  font-size: 14px;
  color: #92400e;
  margin-bottom: 4px;
}

.disclaimer-card p {
  font-size: 13px;
  color: #78350f;
  line-height: 1.6;
}
</style>
