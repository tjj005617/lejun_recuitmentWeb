<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  // 应用初始化时加载用户状态
  await userStore.loadUser()
})
</script>

<style>
#app {
  font-family: var(--font-family-base);
}

/* 消息通知弹窗样式 */
.notify-message {
  border-left: 4px solid #10b981 !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.15) !important;
  overflow: hidden;
}

.notify-message .el-notification__title {
  color: #059669 !important;
  font-weight: 600 !important;
}

.notify-message .el-notification__content {
  color: #334155 !important;
  font-size: 14px !important;
}

.notify-message .el-notification__icon {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  color: #fff !important;
  border-radius: 50% !important;
  width: 32px !important;
  height: 32px !important;
  font-size: 16px !important;
}

.notify-message .el-notification__closeBtn {
  color: #94a3b8 !important;
}

.notify-message .el-notification__closeBtn:hover {
  color: #10b981 !important;
}
</style>
