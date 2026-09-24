import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/device'
  },
  {
    path: '/device',
    name: 'Device',
    component: () => import('@/views/Home.vue'),
    meta: { title: '饮水机管理' }
  },
  {
    path: '/inspection',
    name: 'Inspection',
    component: () => import('@/views/InspectionView.vue'),
    meta: { title: '水质抽检' }
  },
  {
    path: '/inspection/batch/:id',
    name: 'BatchDetail',
    component: () => import('@/views/BatchDetailView.vue'),
    meta: { title: '抽检批次详情' }
  },
  {
    path: '/threshold',
    name: 'Threshold',
    component: () => import('@/views/ThresholdView.vue'),
    meta: { title: '水质阈值管理' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/device'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + ' - 园区公共饮水机管理系统'
  }
  next()
})

export default router
