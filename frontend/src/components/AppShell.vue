<template>
  <div class="app-shell">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="navbar__inner">
        <!-- 移动端汉堡菜单按钮（左侧） -->
        <button class="navbar__hamburger" @click="isMobileMenuOpen = !isMobileMenuOpen">
          <span class="navbar__hamburger-line" :class="{ open: isMobileMenuOpen }"></span>
        </button>

        <!-- Logo -->
        <div class="navbar__logo" @click="$router.push('/')">
          <img src="@/assets/image/logo.webp" alt="logo" class="navbar__logo-img" />
          <span class="navbar__logo-text">俊乐招聘</span>
        </div>

        <!-- 导航项 -->
        <nav class="navbar__nav">
          <router-link to="/" class="navbar__link" :class="{ active: $route.path === '/' }">
            首页
          </router-link>
          <router-link to="/jobs" class="navbar__link" :class="{ active: $route.path === '/jobs' }">
            职位搜索
          </router-link>
          <router-link to="/companies" class="navbar__link" :class="{ active: $route.path === '/companies' }">
            公司搜索
          </router-link>
          <router-link v-if="isLoggedIn" to="/user" class="navbar__link" :class="{ active: $route.path === '/user' }">
            个人中心
          </router-link>
          <router-link v-if="isLoggedIn" to="/messages" class="navbar__link" :class="{ active: $route.path === '/messages' }">
            消息
            <span v-if="messageStore.unreadCount.value > 0" class="navbar__badge">{{ messageStore.unreadCount.value > 99 ? '99+' : messageStore.unreadCount.value }}</span>
          </router-link>
          <router-link v-if="isHR" to="/hr" class="navbar__link" :class="{ active: $route.path.startsWith('/hr') }">
            HR工作台
          </router-link>
        </nav>

        <!-- 右侧操作 -->
        <div class="navbar__actions">
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="navbar__user">
                <el-avatar :size="32" class="navbar__avatar">
                  {{ user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="navbar__username">{{ user?.nickname || user?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="user">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="isHR" command="hr">HR工作台</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" plain @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/login?tab=register')">注册</el-button>
          </template>
        </div>

        <!-- 移动端右侧快捷图标 -->
        <div class="navbar__mobile-actions">
          <router-link v-if="isLoggedIn" to="/messages" class="navbar__mobile-icon">
            <el-icon :size="22"><ChatDotRound /></el-icon>
            <span v-if="messageStore.unreadCount.value > 0" class="navbar__mobile-badge">{{ messageStore.unreadCount.value > 99 ? '99+' : messageStore.unreadCount.value }}</span>
          </router-link>
          <router-link v-if="isLoggedIn" to="/user" class="navbar__mobile-icon">
            <el-icon :size="22"><User /></el-icon>
          </router-link>
          <router-link v-if="!isLoggedIn" to="/login" class="navbar__mobile-icon">
            <el-icon :size="22"><User /></el-icon>
          </router-link>
        </div>
      </div>
    </header>

    <!-- 移动端菜单遮罩 -->
    <Transition name="fade">
      <div v-if="isMobileMenuOpen" class="mobile-menu-overlay" @click="isMobileMenuOpen = false"></div>
    </Transition>

    <!-- 移动端侧边菜单 -->
    <Transition name="slide-right">
      <div v-if="isMobileMenuOpen" class="mobile-menu">
        <div class="mobile-menu__header">
          <div class="mobile-menu__user" v-if="isLoggedIn">
            <el-avatar :size="48" class="mobile-menu__avatar">
              {{ user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="mobile-menu__user-info">
              <div class="mobile-menu__user-name">{{ user?.nickname || user?.username }}</div>
              <div class="mobile-menu__user-id">ID: {{ user?.id }}</div>
            </div>
          </div>
          <div class="mobile-menu__guest" v-else>
            <el-button type="primary" @click="handleMobileNav('/login')">登录</el-button>
            <el-button type="primary" plain @click="handleMobileNav('/login?tab=register')">注册</el-button>
          </div>
        </div>

        <nav class="mobile-menu__nav">
          <router-link to="/" class="mobile-menu__link" :class="{ active: $route.path === '/' }" @click="isMobileMenuOpen = false">
            <span class="mobile-menu__icon"> </span>
            首页
          </router-link>
          <router-link to="/jobs" class="mobile-menu__link" :class="{ active: $route.path === '/jobs' }" @click="isMobileMenuOpen = false">
            <span class="mobile-menu__icon"> </span>
            职位搜索
          </router-link>
          <router-link to="/companies" class="mobile-menu__link" :class="{ active: $route.path === '/companies' }" @click="isMobileMenuOpen = false">
            <span class="mobile-menu__icon"> </span>
            公司搜索
          </router-link>
          <template v-if="isLoggedIn">
            <router-link to="/user" class="mobile-menu__link" :class="{ active: $route.path === '/user' }" @click="isMobileMenuOpen = false">
              <span class="mobile-menu__icon"> </span>
              个人中心
            </router-link>
            <router-link to="/messages" class="mobile-menu__link" :class="{ active: $route.path === '/messages' }" @click="isMobileMenuOpen = false">
              <span class="mobile-menu__icon"> </span>
              消息
              <span v-if="messageStore.unreadCount.value > 0" class="mobile-menu__badge">{{ messageStore.unreadCount.value > 99 ? '99+' : messageStore.unreadCount.value }}</span>
            </router-link>
            <router-link v-if="isHR" to="/hr" class="mobile-menu__link" :class="{ active: $route.path.startsWith('/hr') }" @click="isMobileMenuOpen = false">
              <span class="mobile-menu__icon"> </span>
              HR工作台
            </router-link>
            <div class="mobile-menu__divider"></div>
            <div class="mobile-menu__link" @click="handleLogout">
              <span class="mobile-menu__icon"> ️</span>
              退出登录
            </div>
          </template>
        </nav>
      </div>
    </Transition>

    <!-- 主内容区 -->
    <main class="app-shell__main" :class="{ 'app-shell__main--bg': bgImage }">
      <slot />
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__brand">
          <div class="footer__logo">
            <img src="@/assets/image/logo.webp" alt="logo" class="footer__logo-img" />
            <span class="footer__logo-text">俊乐招聘</span>
          </div>
          <p class="footer__slogan">找到理想工作，从俊乐开始</p>
        </div>
        <div class="footer__links">
          <div class="footer__column">
            <h4>求职者</h4>
            <router-link to="/jobs">职位搜索</router-link>
            <router-link to="/user">我的投递</router-link>
            <router-link to="/user">简历管理</router-link>
          </div>
          <div class="footer__column">
            <h4>招聘方</h4>
            <router-link to="/hr">发布职位</router-link>
            <router-link to="/hr">管理申请</router-link>
            <router-link to="/hr">公司主页</router-link>
          </div>
          <div class="footer__column">
            <h4>关于我们</h4>
            <a href="#">使用条款</a>
            <a href="#">隐私政策</a>
            <a href="#">联系我们</a>
          </div>
        </div>
      </div>
      <div class="footer__bottom">
        <p>© 2026 俊乐招聘. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, ChatDotRound, User } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

defineProps({
  bgImage: { type: Boolean, default: true }
})

const router = useRouter()
const userStore = useUserStore()
const { user, isLoggedIn, logout } = userStore
const messageStore = useMessageStore()

const isHR = computed(() => user.value?.roleType === 2)
const isMobileMenuOpen = ref(false)

// 1. 先注册视频面试邀请回调（SSE连接前必须就绪，否则首访邀约会丢失）
messageStore.setVideoInviteCallback((data) => {
  // WebSocket 已收到 video_invite 并在处理中，SSE 不再重复弹窗
  if (messageStore.videoInviteHandledByWs.value) return

  const conversationId = data.conversationId
  const jobId = data.jobId
  const hrUserId = data.senderId

  ElMessageBox.confirm(
    'HR 邀请您进行视频面试，是否接受？',
    '📹 视频面试邀请',
    {
      confirmButtonText: '接受并前往',
      cancelButtonText: '拒绝',
      type: 'warning'
    }
  ).then(() => {
    // 接受：在用户手势中预热音频（移动端自动播放策略要求）
    try {
      // 已有暖场元素且正在播放，无需重复创建
      if (window.__warmupAudioEl && !window.__warmupAudioEl.paused) {
        window.__relayDebugLog?.('SSE：暖场元素已存在，跳过创建')
      } else {
        const el = document.createElement('audio')
        el.style.display = 'none'
        el.setAttribute('playsinline', '')
        el.setAttribute('webkit-playsinline', '')
        const buf = new ArrayBuffer(45)
        const view = new DataView(buf)
        view.setUint32(0, 0x46464952, true)
        view.setUint32(4, 37, true)
        view.setUint32(8, 0x45564157, true)
        view.setUint32(12, 0x20746d66, true)
        view.setUint32(16, 16, true)
        view.setUint16(20, 1, true)
        view.setUint16(22, 1, true)
        view.setUint32(24, 8000, true)
        view.setUint32(28, 8000, true)
        view.setUint16(32, 1, true)
        view.setUint16(34, 8, true)
        view.setUint32(36, 0x61746164, true)
        view.setUint32(40, 1, true)
        const blob = new Blob([buf], { type: 'audio/wav' })
        el.src = URL.createObjectURL(blob)
        el.loop = true
        document.body.appendChild(el)
        el.play().then(() => {
          el.volume = 0
          el.muted = false
          window.__warmupAudioEl = el
          window.__relayDebugLog?.('SSE音频预热OK')
        }).catch(e => {
          window.__relayDebugLog?.('SSE音频预热失败: ' + e.message)
        })
      }
      // 同时解锁 AudioContext（双重保险）
      try {
        const AudioCtx = window.AudioContext || window.webkitAudioContext
        if (AudioCtx && !window.__audioCtx) {
          const ctx = new AudioCtx()
          ctx.resume().then(() => {
            window.__audioCtx = ctx
          }).catch(() => {})
        } else if (window.__audioCtx && window.__audioCtx.state !== 'running') {
          window.__audioCtx.resume().catch(() => {})
        }
      } catch {}
    } catch {}
    // 跳转到聊天页，带 autoAcceptVideo 参数
    router.push({
      path: '/messages',
      query: { hrUserId, jobId, autoAcceptVideo: '1' }
    })
  }).catch(() => {
    // 拒绝：跳转到聊天页自动发送 reject（通过 WebSocket）
    router.push({
      path: '/messages',
      query: { hrUserId, jobId, autoRejectVideo: '1' }
    })
  })
})

// 2. 再连接SSE（回调已就绪，首访邀约不会丢失）
watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    messageStore.connectSSE()
    messageStore.loadConversations()
  } else {
    messageStore.disconnectSSE()
    messageStore.unreadCount.value = 0
    messageStore.conversations.value = []
  }
}, { immediate: true })

onMounted(() => {
  // 页面刷新时兜底：如果用户已登录但会话列表为空，立即加载
  if (isLoggedIn.value && (!messageStore.conversations.value || messageStore.conversations.value.length === 0)) {
    messageStore.loadConversations()
  }
})

// 不在 onUnmounted 清除回调：SSE 连接和回调都是模块级共享的，
// 新 AppShell 的 onMounted 会直接覆盖回调，无需手动清除。
// 如果清除，页面切换窗口期的 video_invite 事件会丢失。

const handleCommand = (command) => {
  switch (command) {
    case 'user':
      router.push('/user')
      break
    case 'hr':
      router.push('/hr')
      break
    case 'logout':
      logout()
      router.push('/')
      break
  }
}

const handleMobileNav = (path) => {
  isMobileMenuOpen.value = false
  router.push(path)
}

const handleLogout = () => {
  isMobileMenuOpen.value = false
  logout()
  router.push('/')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

/* 导航栏 */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e5e7eb;
}

.navbar__inner {
  padding: 0 40px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.navbar__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.navbar__logo-img {
  height: 32px;
  width: 32px;
  object-fit: contain;
  border-radius: 4px;
}

.navbar__logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #10b981;
}

.navbar__nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.navbar__link {
  padding: 8px 16px;
  font-size: 15px;
  color: #475569;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s;
  position: relative;
}

.navbar__link:hover {
  color: #10b981;
  background: #ecfdf5;
}

.navbar__link.active {
  color: #10b981;
  background: #ecfdf5;
  font-weight: 500;
}

.navbar__badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #dc2626;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.navbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.navbar__user:hover {
  background: #f1f5f9;
}

.navbar__avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-weight: 600;
}

