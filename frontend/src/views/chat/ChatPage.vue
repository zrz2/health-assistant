<template>
  <div class="chat-page">
    <!-- Empty state -->
    <div v-if="!chatStore.currentSessionId && !chatStore.messages.length" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="36" color="#fff"><FirstAidKit /></el-icon>
      </div>
      <h3>医疗健康智能助手</h3>
      <p>输入您的健康问题，获取基于循证医学的专业建议</p>

      <div v-if="chatStore.suggestedQuestions.length" class="suggested-wrap">
        <div class="suggested-label">
          <el-icon :size="13"><Promotion /></el-icon>
          您可以尝试问：
        </div>
        <div class="suggested-grid">
          <div
            v-for="(q, idx) in chatStore.suggestedQuestions"
            :key="idx"
            class="suggested-item"
            @click="handleSend(q)"
          >
            <span>{{ q }}</span>
            <el-icon :size="14" class="suggested-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="disclaimer-tip">
        <el-icon :size="13"><Warning /></el-icon>
        本系统仅供参考，不构成医疗诊断建议
      </div>
    </div>

    <!-- Messages -->
    <el-scrollbar v-else ref="scrollRef" class="message-area">
      <div class="message-list">
        <ChatMessage
          v-for="msg in chatStore.messages"
          :key="msg.messageId"
          :message="msg"
          :is-streaming="chatStore.isStreaming && msg.content === chatStore.streamingContent && (msg.messageType === 2 || msg.messageType === 4)"
          @feedback="(type) => chatStore.sendFeedback(msg.messageId, type)"
        />
      </div>
      <div ref="scrollAnchor" />
    </el-scrollbar>

    <!-- Input -->
    <ChatInput :disabled="chatStore.isStreaming" @send="handleSend" />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { FirstAidKit, Promotion, ArrowRight, Warning } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const scrollRef = ref<any>(null)
const scrollAnchor = ref<HTMLElement>()

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(() => chatStore.messages.length, () => scrollToBottom())
watch(() => chatStore.streamingContent, () => scrollToBottom())

async function handleSend(content: string) {
  if (!chatStore.currentSessionId) {
    const sessionId = await chatStore.createNewSession(content)
    if (!sessionId) return
    chatStore.setSession(sessionId)
  }
  scrollToBottom()
  await chatStore.sendMessage(content)
}
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* Empty state */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  text-align: center;
}

.empty-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.3);
}

.empty-state h3 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.3px;
}

.empty-state p {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 40px;
  max-width: 400px;
  line-height: 1.6;
}

.suggested-wrap {
  width: 100%;
  max-width: 600px;
  margin-bottom: 32px;
}

.suggested-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 12px;
  justify-content: center;
}

.suggested-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.suggested-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: left;
  transition: all 0.2s;
}

.suggested-item:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-bg);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.suggested-arrow {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.suggested-item:hover .suggested-arrow {
  opacity: 1;
}

.disclaimer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-placeholder);
}

/* Messages */
.message-area {
  flex: 1;
}

.message-list {
  padding: 16px 0 8px;
}
</style>
