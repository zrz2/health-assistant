<template>
  <div class="page-container">
    <div class="page-card">
      <h3>健康档案</h3>
      <el-form
        ref="formRef"
        :model="form"
        label-width="100px"
        :disabled="!editing"
      >
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="0" :max="150" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="请选择">
                <el-option label="A型" value="A" />
                <el-option label="B型" value="B" />
                <el-option label="AB型" value="AB" />
                <el-option label="O型" value="O" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="0" :max="300" :precision="1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="0" :max="500" :precision="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">健康状况</el-divider>
        <el-form-item label="既往病史">
          <el-input v-model="form.medicalHistory" type="textarea" :rows="3" placeholder="请输入既往病史" />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergies" type="textarea" :rows="2" placeholder="药物、食物等过敏史" />
        </el-form-item>
        <el-form-item label="慢性病">
          <el-input v-model="form.chronicDiseases" type="textarea" :rows="2" placeholder="高血压、糖尿病等" />
        </el-form-item>
        <el-form-item label="当前用药">
          <el-input v-model="form.currentMedications" type="textarea" :rows="2" placeholder="当前正在服用的药物" />
        </el-form-item>

        <el-divider content-position="left">生活方式</el-divider>
        <el-form-item label="吸烟">
          <el-radio-group v-model="lifestyle.smoking">
            <el-radio value="none">不吸烟</el-radio>
            <el-radio value="occasional">偶尔</el-radio>
            <el-radio value="regular">经常</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="饮酒">
          <el-radio-group v-model="lifestyle.alcohol">
            <el-radio value="none">不饮酒</el-radio>
            <el-radio value="occasional">偶尔</el-radio>
            <el-radio value="regular">经常</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="运动频率">
          <el-select v-model="lifestyle.exercise" placeholder="请选择">
            <el-option label="几乎不运动" value="none" />
            <el-option label="每周1-2次" value="light" />
            <el-option label="每周3-4次" value="moderate" />
            <el-option label="每周5次以上" value="heavy" />
          </el-select>
        </el-form-item>
        <el-form-item label="饮食习惯">
          <el-input v-model="lifestyle.diet" type="textarea" :rows="2" placeholder="例如：清淡、偏辣、素食等" />
        </el-form-item>

        <el-form-item>
          <el-button v-if="!editing" type="primary" @click="startEdit">编辑</el-button>
          <template v-else>
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </template>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHealthRecord, updateHealthRecord } from '@/api/user'

const editing = ref(false)
const saving = ref(false)
const form = reactive({
  age: 0,
  gender: 1,
  height: 0,
  weight: 0,
  bloodType: '',
  medicalHistory: '',
  allergies: '',
  chronicDiseases: '',
  currentMedications: '',
})

const lifestyle = reactive<Record<string, any>>({
  smoking: 'none',
  alcohol: 'none',
  exercise: 'none',
  diet: '',
})

const originalForm = ref('')

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
    Object.assign(form, data)
    Object.assign(lifestyle, data.lifestyle)
  }
  editing.value = false
}

async function handleSave() {
  saving.value = true
  try {
    await updateHealthRecord({ ...form, lifestyle: JSON.stringify(lifestyle) } as any)
    ElMessage.success('保存成功')
    editing.value = false
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function parseLifestyle(data: any) {
  if (!data.lifestyle) return
  if (typeof data.lifestyle === 'string') {
    try { data.lifestyle = JSON.parse(data.lifestyle) } catch { return }
  }
}

onMounted(async () => {
  try {
    const res = await getHealthRecord()
    if (res.data) {
      parseLifestyle(res.data)
      Object.assign(form, res.data)
      if (res.data.lifestyle && typeof res.data.lifestyle === 'object') {
        Object.assign(lifestyle, res.data.lifestyle)
      }
    }
  } catch {
    // empty record is ok
  }
})
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.page-card {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.page-card h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 24px;
}
</style>
