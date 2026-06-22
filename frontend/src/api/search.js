import request from './request'

// ES职位搜索
export const searchJobsByES = (params) => {
  return request.get('/search/jobs', { params })
}

// ES公司搜索
export const searchCompaniesByES = (params) => {
  return request.get('/search/companies', { params })
}

// 全量同步（开发/测试用）
export const syncAll = () => {
  return request.post('/search/sync')
}

// 同步单个职位
export const syncJob = (jobId) => {
  return request.post(`/search/sync/job/${jobId}`)
}

// 同步单个公司
export const syncCompany = (companyId) => {
  return request.post(`/search/sync/company/${companyId}`)
}
