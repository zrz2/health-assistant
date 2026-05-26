<template>
  <div class="chat-message" :class="[isUser ? 'user-message' : 'assistant-message', isClarification ? 'clarification-message' : '']">
    <div class="message-avatar">
      <el-avatar v-if="isUser" :size="34" :src="authStore.userInfo?.avatarUrl" class="user-avatar">
        {{ authStore.userInfo?.nickname?.[0] || 'U' }}
      </el-avatar>
      <div v-else class="ai-avatar">
        <el-icon :size="16" color="#fff"><FirstAidKit /></el-icon>
      </div>
    </div>

    <div class="message-body">
      <div class="message-sender">
        {{ isUser ? (authStore.userInfo?.nickname || '我') : 'AI 健康助手' }}
      </div>

      <div class="message-content" :class="{ 'markdown-content': !isUser && !isClarification }">
        <MarkdownRenderer v-if="!isUser && message.content && !isClarification" :content="message.content" />
        <span v-else>{{ message.content }}</span>
        <span v-if="isStreaming && !message.content" class="typing-indicator">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </span>
      </div>

      <div v-if="isClarification && message.clarificationData" class="clarification-hint">
        <el-tag :type="message.clarificationData.answered ? 'success' : 'warning'" size="small" round>
          {{ message.clarificationData.answered ? '已补充信息' : '请在下方输入框中补充信息' }}
        </el-tag>
      </div>

      <div v-if="!isUser && !isStreaming && message.content && !isClarification" class="message-extra">
        <div v-if="message.sources?.length" class="message-sources">
          <el-collapse class="sources-collapse">
            <el-collapse-item :title="`查看引用来源 (${message.sources.length}条)`">
              <div v-for="(src, idx) in message.sources" :key="idx" class="source-item">
                <span class="source-index">{{ idx + 1 }}</span>
                <a :href="src.url" target="_blank" class="source-title">{{ src.title }}</a>
                <el-tag size="small" type="info" round>{{ src.sourceName }}</el-tag>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="message-footer">
          <div v-if="message.evidenceLevel" class="evidence-badge" :class="`level-${message.evidenceLevel}`">
            <el-icon :size="11"><Medal /></el-icon>
            证据等级 Level {{ message.evidenceLevel }}
          </div>
          <span v-if="message.tokensUsed" class="token-info">{{ message.tokensUsed }} tokens</span>
          <div class="message-feedback">
            <button
              class="feedback-btn"
              :class="{ active: message.feedbackType === 1 }"
              :disabled="!!message.feedbackType"
              @click="$emit('feedback', 1)"
              title="有帮助"
            >
              <el-icon :size="14"><CaretTop /></el-icon>
            </button>
            <button
              class="feedback-btn danger"
              :class="{ active: message.feedbackType === 2 }"
              :disabled="!!message.feedbackType"
              @click="$emit('feedback', 2)"
              title="没帮助"
            >
              <el-icon :size="14"><CaretBottom /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import { FirstAidKit, Medal, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import type { ChatMessage } from '@/api/chat'

const props = defineProps<{
  message: ChatMessage
  isStreaming?: boolean
}>()

defineEmits<{
  feedback: [type: number]
}>()

const authStore = useAuthStore()

const isUser = computed(() => props.message.messageType === 1)
const isClarification = computed(() => props.message.messageType === 4)
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 14px;
  padding: 20px 32px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.chat-message.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  padding-top: 4px;
}

.user-avatar {
  font-size: 14px;
}

.ai-avatar {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.message-body {
  flex: 1;
  min-width: 0;
}

.user-message .message-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 6px;
  letter-spacing: 0.2px;
}

.message-content {
  font-size: 14px;
  line-height: 1.75;
}

.user-message .message-content {
  background: linear-gradient(135deg, var(--primary) 0%, #4f46e5 100%);
  color: #fff;
  padding: 12px 18px;
  border-radius: 16px 4px 16px 16px;
  display: inline-block;
  max-width: 75%;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.assistant-message .message-content {
  color: var(--text-primary);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 4px 16px 16px 16px;
  padding: 14px 18px;
  box-shadow: var(--shadow-sm);
}

.clarification-message .message-content {
  background: #fffbeb;
  border-color: #fde68a;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
  align-items: center;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: blink 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 60%, 100% { opacity: 0.2; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.clarification-hint {
  margin-top: 8px;
}

.message-extra {
  margin-top: 10px;
}

/* Sources */
.sources-collapse {
  --el-collapse-border-color: var(--border-light);
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}

.sources-collapse :deep(.el-collapse-item__header) {
  font-size: 12px;
  color: var(--text-muted);
  padding: 8px 14px;
  height: auto;
  line-height: 1.5;
}

.sources-collapse :deep(.el-collapse-item__content) {
  padding: 0 14px 10px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--border-light);
}

.source-item:last-child {
  border-bottom: none;
}

.source-index {
  width: 18px;
  height: 18px;
  background: var(--primary-bg);
  color: var(--primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.source-title {
  color: var(--primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
}

.source-title:hover {
  text-decoration: underline;
}

/* Footer */
.message-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.evidence-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 100px;
}

.evidence-badge.level-1,
.evidence-badge.level-2 {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.evidence-badge.level-3 {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.evidence-badge.level-4 {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid var(--border-light);
}

.token-info {
  font-size: 11px;
  color: var(--text-placeholder);
}

.message-feedback {
  display: flex;
  gap: 4px;
  margin-left: auto;
}

.feedback-btn {
  background: transparent;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  transition: all 0.15s;
}

.feedback-btn:hover:not(:disabled) {
  background: var(--primary-bg);
  border-color: var(--primary);
  color: var(--primary);
}

.feedback-btn.active {
  background: var(--primary-bg);
  border-color: var(--primary);
  color: var(--primary);
}

.feedback-btn.danger:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #dc2626;
  color: #dc2626;
}

.feedback-btn.danger.active {
  background: #fef2f2;
  border-color: #dc2626;
  color: #dc2626;
}

.feedback-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
