<template>
  <AppShell :bgImage="false">
    <div class="company-edit">
      <div class="page-header">
        <h1>{{ hasCompany ? '编辑公司信息' : '创建公司信息' }}</h1>
      </div>

      <el-card class="form-card">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="公司名称" prop="name">
            <!-- 已选公司显示 -->
            <div v-if="selectedCompanyId && !isNewCompany" class="company-selected">
              <span class="company-selected-name">{{ form.name }}</span>
              <span class="company-selected-industry">{{ form.industry || '-' }}</span>
              <el-button text type="primary" size="small" @click="clearCompanySelection">更换</el-button>
            </div>
            <!-- 公司下拉选择器 -->
            <div v-else class="company-dropdown-wrapper" ref="dropdownWrapperRef">
              <div class="company-dropdown-trigger" @click="toggleDropdown">
                <span class="trigger-placeholder">{{ isNewCompany ? '新建公司：' + form.name : '请选择公司' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <div v-show="dropdownVisible" class="company-dropdown-panel">
                <!-- 搜索框 -->
                <div class="dropdown-search">
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索公司名称..."
                    clearable
                    :prefix-icon="Search"
                    @input="handleSearchInput"
                    @clear="handleSearchClear"
                  />
                </div>
                <!-- 公司列表（无限滚动） -->
                <div class="dropdown-list" ref="listRef" @scroll="handleScroll">
                  <div
                    v-for="item in companyOptions"
                    :key="item.id"
                    class="dropdown-item"
                    @click="handleCompanySelect(item)"
                  >
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-industry">{{ item.industry || '-' }}</span>
                  </div>
                  <div v-if="listLoading" class="dropdown-loading">
                    <el-icon class="is-loading"><Loading /></el-icon> 加载中...
                  </div>
                  <div v-if="!listLoading && companyOptions.length === 0" class="dropdown-empty">
                    暂无公司数据
                  </div>
                </div>
                <!-- 新建公司按钮 -->
                <div class="dropdown-add" @click="handleAddCompany">
                  <el-icon><Plus /></el-icon>
                  <span>新建公司</span>
                </div>
              </div>
            </div>
            <div v-if="isNewCompany" class="new-company-tip">将创建新公司</div>
          </el-form-item>

          <el-form-item label="所属行业" prop="industry">
            <el-select v-model="form.industry" placeholder="请选择所属行业" style="width: 100%">
              <el-option label="互联网/IT" value="互联网/IT" />
              <el-option label="金融" value="金融" />
              <el-option label="教育" value="教育" />
              <el-option label="医疗健康" value="医疗健康" />
              <el-option label="电子商务" value="电子商务" />
              <el-option label="游戏" value="游戏" />
              <el-option label="人工智能" value="人工智能" />
              <el-option label="大数据" value="大数据" />
              <el-option label="物联网" value="物联网" />
              <el-option label="区块链" value="区块链" />
              <el-option label="半导体/芯片" value="半导体/芯片" />
              <el-option label="新能源" value="新能源" />
              <el-option label="汽车" value="汽车" />
              <el-option label="房地产/建筑" value="房地产/建筑" />
              <el-option label="制造业" value="制造业" />
              <el-option label="零售/消费" value="零售/消费" />
              <el-option label="物流/仓储" value="物流/仓储" />
              <el-option label="传媒/娱乐" value="传媒/娱乐" />
              <el-option label="咨询/法律" value="咨询/法律" />
              <el-option label="政府/非营利" value="政府/非营利" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="公司规模" prop="scale">
            <el-select v-model="form.scale" placeholder="请选择公司规模" clearable style="width: 100%">
              <el-option label="0-20人" value="0-20人" />
              <el-option label="20-99人" value="20-99人" />
              <el-option label="100-499人" value="100-499人" />
              <el-option label="500-999人" value="500-999人" />
              <el-option label="1000-9999人" value="1000-9999人" />
              <el-option label="10000人以上" value="10000人以上" />
            </el-select>
          </el-form-item>

          <el-form-item label="公司类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择公司类型" clearable style="width: 100%">
              <el-option label="民营" value="民营" />
              <el-option label="国企" value="国企" />
              <el-option label="外企" value="外企" />
              <el-option label="合资" value="合资" />
              <el-option label="事业单位" value="事业单位" />
              <el-option label="创业公司" value="创业公司" />
            </el-select>
          </el-form-item>

          <el-form-item label="所在地区" prop="region">
            <el-cascader
              v-model="regionValue"
              :options="regionOptions"
              :props="{ value: 'value', label: 'label', children: 'children' }"
              placeholder="请选择省/市/区"
              style="width: 100%"
              @change="handleRegionChange"
            />
          </el-form-item>

          <el-form-item label="详细地址" prop="address">
            <el-input v-model="form.address" placeholder="请输入详细地址" />
          </el-form-item>

          <el-form-item label="公司官网" prop="website">
            <el-input v-model="form.website" placeholder="如：https://www.example.com" />
          </el-form-item>

          <el-form-item label="公司介绍" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入公司介绍" />
          </el-form-item>

          <el-form-item label="福利待遇" prop="benefits">
            <div class="benefits-wrapper">
              <!-- 已添加的福利标签 -->
              <div class="benefits-selected" v-if="benefitsList.length > 0">
                <div class="selected-header">
                  <span class="selected-title">已添加 ({{ benefitsList.length }})</span>
                  <el-button text type="danger" size="small" @click="clearBenefits">清空全部</el-button>
                </div>
                <div class="selected-tags">
                  <el-tag
                    v-for="(benefit, index) in benefitsList"
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
                  <el-button type="primary" @click="addBenefit" :disabled="!newBenefit.trim()">添加</el-button>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              {{ hasCompany ? '保存修改' : '创建公司' }}
            </el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, ArrowDown, Loading } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import { useUserStore } from '@/stores/user'
import { getCompanyDetail, saveCompany, listCompanies } from '@/api/company'
import { getRegionTree, getRegionPath } from '@/api/region'
import { getBenefitTags } from '@/api/benefitTag'

const userStore = useUserStore()
const { user, loadUser } = userStore

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const hasCompany = ref(false)
const companyId = ref(null)

// 公司下拉选择相关
const selectedCompanyId = ref(null)
const companyOptions = ref([])
const searchKeyword = ref('')
const isNewCompany = ref(false)
const dropdownVisible = ref(false)
const listLoading = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 20
let searchTimer = null

const dropdownWrapperRef = ref(null)
const listRef = ref(null)

const form = ref({
  name: '',
  industry: '',
  scale: '',
  type: '',
  regionId: null,
  address: '',
  website: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入或选择公司名称', trigger: 'change' }]
}

const benefitsList = ref([])
const newBenefit = ref('')
const presetBenefits = ref([])

// 省市区级联
const regionValue = ref([])
const regionOptions = ref([])

// 省市区ID到名称的映射
const regionMap = ref({})

// 处理省市区选择（value 为 [省ID, 市ID, 区ID]）
const handleRegionChange = (value) => {
  if (value && value.length > 0) {
    // 存储区级ID（level=3）到 regionId
    form.value.regionId = value[value.length - 1]
  } else {
    form.value.regionId = null
  }
}

// 加载省市区数据
const loadRegions = async () => {
  try {
    const res = await getRegionTree()
    regionOptions.value = res.data || []
  } catch (e) {
    console.error('加载省市区失败', e)
  }
}

// 加载全部公司福利标签，计算出公司尚未使用的福利作为预设列表
const allBenefitTagNames = ref([]) // 全部标签名（用于区分自定义福利和已知标签）
const loadPresetBenefits = async () => {
  try {
    const res = await getBenefitTags('company')
    allBenefitTagNames.value = (res.data || []).map(t => t.name)
    // 已选的不显示在预设区
    presetBenefits.value = allBenefitTagNames.value.filter(name => !benefitsList.value.includes(name))
  } catch (e) {
    console.error('加载福利标签失败', e)
  }
}

// 添加福利标签
const addBenefit = () => {
  const val = newBenefit.value.trim()
  if (val && !benefitsList.value.includes(val)) {
    benefitsList.value.push(val)
    // 从预设中移除（如果存在）
    const pIdx = presetBenefits.value.indexOf(val)
    if (pIdx >= 0) presetBenefits.value.splice(pIdx, 1)
  }
  newBenefit.value = ''
}

// 删除福利标签（从已选中移除，已知标签补回预设，自定义福利不补回）
const removeBenefit = (index) => {
  const removed = benefitsList.value.splice(index, 1)[0]
  if (removed && allBenefitTagNames.value.includes(removed)) {
    presetBenefits.value.push(removed)
  }
}

// 切换预设福利（点击添加/移除）
const togglePreset = (item) => {
  const idx = benefitsList.value.indexOf(item)
  if (idx >= 0) {
    benefitsList.value.splice(idx, 1)
    presetBenefits.value.push(item)
  } else {
    benefitsList.value.push(item)
    const pIdx = presetBenefits.value.indexOf(item)
    if (pIdx >= 0) presetBenefits.value.splice(pIdx, 1)
  }
}

// 清空所有福利（全部补回预设）
const clearBenefits = () => {
  const cleared = [...benefitsList.value]
  benefitsList.value = []
  cleared.forEach(name => {
    if (!presetBenefits.value.includes(name)) {
      presetBenefits.value.push(name)
    }
  })
}

// 切换下拉框显示/隐藏
const toggleDropdown = () => {
  if (dropdownVisible.value) {
    closeDropdown()
  } else {
    dropdownVisible.value = true
    // 打开时加载第一页
    currentPage.value = 1
    hasMore.value = true
    companyOptions.value = []
    loadCompanyPage(1)
  }
}

// 加载公司分页数据
const loadCompanyPage = async (page) => {
  if (listLoading.value) return
  listLoading.value = true
  try {
    const params = { page, size: pageSize }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    const res = await listCompanies(params)
    const list = res.data || []
    if (page === 1) {
      companyOptions.value = list
    } else {
      companyOptions.value = [...companyOptions.value, ...list]
    }
    hasMore.value = list.length >= pageSize
    currentPage.value = page
  } catch (e) {
    console.error('加载公司列表失败', e)
  } finally {
    listLoading.value = false
  }
}

// 搜索输入（防抖300ms）
const handleSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    hasMore.value = true
    loadCompanyPage(1)
  }, 300)
}

