import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'
import './assets/styles/tokens.css'
import './assets/styles/main.css'
import './assets/styles/element-overrides.css'

// ==================== 前端日志写文件（通过后端中转） ====================
;(function initFrontendLogger() {
  const logBuffer = []
  const flushInterval = 2000 // 每2秒批量发送
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'

  const formatTime = () => {
    const d = new Date()
    return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}.${String(d.getMilliseconds()).padStart(3,'0')}`
  }

  const flush = () => {
    if (logBuffer.length === 0) return
    const batch = logBuffer.splice(0)
    // 用 navigator.sendBeacon 不依赖 axios 拦截器（避免 token 过期干扰）
    try {
      const blob = new Blob([JSON.stringify(batch)], { type: 'application/json' })
      navigator.sendBeacon(`${apiBase}/log/batch`, blob)
    } catch {
      // sendBeacon 不支持 POST body 的场景，降级用 fetch
      fetch(`${apiBase}/log/batch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch),
        keepalive: true
      }).catch(() => {})
    }
  }

  // 拦截 console 方法
  for (const level of ['log', 'warn', 'error']) {
    const original = console[level]
    console[level] = function (...args) {
      original.apply(console, args)
      // 只记录 [Relay] [SSE] [WebRTC] [VideoCall] 相关日志
      const msg = args.map(a => (typeof a === 'object' ? JSON.stringify(a) : String(a))).join(' ')
      if (/\[Relay\]|\[SSE\]|\[WebRTC\]|\[VideoCall\]/.test(msg)) {
        logBuffer.push({ time: formatTime(), level, msg: msg.substring(0, 500) })
      }
    }
  }

  // 定时 flush + 页面关闭前 flush
  setInterval(flush, flushInterval)
  window.addEventListener('beforeunload', flush)
  // SPA 路由切换时也 flush
  router.afterEach(() => flush)
})()

// 请求拦截器 - 自动添加token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 后端返回401说明token过期，跳登录
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
