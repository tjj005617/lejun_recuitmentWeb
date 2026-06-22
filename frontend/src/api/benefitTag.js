import request from './request'

// 获取所有启用的福利标签（按类型过滤）
// @param type 'company'-公司福利 'job'-岗位福利
export const getBenefitTags = (type) => {
  return request.get('/benefit-tag/list', { params: { type } })
}

// 获取全部福利标签（管理员用，按类型过滤）
export const getAllBenefitTags = (type) => {
  return request.get('/benefit-tag/all', { params: { type } })
}

// 新增福利标签
export const addBenefitTag = (data) => {
  return request.post('/benefit-tag', data)
}

// 更新福利标签
export const updateBenefitTag = (id, data) => {
  return request.put(`/benefit-tag/${id}`, data)
}

// 删除福利标签
export const deleteBenefitTag = (id) => {
  return request.delete(`/benefit-tag/${id}`)
}
