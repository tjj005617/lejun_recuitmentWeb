<template>
  <div class="kg-page">
    <!-- 顶部横幅 -->
    <div class="kg-banner">
      <div class="kg-banner__inner">
        <h1 class="kg-banner__title">八股面试专栏</h1>
        <p class="kg-banner__desc">基于知识图谱的技术面试准备，浏览知识点关联关系，系统化学习</p>
        <!-- 搜索框 -->
        <div class="kg-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索知识点..."
            clearable
            prefix-icon="Search"
            style="width: 360px"
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          />
        </div>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="kg-categories">
      <div class="kg-categories__inner">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="kg-cat-tab"
          :class="{ active: activeCategoryId === cat.id }"
          @click="selectCategory(cat.id)"
        >
          <span class="kg-cat-tab__icon">{{ cat.icon || '📁' }}</span>
          <span class="kg-cat-tab__name">{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="kg-main" v-if="activeCategoryId">
      <!-- 视图切换 + 统计 -->
      <div class="kg-toolbar">
        <div class="kg-toolbar__left">
          <el-button-group>
            <el-button :type="viewMode === 'graph' ? 'primary' : ''" @click="viewMode = 'graph'">
              <el-icon><Share /></el-icon> 图谱视图
            </el-button>
            <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">
              <el-icon><List /></el-icon> 列表视图
            </el-button>
          </el-button-group>
          <span class="kg-stats" v-if="graphData">
            {{ graphData.totalVertices }} 个知识点 · {{ graphData.totalEdges }} 条关系
          </span>
        </div>
        <div class="kg-toolbar__right" v-if="viewMode === 'graph'">
          <el-button-group>
            <el-button :type="interactionMode === 'pointer' ? 'primary' : ''"
              @click="interactionMode = 'pointer'; chartInstance?.setOption({ series: [{ roam: 'move' }] })"
              title="指针模式 - 点击节点查看详情">
              <el-icon><Pointer /></el-icon>
            </el-button>
            <el-button :type="interactionMode === 'hand' ? 'primary' : ''"
              @click="interactionMode = 'hand'; chartInstance?.setOption({ series: [{ roam: true }] })"
              title="手型模式 - 拖动画布平移">
              <el-icon><Position /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 图谱视图 -->
      <div v-show="viewMode === 'graph'" class="kg-graph-container" :class="{ 'mode-hand': interactionMode === 'hand' }">
        <div ref="graphChartRef" class="kg-graph-chart"></div>
      </div>

      <!-- 列表视图 -->
      <div v-show="viewMode === 'list'" class="kg-list-container">
        <div class="kg-vertex-grid">
          <div
            v-for="vertex in displayedVertices"
            :key="vertex.id"
            class="kg-vertex-card"
            @click="openDetail(vertex)"
          >
            <div class="kg-vertex-card__header">
              <span class="kg-vertex-card__type" :class="vertex.vertexType">{{ typeLabel(vertex.vertexType) }}</span>
              <span class="kg-vertex-card__edges">{{ vertex.edgeCount || 0 }} 关联</span>
            </div>
            <h3 class="kg-vertex-card__name">{{ vertex.name }}</h3>
            <p class="kg-vertex-card__desc">{{ vertex.description || '暂无描述' }}</p>
            <div class="kg-vertex-card__footer" v-if="vertex.documentTitle">
              <span class="kg-vertex-card__source">来源: {{ vertex.documentTitle }}</span>
            </div>
          </div>
        </div>
        <div v-if="displayedVertices.length === 0" class="kg-empty">
          <el-empty description="该分类暂无知识点" />
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="isSearching" class="kg-main">
      <div class="kg-toolbar">
        <div class="kg-toolbar__left">
          <span class="kg-stats">搜索「{{ searchKeyword }}」找到 {{ searchResults.length }} 个知识点</span>
          <el-button text @click="clearSearch">清除搜索</el-button>
        </div>
      </div>
      <div class="kg-list-container">
        <div class="kg-vertex-grid">
          <div
            v-for="vertex in searchResults"
            :key="vertex.id"
            class="kg-vertex-card"
            @click="openDetail(vertex)"
          >
            <div class="kg-vertex-card__header">
              <span class="kg-vertex-card__type" :class="vertex.vertexType">{{ typeLabel(vertex.vertexType) }}</span>
              <span class="kg-vertex-card__edges">{{ vertex.edgeCount || 0 }} 关联</span>
            </div>
            <h3 class="kg-vertex-card__name">{{ vertex.name }}</h3>
            <p class="kg-vertex-card__desc">{{ vertex.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识点详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detailVertex?.name" size="480px" destroy-on-close>
      <div v-if="detailVertex" class="kg-detail">
        <!-- 基本信息 -->
        <div class="kg-detail__section">
          <div class="kg-detail__type-row">
            <span class="kg-vertex-card__type" :class="detailVertex.vertexType">{{ typeLabel(detailVertex.vertexType) }}</span>
            <span v-if="detailVertex.subType" class="kg-detail__subtype">{{ detailVertex.subType }}</span>
          </div>
          <div class="kg-detail__category" v-if="detailVertex.categoryName">
            <span class="kg-detail__category-label">所属分类:</span>
            <el-tag size="small" type="success" effect="plain">{{ detailVertex.categoryName }}</el-tag>
          </div>
          <p class="kg-detail__desc">{{ detailVertex.description || '暂无描述' }}</p>
        </div>

        <!-- 属性 -->
        <div class="kg-detail__section" v-if="parsedProperties">
          <h4>属性</h4>
          <div class="kg-detail__props">
            <div v-if="parsedProperties.difficulty" class="kg-detail__prop">
              <span class="prop-label">难度</span>
              <el-tag size="small" :type="difficultyType(parsedProperties.difficulty)">{{ parsedProperties.difficulty }}</el-tag>
            </div>
            <div v-if="parsedProperties.importance" class="kg-detail__prop">
              <span class="prop-label">重要性</span>
              <el-tag size="small" :type="importanceType(parsedProperties.importance)">{{ parsedProperties.importance }}</el-tag>
            </div>
            <div v-if="parsedProperties.tags?.length" class="kg-detail__prop">
              <span class="prop-label">标签</span>
              <div class="prop-tags">
                <el-tag v-for="tag in parsedProperties.tags" :key="tag" size="small" effect="plain" round>{{ tag }}</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 关联知识点 -->
        <div class="kg-detail__section">
          <h4>关联知识点 ({{ neighbors.length }})</h4>
          <div v-if="neighbors.length" class="kg-detail__neighbors">
            <div
              v-for="n in neighbors"
              :key="n.id"
              class="kg-neighbor-item"
              @click="openDetail(n)"
            >
              <span class="kg-neighbor-item__type" :class="n.vertexType">{{ typeLabel(n.vertexType) }}</span>
              <span class="kg-neighbor-item__name">{{ n.name }}</span>
            </div>
          </div>
          <div v-else class="kg-detail__empty">暂无关联知识点</div>
        </div>

        <!-- 来源文档 -->
        <div class="kg-detail__section" v-if="detailVertex.documentTitle">
          <h4>来源</h4>
          <div class="kg-detail__source">{{ detailVertex.documentTitle }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Share, List, Search, Pointer, Position } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getKgCategories, getKgGraphData, getKgNeighbors, searchKgVertices
} from '@/api/knowledgeGraph'

const route = useRoute()
const router = useRouter()

// 分类数据
const categories = ref([])
const activeCategoryId = ref(null)

// 视图模式
const viewMode = ref('graph')
const interactionMode = ref('pointer') // pointer / hand

// 图谱数据
const graphData = ref(null)
const chartInstance = ref(null)
const graphChartRef = ref(null)

// 搜索
const searchKeyword = ref('')
const isSearching = ref(false)
const searchResults = ref([])

// 详情抽屉
const detailVisible = ref(false)
const detailVertex = ref(null)
const neighbors = ref([])

// 解析后的属性
const parsedProperties = computed(() => {
  if (!detailVertex.value?.properties) return null
  try {
    return typeof detailVertex.value.properties === 'string'
      ? JSON.parse(detailVertex.value.properties)
      : detailVertex.value.properties
  } catch { return null }
})

// 列表视图的顶点
const displayedVertices = computed(() => graphData.value?.nodes || [])

// 类型标签
const typeLabel = (type) => ({
  concept: '概念', technology: '技术', api: 'API',
  theory: '原理', example: '实践', keyword: '术语'
}[type] || type)

const difficultyType = (d) => ({ '简单': 'success', '中等': 'warning', '困难': 'danger' }[d] || 'info')
const importanceType = (i) => ({ '高': 'danger', '中': 'warning', '低': 'info' }[i] || 'info')

// 选择分类
const selectCategory = (id) => {
  activeCategoryId.value = id
  isSearching.value = false
  searchKeyword.value = ''
  router.replace(id ? `/knowledge/${id}` : '/knowledge')
  loadGraphData(id)
}

// 加载图谱数据
const loadGraphData = async (categoryId) => {
  if (!categoryId) return
  try {
    const res = await getKgGraphData(categoryId)
    graphData.value = res.data
    await nextTick()
    renderGraph()
  } catch (e) {
    console.error('加载图谱数据失败', e)
  }
}

// ECharts 图谱渲染
const renderGraph = () => {
  if (!graphChartRef.value || !graphData.value) return

  if (chartInstance.value) {
    chartInstance.value.dispose()
  }

  const chart = echarts.init(graphChartRef.value)
  chartInstance.value = chart

  const { nodes, edges } = graphData.value

  // 顶点类型颜色映射
  const typeColors = {
    concept: '#3b82f6',
    technology: '#10b981',
    api: '#f59e0b',
    theory: '#8b5cf6',
    example: '#ec4899',
    keyword: '#06b6d4'
  }

  // 计算节点大小（按连接数）
  const maxEdges = Math.max(...nodes.map(n => n.edgeCount || 1), 1)

  const chartNodes = nodes.map(n => ({
    id: String(n.id),
    name: n.name,
    symbolSize: 20 + ((n.edgeCount || 0) / maxEdges) * 40,
    itemStyle: {
      color: typeColors[n.vertexType] || '#64748b',
      borderColor: '#fff',
      borderWidth: 2,
      shadowBlur: 6,
      shadowColor: 'rgba(0,0,0,0.1)'
    },
    label: {
      show: (n.edgeCount || 0) >= 2 || nodes.length <= 30,
      fontSize: 11,
      color: '#334155'
    },
    // 存原始数据
    vertexData: n
  }))

  const chartEdges = edges.map(e => ({
    source: String(e.fromId),
    target: String(e.toId),
    label: {
      show: false,
      formatter: e.edgeLabel,
      fontSize: 10,
      color: '#94a3b8'
    },
    lineStyle: {
      width: Math.max(1, (e.weight || 0.5) * 3),
      curveness: 0.2,
      color: '#cbd5e1'
    },
    emphasis: {
      lineStyle: { width: 3, color: '#3b82f6' },
      label: { show: true }
    }
  }))

  chart.setOption({
    tooltip: {
      formatter: (params) => {
        if (params.dataType === 'node') {
          const d = params.data.vertexData
          const catInfo = d.categoryName ? `<span style="color:#10b981;font-size:11px">${d.categoryName}</span><br/>` : ''
          return `<div style="max-width:220px">
            <strong>${d.name}</strong><br/>
            ${catInfo}
            <span style="color:${typeColors[d.vertexType] || '#64748b'}">${typeLabel(d.vertexType)}</span><br/>
            <span style="color:#64748b;font-size:12px">${d.description || ''}</span><br/>
            <span style="color:#94a3b8;font-size:11px">${d.edgeCount || 0} 条关联</span>
          </div>`
        }
        if (params.dataType === 'edge') {
          return params.data.label?.formatter || ''
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      data: chartNodes,
      links: chartEdges,
      force: {
        repulsion: Math.max(200, nodes.length * 15),
        edgeLength: [80, 200],
        gravity: 0.1
      },
      emphasis: {
        focus: 'adjacency',
        blurScope: 'global'
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8]
    }]
  })

  // 点击节点打开详情
  chart.on('click', (params) => {
    if (params.dataType === 'node' && params.data.vertexData) {
      openDetail(params.data.vertexData)
    }
  })

  // 自适应窗口大小
  const resizeHandler = () => chart.resize()
  window.addEventListener('resize', resizeHandler)
  onBeforeUnmount(() => window.removeEventListener('resize', resizeHandler))
}

// 搜索
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  isSearching.value = true
  try {
    const res = await searchKgVertices(searchKeyword.value, activeCategoryId.value)
    searchResults.value = res.data || []
  } catch (e) {
    console.error('搜索失败', e)
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  isSearching.value = false
  searchResults.value = []
}

// 打开详情
const openDetail = async (vertex) => {
  detailVertex.value = vertex
  detailVisible.value = true
  try {
    const res = await getKgNeighbors(vertex.id)
    neighbors.value = res.data || []
  } catch (e) {
    neighbors.value = []
  }
}

// 初始化
// 拦截图谱区域的 Ctrl+滚轮，防止浏览器缩放页面
const preventBrowserZoom = (e) => {
  if (e.ctrlKey) {
    e.preventDefault()
    e.stopPropagation()
  }
}

onMounted(async () => {
  // 在图谱容器上拦截 Ctrl+滚轮
  const container = graphChartRef.value?.closest('.kg-graph-container')
  if (container) {
    container.addEventListener('wheel', preventBrowserZoom, { passive: false })
  }

  try {
    const res = await getKgCategories()
    categories.value = res.data || []

    // 从路由参数或默认第一个分类
    const catId = route.params.categoryId
      ? Number(route.params.categoryId)
      : (categories.value[0]?.id || null)

    if (catId) {
      selectCategory(catId)
    }
  } catch (e) {
    console.error('加载分类失败', e)
  }
})

// 监听路由变化
watch(() => route.params.categoryId, (id) => {
  if (id) selectCategory(Number(id))
})

onBeforeUnmount(() => {
  const container = graphChartRef.value?.closest('.kg-graph-container')
  if (container) {
    container.removeEventListener('wheel', preventBrowserZoom)
  }
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
})
</script>

<style scoped>
.kg-page {
  min-height: calc(100vh - 64px);
  background: #f8fafc;
}

/* 顶部横幅 */
.kg-banner {
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0f4a35 100%);
  padding: 48px 0 36px;
}

.kg-banner__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  text-align: center;
}

.kg-banner__title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px;
}

