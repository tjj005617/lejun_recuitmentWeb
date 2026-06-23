import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 管理后台接口使用 admin_token，其他接口使用 token
    // config.url 可能带 /api 前缀（baseURL 拼接前）也可能不带，两种都检测
    const isAdminApi = config.url && (config.url.startsWith('/api/admin') || config.url.startsWith('/admin'))
    const token = isAdminApi
      ? localStorage.getItem('admin_token')
      : localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.success === false) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    const status = error.response?.status
    // 401 未授权（退出登录后 token 失效），静默处理，不弹错误框
    if (status === 401) {
      // 管理后台 token 失效，清除并跳转登录页
      const url = error.config?.url || ''
      if (url.startsWith('/api/admin') || url.startsWith('/admin')) {
        localStorage.removeItem('admin_token')
        window.location.href = '/admin/login'
      }
    } else if (status === 403) {
      ElMessage.error('无权限访问')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
