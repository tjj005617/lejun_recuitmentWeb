import request from './request'

// 获取分类树形结构
export const getCategoryTree = () => {
  return request.get('/category/tree')
}

// 获取分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}
