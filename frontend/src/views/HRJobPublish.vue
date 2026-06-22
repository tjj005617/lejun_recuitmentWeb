<template>
  <AppShell :bgImage="false">
    <div class="hr-job-publish">
      <div class="hr-job-publish__container">
        <el-card class="publish-card">
          <template #header>
            <div class="card-header">
              <h2>{{ isEdit ? '编辑职位' : '发布职位' }}</h2>
              <el-button text @click="$router.back()">返回</el-button>
            </div>
          </template>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="publish-form">
            <el-form-item label="职位名称" prop="title">
              <el-input v-model="form.title" placeholder="如：Java开发工程师" maxlength="50" show-word-limit />
            </el-form-item>

            <el-form-item label="职位分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择职位分类" style="width: 100%">
                <el-option-group v-for="group in categories" :key="group.id" :label="group.name">
                  <el-option
                    v-for="item in group.children"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-option-group>
              </el-select>
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="工作城市" prop="city">
                  <el-select v-model="form.city" placeholder="请选择城市" style="width: 100%">
                    <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="工作类型" prop="jobType">
                  <el-select v-model="form.jobType" placeholder="请选择工作类型" style="width: 100%">
                    <el-option label="全职" value="full_time" />
                    <el-option label="兼职" value="part_time" />
                    <el-option label="实习" value="intern" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="经验要求">
                  <el-select v-model="form.experience" placeholder="请选择" style="width: 100%" clearable>
                    <el-option label="不限" value="不限" />
                    <el-option label="应届生" value="应届生" />
                    <el-option label="1-3年" value="1-3年" />
                    <el-option label="3-5年" value="3-5年" />
                    <el-option label="5-10年" value="5-10年" />
                    <el-option label="10年以上" value="10年以上" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="学历要求">
                  <el-select v-model="form.education" placeholder="请选择" style="width: 100%" clearable>
                    <el-option label="不限" value="不限" />
                    <el-option label="大专" value="大专" />
                    <el-option label="本科" value="本科" />
                    <el-option label="硕士" value="硕士" />
                    <el-option label="博士" value="博士" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="工作地点">
                  <el-input v-model="form.address" placeholder="详细地址" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="薪资下限（元/月）">
                  <el-input-number v-model="form.salaryMin" :min="0" :step="1000" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="薪资上限（元/月）">
                  <el-input-number v-model="form.salaryMax" :min="0" :step="1000" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="职位描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="6"
                placeholder="详细描述职位职责、工作内容等"
              />
            </el-form-item>

            <el-form-item label="任职要求">
              <el-input
                v-model="form.requirements"
                type="textarea"
                :rows="4"
                placeholder="描述任职要求、技能要求等"
              />
            </el-form-item>

            <el-form-item label="职位福利">
              <!-- 已选标签 -->
              <div class="benefits-selected" v-if="form.benefitsList.length > 0">
                <div class="selected-header">
                  <span class="selected-title">已添加 ({{ form.benefitsList.length }})</span>
                  <el-button text type="danger" size="small" @click="clearBenefits">清空全部</el-button>
                </div>
                <div class="selected-tags">
                  <el-tag
                    v-for="(benefit, index) in form.benefitsList"
                    :key="benefit"
                    closable
                    type="primary"
                    effect="dark"
                    @close="removeBenefit(index)"
                    class="benefit-tag"
                  >
                    {{ benefit }}
                  </el-tag>
                </div>
              </div>

              <!-- 常用福利快捷添加 -->
              <div class="benefits-presets" v-if="presetBenefits.length > 0">
                <div class="presets-title">常用福利（点击添加）</div>
                <div class="presets-tags">
                  <el-tag
                    v-for="item in presetBenefits"
                    :key="item"
                    @click="togglePreset(item)"
                    class="preset-tag"
                  >
                    {{ item }}
                  </el-tag>
                </div>
              </div>

              <!-- 自定义输入 -->
              <div class="benefits-custom">
                <div class="custom-title">自定义福利</div>
                <div class="custom-input-row">
                  <el-input
                    v-model="newBenefit"
                    placeholder="输入自定义福利，按回车或点击添加"
                    @keyup.enter="addBenefit"
                    class="custom-input"
                    clearable
                  />
                  <el-button @click="addBenefit" :disabled="!newBenefit.trim()">添加</el-button>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
                {{ isEdit ? '保存修改' : '发布职位' }}
              </el-button>
              <el-button size="large" @click="$router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppShell from '@/components/AppShell.vue'
import { createJob, updateJob, getJobDetail } from '@/api/job'
import { getCategoryTree } from '@/api/category'
import { getBenefitTags } from '@/api/benefitTag'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const loading = ref(false)

const form = ref({
  title: '',
  categoryId: null,
  city: '',
  jobType: 'full_time',
  experience: '',
  education: '',
  address: '',
  salaryMin: null,
  salaryMax: null,
  description: '',
  requirements: '',
  benefitsList: []
})

