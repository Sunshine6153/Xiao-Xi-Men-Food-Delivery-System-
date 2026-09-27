import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import SessionHome from '../views/SessionHome.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import MerchantStore from '../views/MerchantStore.vue'
import MerchantDishes from '../views/MerchantDishes.vue'
import DishEditor from '../views/DishEditor.vue'
import AdminCategories from '../views/AdminCategories.vue'
import AdminMerchants from '../views/AdminMerchants.vue'
import AdminDishes from '../views/AdminDishes.vue'
import AdminUsers from '../views/AdminUsers.vue'
import AdminOverview from '../views/AdminOverview.vue'
import AdminRankings from '../views/AdminRankings.vue'
import MerchantOrders from '../views/MerchantOrders.vue'
import AdminOrders from '../views/AdminOrders.vue'
import { getIdentityApi } from '../api/auth.js'
import { clearSession } from '../utils/request.js'

const Statistics = () => import('../views/Statistics.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/session' },
    { path: '/login', name: 'Login', component: Login },
    { path: '/admin-pending', redirect: '/admin' },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        {
          path: '', name: 'AdminOverview', component: AdminOverview,
          meta: { title: '平台概览' }
        },
        {
          path: 'merchants', name: 'AdminMerchants', component: AdminMerchants,
          meta: { title: '商户管理' }
        },
        {
          path: 'dishes', name: 'AdminDishes', component: AdminDishes,
          meta: { title: '菜品列表', parentTitle: '菜品管理' }
        },
        {
          path: 'categories', name: 'AdminCategories', component: AdminCategories,
          meta: { title: '分类管理', parentTitle: '菜品管理' }
        },
        {
          path: 'orders', name: 'AdminOrders', component: AdminOrders,
          meta: { title: '订单管理' }
        },
        {
          path: 'users', name: 'AdminUsers', component: AdminUsers,
          meta: { title: '用户管理' }
        },
        {
          path: 'statistics', name: 'AdminStatistics', component: Statistics,
          props: { title: '数据统计' },
          meta: { title: '数据统计' }
        },
        {
          path: 'rankings', name: 'AdminRankings', component: AdminRankings,
          meta: { title: '销量排名' }
        }
      ]
    },
    {
      path: '/session',
      component: AdminLayout,
      meta: { requiresAuth: true, role: 'MERCHANT' },
      children: [
        { path: '', name: 'SessionHome', component: SessionHome, meta: { title: '工作台' } },
        {
          path: 'dishes',
          name: 'MerchantDishes',
          component: MerchantDishes,
          meta: { title: '菜品管理' }
        },
        {
          path: 'dishes/new',
          name: 'MerchantDishCreate',
          component: DishEditor,
          meta: { title: '新增菜品' }
        },
        {
          path: 'dishes/:id/edit',
          name: 'MerchantDishEdit',
          component: DishEditor,
          meta: { title: '编辑菜品' }
        },
        {
          path: 'orders',
          name: 'MerchantOrders',
          component: MerchantOrders,
          meta: { title: '订单管理' }
        },
        {
          path: 'analytics',
          name: 'MerchantAnalytics',
          component: Statistics,
          props: { title: '经营数据' },
          meta: { title: '经营数据' }
        },
        {
          path: 'store',
          name: 'MerchantStore',
          component: MerchantStore,
          meta: { title: '店铺资料' }
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  if (to.path !== '/login' && !to.meta.requiresAuth) return
  if (!localStorage.getItem('xiaoximen_token')) {
    return to.path === '/login' ? undefined : '/login'
  }

  try {
    const result = await getIdentityApi()
    const identity = result.data
    if (result.code !== 200 || !['ADMIN', 'MERCHANT'].includes(identity?.role) || !identity?.id) {
      clearSession()
      return '/login'
    }
    localStorage.setItem('xiaoximen_merchant_id', String(identity.id))
    localStorage.setItem('xiaoximen_role', identity.role)
    const home = identity.role === 'ADMIN' ? '/admin' : '/session'
    if (to.path === '/login' || (to.meta.role && to.meta.role !== identity.role)) return home
  } catch {
    clearSession()
    return '/login'
  }
})

export default router
