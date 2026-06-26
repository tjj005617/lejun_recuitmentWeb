/**
 * 知识图谱 API 封装
 * 包含分类、文档、图谱、搜索、管理员操作
 */
import request from './request'

// ==================== 分类（公开） ====================

/** 获取所有启用分类 */
export const getKgCategories = () => request.get('/kg/category/list')

/** 获取所有分类（含禁用，管理员用） */
export const getAllKgCategories = () => request.get('/kg/category/list/all')

// ==================== 文档（公开） ====================

/** 按分类获取文档列表 */
export const getKgDocuments = (categoryId, page = 1, size = 20) =>
  request.get('/kg/document/list', { params: { categoryId, page, size } })

/** 获取文档详情 */
export const getKgDocument = (id) => request.get(`/kg/document/${id}`)

// ==================== 图谱（公开） ====================

/** 获取分类图谱数据（节点+边） */
export const getKgGraphData = (categoryId) => request.get(`/kg/graph/${categoryId}`)

/** 获取顶点详情 */
export const getKgVertex = (id) => request.get(`/kg/graph/vertex/${id}`)

/** 获取顶点邻居 */
export const getKgNeighbors = (vertexId) => request.get(`/kg/graph/neighbor/${vertexId}`)

/** 搜索知识点 */
export const searchKgVertices = (keyword, categoryId) =>
  request.get('/kg/graph/search', { params: { keyword, categoryId } })

/** 获取分类统计 */
export const getKgStats = (categoryId) => request.get(`/kg/graph/stats/${categoryId}`)

// ==================== 管理员操作 ====================

/** 上传文档（可选指定分类名称，不填则 AI 自动分类） */
export const uploadKgDocument = (file, categoryName, title) => {
  const formData = new FormData()
  formData.append('file', file)
  if (categoryName) formData.append('categoryName', categoryName)
  if (title) formData.append('title', title)
  return request.post('/admin/kg/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 批量上传文档（可选指定分类名称，不填则 AI 自动分类） */
export const batchUploadKgDocuments = (files, categoryName) => {
  const formData = new FormData()
  for (const file of files) {
    formData.append('files', file)
  }
  if (categoryName) formData.append('categoryName', categoryName)
  return request.post('/admin/kg/batch-upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000  // 批量上传超时5分钟
  })
}

/** 重试处理失败的文档 */
export const retryKgDocument = (documentId) =>
  request.post(`/admin/kg/retry/${documentId}`)

/** 删除文档 */
export const deleteKgDocument = (id) =>
  request.delete(`/admin/kg/document/${id}`)