.kg-banner__desc {
  font-size: 15px;
  color: rgba(255,255,255,0.6);
  margin: 0 0 24px;
}

.kg-search {
  display: flex;
  justify-content: center;
}

.kg-search :deep(.el-input__wrapper) {
  border-radius: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}

/* 分类标签 */
.kg-categories {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 64px;
  z-index: 40;
}

.kg-categories__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 4px;
  overflow-x: auto;
}

.kg-cat-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 18px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.2s;
}

.kg-cat-tab:hover {
  color: #1e293b;
}

.kg-cat-tab.active {
  color: #10b981;
  border-bottom-color: #10b981;
  font-weight: 600;
}

.kg-cat-tab__icon {
  font-size: 16px;
}

/* 主内容区 */
.kg-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
}

/* 工具栏 */
.kg-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.kg-toolbar__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.kg-toolbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kg-stats {
  font-size: 13px;
  color: #94a3b8;
}

/* 图谱容器 */
.kg-graph-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
  cursor: default;
}

.kg-graph-container.mode-hand {
  cursor: grab;
}

.kg-graph-container.mode-hand:active {
  cursor: grabbing;
}

.kg-graph-chart {
  width: 100%;
  height: 600px;
}

/* 列表视图 */
.kg-list-container {
  min-height: 400px;
}