.navbar__username {
  font-size: 14px;
  color: #0f172a;
}

/* 主内容区 */
.app-shell__main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.app-shell__main--bg {
  background: url('@/assets/image/background1.webp') no-repeat center center;
  background-size: cover;
  background-attachment: fixed;
}

/* 页脚 */
.footer {
  background: url('@/assets/image/footer-bg.webp') no-repeat center 30%;
  background-size: cover;
  position: relative;
  color: #fff;
  padding: 48px 24px 24px;
}

.footer::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  pointer-events: none;
}

.footer > * {
  position: relative;
  z-index: 1;
}

.footer__inner {
  display: flex;
  justify-content: space-between;
  gap: 48px;
  padding-bottom: 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.footer__brand {
  max-width: 280px;
}

.footer__logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer__logo-img {
  height: 36px;
  width: 36px;
  object-fit: contain;
  border-radius: 6px;
}

.footer__logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.footer__slogan {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.85);
}

.footer__links {
  display: flex;
  gap: 64px;
}

.footer__column h4 {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px;
}

.footer__column a {
  display: block;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 10px;
  transition: color 0.2s;
}

.footer__column a:hover {
  color: #fff;
}

.footer__bottom {
  padding-top: 24px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 汉堡菜单按钮 */
.navbar__hamburger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  order: -1;
}

