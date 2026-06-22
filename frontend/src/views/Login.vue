<template>
  <div class="login-page">
    <!-- 左侧背景 -->
    <div class="login-left">
      <div class="login-brand">
        <h1 class="brand-title">俊乐招聘</h1>
        <p class="brand-subtitle">找到理想工作，从俊乐开始</p>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">登录您的账户</p>
        </div>

        <el-tabs v-model="activeTab" class="login-tabs">
          <!-- 登录 -->
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" @submit.prevent="handleLogin" label-position="top" class="login-form">
              <el-form-item label="用户名">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  prefix-icon="User"
                />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  prefix-icon="Lock"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <div class="login-options">
                <span class="token-tip">登录状态有效期7天</span>
              </div>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <!-- 注册 -->
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" @submit.prevent="handleRegister" label-position="top" class="login-form">
              <el-form-item label="我是">
                <el-radio-group v-model="registerForm.roleType" class="role-radio-group">
                  <el-radio-button :value="1">
                    <span class="role-icon">👤</span>
                    <span>求职者</span>
                  </el-radio-button>
                  <el-radio-button :value="2">
                    <span class="role-icon">🏢</span>
                    <span>面试官/HR</span>
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="用户名">
                <el-input
                  v-model="registerForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  prefix-icon="User"
                />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="请输入昵称（选填）"
                  size="large"
                  prefix-icon="UserFilled"
                />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  prefix-icon="Lock"
                />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  size="large"
                  show-password
                  prefix-icon="Lock"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', nickname: '', password: '', confirmPassword: '', roleType: 1 })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm.value.username, loginForm.value.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  loading.value = true
  try {
    await userStore.register(registerForm.value.username, registerForm.value.password, registerForm.value.nickname, registerForm.value.roleType)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f5f6f8;
}

/* 左侧背景 */
.login-left {
  flex: 1;
  background: url('@/assets/image/background1.webp') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 48px;
}

.login-brand {
  color: #fff;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 8px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.brand-subtitle {
  font-size: 18px;
  margin: 0;
  opacity: 0.9;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

/* 右侧表单 */
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 48px;
}

.login-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.login-header {
  margin-bottom: 32px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #334155;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.token-tip {
  font-size: 13px;
  color: #94a3b8;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
}

.role-radio-group {
  width: 100%;
  display: flex;
  gap: 12px;
}

.role-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.role-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border-radius: 8px;
}

.role-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #ecfdf5;
  border-color: #10b981;
  color: #10b981;
}

.role-icon {
  font-size: 20px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-left {
    min-height: 200px;
    padding: 32px;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-subtitle {
    font-size: 15px;
  }

  .login-right {
    width: 100%;
    padding: 32px;
  }
}
</style>
