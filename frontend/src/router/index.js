import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 后台管理路由
  { path: '/admin/login', name: 'AdminLogin', component: () => import('../views/admin/AdminLogin.vue'), meta: { requiresAdmin: true } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/AdminDashboard.vue'), meta: { title: '数据概览' } },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/AdminUsers.vue'), meta: { title: '用户管理' } },
      { path: 'companies', name: 'AdminCompanies', component: () => import('../views/admin/AdminCompanies.vue'), meta: { title: '公司管理' } },
      { path: 'jobs', name: 'AdminJobs', component: () => import('../views/admin/AdminJobs.vue'), meta: { title: '职位管理' } },
      { path: 'applications', name: 'AdminApplications', component: () => import('../views/admin/AdminApplications.vue'), meta: { title: '申请管理' } },
      { path: 'interviews', name: 'AdminInterviews', component: () => import('../views/admin/AdminInterviews.vue'), meta: { title: '面试管理' } },
      { path: 'benefit-tags', name: 'AdminBenefitTags', component: () => import('../views/admin/AdminBenefitTags.vue'), meta: { title: '福利标签' } },
      { path: 'job-categories', name: 'AdminJobCategories', component: () => import('../views/admin/AdminJobCategories.vue'), meta: { title: '职位分类' } },
      { path: 'regions', name: 'AdminRegions', component: () => import('../views/admin/AdminRegions.vue'), meta: { title: '地区管理' } },
      { path: 'knowledge', name: 'AdminKnowledge', component: () => import('../views/admin/AdminKnowledge.vue'), meta: { title: '八股面试' } },
    ]
  },

  // 前台路由
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/jobs', name: 'JobSearch', component: () => import('../views/JobSearch.vue') },
  { path: '/companies', name: 'CompanySearch', component: () => import('../views/CompanySearch.vue') },
  { path: '/job/:id', name: 'JobDetail', component: () => import('../views/JobDetail.vue') },
  { path: '/company/:id', name: 'CompanyDetail', component: () => import('../views/CompanyDetail.vue') },
  { path: '/user', name: 'UserCenter', component: () => import('../views/UserCenter.vue'), meta: { requiresAuth: true } },
  { path: '/upload', name: 'ResumeUpload', component: () => import('../views/ResumeUpload.vue'), meta: { requiresAuth: true } },
  { path: '/interview/:id/immersive', name: 'ImmersiveInterview', component: () => import('../views/ImmersiveInterview.vue'), meta: { requiresAuth: true } },
  { path: '/interview/:id/choice', name: 'ChoiceInterview', component: () => import('../views/ChoiceInterview.vue'), meta: { requiresAuth: true } },
  { path: '/interview/:id', name: 'InterviewRoom', component: () => import('../views/InterviewRoom.vue'), meta: { requiresAuth: true } },
  { path: '/report/:id', name: 'ReportDetail', component: () => import('../views/ReportDetail.vue'), meta: { requiresAuth: true } },
  { path: '/messages', name: 'MessageCenter', component: () => import('../views/MessageCenter.vue'), meta: { requiresAuth: true } },
  { path: '/knowledge', name: 'KnowledgeGraph', component: () => import('../views/KnowledgeGraph.vue') },
  { path: '/knowledge/:categoryId', name: 'KnowledgeGraphCategory', component: () => import('../views/KnowledgeGraph.vue') },
  { path: '/hr', name: 'HRDashboard', component: () => import('../views/HRDashboard.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/hr/company', name: 'HRCompanyEdit', component: () => import('../views/hr/HRCompanyEdit.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/hr/jobs', name: 'HRJobList', component: () => import('../views/hr/HRJobList.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/hr/job/publish', name: 'HRJobPublish', component: () => import('../views/HRJobPublish.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/hr/job/:id/edit', name: 'HRJobEdit', component: () => import('../views/HRJobPublish.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/hr/applications', name: 'HRApplications', component: () => import('../views/hr/HRApplications.vue'), meta: { requiresAuth: true, requiresHR: true } },
  { path: '/job-match', name: 'JobMatch', component: () => import('../views/JobMatch.vue'), meta: { requiresAuth: true } },
  { path: '/asr-test', name: 'AsrTest', component: () => import('../views/AsrTest.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to) => {
  // 管理后台路由守卫
  if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
    if (!localStorage.getItem('admin_token')) {
      // 未登录时跳转到管理员登录页
      return { path: '/admin/login' }
    }
  }
  // 已登录访问登录页时，跳转到仪表盘
  if (to.path === '/admin/login' && localStorage.getItem('admin_token')) {
    return { path: '/admin/dashboard' }
  }
  // 前台路由守卫
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
})

export default router