.navbar__hamburger:hover {
  background: #f1f5f9;
}

.navbar__hamburger-line {
  display: block;
  width: 20px;
  height: 2px;
  background: #475569;
  position: relative;
  transition: all 0.3s;
}

.navbar__hamburger-line::before,
.navbar__hamburger-line::after {
  content: '';
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  background: #475569;
  transition: all 0.3s;
}

.navbar__hamburger-line::before {
  top: -6px;
}

.navbar__hamburger-line::after {
  bottom: -6px;
}

.navbar__hamburger-line.open {
  background: transparent;
}

.navbar__hamburger-line.open::before {
  top: 0;
  transform: rotate(45deg);
}

.navbar__hamburger-line.open::after {
  bottom: 0;
  transform: rotate(-45deg);
}

/* 移动端右侧快捷图标 */
.navbar__mobile-actions {
  display: none;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}

.navbar__mobile-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #475569;
  border-radius: 8px;
  transition: all 0.2s;
}

.navbar__mobile-icon:hover {
  background: #f1f5f9;
  color: #10b981;
}

.navbar__mobile-badge {
  position: absolute;
  top: 2px;
  right: 0;
  background: #dc2626;
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 8px;
  min-width: 14px;
  text-align: center;
}

/* 移动端菜单遮罩 */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
}

