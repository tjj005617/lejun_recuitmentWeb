import request from './request'

// 投递简历
export const applyJob = (data) => {
  return request.post('/application', data)
}

// 撤回申请
export const withdrawApplication = (id) => {
  return request.delete(`/application/${id}`)
}

// 我的投递记录
export const getMyApplications = () => {
  return request.get('/application/my')
}

// 获取职位的申请列表（HR）
export const getJobApplications = (jobId) => {
  return request.get(`/application/job/${jobId}`)
}

// 更新申请状态（HR）
export const updateApplicationStatus = (id, data) => {
  return request.put(`/application/${id}/status`, data)
}

// 获取公司所有投递（HR工作台）
export const getCompanyApplications = (companyId) => {
  return request.get(`/application/company/${companyId}`)
}
