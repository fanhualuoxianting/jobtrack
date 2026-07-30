import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guestOnly: true, title: '注册' },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { requiresAuth: true, title: '首页' },
      },
      {
        path: 'companies',
        name: 'Companies',
        component: () => import('@/views/companies/CompanyListView.vue'),
        meta: { requiresAuth: true, title: '公司管理' },
      },
      {
        path: 'positions',
        name: 'Positions',
        component: () => import('@/views/positions/PositionListView.vue'),
        meta: { requiresAuth: true, title: '岗位管理' },
      },
      {
        path: 'resumes',
        name: 'Resumes',
        component: () => import('@/views/resumes/ResumeListView.vue'),
        meta: { requiresAuth: true, title: '简历管理' },
      },
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('@/views/applications/ApplicationListView.vue'),
        meta: { requiresAuth: true, title: '投递管理' },
      },
      {
        path: 'settings/security',
        name: 'SecuritySettings',
        component: () => import('@/views/settings/SecurityView.vue'),
        meta: { requiresAuth: true, title: '安全设置' },
      },
    ],
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/errors/NotFoundView.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 全局守卫：
 * 1. 首次导航前先恢复会话（Refresh Cookie -> Access Token），避免登录态闪烁；
 * 2. 未登录访问受保护页跳登录并记录 redirect；
 * 3. 已登录访问登录/注册页跳首页。
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.sessionRestored) {
    await authStore.restoreSession()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { path: '/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · JobTrack` : 'JobTrack'
})

export default router
