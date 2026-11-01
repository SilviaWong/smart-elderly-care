import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '用户登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/default/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运营工作台' }
      },
      {
        path: 'elder',
        name: 'Elder',
        component: () => import('@/views/elder/index.vue'),
        meta: { title: '老人档案' }
      },
      {
        path: 'elder/detail/:id',
        name: 'ElderDetail',
        component: () => import('@/views/elder/detail.vue'),
        meta: { title: '老人全景档案' }
      },
      {
        path: 'admission',
        name: 'Admission',
        component: () => import('@/views/admission/index.vue'),
        meta: { title: '入住与床位' }
      },
      {
        path: 'serviceorder',
        name: 'ServiceOrder',
        component: () => import('@/views/serviceorder/index.vue'),
        meta: { title: '服务工单' }
      },
      {
        path: 'health',
        name: 'Health',
        component: () => import('@/views/health/index.vue'),
        meta: { title: '健康管理' }
      },
      {
        path: 'care',
        name: 'Care',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '护理计划' }
      },
      {
        path: 'dining',
        name: 'Dining',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '餐饮管理' }
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '财务管理' }
      },
      {
        path: 'iot',
        name: 'IoT',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '设备物联' }
      },
      {
        path: 'risk',
        name: 'Risk',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '安全风险' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '统计报表' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404 页面未找到', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录校验与标题更新
router.beforeEach((to, _from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 智慧养老管理系统`
  }

  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next({ path: '/login' })
  } else {
    next()
  }
})

export default router