/* 移动端侧边菜单 */
.mobile-menu {
  position: fixed;
  top: 0;
  left: 0;
  width: 200px;
  height: 100vh;
  background: #fff;
  z-index: 201;
  overflow-y: auto;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
}

.mobile-menu__header {
  padding: 20px 16px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.mobile-menu__user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-menu__avatar {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 600;
}

.mobile-menu__user-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 2px;
}

.mobile-menu__user-id {
  font-size: 12px;
  opacity: 0.8;
}

.mobile-menu__guest {
  display: flex;
  gap: 12px;
}

.mobile-menu__guest .el-button {
  flex: 1;
}

.mobile-menu__nav {
  padding: 12px 0;
}

.mobile-menu__link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  font-size: 15px;
  color: #475569;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-menu__link:hover {
  background: #f8fafc;
  color: #10b981;
}

.mobile-menu__link.active {
  background: #ecfdf5;
  color: #10b981;
  font-weight: 500;
}

.mobile-menu__icon {
  font-size: 18px;
}

.mobile-menu__badge {
  margin-left: auto;
  background: #dc2626;
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.mobile-menu__divider {
  height: 1px;
  background: #e5e7eb;
  margin: 8px 16px;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-right-enter-active {
  transition: transform 0.3s ease;
}
.slide-right-leave-active {
  transition: transform 0.3s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(-100%);
}

/* 移动端适配（必须放最后，覆盖前面的默认样式） */
@media (max-width: 768px) {
  .navbar__inner {
    padding: 0 8px;
    height: 52px;
  }

  .navbar__nav {
    display: none;
  }

  .navbar__actions {
    display: none;
  }

  .navbar__hamburger {
    display: flex;
    flex-shrink: 0;
    width: 36px;
    height: 36px;
  }

  .navbar__hamburger-line {
    width: 18px;
  }

  .navbar__hamburger-line::before {
    top: -5px;
  }

  .navbar__hamburger-line::after {
    bottom: -5px;
  }

  .navbar__mobile-actions {
    display: flex;
    flex-shrink: 0;
    gap: 8px;
  }

  .navbar__mobile-icon {
    width: 32px;
    height: 32px;
  }

  .navbar__logo {
    flex: 1;
    justify-content: center;
  }

  .navbar__logo-img {
    height: 26px;
    width: 26px;
  }

  .navbar__logo-text {
    font-size: 16px;
  }

  .footer {
    padding: 32px 16px 16px;
  }

  .footer__inner {
    flex-direction: column;
    gap: 24px;
  }

  .footer__links {
    gap: 24px;
    flex-wrap: wrap;
  }
}
</style>
