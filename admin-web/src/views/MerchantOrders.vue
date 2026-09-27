<template>
  <section class="order-page">
    <div class="page-heading">
      <div><p class="section-label">订单管理</p><h1>本店订单</h1></div>
      <span class="scope-note">只显示当前商户负责的菜品</span>
    </div>

    <section class="filter-panel">
      <div class="status-tabs" aria-label="订单状态筛选">
        <button v-for="item in statusOptions" :key="item.value" type="button" :class="{ active: filters.status === item.value }" @click="selectStatus(item.value)">{{ item.label }}</button>
      </div>
      <form class="search-form" @submit.prevent="applyFilters">
        <input v-model.trim="filters.number" type="search" placeholder="输入订单号查询" />
        <button class="primary-button" type="submit">查询</button>
        <button class="secondary-button" type="button" @click="resetFilters">重置</button>
      </form>
    </section>

    <section class="list-panel">
      <div class="panel-heading"><h2>订单列表</h2><span v-if="!loading && !errorMessage">共 {{ total }} 单</span></div>
      <div v-if="actionMessage" class="action-message" :class="actionType">{{ actionMessage }}</div>
      <div v-if="loading" class="state-panel">正在加载订单...</div>
      <div v-else-if="errorMessage" class="state-panel state-error"><p>{{ errorMessage }}</p><button class="secondary-button" type="button" @click="loadOrders">重新加载</button></div>
      <div v-else-if="orders.length === 0" class="state-panel"><p>暂无符合条件的订单</p><button class="secondary-button" type="button" @click="resetFilters">清除筛选</button></div>
      <template v-else>
        <div class="order-list">
          <article v-for="order in orders" :key="order.id" class="order-card">
            <header class="order-header">
              <div><span class="status" :class="`status-${order.merchantStatus}`">{{ statusText(order.merchantStatus) }}</span><strong>订单 {{ order.number }}</strong></div>
              <span>{{ formatDate(order.orderTime) }}</span>
            </header>
            <div class="order-body">
              <div class="dish-summary">
                <div v-for="dish in order.dishes" :key="dish.id" class="dish-item">
                  <img v-if="dish.image" :src="resolveImageUrl(dish.image)" :alt="dish.name" />
                  <span v-else class="image-placeholder">无图</span>
                  <div><strong>{{ dish.name }}</strong><small>{{ dish.dishFlavor || '默认规格' }}</small></div>
                  <span>× {{ dish.number }}</span>
                </div>
              </div>
              <dl class="order-summary">
                <div><dt>本店金额</dt><dd>¥ {{ formatMoney(order.merchantAmount) }}</dd></div>
                <div><dt>商品数量</dt><dd>{{ dishCount(order.dishes) }} 份</dd></div>
                <div><dt>预约时间</dt><dd>{{ formatDate(order.orderDeliveryTime) }}</dd></div>
              </dl>
              <div class="order-actions">
                <button class="secondary-button" type="button" @click="openDetail(order.id)">查看详情</button>
                <button v-if="order.merchantStatus === 1" class="primary-button" type="button" :disabled="processingId === order.id" @click="confirmOrder(order)">{{ processingId === order.id ? '处理中' : '接单' }}</button>
                <button v-if="order.merchantStatus === 2" class="primary-button" type="button" :disabled="processingId === order.id" @click="completeOrder(order)">{{ processingId === order.id ? '处理中' : '确认出餐' }}</button>
              </div>
            </div>
          </article>
        </div>
        <div class="pagination"><span>第 {{ page }} / {{ pageCount }} 页</span><div><button class="secondary-button" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><button class="secondary-button" type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button></div></div>
      </template>
    </section>

    <div v-if="detailVisible" class="drawer-mask" @click.self="closeDetail">
      <aside class="drawer" aria-label="订单详情">
        <div class="drawer-heading"><div><p>订单详情</p><h2>{{ detailOrder?.number || '-' }}</h2></div><button class="close-button" type="button" aria-label="关闭" @click="closeDetail">×</button></div>
        <div v-if="detailLoading" class="state-panel">正在加载详情...</div>
        <div v-else-if="detailError" class="state-panel state-error"><p>{{ detailError }}</p><button class="secondary-button" type="button" @click="loadDetail(detailId)">重新加载</button></div>
        <template v-else-if="detailOrder">
          <div class="detail-status"><span class="status" :class="`status-${detailOrder.merchantStatus}`">{{ statusText(detailOrder.merchantStatus) }}</span><strong>本店 ¥ {{ formatMoney(detailOrder.merchantAmount) }}</strong></div>
          <div class="detail-dishes"><div v-for="dish in detailOrder.dishes" :key="dish.id" class="detail-dish"><div><strong>{{ dish.name }}</strong><small>{{ dish.dishFlavor || '默认规格' }}</small></div><span>× {{ dish.number }}</span><span>¥ {{ formatMoney(Number(dish.amount || 0) * Number(dish.number || 0)) }}</span></div></div>
          <dl class="detail-list">
            <div><dt>下单时间</dt><dd>{{ formatDate(detailOrder.orderTime) }}</dd></div>
            <div><dt>预约时间</dt><dd>{{ formatDate(detailOrder.orderDeliveryTime) }}</dd></div>
            <div><dt>收货人</dt><dd>{{ detailOrder.consignee || '-' }}</dd></div>
            <div><dt>联系电话</dt><dd>{{ detailOrder.phone || '-' }}</dd></div>
            <div><dt>配送地址</dt><dd>{{ detailOrder.address || '-' }}</dd></div>
            <div><dt>顾客备注</dt><dd>{{ detailOrder.remark || '无' }}</dd></div>
          </dl>
          <div class="drawer-actions"><button class="secondary-button" type="button" @click="closeDetail">关闭</button><button v-if="detailOrder.merchantStatus === 1" class="primary-button" type="button" :disabled="processingId === detailOrder.id" @click="confirmOrder(detailOrder)">接单</button><button v-if="detailOrder.merchantStatus === 2" class="primary-button" type="button" :disabled="processingId === detailOrder.id" @click="completeOrder(detailOrder)">确认出餐</button></div>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useOrderRefresh } from '../utils/order-refresh.js'