.kg-vertex-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 知识点卡片 */
.kg-vertex-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.kg-vertex-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border-color: #e2e8f0;
  transform: translateY(-2px);
}

.kg-vertex-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.kg-vertex-card__type {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.kg-vertex-card__type.concept { background: #eff6ff; color: #3b82f6; }
.kg-vertex-card__type.technology { background: #ecfdf5; color: #10b981; }
.kg-vertex-card__type.api { background: #fff7ed; color: #f59e0b; }
.kg-vertex-card__type.theory { background: #faf5ff; color: #8b5cf6; }
.kg-vertex-card__type.example { background: #fdf2f8; color: #ec4899; }
.kg-vertex-card__type.keyword { background: #ecfeff; color: #06b6d4; }

.kg-vertex-card__edges {
  font-size: 12px;
  color: #94a3b8;
}

.kg-vertex-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
  line-height: 1.4;
}

.kg-vertex-card__desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kg-vertex-card__footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.kg-vertex-card__source {
  font-size: 12px;
  color: #94a3b8;
}

/* 空状态 */
.kg-empty {
  padding: 60px 0;
}

/* 详情抽屉 */
.kg-detail {
  padding: 0 4px;
}

.kg-detail__section {
  margin-bottom: 24px;
}

.kg-detail__section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px;
}

.kg-detail__type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.kg-detail__subtype {
  font-size: 12px;
  color: #94a3b8;
}

.kg-detail__desc {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
}

.kg-detail__category {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.kg-detail__category-label {
  font-size: 13px;
  color: #64748b;
}

.kg-detail__props {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kg-detail__prop {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prop-label {
  font-size: 13px;
  color: #64748b;
  min-width: 50px;
}

.prop-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 邻居列表 */
.kg-detail__neighbors {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kg-neighbor-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.kg-neighbor-item:hover {
  background: #f1f5f9;
}

.kg-neighbor-item__type {
  display: inline-flex;
  padding: 1px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
}

.kg-neighbor-item__type.concept { background: #eff6ff; color: #3b82f6; }
.kg-neighbor-item__type.technology { background: #ecfdf5; color: #10b981; }
.kg-neighbor-item__type.api { background: #fff7ed; color: #f59e0b; }
.kg-neighbor-item__type.theory { background: #faf5ff; color: #8b5cf6; }
.kg-neighbor-item__type.example { background: #fdf2f8; color: #ec4899; }
.kg-neighbor-item__type.keyword { background: #ecfeff; color: #06b6d4; }

.kg-neighbor-item__name {
  font-size: 13px;
  color: #1e293b;
}

.kg-detail__empty {
  font-size: 13px;
  color: #94a3b8;
  padding: 12px 0;
}

.kg-detail__source {
  font-size: 13px;
  color: #475569;
}
</style>
