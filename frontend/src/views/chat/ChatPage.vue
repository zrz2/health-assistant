<template>
  <div class="chat-page">
    <!-- Empty state -->
    <div v-if="!chatStore.currentSessionId && !chatStore.messages.length" class="empty-state">
      <el-icon :size="72" color="#c0c4cc"><FirstAidKit /></el-icon>
      <h3>医疗健康智能助手</h3>
      <p>输入您的健康问题，获取基于循证医学的专业建议</p>
      <div class="suggested-questions" v-if="chatStore.suggestedQuestions.length">
        <div class="suggested-label">您可以尝试问：</div>
        <div
          v-for="(q, idx) in chatStore.suggestedQuestions"
          :key="idx"
          class="suggested-item"
          @click="handleSend(q)"
        >
          {{ q }}
        </div>
      </div>
    </div>

    <!-- Messages -->
    <el-scrollbar v-else ref="scrollRef" class="message-area">
      <ChatMessage
        v-for="msg in chatStore.messages"
        :key="msg.messageId"
        :message="msg"
        :is-streaming="chatStore.isStreaming && msg.content === chatStore.streamingContent && (msg.messageType === 2 || msg.messageType === 4)"
        @feedback="(type) => chatStore.sendFeedback(msg.messageId, type)"
      />
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

const chatStore = useChatStore()
const scrollRef = ref<any>(null)
const scrollAnchor = ref<HTMLElement>()

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(
  () => chatStore.messages.length,
  () => scrollToBottom()
)

watch(
  () => chatStore.streamingContent,
  () => scrollToBottom()
)

async function handleSend(content: string) {
  // First message: create session first, then send
  if (!chatStore.currentSessionId) {
    const sessionId = await chatStore.createNewSession(content)
    if (!sessionId) return
    chatStore.currentSessionId = sessionId
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
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  padding: 40px;
}

.empty-state h3 {
  font-size: 22px;
  color: #606266;
  margin: 20px 0 8px;
}

.empty-state p {
  font-size: 14px;
  margin-bottom: 32px;
}

.suggested-questions {
  max-width: 500px;
  width: 100%;
}

.suggested-label {
  font-size: 13px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.suggested-item {
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: border-color 0.2s;
}

.suggested-item:hover {
  border-color: #409EFF;
  color: #409EFF;
}

.message-area {
  flex: 1;
  background: #f5f7fa;
}
</style>