// 清空搜索
const handleSearchClear = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  hasMore.value = true
  loadCompanyPage(1)
}

// 无限滚动检测
const handleScroll = () => {
  if (!listRef.value || listLoading.value || !hasMore.value) return
  const { scrollTop, scrollHeight, clientHeight } = listRef.value
  if (scrollTop + clientHeight >= scrollHeight - 10) {
    loadCompanyPage(currentPage.value + 1)
  }
}

// 选择已有公司 → 自动填充所有字段
const handleCompanySelect = async (item) => {
  isNewCompany.value = false
  selectedCompanyId.value = item.id
  closeDropdown()

  form.value.name = item.name || ''
  form.value.industry = item.industry || ''
  form.value.scale = item.scale || ''
  form.value.type = item.type || ''
  form.value.regionId = item.regionId || null
  form.value.address = item.address || ''
  form.value.website = item.website || ''
  form.value.description = item.description || ''
  // 福利列表（后端直接返回数组）
  benefitsList.value = Array.isArray(item.benefits) ? item.benefits : []
  // 根据 regionId 反查省市区级联路径
  if (item.regionId) {
    try {
      const pathRes = await getRegionPath(item.regionId)
      if (pathRes.data) regionValue.value = pathRes.data
    } catch (e) {
      regionValue.value = []
    }
  } else {
    regionValue.value = []
  }
}

