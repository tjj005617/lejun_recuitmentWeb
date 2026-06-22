import request from './request'

// 获取公司详情
export const getCompanyDetail = (id) => {
  return request.get(`/company/${id}`)
}

// 获取公司职位列表（分页+筛选）
export const getCompanyJobs = (companyId, params) => {
  return request.get(`/company/${companyId}/jobs`, { params })
}

// 获取我的公司信息（HR）
export const getMyCompany = () => {
  return request.get('/company/my')
}

// 创建/更新公司信息（HR）
export const saveCompany = (data) => {
  return request.post('/company', data)
}

// 更新公司信息（HR）
export const updateCompany = (id, data) => {
  return request.put(`/company/${id}`, data)
}

// 按名称模糊搜索公司
export const searchCompanies = (keyword) => {
  return request.get('/company/search', { params: { keyword } })
}

// 分页获取公司列表
export const listCompanies = (params) => {
  return request.get('/company/list', { params })
}

// 获取热门公司
export const getHotCompanies = (limit = 6) => {
  return request.get('/company/hot', { params: { limit } })
}

// 增加公司浏览量
export const increaseCompanyViewCount = (id) => {
  return request.post(`/company/${id}/view`)
}

// 关注/取消关注公司
export const toggleCompanyFollow = (id) => {
  return request.post(`/company/${id}/follow`)
}

// 检查是否已关注
export const checkCompanyFollowed = (id) => {
  return request.get(`/company/${id}/followed`)
}

// 获取用户关注的公司列表
export const getUserFollowedCompanies = () => {
  return request.get('/company/followed')
}
