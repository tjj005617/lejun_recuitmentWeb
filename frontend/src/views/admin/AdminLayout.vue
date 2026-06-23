<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- Logo 区域 -->
      <div class="sidebar-logo" @click="router.push('/admin/dashboard')">
        <div class="logo-icon">
          <span class="logo-letter">A</span>
        </div>
        <transition name="fade">
          <span v-show="!sidebarCollapsed" class="logo-text">管理后台</span>
        </transition>
      </div>

      <!-- 管理员信息 -->
      <div class="sidebar-user">
        <div class="user-avatar" :class="{ small: sidebarCollapsed }">
          <el-avatar :size="sidebarCollapsed ? 32 : 36" style="background: linear-gradient(135deg, #10b981, #059669);">
            <el-icon :size="18"><User /></el-icon>
          </el-avatar>
        </div>
        <transition name="fade">
          <div v-show="!sidebarCollapsed" class="user-info">
            <div class="user-name">超级管理员</div>
            <div class="user-role">admin</div>
          </div>
        </transition>
      </div>

      <div class="sidebar-divider"></div>

      <!-- 菜单 -->
      <div class="sidebar-menu">
        <el-menu
          :default-active="activeMenu"
          :collapse="sidebarCollapsed"
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#10b981"
          :collapse-transition="false"
        >
          <!-- 核心功能 -->
          <div class="menu-group">
            <div v-show="!sidebarCollapsed" class="group-label">核心功能</div>
            <el-tooltip content="数据概览" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/dashboard" @click="go('/admin/dashboard')">
                <el-icon><DataBoard /></el-icon>
                <template #title>数据概览</template>
              </el-menu-item>
            </el-tooltip>
            <el-tooltip content="用户管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/users" @click="go('/admin/users')">
                <el-icon><User /></el-icon>
                <template #title>
                  <span>用户管理</span>
                  <span class="menu-badge">18</span>
                </template>
              </el-menu-item>
            </el-tooltip>
            <el-tooltip content="公司管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/companies" @click="go('/admin/companies')">
                <el-icon><OfficeBuilding /></el-icon>
                <template #title>公司管理</template>
              </el-menu-item>
            </el-tooltip>
          </div>

          <!-- 招聘管理 -->
          <div class="menu-group">
            <div v-show="!sidebarCollapsed" class="group-label">招聘管理</div>
            <el-tooltip content="职位管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/jobs" @click="go('/admin/jobs')">
                <el-icon><Suitcase /></el-icon>
                <template #title>
                  <span>职位管理</span>
                  <span class="menu-badge orange">12</span>
                </template>
              </el-menu-item>
            </el-tooltip>
            <el-tooltip content="申请管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/applications" @click="go('/admin/applications')">
                <el-icon><Document /></el-icon>
                <template #title>
                  <span>申请管理</span>
                  <span class="menu-badge red">6</span>
                </template>
              </el-menu-item>
            </el-tooltip>
            <el-tooltip content="面试管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/interviews" @click="go('/admin/interviews')">
                <el-icon><VideoCamera /></el-icon>
                <template #title>
                  <span>面试管理</span>
                  <span class="menu-badge purple">4</span>
                </template>
              </el-menu-item>
            </el-tooltip>
          </div>

          <!-- 八股面试 -->
          <div class="menu-group">
            <div v-show="!sidebarCollapsed" class="group-label">八股面试</div>
            <el-tooltip content="八股面试" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-menu-item index="/admin/knowledge" @click="go('/admin/knowledge')">
                <el-icon><Collection /></el-icon>
                <template #title>八股文档管理</template>
              </el-menu-item>
            </el-tooltip>
          </div>

          <!-- 系统配置 -->
          <div class="menu-group">
            <div v-show="!sidebarCollapsed" class="group-label">系统配置</div>
            <el-tooltip content="标签管理" placement="right" :disabled="!sidebarCollapsed" :show-after="300">
              <el-sub-menu index="tags">
                <template #title>
                  <el-icon><PriceTag /></el-icon>
                  <span>标签管理</span>
                </template>
                <el-menu-item index="/admin/benefit-tags" @click="go('/admin/benefit-tags')">福利标签</el-menu-item>
                <el-menu-item index="/admin/job-categories" @click="go('/admin/job-categories')">职位分类</el-menu-item>
                <el-menu-item index="/admin/regions" @click="go('/admin/regions')">地区管理</el-menu-item>
              </el-sub-menu>
            </el-tooltip>
          </div>
        </el-menu>
      </div>

      <!-- 底部信息 -->
      <div class="sidebar-footer">
        <div class="footer-divider"></div>
        <transition name="fade">
          <div v-show="!sidebarCollapsed" class="footer-info">
            <span class="footer-version">v1.0.0</span>
            <span class="footer-dot">·</span>
            <span class="footer-text">AI面试平台</span>
          </div>
        </transition>
        <div class="collapse-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon :size="18">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 主体 -->
    <div class="admin-main" :class="{ expanded: sidebarCollapsed }">
      <!-- 顶栏 -->
      <header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta.title">{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="通知" placement="bottom">
            <div class="header-icon-btn">
              <el-badge :value="5" :max="99">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
            </div>
          </el-tooltip>
          <el-tooltip content="全屏" placement="bottom">
            <div class="header-icon-btn" @click="toggleFullscreen">
              <el-icon :size="20"><FullScreen /></el-icon>
            </div>
          </el-tooltip>
          <el-dropdown @command="handleCommand">
            <span class="admin-avatar">
              <el-avatar :size="32" style="background: linear-gradient(135deg, #10b981, #059669);">
                <el-icon :size="16"><User /></el-icon>
              </el-avatar>
              <span class="admin-name">管理员</span>
              <el-icon class="avatar-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon> 系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="home">
                  <el-icon><Monitor /></el-icon> 返回前台
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataBoard, User, OfficeBuilding, Suitcase, Document,
  VideoCamera, PriceTag, Fold, Expand, ArrowDown,
  Bell, FullScreen, Setting, Monitor, SwitchButton, Collection
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const sidebarCollapsed = ref(false)

