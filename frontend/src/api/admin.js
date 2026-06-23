/**
 * 管理后台 API 封装
 */
import request from './request'

// ==================== 管理员认证 ====================

/** 获取当前管理员信息 */
export const getAdminProfile = () => request.get('/api/admin/profile')

// ==================== 仪表盘 ====================

/** 获取统计数据 */
export const getDashboardSummary = () => request.get('/api/admin/dashboard/summary')

/** 用户增长趋势 */
export const getUserTrend = (days = 30) => request.get('/api/admin/dashboard/user-trend', { params: { days } })

/** 投递趋势 */
export const getApplicationTrend = (days = 30) => request.get('/api/admin/dashboard/application-trend', { params: { days } })

/** 职位类型分布 */
export const getJobTypeDistribution = () => request.get('/api/admin/dashboard/job-type')

/** 面试状态分布 */
export const getInterviewStatusDistribution = () => request.get('/api/admin/dashboard/interview-status')

/** 行业职位 TOP10 */
export const getJobIndustryTop10 = () => request.get('/api/admin/dashboard/job-industry')

/** 城市职位 TOP10 */
export const getJobCityTop10 = () => request.get('/api/admin/dashboard/job-city')

/** 待处理事项 */
export const getPendingTasks = () => request.get('/api/admin/dashboard/pending-tasks')

/** 最近活动 */
export const getRecentActivity = () => request.get('/api/admin/dashboard/recent-activity')

// ==================== 用户管理 ====================

/** 用户列表 */
export const getAdminUsers = (params) => request.get('/api/admin/users', { params })

/** 用户详情 */
export const getAdminUserDetail = (id) => request.get(`/api/admin/users/${id}`)

/** 修改用户状态 */
export const updateUserStatus = (id, status) => request.put(`/api/admin/users/${id}/status`, { status })

/** 修改用户角色 */
export const updateUserRole = (id, roleType) => request.put(`/api/admin/users/${id}/role`, { roleType })

/** 删除用户 */
export const deleteAdminUser = (id) => request.delete(`/api/admin/users/${id}`)

/** 批量导入用户 */
export const importUsers = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/users/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 导出用户列表 */
export const exportUsers = (params) => request.get('/api/admin/users/export', {
  params,
  responseType: 'blob'
})

// ==================== 公司管理 ====================

/** 公司列表 */
export const getAdminCompanies = (params) => request.get('/api/admin/companies', { params })

/** 公司详情 */
export const getAdminCompanyDetail = (id) => request.get(`/api/admin/companies/${id}`)

/** 修改公司状态 */
export const updateCompanyStatus = (id, status) => request.put(`/api/admin/companies/${id}/status`, { status })

/** 删除公司 */
export const deleteAdminCompany = (id) => request.delete(`/api/admin/companies/${id}`)

/** 批量导入公司 */
export const importCompanies = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/companies/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 导出公司列表 */
export const exportCompanies = (params) => request.get('/api/admin/companies/export', {
  params,
  responseType: 'blob'
})

// ==================== 职位管理 ====================

/** 职位列表 */
export const getAdminJobs = (params) => request.get('/api/admin/jobs', { params })

/** 职位详情 */
export const getAdminJobDetail = (id) => request.get(`/api/admin/jobs/${id}`)

/** 审核职位 */
export const auditJob = (id, status, reason = '') => request.put(`/api/admin/jobs/${id}/status`, { status, reason })

/** 删除职位 */
export const deleteAdminJob = (id) => request.delete(`/api/admin/jobs/${id}`)

/** 获取职位申请列表 */
export const getJobApplications = (id) => request.get(`/api/admin/jobs/${id}/applications`)

/** 批量导入职位 */
export const importJobs = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/jobs/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 导出职位列表 */
export const exportJobs = (params) => request.get('/api/admin/jobs/export', {
  params,
  responseType: 'blob'
})

// ==================== 申请管理 ====================

/** 申请列表 */
export const getAdminApplications = (params) => request.get('/api/admin/applications', { params })

/** 申请详情 */
export const getAdminApplicationDetail = (id) => request.get(`/api/admin/applications/${id}`)

/** 更新申请状态 */
export const updateApplicationStatus = (id, status, hrRemark = '') =>
  request.put(`/api/admin/applications/${id}/status`, { status, hrRemark })

/** 删除申请 */
export const deleteAdminApplication = (id) => request.delete(`/api/admin/applications/${id}`)

/** 导出申请列表 */
export const exportApplications = (params) => request.get('/api/admin/applications/export', {
  params,
  responseType: 'blob'
})

// ==================== 面试管理 ====================

/** 面试列表 */
export const getAdminInterviews = (params) => request.get('/api/admin/interviews', { params })

/** 面试详情 */
export const getAdminInterviewDetail = (id) => request.get(`/api/admin/interviews/${id}`)

/** 面试报告 */
export const getInterviewReport = (id) => request.get(`/api/admin/interviews/${id}/report`)

/** 删除面试记录 */
export const deleteAdminInterview = (id) => request.delete(`/api/admin/interviews/${id}`)

/** 导出面试列表 */
export const exportInterviews = (params) => request.get('/api/admin/interviews/export', {
  params,
  responseType: 'blob'
})

// ==================== 福利标签管理 ====================

/** 标签列表（含使用统计） */
export const getAdminBenefitTags = () => request.get('/api/admin/benefit-tags')

/** 新增标签 */
export const createBenefitTag = (data) => request.post('/api/admin/benefit-tags', data)

/** 编辑标签 */
export const updateBenefitTag = (id, data) => request.put(`/api/admin/benefit-tags/${id}`, data)

/** 删除标签 */
export const deleteBenefitTag = (id) => request.delete(`/api/admin/benefit-tags/${id}`)

/** 标签使用详情 */
export const getBenefitTagUsage = (id) => request.get(`/api/admin/benefit-tags/${id}/usage`)

// ==================== 职位分类管理 ====================

/** 分类树（含职位数统计） */
export const getAdminJobCategories = () => request.get('/api/admin/job-categories')

/** 新增分类 */
export const createJobCategory = (data) => request.post('/api/admin/job-categories', data)

/** 编辑分类 */
export const updateJobCategory = (id, data) => request.put(`/api/admin/job-categories/${id}`, data)

/** 删除分类 */
export const deleteJobCategory = (id) => request.delete(`/api/admin/job-categories/${id}`)

// ==================== 地区管理 ====================

/** 地区树（含关联统计） */
export const getAdminRegions = () => request.get('/api/admin/regions')

/** 新增地区 */
export const createRegion = (data) => request.post('/api/admin/regions', data)

/** 编辑地区 */
export const updateRegion = (id, data) => request.put(`/api/admin/regions/${id}`, data)

/** 删除地区 */
export const deleteRegion = (id) => request.delete(`/api/admin/regions/${id}`)
