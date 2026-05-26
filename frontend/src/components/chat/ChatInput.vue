<template>
  <div class="chat-input-wrap">
    <div class="input-container">
      <div class="input-box" :class="{ focused: isFocused, disabled: disabled }">
        <textarea
          ref="textareaRef"
          v-model="inputText"
          class="input-textarea"
          placeholder="输入您的健康问题... (Enter 发送，Shift+Enter 换行)"
          :disabled="disabled"
          rows="1"
          @keydown="handleKeydown"
          @focus="isFocused = true"
          @blur="isFocused = false"
          @input="autoResize"
        ></textarea>
        <div class="input-actions">
          <span class="char-count" :class="{ warn: inputText.length > 800 }">
            {{ inputText.length }}/1000
          </span>
          <button
            class="send-btn"
            :class="{ active: inputText.trim() && !disabled }"
            :disabled="!inputText.trim() || disabled"
            @click="handleSend"
          >
            <el-icon v-if="!disabled" :size="18"><Promotion /></el-icon>
            <el-icon v-else :size="18" class="loading-icon"><Loading /></el-icon>
          </button>
        </div>
      </div>
      <div class="input-hint">
        <el-icon :size="12"><InfoFilled /></el-icon>
        AI 回答仅供参考，不构成医疗诊断建议
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Promotion, Loading, InfoFilled } from '@element-plus/icons-vue'

const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ send: [content: string] }>()

const inputText = ref('')
const isFocused = ref(false)
const textareaRef = ref<HTMLTextAreaElement>()

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<style scoped>
.chat-input-wrap {
  padding: 12px 24px 20px;
  background: var(--bg-page);
  border-top: 1px solid var(--border-light);
}

.input-container {
  max-width: 860px;
  margin: 0 auto;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: var(--bg-card);
  border: 1.5px solid var(--border-medium);
  border-radius: 14px;
  padding: 12px 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-box.focused {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.input-box.disabled {
  background: var(--bg-hover);
  border-color: var(--border-light);
}

.input-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  background: transparent;
  font-family: inherit;
  min-height: 24px;
  max-height: 160px;
  overflow-y: auto;
}

.input-textarea::placeholder {
  color: var(--text-placeholder);
}

.input-textarea:disabled {
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.char-count {
  font-size: 11px;
  color: var(--text-placeholder);
  transition: color 0.2s;
}

.char-count.warn {
  color: #d97706;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: var(--border-light);
  color: var(--text-placeholder);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.35);
}

.send-btn.active:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.45);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-placeholder);
  margin-top: 8px;
  justify-content: center;
}
</style>