const go = (path) => {
  if (route.path !== path) {
    router.push(path)
  }
}

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 响应式：窗口变窄自动折叠
const handleResize = () => {
  if (window.innerWidth < 1024) {
    sidebarCollapsed.value = true
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('admin_token')
    ElMessage.success('已退出登录')
    router.push('/admin/login')
  } else if (cmd === 'home') {
    router.push('/')
  } else if (cmd === 'profile') {
    ElMessage.info('个人信息（开发中）')
  } else if (cmd === 'settings') {
    ElMessage.info('系统设置（开发中）')
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f1f5f9;
}

/* ===== 侧边栏 ===== */
.admin-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #0a3d2c 0%, #0f4a35 40%, #12593f 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  overflow: hidden;
}

.admin-sidebar.collapsed {
  width: 64px;
}

/* Logo 区域 */
.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);
}

.logo-letter {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
}

.logo-text {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

/* 用户信息 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  flex-shrink: 0;
}

.user-avatar.small {
  display: flex;
  justify-content: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 2px;
}

/* 分隔线 */
.sidebar-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 0 16px;
  flex-shrink: 0;
}

/* 菜单区域 */
.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

/* 菜单分组 */
.menu-group {
  margin-bottom: 4px;
}

.group-label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 12px 20px 6px;
  white-space: nowrap;
}

/* 菜单项 */
.admin-sidebar :deep(.el-menu) {
  border-right: none;
}

.admin-sidebar :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.admin-sidebar :deep(.el-menu-item.is-active) {
  background: rgba(16, 185, 129, 0.25) !important;
  color: #34d399 !important;
  font-weight: 500;
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.1);
}

.admin-sidebar :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.admin-sidebar :deep(.el-sub-menu .el-menu-item) {
  padding-left: 52px !important;
  height: 38px;
  line-height: 38px;
  font-size: 13px;
  margin: 1px 8px;
  border-radius: 6px;
}

.admin-sidebar :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  margin: 2px 8px;
  border-radius: 8px;
}

.admin-sidebar :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

/* 菜单角标 */
.menu-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  margin-left: 6px;
}

.menu-badge.orange {
  background: #f59e0b;
}

.menu-badge.purple {
  background: #8b5cf6;
}

/* 底部区域 */
.sidebar-footer {
  flex-shrink: 0;
  padding: 0 0 8px;
}

.footer-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 0 16px 8px;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 4px 0;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

.footer-dot {
  color: rgba(255, 255, 255, 0.15);
}

.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
  border-radius: 8px;
  margin: 4px 8px 0;
}

.collapse-toggle:hover {
  color: #34d399;
  background: rgba(255, 255, 255, 0.08);
}

/* ===== 主体 ===== */
.admin-main {
  flex: 1;
  margin-left: 220px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-main.expanded {
  margin-left: 64px;
}

/* 顶栏 */
.admin-header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.header-icon-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.admin-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.admin-avatar:hover {
  background: #f1f5f9;
}

.admin-name {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.avatar-arrow {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.2s;
}

/* 内容区 */
.admin-content {
  flex: 1;
  padding: 20px;
}

/* ===== 动画 ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-fade-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-fade-leave-active {
  transition: opacity 0.15s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .admin-sidebar {
    width: 64px;
  }

  .admin-sidebar .logo-text,
  .admin-sidebar .user-info,
  .admin-sidebar .group-label,
  .admin-sidebar .footer-info {
    display: none;
  }

  .admin-main {
    margin-left: 64px;
  }
}
</style>