// 新建公司
const handleAddCompany = () => {
  isNewCompany.value = true
  selectedCompanyId.value = null
  form.value.name = searchKeyword.value || ''
  form.value.industry = ''
  form.value.scale = ''
  form.value.type = ''
  form.value.regionId = null
  form.value.address = ''
  form.value.website = ''
  form.value.description = ''
  benefitsList.value = []
  regionValue.value = []
  closeDropdown()
}

// 更换公司选择
const clearCompanySelection = () => {
  selectedCompanyId.value = null
  isNewCompany.value = false
  form.value.name = ''
}

// 关闭下拉框
const closeDropdown = () => {
  dropdownVisible.value = false
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (dropdownWrapperRef.value && !dropdownWrapperRef.value.contains(e.target)) {
    closeDropdown()
  }
}

// 加载公司信息
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  // 第一步：并行加载省市区 + 公司详情
  await loadRegions()
  const userCompanyId = user.value?.companyId
  if (userCompanyId) {
    try {
      const res = await getCompanyDetail(userCompanyId)
      if (res.data) {
        hasCompany.value = true
        companyId.value = res.data.id
        selectedCompanyId.value = res.data.id
        form.value = {
          name: res.data.name || '',
          industry: res.data.industry || '',
          scale: res.data.scale || '',
          type: res.data.type || '',
          regionId: res.data.regionId || null,
          address: res.data.address || '',
          website: res.data.website || '',
          description: res.data.description || ''
        }
        // 福利列表（后端直接返回数组）
        benefitsList.value = Array.isArray(res.data.benefits) ? res.data.benefits : []
        // 根据 regionId 反查省市区级联路径
        if (res.data.regionId) {
          try {
            const pathRes = await getRegionPath(res.data.regionId)
            if (pathRes.data) regionValue.value = pathRes.data
          } catch (e) {
            // ignore
          }
        }
      }
    } catch (e) {
      // 公司信息不存在，等待创建
    }
  }
  // 第二步：加载全部公司福利标签，计算差集作为预设列表
  await loadPresetBenefits()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  clearTimeout(searchTimer)
})

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    let payload = { ...form.value, benefits: benefitsList.value }
    // 编辑已有公司时带上companyId
    if (selectedCompanyId.value && !isNewCompany.value) {
      payload.companyId = selectedCompanyId.value
    }
    const res = await saveCompany(payload)
    // 更新 user store 中的 companyId，确保回显正确
    if (res.data?.id) {
      user.value.companyId = res.data.id
    }
    ElMessage.success(hasCompany.value ? '公司信息已更新' : '公司创建成功')
    router.push('/hr')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.company-edit {
  width: 70%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 24px 0;
}

