<template>
  <div class="admin-shell">
    <aside class="sidebar" :class="{ 'sidebar-open': sidebarOpen }">
      <div class="brand">
        <span class="brand-name">小西门管理端</span>
        <span class="brand-role">{{ roleLabel }}</span>
      </div>

      <nav class="nav-list" :aria-label="`${roleLabel}导航`">
        <template v-for="item in menu" :key="item.path || item.label">
          <div v-if="item.children" class="nav-group">
            <button
              class="nav-item nav-group-toggle"
              :class="{ 'nav-group-current': item.children.some(isItemActive) }"
              type="button"
              :aria-expanded="!!expandedGroups[item.id]"
              :aria-controls="`nav-${item.id}`"
              @click="expandedGroups[item.id] = !expandedGroups[item.id]"
            >
              <span>{{ item.label }}</span>
              <span class="nav-chevron" :class="{ 'nav-chevron-open': expandedGroups[item.id] }" aria-hidden="true"></span>
            </button>
            <div v-show="expandedGroups[item.id]" :id="`nav-${item.id}`" class="nav-group-children">
              <RouterLink
                v-for="child in item.children"
                :key="child.path"
                :to="child.path"
                class="nav-item nav-subitem"
                :class="{ 'nav-item-active': isItemActive(child) }"
                @click="sidebarOpen = false"
              >
                <span>{{ child.label }}</span>
                <span v-if="child.pending" class="nav-status">待接入</span>
              </RouterLink>
            </div>
          </div>
          <RouterLink
            v-else
            :to="item.path"
            class="nav-item"
            :class="{ 'nav-item-active': isItemActive(item) }"
            @click="sidebarOpen = false"
          >
            <span>{{ item.label }}</span>
            <span v-if="item.pending" class="nav-status">待接入</span>
          </RouterLink>
        </template>
      </nav>
    </aside>

    <button
      v-if="sidebarOpen"
      class="sidebar-mask"
      type="button"
      aria-label="关闭导航"
      @click="sidebarOpen = false"
    />

    <div class="shell-main">
      <header class="topbar">
        <button class="menu-toggle" type="button" aria-label="打开导航" @click="sidebarOpen = true">
          <span></span><span></span><span></span>
        </button>
        <div class="breadcrumb">
          <span>{{ roleLabel }}</span>
          <template v-if="route.meta.parentTitle">
            <span class="breadcrumb-divider">/</span>
            <span>{{ route.meta.parentTitle }}</span>
          </template>
          <span class="breadcrumb-divider">/</span>
          <strong>{{ route.meta.title || (isAdmin ? '平台概览' : '工作台') }}</strong>
        </div>
        <div class="account-area">
          <span class="account-name">{{ merchantName || (isAdmin ? '管理员' : '商户') }}</span>
          <button class="logout-button" type="button" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="workspace">
        <div v-if="notification" class="order-notification" role="status">
          <span>{{ notification }}</span>
          <button type="button" @click="notification = ''" aria-label="关闭通知">×</button>
        </div>
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logoutApi } from '../api/auth.js'
import { clearSession } from '../utils/request.js'
import { startOrderNotifications } from '../utils/order-notifications.js'

const route = useRoute()
const router = useRouter()
const sidebarOpen = ref(false)
const expandedGroups = ref({})
const merchantName = ref(localStorage.getItem('xiaoximen_merchant_name'))
const notification = ref('')
let stopNotifications
onMounted(() => {
  stopNotifications = startOrderNotifications(message => { notification.value = message.content || '订单状态已更新' })
})
onUnmounted(() => stopNotifications?.())
function refreshMerchantName() {
  merchantName.value = localStorage.getItem('xiaoximen_merchant_name')
}
onMounted(() => window.addEventListener('xiaoximen-merchant-profile-updated', refreshMerchantName))
onUnmounted(() => window.removeEventListener('xiaoximen-merchant-profile-updated', refreshMerchantName))
const isAdmin = computed(() => route.meta.role === 'ADMIN')
const roleLabel = computed(() => isAdmin.value ? '系统管理员' : '商家端')

const merchantMenu = [
  { label: '工作台', path: '/session', pending: false },
  { label: '菜品管理', path: '/session/dishes', pending: false },
  { label: '订单管理', path: '/session/orders', pending: false },
  { label: '经营数据', path: '/session/analytics', pending: false },
  { label: '店铺资料', path: '/session/store', pending: false }
]

const adminMenu = [
  { label: '平台概览', path: '/admin', pending: false },
  { label: '商户管理', path: '/admin/merchants', pending: false },
  { id: 'dish-management', label: '菜品管理', children: [
    { label: '菜品列表', path: '/admin/dishes', pending: false },
    { label: '分类管理', path: '/admin/categories', pending: false }
  ] },
  { label: '订单管理', path: '/admin/orders', pending: false },
  { label: '用户管理', path: '/admin/users', pending: false },
  { label: '数据统计', path: '/admin/statistics', pending: false },
  { label: '销量排名', path: '/admin/rankings', pending: false }
]

