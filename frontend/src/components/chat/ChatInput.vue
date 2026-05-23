<template>
  <div class="chat-input">
    <div class="input-wrapper">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入您的健康问题..."
        resize="none"
        :disabled="disabled"
        @keydown="handleKeydown"
      />
      <div class="input-actions">
        <span class="input-hint">Enter 发送，Shift+Enter 换行</span>
        <el-button
          type="primary"
          :disabled="!inputText.trim() || disabled"
          :loading="disabled"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ send: [content: string] }>()

const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<style scoped>
.chat-input {
  padding: 16px 24px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
