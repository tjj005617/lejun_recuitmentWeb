import request from './request'

// 用户登录
export const login = (data) => {
  return request.post('/user/login', data)
}

// 用户注册
export const register = (data) => {
  return request.post('/user/register', data)
}

// 获取用户信息
export const getUserInfo = (id) => {
  return request.get(`/user/${id}`)
}

// 更新用户信息
export const updateUserInfo = (id, data) => {
  return request.put(`/user/${id}`, data)
}

// 修改密码
export const changePassword = (id, data) => {
  return request.put(`/user/${id}/password`, data)
}

// 获取用户简历列表
export const getUserResumes = (id) => {
  return request.get(`/user/${id}/resumes`)
}

// 获取收藏列表
export const getFavorites = (id) => {
  return request.get(`/user/${id}/favorites`)
}

// 获取用户档案（扩展信息）
export const getUserProfile = (userId) => {
  return request.get(`/user-profile/${userId}`)
}

// 保存/更新用户档案
export const saveUserProfile = (userId, data) => {
  return request.put(`/user-profile/${userId}`, data)
}
