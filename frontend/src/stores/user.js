import { ref, computed } from 'vue'
import axios from 'axios'

const user = ref(null)
const token = ref(localStorage.getItem('token') || '')

export function useUserStore() {
  const isLoggedIn = computed(() => !!user.value)

  const login = async (username, password) => {
    const res = await axios.post('/api/user/login', { username, password })
    if (res.data.success) {
      user.value = res.data.data
      token.value = res.data.data.token

      // token存localStorage，有效期7天
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('userId', res.data.data.id)

      return true
    }
    throw new Error(res.data.message)
  }

  const register = async (username, password, nickname, roleType = 1) => {
    const res = await axios.post('/api/user/register', { username, password, nickname, roleType })
    if (res.data.success) {
      user.value = res.data.data
      token.value = res.data.data.token

      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('userId', res.data.data.id)

      return true
    }
    throw new Error(res.data.message)
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
  }

  const loadUser = async () => {
    const savedToken = localStorage.getItem('token')
    if (!savedToken) return

    token.value = savedToken

    const userId = localStorage.getItem('userId')
    if (!userId) return

    try {
      const res = await axios.get(`/api/user/${userId}`)
      if (res.data.success) {
        user.value = res.data.data
      }
    } catch {
      // 静默失败，不清除token
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    login,
    register,
    logout,
    loadUser
  }
}
