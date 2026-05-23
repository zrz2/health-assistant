<template>
  <div class="admin-page">
    <h3>系统配置</h3>

    <el-form label-width="140px" v-loading="loading">
      <el-divider content-position="left">AI 配置</el-divider>
      <el-form-item v-for="cfg in configsByCategory('ai')" :key="cfg.id" :label="cfg.description || cfg.configKey">
        <el-input v-model="cfg.configValue" />
      </el-form-item>

      <el-divider content-position="left">对话配置</el-divider>
      <el-form-item v-for="cfg in configsByCategory('chat')" :key="cfg.id" :label="cfg.description || cfg.configKey">
        <el-input v-model="cfg.configValue" />
      </el-form-item>

      <el-divider content-position="left">证据等级权重</el-divider>
      <el-form-item v-for="cfg in configsByCategory('evidence')" :key="cfg.id" :label="cfg.description || cfg.configKey">
        <el-input v-model="cfg.configValue" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfigs, updateSystemConfig, type SystemConfig } from '@/api/admin'

const configs = ref<SystemConfig[]>([])
const loading = ref(false)
const saving = ref(false)

function configsByCategory(category: string) {
  return configs.value.filter((c) => c.category === category)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    if (res.data) configs.value = res.data
  } catch { /* ignore */ } finally {
    loading.value = false
  }
})

async function handleSave() {
  saving.value = true
  try {
    for (const cfg of configs.value) {
      await updateSystemConfig(cfg)
    }
    ElMessage.success('配置保存成功')
  } catch { /* ignore */ } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.admin-page h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 24px;
}
</style>
