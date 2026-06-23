<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><Collection /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalDocuments }}</div>
          <div class="stat-label">总文档数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ completedCount }}</div>
          <div class="stat-label">处理完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><WarningFilled /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ failedCount }}</div>
          <div class="stat-label">处理失败</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><DataAnalysis /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalVertices }}</div>
          <div class="stat-label">知识点总数</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterCategory" placeholder="选择分类" clearable style="width: 160px" @change="loadDocuments">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width: 130px" @change="loadDocuments">
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="processing" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-button type="primary" @click="loadDocuments">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="uploadVisible = true">
          <el-icon><Upload /></el-icon> 上传文档
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table
        :data="documentList"
        stripe
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" round>{{ row.categoryName || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.parseStatus)" size="small" effect="light" round>
              {{ statusLabel(row.parseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="vertexCount" label="知识点" width="80" align="center" sortable />
        <el-table-column prop="edgeCount" label="关系数" width="80" align="center" sortable />
        <el-table-column label="创建时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
            <span v-else class="empty-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="viewDetail(row)">
                <el-icon><View /></el-icon> 详情
              </el-button>
              <el-button
                v-if="row.parseStatus === 'failed'"
                link type="warning" size="small"
                @click="handleRetry(row)"
              >
                <el-icon><RefreshRight /></el-icon> 重试
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <span class="page-total">共 {{ totalDocs }} 条</span>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :total="totalDocs"
          :page-sizes="[10, 20, 50]"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          @current-change="loadDocuments"
          @size-change="loadDocuments"
        />
      </div>
    </div>

    <!-- 上传文档弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传八股文档" width="520px" :close-on-click-modal="!processing" :close-on-press-escape="!processing" destroy-on-close>
      <!-- 上传表单 -->
      <el-form v-if="!processing" :model="uploadForm" ref="uploadFormRef" label-width="80px">
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" placeholder="可选，默认使用文件名" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-input v-model="uploadForm.categoryName" placeholder="如：Java、前端、数据库（不填则 AI 自动分类）" />
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".md,.txt,.markdown"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <el-icon :size="40" style="color: #94a3b8;"><UploadFilled /></el-icon>
            <div style="margin-top: 8px; color: #64748b; font-size: 13px;">
              拖拽文件到此处，或<em style="color: #10b981;">点击上传</em>
            </div>
            <div style="margin-top: 4px; color: #94a3b8; font-size: 12px;">
              支持 .md / .txt 格式，AI 将自动提取知识点和关系
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <!-- 处理进度 -->
      <div v-else class="processing-view">
        <div class="processing-title">
          <el-icon class="is-loading" :size="20" style="color: #3b82f6;"><Loading /></el-icon>
          <span>AI 正在处理文档...</span>
        </div>
        <el-progress
          :percentage="processingPercent"
          :status="processingStatus"
          :stroke-width="20"
          text-inside
          style="margin: 24px 0 16px"
        />
        <div class="processing-step">{{ processingStepLabel }}</div>
        <div v-if="processingResult === 'failed'" class="processing-error">
          处理失败: {{ processingErrorMsg }}
        </div>
      </div>
      <template #footer>
        <template v-if="!processing">
          <el-button @click="uploadVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="handleUpload">
            {{ uploading ? '上传中...' : '上传并处理' }}
          </el-button>
        </template>
        <template v-else-if="processingResult === 'completed'">
          <el-button type="primary" @click="closeProcessing">完成</el-button>
        </template>
        <template v-else-if="processingResult === 'failed'">
          <el-button @click="closeProcessing">关闭</el-button>
          <el-button type="warning" @click="retryFromDialog">重试</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 文档详情弹窗 -->
    <el-dialog v-model="detailVisible" title="文档详情" width="600px" destroy-on-close>
      <div v-if="detailDoc" class="detail-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="文档标题">{{ detailDoc.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detailDoc.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="statusType(detailDoc.parseStatus)" size="small">
              {{ statusLabel(detailDoc.parseStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件名">{{ detailDoc.fileName }}</el-descriptions-item>
          <el-descriptions-item label="知识点数">{{ detailDoc.vertexCount }}</el-descriptions-item>
          <el-descriptions-item label="关系数">{{ detailDoc.edgeCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detailDoc.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailDoc.errorMessage" label="错误信息" :span="2">
            <span class="error-text">{{ detailDoc.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailDoc?.parseStatus === 'failed'" type="warning" @click="handleRetry(detailDoc); detailVisible = false">
          重试处理
        </el-button>
        <el-button v-if="detailDoc" type="primary" plain @click="viewGraph(detailDoc)">
          查看图谱
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection, CircleCheck, WarningFilled, DataAnalysis,
  Search, Upload, UploadFilled, View, RefreshRight, Delete, Loading
} from '@element-plus/icons-vue'
import {
  getAllKgCategories, getKgDocuments, getKgDocument,
  uploadKgDocument, retryKgDocument, deleteKgDocument
} from '@/api/knowledgeGraph'

const router = useRouter()

// 分类数据
const categories = ref([])

// 表格数据
const documentList = ref([])
const loading = ref(false)
const totalDocs = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 筛选
const filterCategory = ref(null)
const filterStatus = ref('')

// 统计
const totalDocuments = computed(() => documentList.value.length)
const completedCount = computed(() => documentList.value.filter(d => d.parseStatus === 'completed').length)
const failedCount = computed(() => documentList.value.filter(d => d.parseStatus === 'failed').length)
const totalVertices = computed(() => documentList.value.reduce((s, d) => s + (d.vertexCount || 0), 0))

// 状态映射
const statusType = (status) => ({
  pending: 'info',
  processing: 'warning',
  completed: 'success',
  failed: 'danger'
}[status] || 'info')

const statusLabel = (status) => ({
  pending: '待处理',
  processing: '处理中',
  completed: '已完成',
  failed: '失败'
}[status] || status)

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getAllKgCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getKgDocuments(filterCategory.value, currentPage.value, pageSize.value)
    let list = res.data || []
    // 前端按状态筛选（后端暂不支持）
    if (filterStatus.value) {
      list = list.filter(d => d.parseStatus === filterStatus.value)
    }
    documentList.value = list
    totalDocs.value = list.length
  } catch (e) {
    console.error('加载文档失败', e)
  } finally {
    loading.value = false
  }
}

// ==================== 上传 ====================
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref()
const uploadRef = ref()
const uploadForm = reactive({ title: '', categoryName: '', file: null })

// 处理进度
const processing = ref(false)
const processingPercent = ref(0)
const processingStep = ref('')
const processingResult = ref('') // completed / failed / ''
const processingErrorMsg = ref('')
const processingDocId = ref(null)
let kgEventSource = null

// 步骤 → 中文标签
const stepLabelMap = {
  parse: '解析文档...',
  extract: 'AI 提取知识点...',
  analyze: 'AI 分析关系...',
  adjacency: '构建知识图谱...'
}

const processingStepLabel = computed(() => {
  if (processingResult.value === 'completed') return '处理完成'
  if (processingResult.value === 'failed') return '处理失败'
  return stepLabelMap[processingStep.value] || '准备中...'
})

const processingStatus = computed(() => {
  if (processingResult.value === 'completed') return 'success'
  if (processingResult.value === 'failed') return 'exception'
  return ''
})

const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const handleUpload = async () => {
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    const res = await uploadKgDocument(uploadForm.file, uploadForm.categoryName, uploadForm.title)
    processingDocId.value = res.data?.id
    // 切换到进度视图
    processing.value = true
    processingPercent.value = 0
    processingStep.value = 'parse'
    processingResult.value = ''
    processingErrorMsg.value = ''
    // 清空表单
    uploadForm.file = null
    uploadForm.title = ''
    uploadForm.categoryName = ''
    if (uploadRef.value) uploadRef.value.clearFiles()
    loadDocuments()
    // 连接 SSE 接收处理进度
    connectKgSSE(res.data.id)
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

/** 连接 SSE 接收文档处理进度 */
const connectKgSSE = (documentId) => {
  disconnectKgSSE()
  const token = localStorage.getItem('admin_token')
  if (!token) return
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  const es = new EventSource(
    `${apiBase}/admin/kg/subscribe?token=${token}&documentId=${documentId}`
  )

  es.addEventListener('kg_progress', (e) => {
    try {
      const data = JSON.parse(e.data)
      processingStep.value = data.step
      processingPercent.value = data.progress
      if (data.status === 'completed') {
        processingResult.value = 'completed'
        processingPercent.value = 100
        disconnectKgSSE()
      } else if (data.status === 'failed') {
        processingResult.value = 'failed'
        processingErrorMsg.value = data.errorMessage || '未知错误'
        disconnectKgSSE()
      }
    } catch { /* 静默 */ }
  })

  es.onerror = () => {
    disconnectKgSSE()
  }

  kgEventSource = es
}

const disconnectKgSSE = () => {
  if (kgEventSource) {
    kgEventSource.close()
    kgEventSource = null
  }
}

const closeProcessing = () => {
  disconnectKgSSE()
  processing.value = false
  processingDocId.value = null
  processingResult.value = ''
  uploadVisible.value = false
  loadDocuments()
}

const retryFromDialog = async () => {
  const doc = documentList.value.find(d => d.id === processingDocId.value)
  if (!doc) return
  processingResult.value = ''
  processingPercent.value = 0
  processingStep.value = 'parse'
  try {
    await retryKgDocument(doc.id)
    connectKgSSE(doc.id)
  } catch (e) {
    ElMessage.error('重试失败')
  }
}

onBeforeUnmount(() => disconnectKgSSE())

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailDoc = ref(null)

const viewDetail = async (row) => {
  try {
    const res = await getKgDocument(row.id)
    detailDoc.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const viewGraph = (doc) => {
  detailVisible.value = false
  router.push(`/knowledge/${doc.categoryId}`)
}

// ==================== 重试 ====================
const handleRetry = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要重试处理文档「${row.title}」吗？`, '重试确认', {
      type: 'warning',
      confirmButtonText: '重试',
      cancelButtonText: '取消'
    })
    await retryKgDocument(row.id)
    ElMessage.success('已重新触发处理')
    loadDocuments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重试失败')
  }
}

// ==================== 删除 ====================
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档「${row.title}」吗？关联的知识点和关系将被清除。`, '删除确认', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteKgDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadCategories()
  loadDocuments()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue { background: #eff6ff; color: #3b82f6; }
.stat-icon.green { background: #ecfdf5; color: #10b981; }
.stat-icon.orange { background: #fff7ed; color: #f59e0b; }
.stat-icon.purple { background: #faf5ff; color: #8b5cf6; }

.stat-value { font-size: 22px; font-weight: 700; color: #0f172a; line-height: 1.2; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

/* 筛选栏 */
.filter-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-right {
  display: flex;
  gap: 8px;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 0 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 状态文字 */
.error-text { color: #ef4444; font-size: 13px; }
.empty-text { color: #cbd5e1; }
.time-text { font-size: 13px; color: #64748b; }

/* 操作按钮 */
.action-btns {
  display: flex;
  gap: 2px;
  justify-content: center;
}

.action-btns .el-button { padding: 4px 5px; }

/* 分页 */
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  margin-top: 12px;
}

.page-total { font-size: 13px; color: #94a3b8; }

/* 处理进度视图 */
.processing-view {
  padding: 20px 0;
}

.processing-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.processing-step {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-top: 8px;
}

.processing-error {
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