import { completeMerchantOrderApi, confirmMerchantOrderApi, getMerchantOrderApi, pageMerchantOrdersApi } from '../api/merchantOrder.js'

const pageSize = 8
const page = ref(1)
const total = ref(0)
const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({ status: '', number: '' })
const processingId = ref(null)
const actionMessage = ref('')
const actionType = ref('success')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailOrder = ref(null)
const detailId = ref(null)
const statusOptions = [{ label: '全部', value: '' }, { label: '待确认', value: 1 }, { label: '制作中', value: 2 }, { label: '已出餐', value: 3 }]
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function queryParams() { const params = { page: page.value, pageSize }; if (filters.status !== '') params.status = filters.status; if (filters.number) params.number = filters.number; return params }
let orderRequestSequence = 0
async function loadOrders(silent = false) {
  if (silent === true && loading.value) return
  const sequence = ++orderRequestSequence
  if (silent !== true) loading.value = true
  errorMessage.value = ''
  try {
    const response = await pageMerchantOrdersApi(queryParams())
    if (sequence !== orderRequestSequence) return
    if (response.data?.code !== 200) throw new Error(response.data?.message || '订单加载失败')
    total.value = Number(response.data.data?.total || 0)
    orders.value = response.data.data?.records || []
  } catch (error) {
    if (sequence !== orderRequestSequence) return
    if (silent !== true) { orders.value = []; total.value = 0 }
    errorMessage.value = error.response?.data?.message || error.message || '订单加载失败，请稍后重试'
  } finally {
    if (sequence === orderRequestSequence) loading.value = false
  }
}
function selectStatus(status) { filters.status = status; page.value = 1; loadOrders() }
function applyFilters() { page.value = 1; loadOrders() }
function resetFilters() { filters.status = ''; filters.number = ''; page.value = 1; loadOrders() }
function changePage(nextPage) { if (nextPage >= 1 && nextPage <= pageCount.value && nextPage !== page.value) { page.value = nextPage; loadOrders() } }
function showMessage(message, type = 'success') { actionMessage.value = message; actionType.value = type; window.setTimeout(() => { actionMessage.value = '' }, 3500) }
async function runAction(order, action, successMessage) { processingId.value = order.id; try { const response = await action(order.id); if (response.data?.code !== 200) throw new Error(response.data?.message || '操作失败'); showMessage(successMessage); detailVisible.value = false; await loadOrders() } catch (error) { showMessage(error.response?.data?.message || error.message || '操作失败，请稍后重试', 'error') } finally { processingId.value = null } }
function confirmOrder(order) { if (window.confirm(`确认接收订单 ${order.number} 并进入制作吗？`)) runAction(order, confirmMerchantOrderApi, '订单已接收，进入制作中') }
function completeOrder(order) { if (window.confirm(`确认订单 ${order.number} 的本店菜品已经全部出餐吗？`)) runAction(order, completeMerchantOrderApi, '本店菜品已确认出餐') }
async function openDetail(id) { detailVisible.value = true; detailId.value = id; await loadDetail(id) }
async function loadDetail(id) { detailLoading.value = true; detailError.value = ''; detailOrder.value = null; try { const response = await getMerchantOrderApi(id); if (response.data?.code !== 200) throw new Error(response.data?.message || '详情加载失败'); detailOrder.value = response.data.data } catch (error) { detailError.value = error.response?.data?.message || error.message || '详情加载失败，请稍后重试' } finally { detailLoading.value = false } }
function closeDetail() { detailVisible.value = false }
function statusText(status) { return ({ 1: '待确认', 2: '制作中', 3: '已出餐' })[status] || '未知状态' }
function formatMoney(value) { return Number(value || 0).toFixed(2) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function dishCount(dishes) { return (dishes || []).reduce((sum, dish) => sum + Number(dish.number || 0), 0) }
function resolveImageUrl(image) { if (/^https?:\/\//.test(image)) return image; const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'; return `${baseUrl}${image.startsWith('/') ? image : `/${image}`}` }

onMounted(loadOrders)
useOrderRefresh(async () => {
  if (processingId.value !== null) return
  await loadOrders(true)
  if (!detailVisible.value || detailLoading.value) return
  const id = detailId.value
  try {
    const response = await getMerchantOrderApi(id)
    if (detailVisible.value && detailId.value === id && response.data?.code === 200) detailOrder.value = response.data.data
  } catch { /* 后台刷新失败时保留当前详情 */ }
})
</script>

<style scoped>
.order-page{max-width:1240px;margin:0 auto}.page-heading{min-height:52px;display:flex;align-items:end;justify-content:space-between;gap:18px}.section-label{margin-bottom:7px;color:#6f8192;font-size:12px}h1{color:#20364e;font-size:24px;font-weight:600}.scope-note{color:#6f8192;font-size:13px}.filter-panel,.list-panel{margin-top:22px;border:1px solid #e4eaf0;background:#fff}.filter-panel{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:18px 20px}.status-tabs{display:flex;border:1px solid #ccd8e2}.status-tabs button{height:36px;min-width:78px;padding:0 14px;border:0;border-right:1px solid #ccd8e2;background:#fff;color:#506579;cursor:pointer}.status-tabs button:last-child{border-right:0}.status-tabs button.active,.status-tabs button:hover{color:#1766a6;background:#edf5fb}.search-form{display:flex;gap:8px}.search-form input{width:220px;height:36px;padding:0 11px;border:1px solid #d7e1e9;border-radius:3px;outline:none}.search-form input:focus{border-color:#1766a6}button{font:inherit}.primary-button,.secondary-button{min-height:36px;padding:0 15px;border-radius:3px;cursor:pointer}.primary-button{border:1px solid #1766a6;background:#1766a6;color:#fff}.secondary-button{border:1px solid #cbd8e2;background:#fff;color:#1766a6}.primary-button:disabled,.secondary-button:disabled{cursor:not-allowed;opacity:.6}.panel-heading{display:flex;align-items:center;justify-content:space-between;padding:18px 20px;border-bottom:1px solid #e4eaf0}.panel-heading h2,.drawer-heading h2{color:#20364e;font-size:16px;font-weight:600}.panel-heading span{color:#6f8192;font-size:12px}.action-message{padding:10px 20px;border-bottom:1px solid #e4eaf0;font-size:13px}.action-message.success{color:#2f8a60;background:#f2fbf5}.action-message.error{color:#c4544e;background:#fff5f4}.order-card{padding:0 20px;border-bottom:1px solid #e4eaf0}.order-card:last-child{border-bottom:0}.order-header{min-height:58px;display:flex;align-items:center;justify-content:space-between;gap:16px;color:#7b8c9b;font-size:12px}.order-header>div{display:flex;align-items:center;gap:12px}.order-header strong{color:#20364e;font-size:13px}.status{display:inline-block;padding:4px 8px;font-size:11px}.status-1{color:#9a6b2f;background:#fff7e9}.status-2{color:#1766a6;background:#edf5fb}.status-3{color:#2f8a60;background:#edf8f2}.order-body{display:grid;grid-template-columns:minmax(0,1fr) 260px 150px;gap:24px;padding:0 0 18px}.dish-summary{border:1px solid #edf1f4}.dish-item{min-height:55px;display:grid;grid-template-columns:38px minmax(0,1fr) 42px;align-items:center;gap:10px;padding:8px 10px;border-bottom:1px solid #edf1f4}.dish-item:last-child{border-bottom:0}.dish-item img,.image-placeholder{width:38px;height:38px;border:1px solid #e4eaf0;object-fit:cover}.image-placeholder{display:grid;place-items:center;color:#94a2ae;background:#eef1f3;font-size:10px}.dish-item div{min-width:0}.dish-item strong,.detail-dish strong{display:block;overflow:hidden;color:#20364e;font-size:13px;text-overflow:ellipsis;white-space:nowrap}.dish-item small,.detail-dish small{display:block;margin-top:4px;color:#82919e;font-size:11px}.dish-item>span{color:#5f7285;font-size:12px}.order-summary{padding-top:1px}.order-summary div{display:grid;grid-template-columns:70px 1fr;gap:10px;padding:6px 0;font-size:12px}.order-summary dt,.detail-list dt{color:#82919e}.order-summary dd,.detail-list dd{margin:0;color:#40566c}.order-actions{display:flex;align-items:end;justify-content:flex-end;gap:8px}.state-panel{min-height:190px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;padding:28px;color:#6f8192;font-size:13px}.state-error{color:#b5534e}.pagination{display:flex;align-items:center;justify-content:space-between;padding:15px 20px;border-top:1px solid #e4eaf0;color:#6f8192;font-size:12px}.pagination div{display:flex;gap:8px}.drawer-mask{position:fixed;z-index:40;inset:0;display:flex;justify-content:flex-end;background:rgba(32,54,78,.3)}.drawer{width:min(540px,100%);height:100%;display:flex;flex-direction:column;overflow-y:auto;background:#fff;box-shadow:-8px 0 24px rgba(18,58,99,.16)}.drawer-heading{display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #e4eaf0}.drawer-heading p{margin-bottom:6px;color:#6f8192;font-size:12px}.close-button{padding:0;border:0;background:transparent;color:#6f8192;font-size:26px;cursor:pointer}.detail-status{display:flex;align-items:center;justify-content:space-between;padding:18px 24px;border-bottom:1px solid #edf1f4}.detail-status strong{color:#20364e;font-size:15px}.detail-dishes{margin:20px 24px;border:1px solid #e4eaf0}.detail-dish{display:grid;grid-template-columns:minmax(0,1fr) 44px 78px;align-items:center;gap:10px;padding:12px;border-bottom:1px solid #edf1f4;font-size:12px}.detail-dish:last-child{border-bottom:0}.detail-dish>span:last-child{text-align:right}.detail-list{flex:1;padding:0 24px 20px}.detail-list>div{display:grid;grid-template-columns:80px 1fr;gap:16px;padding:13px 0;border-bottom:1px solid #edf1f4;font-size:13px}.detail-list dd{word-break:break-word}.drawer-actions{display:flex;justify-content:flex-end;gap:10px;padding:18px 24px;border-top:1px solid #e4eaf0;background:#fff}
@media(max-width:900px){.filter-panel{align-items:stretch;flex-direction:column}.search-form input{flex:1;width:auto}.order-body{grid-template-columns:1fr 1fr}.order-actions{grid-column:1/-1}}
@media(max-width:620px){.page-heading{align-items:start;flex-direction:column}.filter-panel{padding:14px}.status-tabs{width:100%;overflow-x:auto}.status-tabs button{min-width:72px;flex:1}.search-form{flex-wrap:wrap}.search-form input{width:100%;flex-basis:100%}.order-card{padding:0 14px}.order-header{align-items:start;flex-direction:column;padding:12px 0}.order-body{grid-template-columns:1fr;gap:12px}.order-actions{grid-column:auto;justify-content:flex-start}.pagination{align-items:start;flex-direction:column;gap:12px}}
</style>
