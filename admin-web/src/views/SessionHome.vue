<template>
  <section class="dashboard-page">
    <div class="page-heading">
      <div>
        <p class="section-label">商家端</p>
        <h1>工作台</h1>
      </div>
      <div class="heading-actions">
        <span>{{ todayText }}</span>
        <button class="refresh-button" type="button" :disabled="loading" @click="loadWorkspace">
          {{ loading ? '刷新中' : '刷新数据' }}
        </button>
      </div>
    </div>

    <div v-if="overviewError" class="error-panel" role="alert">
      <span>{{ overviewError }}</span>
      <button type="button" @click="loadOverview">重新加载</button>
    </div>

    <section class="dashboard-section today-section">
      <div class="section-heading">
        <div>
          <h2>今日数据</h2>
          <p>仅统计当前商户，截至当前时间</p>
        </div>
      </div>
      <div class="metric-grid" :class="{ 'is-loading': overviewLoading }">
        <article v-for="item in businessItems" :key="item.label" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.note }}</small>
        </article>
      </div>
    </section>

    <section class="dashboard-section overview-section">
      <div class="section-heading">
        <div>
          <h2>订单概览</h2>
          <p>当前商户全部订单的实时状态</p>
        </div>
        <span class="summary-count">共 {{ orderOverview.allOrders || 0 }} 单</span>
      </div>
      <div class="status-grid" :class="{ 'is-loading': overviewLoading }">
        <button
          v-for="item in orderStatusItems"
          :key="item.status"
          type="button"
          class="status-card"
          :class="{ active: selectedStatus === item.status && item.filterable }"
          :disabled="!item.filterable"
          @click="item.filterable && selectStatus(item.status)"
        >
          <strong>{{ item.count }}</strong>
          <span>{{ item.label }}</span>
          <small v-if="item.filterable">查看订单</small>
        </button>
      </div>
    </section>

    <section class="dashboard-section pending-section">
      <div class="section-heading pending-heading">
        <div>
          <h2>商户订单</h2>
          <p>展示待确认、制作中和已出餐订单，以及本店负责的菜品</p>
        </div>
        <div class="order-tabs" aria-label="待处理订单筛选">
          <button
            v-for="item in pendingTabs"
            :key="item.value"
            type="button"
            :class="{ active: selectedStatus === item.value }"
            @click="selectStatus(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div v-if="ordersLoading" class="state-panel">正在加载订单...</div>
      <div v-else-if="ordersError" class="state-panel state-error">
        <p>{{ ordersError }}</p>
        <button class="secondary-button" type="button" @click="loadOrders">重新加载</button>
      </div>
      <div v-else-if="orders.length === 0" class="state-panel">
        <p>{{ emptyOrderText }}</p>
      </div>
      <template v-else>
        <div class="order-list">
          <article v-for="order in orders" :key="order.id" class="order-card">
            <header class="order-header">
              <div class="order-identity">
                <span class="order-status" :class="`status-${order.merchantStatus}`">
                  {{ orderStatusText(order.merchantStatus) }}
                </span>
                <strong>订单 {{ order.number }}</strong>
              </div>
              <div class="order-meta">
                <span>{{ formatDate(order.orderTime) }}</span>
                <strong>本店 ¥ {{ formatMoney(order.merchantAmount) }}</strong>
              </div>
            </header>

            <div class="order-content">
              <div class="dish-list">
                <div v-for="dish in order.dishes" :key="dish.id" class="dish-row">
                  <img v-if="dish.image" :src="resolveImageUrl(dish.image)" :alt="dish.name" />
                  <span v-else class="dish-placeholder">无图</span>
                  <div class="dish-info">
                    <strong>{{ dish.name }}</strong>
                    <small>{{ dish.dishFlavor || '默认规格' }}</small>
                  </div>
                  <span class="dish-number">× {{ dish.number }}</span>
                  <span class="dish-price">¥ {{ formatMoney(Number(dish.amount || 0) * Number(dish.number || 0)) }}</span>
                </div>
              </div>

              <dl class="order-info">
                <div><dt>预计时间</dt><dd>{{ formatDate(order.orderDeliveryTime) }}</dd></div>
                <div><dt>顾客备注</dt><dd>{{ order.remark || '无' }}</dd></div>
                <div><dt>菜品数量</dt><dd>{{ dishCount(order.dishes) }} 份</dd></div>
              </dl>
            </div>
          </article>
        </div>

        <div class="pagination">
          <span>共 {{ total }} 单，第 {{ page }} / {{ pageCount }} 页</span>
          <div>
            <button class="secondary-button" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
            <button class="secondary-button" type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </template>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useOrderRefresh } from '../utils/order-refresh.js'
