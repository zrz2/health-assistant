<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-title">
        <h2>知识库管理</h2>
        <p>管理医疗健康知识条目</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索标题/内容"
          style="width: 240px;"
          :prefix-icon="Search"
          clearable
          @change="fetchData"
        />
        <el-button type="primary" @click="showDialog()">
          <el-icon><Plus /></el-icon>
          添加知识
        </el-button>
        <el-upload accept=".json,.csv" :show-file-list="false" :http-request="handleImport">
          <el-button>
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
        </el-upload>
      </div>
    </div>

    <div class="table-card">
      <div v-if="selected.length" class="batch-bar">
        <span>已选 {{ selected.length }} 条</span>
        <el-button type="danger" size="small" plain @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>

      <el-table :data="items" v-loading="loading" stripe @selection-change="onSelectionChange" class="data-table">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sourceName" label="来源" width="140" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" round>{{ sourceTypeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="证据等级" width="110">
          <template #default="{ row }">
            <el-tag :type="evidenceTag(row.evidenceLevel)" size="small" round>
              Level {{ row.evidenceLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- Dialog -->
    <el-dialog
      :model-value="dialogVisible"
      :title="editId ? '编辑知识' : '添加知识'"
      width="580px"
      @close="dialogVisible = false"
      class="knowledge-dialog"
    >
      <el-form ref="formRef" :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="知识标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="知识内容" />
        </el-form-item>
        <el-form-item label="来源名称">
          <el-input v-model="form.sourceName" placeholder="例如：世界卫生组织" />
        </el-form-item>
        <el-form-item label="来源URL">
          <el-input v-model="form.sourceUrl" placeholder="https://..." />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="form.sourceType" style="width: 100%">
                <el-option label="权威组织" :value="1" />
                <el-option label="医疗机构" :value="2" />
                <el-option label="学术期刊" :value="3" />
                <el-option label="科普" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="例如：呼吸科" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Upload, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKnowledgeItems,
  createKnowledgeItem,
  updateKnowledgeItem,
  deleteKnowledgeItem,
  batchDeleteKnowledgeItems,
  importBatchKnowledge,
  type KnowledgeItem,
} from '@/api/admin'

const items = ref<KnowledgeItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const selected = ref<KnowledgeItem[]>([])
const dialogVisible = ref(false)
const editId = ref<string | null>(null)
const saving = ref(false)

const form = reactive({
  title: '',
  content: '',
  sourceName: '',
  sourceUrl: '',
  sourceType: 1,
  category: '',
})

function sourceTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '权威组织', 2: '医疗机构', 3: '学术期刊', 4: '科普' }
  return map[type] || ''
}

function evidenceTag(level: number) {
  if (level <= 2) return 'success'
  if (level === 3) return 'warning'
  return 'info'
}

function onSelectionChange(val: KnowledgeItem[]) {
  selected.value = val
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getKnowledgeItems({ page: page.value - 1, size: size.value, keyword: keyword.value })
    if (res.data) {
      items.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

function showDialog(item?: KnowledgeItem) {
  if (item) {
    editId.value = item.docId
    Object.assign(form, item)
  } else {
    editId.value = null
    Object.assign(form, { title: '', content: '', sourceName: '', sourceUrl: '', sourceType: 1, category: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editId.value) {
      await updateKnowledgeItem(editId.value, form)
    } else {
      await createKnowledgeItem(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ } finally {
    saving.value = false
  }
}

async function handleDelete(item: KnowledgeItem) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」？`, '确认删除', { type: 'warning' })
    await deleteKnowledgeItem(item.docId)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* ignore */ }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条知识？`, '确认删除', { type: 'warning' })
    await batchDeleteKnowledgeItems(selected.value.map((i) => i.docId))
    ElMessage.success('删除成功')
    selected.value = []
    fetchData()
  } catch { /* ignore */ }
}

async function handleImport(options: any) {
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    await importBatchKnowledge(formData)
    ElMessage.success('导入成功')
    fetchData()
  } catch {
    ElMessage.error('导入失败')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.admin-page {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.page-title p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #eff6ff;
  border-bottom: 1px solid var(--primary-border);
  font-size: 13px;
  color: var(--primary);
  font-weight: 500;
}

.data-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: #f8fafc;
  --el-table-header-text-color: var(--text-secondary);
  --el-table-row-hover-bg-color: var(--bg-hover);
}

.data-table :deep(th) {
  font-weight: 600;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--border-light);
}

.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-textarea__inner) {
  border-radius: 8px !important;
}
</style>
