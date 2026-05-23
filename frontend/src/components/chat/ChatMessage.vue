<template>
  <div class="chat-message" :class="[isUser ? 'user-message' : 'assistant-message', isClarification ? 'clarification-message' : '']">
    <div class="message-avatar">
      <el-avatar v-if="isUser" :size="32" :src="authStore.userInfo?.avatarUrl">
        {{ authStore.userInfo?.nickname?.[0] || 'U' }}
      </el-avatar>
      <el-icon v-else :size="32" color="#409EFF"><FirstAidKit /></el-icon>
    </div>

    <div class="message-body">
      <div class="message-content" :class="{ 'markdown-content': !isUser && !isClarification }">
        <MarkdownRenderer v-if="!isUser && message.content && !isClarification" :content="message.content" />
        <span v-else>{{ message.content }}</span>
        <span v-if="isStreaming && !message.content" class="typing-indicator">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </span>
      </div>

      <div v-if="isClarification && message.clarificationData" class="clarification-hint">
        <el-tag :type="message.clarificationData.answered ? 'success' : 'warning'" size="small">
          {{ message.clarificationData.answered ? '已补充信息' : '请在下方输入框中补充信息' }}
        </el-tag>
      </div>

      <div v-if="!isUser && !isStreaming && message.content && !isClarification" class="message-extra">
        <div v-if="message.sources?.length" class="message-sources">
          <el-collapse>
            <el-collapse-item title="查看引用来源 ({{ message.sources.length }}条)">
              <div v-for="(src, idx) in message.sources" :key="idx" class="source-item">
                <span class="source-index">{{ idx + 1 }}.</span>
                <a :href="src.url" target="_blank" class="source-title">{{ src.title }}</a>
                <el-tag size="small" type="info">{{ src.sourceName }}</el-tag>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div v-if="message.evidenceLevel" class="message-meta">
          <el-tag size="small" :type="evidenceTagType">
            证据等级: Level {{ message.evidenceLevel }}
          </el-tag>
          <span v-if="message.tokensUsed" class="token-info">Token: {{ message.tokensUsed }}</span>
        </div>

        <div class="message-feedback">
          <el-button
            text
            size="small"
            :type="message.feedbackType === 1 ? 'primary' : ''"
            :disabled="!!message.feedbackType"
            @click="$emit('feedback', 1)"
          >
            <el-icon><ThumbsUp /></el-icon>
          </el-button>
          <el-button
            text
            size="small"
            :type="message.feedbackType === 2 ? 'danger' : ''"
            :disabled="!!message.feedbackType"
            @click="$emit('feedback', 2)"
          >
            <el-icon><ThumbsDown /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
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

const evidenceTagType = computed(() => {
  const level = props.message.evidenceLevel
  if (level && level <= 2) return 'success'
  if (level === 3) return 'warning'
  return 'info'
})
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.chat-message.assistant-message {
  background: #fff;
}

.chat-message.clarification-message {
  background: #fef9f0;
}

.message-avatar {
  flex-shrink: 0;
  padding-top: 2px;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-content {
  font-size: 14px;
  line-height: 1.75;
}

.user-message .message-content {
  background: #ecf5ff;
  color: #303133;
  padding: 10px 16px;
  border-radius: 12px;
  display: inline-block;
  max-width: 80%;
}

.assistant-message .message-content {
  color: #303133;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: blink 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 60%, 100% { opacity: 0.2; }
  30% { opacity: 1; }
}

.clarification-hint {
  margin-top: 8px;
}

.message-extra {
  margin-top: 8px;
}

.message-sources {
  margin-bottom: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

.source-index {
  color: #909399;
  min-width: 20px;
}

.source-title {
  color: #409EFF;
  flex: 1;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.token-info {
  font-size: 12px;
  color: #c0c4cc;
}

.message-feedback {
  display: flex;
  gap: 4px;
}
</style>
