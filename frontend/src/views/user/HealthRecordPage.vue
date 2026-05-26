<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>健康档案</h2>
        <p>完善您的健康信息，获取更精准的个性化建议</p>
      </div>
      <div class="header-actions">
        <el-button v-if="!editing" type="primary" @click="startEdit">
          <el-icon><Edit /></el-icon>
          编辑档案
        </el-button>
        <template v-else>
          <el-button type="primary" :loading="saving" @click="handleSave">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </template>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      label-position="top"
      :disabled="!editing"
      class="health-form"
    >
      <div class="form-section">
        <div class="section-title">
          <el-icon :size="16" color="#2563eb"><User /></el-icon>
          基本信息
        </div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择" style="width: 100%">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="请选择" style="width: 100%">
                <el-option label="A型" value="A" />
                <el-option label="B型" value="B" />
                <el-option label="AB型" value="AB" />
                <el-option label="O型" value="O" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="身高 (cm)">
              <el-input-number v-model="form.height" :min="0" :max="300" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重 (kg)">
              <el-input-number v-model="form.weight" :min="0" :max="500" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="BMI">
              <div class="bmi-display" :class="bmiClass">
                {{ bmiValue }} <span class="bmi-label">{{ bmiLabel }}</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <el-icon :size="16" color="#dc2626"><FirstAidKit /></el-icon>
          健康状况
        </div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="既往病史">
              <el-input v-model="form.medicalHistory" type="textarea" :rows="3" placeholder="请输入既往病史" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过敏史">
              <el-input v-model="form.allergies" type="textarea" :rows="3" placeholder="药物、食物等过敏史" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="慢性病">
              <el-input v-model="form.chronicDiseases" type="textarea" :rows="3" placeholder="高血压、糖尿病等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前用药">
              <el-input v-model="form.currentMedications" type="textarea" :rows="3" placeholder="当前正在服用的药物" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-title">
          <el-icon :size="16" color="#16a34a"><Sunny /></el-icon>
          生活方式
        </div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="吸烟">
              <el-radio-group v-model="lifestyle.smoking">
                <el-radio value="none">不吸烟</el-radio>
                <el-radio value="occasional">偶尔</el-radio>
                <el-radio value="regular">经常</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="饮酒">
              <el-radio-group v-model="lifestyle.alcohol">
                <el-radio value="none">不饮酒</el-radio>
                <el-radio value="occasional">偶尔</el-radio>
                <el-radio value="regular">经常</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运动频率">
              <el-select v-model="lifestyle.exercise" placeholder="请选择" style="width: 100%">
                <el-option label="几乎不运动" value="none" />
                <el-option label="每周1-2次" value="light" />
                <el-option label="每周3-4次" value="moderate" />
                <el-option label="每周5次以上" value="heavy" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="饮食习惯">
          <el-input v-model="lifestyle.diet" type="textarea" :rows="2" placeholder="例如：清淡、偏辣、素食等" />
        </el-form-item>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Check, User, FirstAidKit, Sunny } from '@element-plus/icons-vue'
import { getHealthRecord, updateHealthRecord } from '@/api/user'

const editing = ref(false)
const saving = ref(false)

const defaultForm = () => ({
  age: 0 as number | undefined,
  gender: undefined as number | undefined,
  height: undefined as number | undefined,
  weight: undefined as number | undefined,
  bloodType: '' as string,
  medicalHistory: '' as string,
  allergies: '' as string,
  chronicDiseases: '' as string,
  currentMedications: '' as string,
})

const form = reactive(defaultForm())

const lifestyle = reactive<Record<string, any>>({
  smoking: 'none',
  alcohol: 'none',
  exercise: 'none',
  diet: '',
})

const originalForm = ref('')

const bmiValue = computed(() => {
  if (!form.height || !form.weight) return '--'
  const h = form.height / 100
  return (form.weight / (h * h)).toFixed(1)
})

const bmiClass = computed(() => {
  const v = parseFloat(bmiValue.value)
  if (isNaN(v)) return ''
  if (v < 18.5) return 'bmi-low'
  if (v < 24) return 'bmi-normal'
  if (v < 28) return 'bmi-high'
  return 'bmi-obese'
})

const bmiLabel = computed(() => {
  const v = parseFloat(bmiValue.value)
  if (isNaN(v)) return ''
  if (v < 18.5) return '偏瘦'
  if (v < 24) return '正常'
  if (v < 28) return '偏重'
  return '肥胖'
})

function saveOriginal() {
  originalForm.value = JSON.stringify({ ...form, lifestyle: { ...lifestyle } })
}

function startEdit() {
  saveOriginal()
  editing.value = true
}

function cancelEdit() {
  if (originalForm.value) {
    const data = JSON.parse(originalForm.value)
    Object.assign(form, defaultForm(), data)
    if (data.lifestyle) {
      Object.assign(lifestyle, { smoking: 'none', alcohol: 'none', exercise: 'none', diet: '' }, data.lifestyle)
    }
  }
  editing.value = false
}

async function handleSave() {
  saving.value = true
  try {
    const body: Record<string, any> = {}
    for (const key of Object.keys(form) as (keyof typeof form)[]) {
      if (form[key] !== undefined && form[key] !== '') {
        body[key] = form[key]
      }
    }
    body.lifestyle = JSON.stringify(lifestyle)
    await updateHealthRecord(body as any)
    ElMessage.success('保存成功')
    editing.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getHealthRecord()
    const record = res.data
    if (record) {
      let lifestyleData = null
      if (record.lifestyle) {
        try {
          lifestyleData = typeof record.lifestyle === 'string'
            ? JSON.parse(record.lifestyle)
            : record.lifestyle
        } catch { /* ignore */ }
      }
      const { lifestyle: _, ...formFields } = record
      Object.assign(form, formFields)
      if (lifestyleData) {
        Object.assign(lifestyle, lifestyleData)
      }
    }
  } catch {
    // 无记录时显示空表单
  }
})
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 28px;
  background: var(--bg-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.page-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.health-form {
  max-width: 960px;
  margin: 0 auto;
}

.form-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.health-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 13px;
}

.health-form :deep(.el-input__wrapper),
.health-form :deep(.el-textarea__inner) {
  border-radius: 8px !important;
}

.bmi-display {
  height: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-muted);
  padding: 0 4px;
}

.bmi-label {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 100px;
}

.bmi-normal { color: #16a34a; }
.bmi-normal .bmi-label { background: #f0fdf4; color: #16a34a; }

.bmi-low { color: #2563eb; }
.bmi-low .bmi-label { background: #eff6ff; color: #2563eb; }

.bmi-high { color: #d97706; }
.bmi-high .bmi-label { background: #fffbeb; color: #d97706; }

.bmi-obese { color: #dc2626; }
.bmi-obese .bmi-label { background: #fef2f2; color: #dc2626; }
</style>
