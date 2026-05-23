<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>知识库管理</h3>
      <div class="header-actions">
        <el-input v-model="keyword" placeholder="搜索标题/内容" style="width: 240px;" :prefix-icon="Search" clearable @change="fetchData" />
        <el-button type="primary" @click="showDialog()">添加知识</el-button>
        <el-upload
          accept=".json,.csv"
          :show-file-list="false"
          :http-request="handleImport"
        >
          <el-button>批量导入</el-button>
        </el-upload>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sourceName" label="来源" width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ sourceTypeLabel(row.sourceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="证据等级" width="100">
        <template #default="{ row }">
          <el-tag :type="evidenceTag(row.evidenceLevel)" size="small">
            Level {{ row.evidenceLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="showDialog(row)">编辑</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="selected.length">
      <el-button type="danger" plain @click="handleBatchDelete">批量删除 ({{ selected.length }})</el-button>
    </div>
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <!-- Dialog -->
    <el-dialog
      :model-value="dialogVisible"
      :title="editItem ? '编辑知识' : '添加知识'"
      width="600px"
      @close="dialogVisible = false"
    >
      <el-form ref="formRef" :model="form" label-width="90px">
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
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType">
            <el-option label="权威组织" :value="1" />
            <el-option label="医疗机构" :value="2" />
            <el-option label="学术期刊" :value="3" />
            <el-option label="科普" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例如：呼吸科" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
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
    const res = await getKnowledgeItems({ page: page.value, size: size.value, keyword: keyword.value })
    if (res.data) {
      items.value = res.data?.content || []
      total.value = res.data?.totalElements || 0
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
.admin-page h3 {
  font-size: 20px;
  color: #303133;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  gap: 8px;
}
</style>