import {
  getBusinessDataApi,
  getOrderOverviewApi
} from '../api/workspace.js'
import { pageMerchantOrdersApi } from '../api/merchantOrder.js'

const pageSize = 6
const page = ref(1)
const total = ref(0)
const orders = ref([])
const selectedStatus = ref(1)
const overviewLoading = ref(false)
const ordersLoading = ref(false)
const overviewError = ref('')
const ordersError = ref('')
const businessData = reactive({ turnover: 0, orderCount: 0, orderCompletionRate: 0, unitPrice: 0 })
const orderOverview = reactive({
  pendingPreparationOrders: 0,
  preparingOrders: 0,
  completedPreparationOrders: 0,
  readyForPickupOrders: 0,
  deliveringOrders: 0,
  pendingReceiptOrders: 0,
  completedOrders: 0,
  cancelledOrders: 0,
  allOrders: 0
})

const loading = computed(() => overviewLoading.value || ordersLoading.value)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const todayText = computed(() => {
  const today = new Date()
  return `${today.getFullYear()}年${today.getMonth() + 1}月${today.getDate()}日`
})

const businessItems = computed(() => [
  { label: '今日营业额', value: `¥ ${formatMoney(businessData.turnover)}`, note: '已完成订单' },
  { label: '今日订单', value: `${businessData.orderCount || 0} 单`, note: '已支付业务订单' },
  { label: '订单完成率', value: formatRate(businessData.orderCompletionRate), note: '已完成 / 今日订单' },
  { label: '平均客单价', value: `¥ ${formatMoney(businessData.unitPrice)}`, note: '按已完成订单计算' }
])

const orderStatusItems = computed(() => [
  { label: '待确认', count: orderOverview.pendingPreparationOrders || 0, status: 1, filterable: true },
  { label: '制作中', count: orderOverview.preparingOrders || 0, status: 2, filterable: true },
  { label: '已出餐', count: orderOverview.completedPreparationOrders || 0, status: 3, filterable: true }
])

const pendingTabs = [
  { label: '待确认', value: 1 },
  { label: '制作中', value: 2 },
  { label: '已出餐', value: 3 }
]

const emptyOrderText = computed(() => {
  if (selectedStatus.value === 1) return '当前没有待确认订单'
  if (selectedStatus.value === 2) return '当前没有制作中订单'
  if (selectedStatus.value === 3) return '当前没有已出餐订单'
  return '当前没有订单'
})

function businessResult(response) {
  if (response.data?.code !== 200) {
    throw new Error(response.data?.message || '工作台数据加载失败')
  }
  return response.data.data || {}
}

async function loadOverview(silent = false) {
  if (overviewLoading.value) return
  if (!silent) overviewLoading.value = true
  overviewError.value = ''
  try {
    const [businessResponse, orderResponse] = await Promise.all([
      getBusinessDataApi(),
      getOrderOverviewApi()
    ])
    Object.assign(businessData, businessResult(businessResponse))
    Object.assign(orderOverview, businessResult(orderResponse))
  } catch (error) {
    overviewError.value = error.response?.data?.message || error.message || '工作台数据加载失败，请稍后重试'
  } finally {
    overviewLoading.value = false
  }
}

