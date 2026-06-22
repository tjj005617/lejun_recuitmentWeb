import request from './request'

// 获取省市区树形结构
export const getRegionTree = () => {
  return request.get('/region/tree')
}

// 根据父级ID获取下级区域
export const getRegionChildren = (parentId) => {
  return request.get(`/region/children/${parentId}`)
}

// 根据区级ID获取完整路径ID数组 [省ID, 市ID, 区ID]
export const getRegionPath = (regionId) => {
  return request.get(`/region/path/${regionId}`)
}