.form-card {
  padding: 24px;
}

.benefits-wrapper {
  width: 100%;
}

/* 已添加的福利 */
.benefits-selected {
  margin-bottom: 16px;
}

.selected-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.selected-title {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.benefit-tag {
  font-size: 14px;
  cursor: default;
}

/* 常用福利预设 */
.benefits-presets {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.presets-title,
.custom-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.presets-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-tag {
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.preset-tag:hover:not(.is-used) {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.preset-tag.is-used {
  cursor: pointer;
  opacity: 0.6;
}

.check-icon {
  margin-right: 2px;
  font-size: 12px;
}

/* 自定义输入 */
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

/* 公司下拉选择器 */
.company-dropdown-wrapper {
  width: 100%;
  position: relative;
}

.company-dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s;
}

.company-dropdown-trigger:hover {
  border-color: #409eff;
}

.trigger-placeholder {
  color: #a8abb2;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.company-dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  width: 100%;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  overflow: hidden;
}

.dropdown-search {
  padding: 8px;
  border-bottom: 1px solid #ebeef5;
}

.dropdown-list {
  max-height: 320px;
  overflow-y: auto;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.dropdown-item:hover {
  background: #f5f7fa;
}

.item-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-industry {
  font-size: 12px;
  color: #8492a6;
  margin-left: 12px;
  flex-shrink: 0;
}

.dropdown-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #909399;
  font-size: 13px;
  gap: 4px;
}

.dropdown-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.dropdown-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  border-top: 1px solid #ebeef5;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.dropdown-add:hover {
  background: #ecf5ff;
}

/* 已选公司显示 */
.company-selected {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
}

.company-selected-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.company-selected-industry {
  font-size: 12px;
  color: #8492a6;
}

.new-company-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}
</style>
