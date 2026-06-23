<template>
  <div class="admin-login">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">A</div>
        <h1>管理后台</h1>
        <p>AI 面试平台管理系统</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login({
      username: form.username,
      password: form.password
    })
    if (res.data.roleType === 3) {
      localStorage.setItem('admin_token', res.data.token)
      ElMessage.success('登录成功')
      router.push('/admin/dashboard')
    } else {
      ElMessage.error('该账号非管理员，无权登录后台')
    }
  } catch (e) {
    // request.js 已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('@/assets/image/admin-login-bg.webp') center/cover no-repeat;
}

.login-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border-top: 3px solid #10b981;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.35);
}

.login-header h1 {
  font-size: 22px;
  color: #1e293b;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-header p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}
</style>