let orderRequestSequence = 0
async function loadOrders(silent = false) {
  if (silent === true && ordersLoading.value) return
  const sequence = ++orderRequestSequence
  if (silent !== true) ordersLoading.value = true
  ordersError.value = ''
  try {
    const params = { page: page.value, pageSize }
    if (selectedStatus.value !== '') {
      params.status = selectedStatus.value
    }
    const response = await pageMerchantOrdersApi(params)
    const result = businessResult(response)
    if (sequence !== orderRequestSequence) return
    total.value = Number(result.total || 0)
    orders.value = result.records || []
  } catch (error) {
    if (sequence !== orderRequestSequence) return
    total.value = 0
    orders.value = []
    ordersError.value = error.response?.data?.message || error.message || '待处理订单加载失败，请稍后重试'
  } finally {
    if (sequence === orderRequestSequence) ordersLoading.value = false
  }
}

function loadWorkspace() {
  loadOverview()
  loadOrders()
}

function selectStatus(status) {
  selectedStatus.value = status
  page.value = 1
  loadOrders()
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > pageCount.value || nextPage === page.value) {
    return
  }
  page.value = nextPage
  loadOrders()
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

function formatRate(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function orderStatusText(status) {
  if (status === 1) return '待确认'
  if (status === 2) return '制作中'
  if (status === 3) return '已出餐'
  return '待处理'
}

function dishCount(dishes) {
  return (dishes || []).reduce((totalNumber, dish) => totalNumber + Number(dish.number || 0), 0)
}

function resolveImageUrl(image) {
  if (/^https?:\/\//.test(image)) return image
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseUrl}${image.startsWith('/') ? image : `/${image}`}`
}

onMounted(loadWorkspace)
useOrderRefresh(() => Promise.all([loadOverview(true), loadOrders(true)]))
</script>

<style scoped>
.dashboard-page { max-width: 1240px; margin: 0 auto; }
.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 24px; }
.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; }
.heading-actions { display: flex; align-items: center; gap: 16px; color: #6f8192; font-size: 13px; }
button { font: inherit; }
.refresh-button, .secondary-button { min-height: 34px; padding: 0 14px; border: 1px solid #cbd8e2; border-radius: 3px; background: #fff; color: #1766a6; cursor: pointer; }
.refresh-button:disabled, .secondary-button:disabled { color: #aebbc5; border-color: #e4eaf0; cursor: not-allowed; }
.dashboard-section { margin-top: 20px; border: 1px solid #e4eaf0; background: #fff; }
.section-heading { min-height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 16px 20px; border-bottom: 1px solid #e4eaf0; }
h2 { color: #20364e; font-size: 16px; font-weight: 600; }
.section-heading p { margin-top: 6px; color: #7b8c9b; font-size: 12px; }
.summary-count { color: #6f8192; font-size: 12px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.metric-card { min-width: 0; padding: 22px 24px; border-right: 1px solid #edf1f4; }
.metric-card:last-child { border-right: 0; }
.metric-card span { display: block; color: #6f8192; font-size: 13px; }
.metric-card strong { display: block; margin-top: 13px; color: #173f68; font-size: 25px; font-weight: 600; }
.metric-card small { display: block; margin-top: 8px; color: #95a2ad; font-size: 11px; }
.status-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.status-card { min-height: 104px; display: flex; flex-direction: column; align-items: start; justify-content: center; padding: 15px 18px; border: 0; border-right: 1px solid #edf1f4; background: #fff; color: #20364e; text-align: left; }
.status-card:last-child { border-right: 0; }
.status-card:enabled { cursor: pointer; }
.status-card:enabled:hover, .status-card.active { background: #f2f7fb; }
.status-card.active { box-shadow: inset 0 -3px #1766a6; }
.status-card strong { color: #1766a6; font-size: 23px; font-weight: 600; }
.status-card span { margin-top: 7px; font-size: 12px; }
.status-card small { margin-top: 5px; color: #7690a5; font-size: 10px; }
.pending-heading { align-items: end; }
.order-tabs { display: flex; border: 1px solid #ccd8e2; }
.order-tabs button { min-width: 84px; height: 32px; padding: 0 12px; border: 0; border-right: 1px solid #ccd8e2; background: #fff; color: #506579; cursor: pointer; font-size: 12px; }
.order-tabs button:last-child { border-right: 0; }
.order-tabs button:hover, .order-tabs button.active { color: #1766a6; background: #edf5fb; }
.order-list { padding: 0 20px; }
.order-card { border-bottom: 1px solid #dfe7ee; }
.order-card:last-child { border-bottom: 0; }
.order-header { min-height: 58px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 10px 0; }
.order-identity, .order-meta { display: flex; align-items: center; gap: 12px; }
.order-identity strong { color: #20364e; font-size: 13px; }
.order-meta { color: #7b8c9b; font-size: 12px; }
.order-meta strong { color: #20364e; font-size: 14px; }
.order-status { padding: 4px 8px; font-size: 11px; }
.status-1 { color: #9a6b2f; background: #fff7e9; }
.status-2 { color: #1766a6; background: #edf5fb; }
.status-3 { color: #2f8a60; background: #edf8f2; }
.order-content { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 28px; padding: 0 0 18px; }
.dish-list { border: 1px solid #edf1f4; }
.dish-row { min-height: 58px; display: grid; grid-template-columns: 40px minmax(0, 1fr) 54px 82px; align-items: center; gap: 12px; padding: 9px 12px; border-bottom: 1px solid #edf1f4; }
.dish-row:last-child { border-bottom: 0; }
.dish-row img, .dish-placeholder { width: 40px; height: 40px; border: 1px solid #e4eaf0; object-fit: cover; }
.dish-placeholder { display: grid; place-items: center; background: #eef1f3; color: #94a2ae; font-size: 10px; }
.dish-info { min-width: 0; }
.dish-info strong { display: block; overflow: hidden; color: #20364e; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 600; }
.dish-info small { display: block; margin-top: 5px; color: #82919e; font-size: 11px; }
.dish-number { color: #5f7285; font-size: 12px; text-align: center; }
.dish-price { color: #20364e; font-size: 12px; text-align: right; }
.order-info { padding: 4px 0; }
.order-info div { display: grid; grid-template-columns: 66px 1fr; gap: 12px; padding: 7px 0; font-size: 12px; }
.order-info dt { color: #82919e; }
.order-info dd { margin: 0; color: #40566c; word-break: break-word; }
.state-panel { min-height: 160px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 28px; color: #6f8192; font-size: 13px; }
.state-error { color: #b5534e; }
.error-panel { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 18px; padding: 13px 18px; border: 1px solid #e2c8c8; background: #fff7f7; color: #a04444; font-size: 13px; }
.error-panel button { border: 0; background: transparent; color: #1766a6; cursor: pointer; font-weight: 600; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 15px 20px; border-top: 1px solid #e4eaf0; color: #6f8192; font-size: 12px; }
.pagination div { display: flex; gap: 8px; }
.is-loading { opacity: .55; }

@media (max-width: 1100px) {
  .status-grid { grid-template-columns: repeat(4, 1fr); }
  .status-card { border-bottom: 1px solid #edf1f4; }
}

@media (max-width: 800px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .metric-card:nth-child(2) { border-right: 0; }
  .metric-card:nth-child(-n + 2) { border-bottom: 1px solid #edf1f4; }
  .order-content { grid-template-columns: 1fr; gap: 10px; }
}

@media (max-width: 620px) {
  .page-heading, .pending-heading { align-items: start; flex-direction: column; }
  .heading-actions { width: 100%; justify-content: space-between; }
  h1 { font-size: 21px; }
  .metric-grid { grid-template-columns: 1fr; }
  .metric-card { border-right: 0; border-bottom: 1px solid #edf1f4; }
  .metric-card:last-child { border-bottom: 0; }
  .status-grid { grid-template-columns: repeat(2, 1fr); }
  .section-heading { padding: 15px 16px; }
  .pending-heading { gap: 14px; }
  .order-tabs { width: 100%; }
  .order-tabs button { flex: 1; min-width: 0; }
  .order-list { padding: 0 15px; }
  .order-header { align-items: start; flex-direction: column; gap: 8px; }
  .order-meta { width: 100%; justify-content: space-between; }
  .dish-row { grid-template-columns: 36px minmax(0, 1fr) 36px 66px; gap: 8px; padding: 8px; }
  .dish-row img, .dish-placeholder { width: 36px; height: 36px; }
  .pagination { align-items: start; flex-direction: column; gap: 12px; }
}
</style>
