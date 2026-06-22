import request from './request'

// 获取职位详情
export const getJobDetail = (id) => {
  return request.get(`/job/${id}`)
}

// 搜索职位
export const searchJobs = (params) => {
  return request.get('/job/list', { params })
}

// 获取热门职位
export const getHotJobs = () => {
  return request.get('/job/hot')
}

// 获取推荐职位
export const getRecommendJobs = () => {
  return request.get('/job/recommend')
}

// 获取公司下的职位列表（HR，分页+条件搜索）
export const getMyJobs = (params) => {
  return request.get('/job/my', { params })
}

// 发布职位（HR）
export const createJob = (data) => {
  return request.post('/job', data)
}

// 编辑职位（HR）
export const updateJob = (id, data) => {
  return request.put(`/job/${id}`, data)
}

// 删除职位（HR）
export const deleteJob = (id) => {
  return request.delete(`/job/${id}`)
}

// 认领职位（HR）
export const claimJob = (id) => {
  return request.post(`/job/${id}/claim`)
}

// 收藏/取消收藏职位
export const toggleFavorite = (jobId) => {
  return request.post(`/job/${jobId}/favorite`)
}

// 查询是否已收藏
export const checkFavorite = (jobId) => {
  return request.get(`/job/${jobId}/favorite`)
}

// 获取我的收藏列表
export const getMyFavorites = () => {
  return request.get('/job/favorites')
}

// 猜你喜欢（随机推荐）
export const getGuessYouLike = (currentId) => {
  return request.get('/job/guess', { params: { currentId } })
}