const rules = {
  title: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择职位分类', trigger: 'change' }],
  city: [{ required: true, message: '请选择工作城市', trigger: 'change' }],
  jobType: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入职位描述', trigger: 'blur' }]
}

const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京', '重庆', '西安']
const categories = ref([
  {
    id: 1, name: '技术类', children: [
      { id: 6, name: 'Java开发' },
      { id: 7, name: '前端开发' },
      { id: 8, name: 'Python开发' }
    ]
  },
  {
    id: 2, name: '产品类', children: [
      { id: 9, name: '产品经理' }
    ]
  },
  {
    id: 3, name: '设计类', children: [
      { id: 10, name: 'UI设计' }
    ]
  }
])

const newBenefit = ref('')
const presetBenefits = ref([])
const allBenefitTagNames = ref([]) // 全部标签名（用于区分自定义福利和已知标签）

// 加载全部岗位福利标签，计算出差集作为预设列表
const loadPresetBenefits = async () => {
  try {
    const res = await getBenefitTags('job')
    allBenefitTagNames.value = (res.data || []).map(t => t.name)
    // 已选的不显示在预设区
    presetBenefits.value = allBenefitTagNames.value.filter(name => !form.value.benefitsList.includes(name))
  } catch (e) {
    // 使用默认空列表
  }
}

// 添加福利标签
const addBenefit = () => {
  const val = newBenefit.value.trim()
  if (val && !form.value.benefitsList.includes(val)) {
    form.value.benefitsList.push(val)
    const pIdx = presetBenefits.value.indexOf(val)
    if (pIdx >= 0) presetBenefits.value.splice(pIdx, 1)
  }
  newBenefit.value = ''
}

// 删除福利标签（已知标签补回预设，自定义福利不补回）
const removeBenefit = (index) => {
  const removed = form.value.benefitsList.splice(index, 1)[0]
  if (removed && allBenefitTagNames.value.includes(removed)) {
    presetBenefits.value.push(removed)
  }
}

// 切换预设福利（点击添加/移除）
const togglePreset = (item) => {
  const idx = form.value.benefitsList.indexOf(item)
  if (idx >= 0) {
    form.value.benefitsList.splice(idx, 1)
    presetBenefits.value.push(item)
  } else {
    form.value.benefitsList.push(item)
    const pIdx = presetBenefits.value.indexOf(item)
    if (pIdx >= 0) presetBenefits.value.splice(pIdx, 1)
  }
}

// 清空所有福利（全部补回预设）
const clearBenefits = () => {
  const cleared = [...form.value.benefitsList]
  form.value.benefitsList = []
  cleared.forEach(name => {
    if (!presetBenefits.value.includes(name)) {
      presetBenefits.value.push(name)
    }
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const data = {
        ...form.value,
        benefits: form.value.benefitsList
      }
      delete data.benefitsList

      if (isEdit.value) {
        await updateJob(route.params.id, data)
        ElMessage.success('职位更新成功')
      } else {
        await createJob(data)
        ElMessage.success('职位发布成功')
      }
      router.push('/hr')
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  // 加载分类
  try {
    const res = await getCategoryTree()
    if (res?.data) {
      categories.value = res.data
    }
  } catch (e) {
    // 使用默认分类
  }

  // 编辑模式加载职位数据
  if (isEdit.value) {
    try {
      const res = await getJobDetail(route.params.id)
      if (res?.data) {
        const job = res.data
        form.value = {
          title: job.title,
          categoryId: job.categoryId,
          city: job.city,
          jobType: job.jobType,
          experience: job.experience,
          education: job.education,
          address: job.address,
          salaryMin: job.salaryMin,
          salaryMax: job.salaryMax,
          description: job.description,
          requirements: job.requirements,
          benefitsList: Array.isArray(job.benefits) ? job.benefits : []
        }
      }
    } catch (e) {
      ElMessage.error('加载职位信息失败')
    }
  }

  // 最后加载全部岗位福利标签，计算差集作为预设列表
  await loadPresetBenefits()
})
</script>

<style scoped>
.hr-job-publish {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
}

.hr-job-publish__container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.publish-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.benefits-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.benefits-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.benefits-selected {
  margin-bottom: 12px;
}

.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.selected-title {
  font-size: 13px;
  color: #666;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.benefits-presets {
  margin-bottom: 12px;
}

.presets-title, .custom-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.presets-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.preset-tag:hover {
  transform: translateY(-1px);
}

.preset-tag.is-used {
  opacity: 0.5;
}

.check-icon {
  margin-right: 2px;
}

.benefits-custom {
  margin-top: 4px;
}

.custom-input-row {
  display: flex;
  gap: 8px;
}

.custom-input {
  flex: 1;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .hr-job-publish__container {
    padding: 16px;
  }
}
</style>
