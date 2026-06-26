<template>
  <div class="kg-page">
    <!-- 左侧侧边栏 -->
    <aside class="kg-sidebar">
      <div class="kg-sidebar__title">八股面试专栏</div>

      <!-- 分类列表 -->
      <div class="kg-sidebar__section">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="kg-sidebar__cat"
          :class="{ active: activeCategoryId === cat.id }"
          @click="selectCategory(cat.id)"
        >
          <span class="kg-sidebar__cat-name">{{ cat.name }}</span>
          <span class="kg-sidebar__cat-count" v-if="cat.vertexCount">{{ cat.vertexCount }}</span>
        </div>
      </div>

      <!-- 底部统计 -->
      <div class="kg-sidebar__footer" v-if="graphData">
        <span>{{ graphData.totalVertices }} 个知识点</span>
        <span>{{ graphData.totalEdges }} 条关系</span>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="kg-content">
      <!-- 顶部工具栏 -->
      <div class="kg-content__header">
        <div class="kg-content__header-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索知识点..."
            clearable
            prefix-icon="Search"
            size="small"
            style="width: 240px"
            @input="onSearchInput"
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          />
          <el-button-group>
            <el-button :type="viewMode === 'graph' ? 'primary' : ''" @click="viewMode = 'graph'" size="small">
              <el-icon><Share /></el-icon> 图谱
            </el-button>
            <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'" size="small">
              <el-icon><List /></el-icon> 列表
            </el-button>
          </el-button-group>
        </div>
        <div class="kg-content__header-right" v-if="viewMode === 'graph'">
          <el-button-group>
            <el-button :type="interactionMode === 'pointer' ? 'primary' : ''"
              @click="interactionMode = 'pointer'; chartInstance?.setOption({ series: [{ roam: 'move' }] })"
              title="指针模式" size="small">
              <el-icon><Pointer /></el-icon>
            </el-button>
            <el-button :type="interactionMode === 'hand' ? 'primary' : ''"
              @click="interactionMode = 'hand'; chartInstance?.setOption({ series: [{ roam: true }] })"
              title="手型模式" size="small">
              <el-icon><Position /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 有分类选中时的内容 -->
      <template v-if="activeCategoryId">
        <!-- 实时搜索结果 -->
        <div v-if="suggestions.length > 0" class="kg-suggestions-bar">
          <div class="kg-suggestions-bar__header">
            <span class="kg-suggestions-bar__title">搜索到 {{ suggestions.length }} 个相关知识点</span>
            <el-button text type="primary" size="small" @click="clearSearch">清除</el-button>
          </div>
          <div class="kg-suggestions-list">
            <div
              v-for="item in suggestions"
              :key="item.id"
              class="kg-suggestion-item"
              @click="onSuggestionClick(item)"
            >
              <span class="kg-suggestion-item__type" :class="item.vertexType">{{ typeLabel(item.vertexType) }}</span>
              <div class="kg-suggestion-item__info">
                <span class="kg-suggestion-item__name">{{ item.name }}</span>
                <span class="kg-suggestion-item__desc">{{ item.description || '' }}</span>
              </div>
            </div>
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
      </template>

      <!-- 搜索结果 -->
      <div v-if="isSearching" class="kg-search-results">
        <div class="kg-search-results__header">
          <span class="kg-search-results__title">搜索「{{ searchKeyword }}」找到 {{ searchResults.length }} 个知识点</span>
          <el-button text @click="clearSearch" size="small">清除搜索</el-button>
        </div>
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
    </main>

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
const activeCategoryName = computed(() => categories.value.find(c => c.id === activeCategoryId.value)?.name || '')

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
const suggestions = ref([])
let searchTimer = null

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
  suggestions.value = []
}

// 输入时防抖获取建议
const onSearchInput = () => {
  clearTimeout(searchTimer)
  const kw = searchKeyword.value.trim()
  if (!kw) {
    suggestions.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchKgVertices(kw, activeCategoryId.value)
      suggestions.value = (res.data || []).slice(0, 20)
    } catch { suggestions.value = [] }
  }, 300)
}

const onSuggestionClick = (item) => {
  openDetail(item)
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
  clearTimeout(searchTimer)
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
})
</script>

<style scoped>
.kg-page {
  display: flex;
  min-height: calc(100vh - 64px);
  background: #f5f5f5;
}