const menu = computed(() => isAdmin.value ? adminMenu : merchantMenu)

// 直接进入子页面时自动展开，当前页面仍允许手动收起菜单。
watch(() => route.path, () => {
  for (const item of menu.value) {
    if (item.children?.some(isItemActive)) expandedGroups.value[item.id] = true
  }
}, { immediate: true })

function isItemActive(item) {
  if (item.path === '/session/dishes') return route.path.startsWith('/session/dishes')
  return route.path === item.path
}

async function handleLogout() {
  try {
    await logoutApi()
  } finally {
    clearSession()
    router.replace('/login')
  }
}
</script>

<style scoped>
.admin-shell {
  min-height: 100%;
  display: flex;
  background: #f4f7fa;
  color: #20364e;
}
.order-notification { display: flex; justify-content: space-between; gap: 16px; padding: 12px 16px; margin-bottom: 18px; border: 1px solid #c8dfee; background: #edf6fc; color: #1766a6; font-size: 14px; }
.order-notification button { border: 0; background: transparent; color: inherit; cursor: pointer; font-size: 20px; }

.sidebar {
  position: fixed;
  z-index: 20;
  inset: 0 auto 0 0;
  width: 216px;
  background: #123a63;
  color: #d7e5f2;
}

.brand {
  height: 76px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.brand-name { color: #fff; font-size: 17px; font-weight: 700; }
.brand-role { margin-top: 6px; color: #91b5d2; font-size: 12px; }
.nav-list { padding: 18px 10px; }
.nav-group { margin-bottom: 4px; }

.nav-item {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  padding: 0 14px;
  border-left: 3px solid transparent;
  color: #d7e5f2;
  font-size: 14px;
  text-decoration: none;
}

.nav-item:hover { background: rgba(255, 255, 255, 0.08); color: #fff; }
.nav-item-active { border-left-color: #83c4ee; background: #e7f1fa; color: #1766a6; font-weight: 600; }
.nav-status { color: #91a8bc; font-size: 11px; font-weight: 400; }
.nav-item-active .nav-status { color: #5f809b; }
.nav-group-toggle { width: 100%; border-top: 0; border-right: 0; border-bottom: 0; background: transparent; text-align: left; font-family: inherit; cursor: pointer; }
.nav-group-toggle:focus-visible { outline: 2px solid #83c4ee; outline-offset: -2px; }
.nav-group-current { color: #fff; font-weight: 600; }
.nav-subitem { min-height: 40px; padding-left: 32px; font-size: 13px; }
.nav-chevron { width: 7px; height: 7px; margin-right: 3px; border-right: 1.5px solid currentColor; border-bottom: 1.5px solid currentColor; transform: rotate(-45deg); transition: transform 160ms ease; }
.nav-chevron-open { transform: rotate(45deg); }

.shell-main { width: 100%; min-width: 0; margin-left: 216px; }
.topbar { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 32px; border-bottom: 1px solid #e4eaf0; background: #fff; }
.breadcrumb { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #6f8192; }
.breadcrumb strong { color: #20364e; font-weight: 600; }
.breadcrumb-divider { color: #b5c1ca; }
.account-area { display: flex; align-items: center; gap: 20px; font-size: 13px; }
.account-name { color: #20364e; }
.logout-button { padding: 0; border: 0; background: transparent; color: #6f8192; cursor: pointer; font: inherit; }
.logout-button:hover { color: #1766a6; }
.workspace { width: min(100%, 1440px); min-height: calc(100vh - 64px); margin: 0 auto; padding: 32px; }
.menu-toggle, .sidebar-mask { display: none; }

@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); transition: transform 160ms ease; box-shadow: 8px 0 24px rgba(18, 58, 99, 0.15); }
  .sidebar-open { transform: translateX(0); }
  .sidebar-mask { position: fixed; z-index: 10; inset: 0; display: block; border: 0; background: rgba(32, 54, 78, 0.28); }
  .shell-main { margin-left: 0; }
  .menu-toggle { width: 32px; height: 32px; display: inline-flex; flex-direction: column; justify-content: center; gap: 4px; padding: 7px; border: 1px solid #d7e1e9; background: #fff; cursor: pointer; }
  .menu-toggle span { display: block; height: 2px; background: #1766a6; }
  .topbar { padding: 0 20px; gap: 16px; }
  .breadcrumb { flex: 1; }
  .workspace { padding: 24px; }
}

@media (max-width: 560px) {
  .topbar { height: 58px; padding: 0 14px; }
  .account-name { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .account-area { gap: 12px; }
  .workspace { min-height: calc(100vh - 58px); padding: 18px 14px; }
}
</style>