/* ==================== 左侧侧边栏 ==================== */
.kg-sidebar {
  width: 220px;
  min-width: 220px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.kg-sidebar__title {
  padding: 20px 16px 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.kg-sidebar__section {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.kg-sidebar__cat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: all 0.15s;
}

.kg-sidebar__cat:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.kg-sidebar__cat.active {
  background: #eef2ff;
  color: #4338ca;
  font-weight: 500;
}

.kg-sidebar__cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-sidebar__cat-count {
  font-size: 12px;
  color: #999;
}

.kg-sidebar__footer {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

/* ==================== 主内容区 ==================== */
.kg-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.kg-content__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.kg-content__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kg-content__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ==================== 搜索结果 ==================== */
.kg-search-results {
  padding: 24px;
}

.kg-search-results__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.kg-search-results__title {
  font-size: 14px;
  color: #666;
}

/* ==================== 搜索建议条 ==================== */
.kg-suggestions-bar {
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 12px 24px 0;
}

.kg-suggestions-bar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.kg-suggestions-bar__title {
  font-size: 13px;
  color: #666;
}

.kg-suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kg-suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.kg-suggestion-item:hover {
  background: #f3f4f6;
}

.kg-suggestion-item__type {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

/* 类型标签颜色 */
.kg-suggestion-item__type.concept,
.kg-vertex-card__type.concept,
.kg-neighbor-item__type.concept { background: #eff6ff; color: #3b82f6; }

.kg-suggestion-item__type.technology,
.kg-vertex-card__type.technology,
.kg-neighbor-item__type.technology { background: #ecfdf5; color: #10b981; }

.kg-suggestion-item__type.api,
.kg-vertex-card__type.api,
.kg-neighbor-item__type.api { background: #fff7ed; color: #f59e0b; }

.kg-suggestion-item__type.theory,
.kg-vertex-card__type.theory,
.kg-neighbor-item__type.theory { background: #faf5ff; color: #8b5cf6; }

.kg-suggestion-item__type.example,
.kg-vertex-card__type.example,
.kg-neighbor-item__type.example { background: #fdf2f8; color: #ec4899; }

.kg-suggestion-item__type.keyword,
.kg-vertex-card__type.keyword,
.kg-neighbor-item__type.keyword { background: #ecfeff; color: #06b6d4; }

.kg-suggestion-item__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.kg-suggestion-item__name {
  font-size: 14px;
  color: #1a1a1a;
}

.kg-suggestion-item__desc {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ==================== 图谱容器 ==================== */
.kg-graph-container {
  margin: 12px 24px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  cursor: default;
}

.kg-graph-container.mode-hand { cursor: grab; }
.kg-graph-container.mode-hand:active { cursor: grabbing; }

.kg-graph-chart {
  width: 100%;
  height: calc(100vh - 180px);
  min-height: 480px;
}

/* ==================== 列表视图 ==================== */
.kg-list-container {
  padding: 12px 24px 24px;
}

.kg-vertex-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.kg-vertex-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.15s;
}

.kg-vertex-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.kg-vertex-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.kg-vertex-card__type {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.kg-vertex-card__edges {
  font-size: 12px;
  color: #999;
}

.kg-vertex-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
  line-height: 1.4;
}

.kg-vertex-card__desc {
  font-size: 13px;
  color: #666;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kg-vertex-card__footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}

.kg-vertex-card__source {
  font-size: 12px;
  color: #999;
}

.kg-empty {
  padding: 60px 0;
}

/* ==================== 详情抽屉 ==================== */
.kg-detail {
  padding: 0 4px;
}

.kg-detail__section {
  margin-bottom: 24px;
}

.kg-detail__section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
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
  color: #999;
}

.kg-detail__desc {
  font-size: 14px;
  color: #444;
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
  color: #666;
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
  color: #666;
  min-width: 50px;
}

.prop-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.kg-detail__neighbors {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kg-neighbor-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.kg-neighbor-item:hover {
  background: #f3f4f6;
}

.kg-neighbor-item__type {
  display: inline-flex;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.kg-neighbor-item__name {
  font-size: 13px;
  color: #1a1a1a;
}

.kg-detail__empty {
  font-size: 13px;
  color: #999;
  padding: 12px 0;
}

.kg-detail__source {
  font-size: 13px;
  color: #444;
}

/* ==================== 滚动条 ==================== */
.kg-sidebar__section::-webkit-scrollbar {
  width: 4px;
}

.kg-sidebar__section::-webkit-scrollbar-track {
  background: transparent;
}

.kg-sidebar__section::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 4px;
}
</style>
