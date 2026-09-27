<template>
  <section class="page">
    <div class="page-heading">
      <div><p class="section-label">平台管理</p><h1>菜品列表</h1></div>
      <span class="scope-note">展示全部商户菜品</span>
    </div>

    <form class="filter-panel" @submit.prevent="applyFilters">
      <label class="filter-field filter-name"><span>菜品名称</span><el-input v-model="filters.name" clearable placeholder="请输入菜品名称" aria-label="菜品名称" /></label>
      <label class="filter-field"><span>所属商户</span><el-select v-model="filters.merchantId" filterable placeholder="全部商户" aria-label="所属商户"><el-option label="全部商户" value="" /><el-option v-for="merchant in merchants" :key="merchant.id" :value="String(merchant.id)" :label="merchant.merchantName" /></el-select></label>
      <label class="filter-field"><span>菜品分类</span><el-select v-model="filters.categoryId" filterable placeholder="全部分类" aria-label="菜品分类"><el-option label="全部分类" value="" /><el-option v-for="category in categories" :key="category.id" :value="String(category.id)" :label="category.name" /></el-select></label>
      <label class="filter-field"><span>销售状态</span><el-select v-model="filters.status" placeholder="全部状态" aria-label="销售状态"><el-option label="全部状态" value="" /><el-option label="已上架" value="1" /><el-option label="已下架" value="0" /></el-select></label>
      <div class="filter-actions"><el-button class="ui-action primary-button" native-type="submit" type="primary" :icon="Search">查询</el-button><el-button class="ui-action secondary-button" native-type="button" @click="resetFilters">重置</el-button></div>
    </form>

    <section class="table-panel">
      <div class="table-heading"><h2>全部菜品</h2><span v-if="!loading && !loadError" class="total-count">共 {{ total }} 项</span></div>
      <div v-if="actionMessage" class="action-message" :class="actionType">{{ actionMessage }}</div>
      <div v-if="loading" class="state-panel">正在加载菜品...</div>
      <div v-else-if="loadError" class="state-panel state-error"><p>{{ loadError }}</p><el-button class="ui-action secondary-button" native-type="button" @click="loadDishes" :icon="Refresh">重新加载</el-button></div>
      <div v-else-if="dishes.length === 0" class="state-panel"><p>暂无符合条件的菜品</p><el-button class="ui-action secondary-button" native-type="button" @click="resetFilters">清除筛选</el-button></div>
      <template v-else>
        <div class="table-scroll"><table>
          <thead><tr><th>菜品</th><th>所属商户</th><th>分类</th><th>价格</th><th>状态</th><th>更新时间</th><th class="action-column">操作</th></tr></thead>
          <tbody><tr v-for="dish in dishes" :key="dish.id">
            <td><div class="dish-cell"><img v-if="dish.image" :src="resolveImageUrl(dish.image)" :alt="dish.name" /><span v-else class="image-placeholder">无图</span><div><strong>{{ dish.name }}</strong><small v-if="dish.description">{{ dish.description }}</small></div></div></td>
            <td><strong>{{ dish.merchantName || merchantName(dish.merchantId) || '-' }}</strong><small class="secondary-text">{{ dish.merchantLocation || '-' }}</small></td>
            <td>{{ dish.categoryName || categoryName(dish.categoryId) || '未分类' }}</td>
            <td>¥ {{ formatPrice(dish.price) }}</td>
            <td><span class="status" :class="dish.status === 1 ? 'status-on' : 'status-off'">{{ dish.status === 1 ? '已上架' : '已下架' }}</span></td>
            <td>{{ formatDate(dish.updateTime || dish.createTime) }}</td>
            <td class="action-column"><el-button class="ui-action link-button" native-type="button" @click="openDetail(dish.id)" type="primary" link>查看</el-button><el-button class="ui-action link-button" :class="dish.status === 1 ? 'danger-link' : ''" native-type="button" :disabled="statusUpdatingId === dish.id" @click="toggleStatus(dish)" type="primary" link :loading="statusUpdatingId === dish.id">{{ statusUpdatingId === dish.id ? '处理中' : dish.status === 1 ? '下架' : '上架' }}</el-button></td>
          </tr></tbody>
        </table></div>
        <div class="pagination"><span>第 {{ page }} / {{ pageCount }} 页</span><el-pagination :current-page="page" :page-size="pageSize" :total="total" :pager-count="5" layout="prev, pager, next" :disabled="loading" @current-change="changePage" /></div>
      </template>
    </section>

    <div v-if="detailVisible" class="drawer-mask" @click.self="closeDetail">
      <aside class="drawer" role="dialog" aria-modal="true" aria-label="菜品详情">
        <div class="drawer-heading"><div><p>平台菜品</p><h2>菜品详情</h2></div><button class="close-button" type="button" aria-label="关闭详情" @click="closeDetail">×</button></div>
        <div v-if="detailLoading" class="state-panel">正在加载详情...</div>
        <div v-else-if="detailError" class="state-panel state-error"><p>{{ detailError }}</p><el-button class="ui-action secondary-button" native-type="button" @click="loadDetail(detailId)" :icon="Refresh">重新加载</el-button></div>
        <template v-else-if="detailDish">
          <div class="detail-summary"><img v-if="detailDish.image" :src="resolveImageUrl(detailDish.image)" :alt="detailDish.name" /><span v-else class="detail-image-placeholder">暂无图片</span><div><h3>{{ detailDish.name }}</h3><p>¥ {{ formatPrice(detailDish.price) }}</p></div></div>
          <dl class="detail-list">
            <div><dt>所属商户</dt><dd>{{ detailDish.merchantName || merchantName(detailDish.merchantId) || '-' }}</dd></div>
            <div><dt>摊位位置</dt><dd>{{ detailDish.merchantLocation || '-' }}</dd></div>
            <div><dt>所属分类</dt><dd>{{ detailDish.categoryName || categoryName(detailDish.categoryId) || '未分类' }}</dd></div>
            <div><dt>销售状态</dt><dd>{{ detailDish.status === 1 ? '已上架' : '已下架' }}</dd></div>
            <div><dt>菜品描述</dt><dd>{{ detailDish.description || '暂无描述' }}</dd></div>
            <div><dt>口味规格</dt><dd><span v-if="!detailDish.flavors?.length">无</span><ul v-else class="flavor-list"><li v-for="flavor in detailDish.flavors" :key="flavor.id || flavor.name"><strong>{{ flavor.name }}</strong>：{{ flavor.value || '-' }}</li></ul></dd></div>
            <div><dt>创建时间</dt><dd>{{ formatDate(detailDish.createTime) }}</dd></div>
            <div><dt>更新时间</dt><dd>{{ formatDate(detailDish.updateTime) }}</dd></div>
          </dl>
          <div class="drawer-actions"><el-button class="ui-action secondary-button" native-type="button" @click="closeDetail">关闭</el-button><el-button class="ui-action primary-button" native-type="button" :disabled="statusUpdatingId === detailDish.id" @click="toggleStatus(detailDish)" type="primary" :loading="statusUpdatingId === detailDish.id">{{ detailDish.status === 1 ? '下架菜品' : '上架菜品' }}</el-button></div>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { listCategoriesApi } from '../api/category.js'
import { getDishApi, pageDishesApi, updateDishStatusApi } from '../api/dish.js'
import { pageMerchantsApi } from '../api/merchant.js'

const pageSize = 10
const page = ref(1), total = ref(0), dishes = ref([]), categories = ref([]), merchants = ref([])
const loading = ref(false), loadError = ref(''), actionMessage = ref(''), actionType = ref('success')
const statusUpdatingId = ref(null), detailVisible = ref(false), detailLoading = ref(false), detailError = ref(''), detailDish = ref(null), detailId = ref(null)
const filters = reactive({ name: '', merchantId: '', categoryId: '', status: '' })
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function queryParams() {
  const params = { page: page.value, pageSize }
  if (filters.name.trim()) params.name = filters.name.trim()
  if (filters.merchantId) params.merchantId = filters.merchantId
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.status !== '') params.status = filters.status
  return params
}
async function loadOptions() {
  const [categoryResult, merchantResult] = await Promise.allSettled([listCategoriesApi(), pageMerchantsApi({ page: 1, pageSize: 100 })])
  if (categoryResult.status === 'fulfilled' && categoryResult.value.data?.code === 200) categories.value = categoryResult.value.data.data || []
  if (merchantResult.status === 'fulfilled' && merchantResult.value.data?.code === 200) merchants.value = merchantResult.value.data.data?.records || []
}
async function loadDishes() {
  loading.value = true; loadError.value = ''
  try {
    const response = await pageDishesApi(queryParams())
    if (response.data?.code !== 200) throw new Error(response.data?.message || '菜品加载失败')
    total.value = Number(response.data.data?.total || 0); dishes.value = response.data.data?.records || []
  } catch (error) { dishes.value = []; total.value = 0; loadError.value = error.response?.data?.message || error.message || '菜品加载失败，请稍后重试' }
  finally { loading.value = false }
}
function applyFilters() { page.value = 1; loadDishes() }
function resetFilters() { Object.assign(filters, { name: '', merchantId: '', categoryId: '', status: '' }); page.value = 1; loadDishes() }
function changePage(next) { if (next >= 1 && next <= pageCount.value && next !== page.value) { page.value = next; loadDishes() } }
function showMessage(message, type = 'success') { actionMessage.value = message; actionType.value = type; window.setTimeout(() => { actionMessage.value = '' }, 3500) }
async function toggleStatus(dish) {
  const nextStatus = dish.status === 1 ? 0 : 1
  if (nextStatus === 0 && !window.confirm(`确认下架“${dish.name}”吗？下架后用户端将无法购买。`)) return
  statusUpdatingId.value = dish.id
  try {
    const response = await updateDishStatusApi(nextStatus, dish.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '状态更新失败')
    showMessage(`${dish.name}已${nextStatus === 1 ? '上架' : '下架'}`); detailVisible.value = false; await loadDishes()
  } catch (error) { showMessage(error.response?.data?.message || error.message || '状态更新失败，请稍后重试', 'error') }
  finally { statusUpdatingId.value = null }
}
async function openDetail(id) { detailVisible.value = true; detailId.value = id; detailDish.value = null; await loadDetail(id) }
async function loadDetail(id) {
  detailLoading.value = true; detailError.value = ''
  try { const response = await getDishApi(id); if (response.data?.code !== 200) throw new Error(response.data?.message || '详情加载失败'); detailDish.value = response.data.data }
  catch (error) { detailError.value = error.response?.data?.message || error.message || '详情加载失败，请稍后重试' }
  finally { detailLoading.value = false }
}
function closeDetail() { detailVisible.value = false }
function categoryName(id) { return categories.value.find((item) => String(item.id) === String(id))?.name || '' }
function merchantName(id) { return merchants.value.find((item) => String(item.id) === String(id))?.merchantName || '' }
function formatPrice(value) { return Number(value || 0).toFixed(2) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function resolveImageUrl(image) { if (/^https?:\/\//.test(image)) return image; return `${import.meta.env.VITE_API_BASE_URL || '/api'}${image.startsWith('/') ? image : `/${image}`}` }
onMounted(() => { loadOptions(); loadDishes() })
</script>

<style scoped>
.page { max-width: 1320px; margin: 0 auto; }.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 18px; }.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }h1 { color: #20364e; font-size: 24px; font-weight: 600; }.scope-note { color: #6f8192; font-size: 13px; }
.filter-panel,.table-panel { margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }.filter-panel { display: flex; flex-wrap: wrap; align-items: end; gap: 16px; padding: 20px; }.filter-field { min-width: 150px; display: flex; flex-direction: column; gap: 8px; color: #6f8192; font-size: 12px; }.filter-name { min-width: 220px; }input,select { height: 36px; min-width: 150px; padding: 0 10px; border: 1px solid #d7e1e9; border-radius: 3px; background: #fff; color: #20364e; font: inherit; outline: none; }input:focus,select:focus { border-color: #1766a6; }button { font: inherit; cursor: pointer; }.filter-actions,.pagination>div { display: flex; gap: 8px; }.primary-button,.secondary-button { min-height: 36px; padding: 0 15px; border-radius: 3px; }.primary-button { border: 1px solid #1766a6; background: #1766a6; color: #fff; }.secondary-button { border: 1px solid #cbd8e2; background: #fff; color: #1766a6; }.primary-button:disabled,.secondary-button:disabled { cursor: not-allowed; opacity: .6; }
.table-heading { display: flex; align-items: center; justify-content: space-between; padding: 20px; border-bottom: 1px solid #e4eaf0; }.table-heading h2,.drawer-heading h2 { color: #20364e; font-size: 16px; font-weight: 600; }.total-count { color: #6f8192; font-size: 12px; }.table-scroll { overflow-x: auto; }table { width: 100%; min-width: 1080px; border-collapse: collapse; font-size: 13px; }th,td { min-height: 58px; padding: 10px 18px; border-bottom: 1px solid #edf1f4; text-align: left; white-space: nowrap; }th { height: 42px; color: #6f8192; background: #fbfcfd; font-size: 12px; font-weight: 500; }tr:last-child td { border-bottom: 0; }.dish-cell { display: flex; align-items: center; gap: 11px; }.dish-cell img,.image-placeholder { width: 44px; height: 44px; flex: 0 0 44px; object-fit: cover; border: 1px solid #e4eaf0; }.image-placeholder { display: grid; place-items: center; color: #91a0ad; background: #eef1f3; font-size: 11px; }.dish-cell div { min-width: 0; }.dish-cell strong { display: block; max-width: 190px; overflow: hidden; color: #20364e; text-overflow: ellipsis; }.dish-cell small,.secondary-text { display: block; max-width: 190px; margin-top: 4px; overflow: hidden; color: #81909d; font-size: 11px; text-overflow: ellipsis; }.status { display: inline-block; padding: 4px 8px; font-size: 12px; }.status-on { color: #2f8a60; background: #edf8f2; }.status-off { color: #9a6b2f; background: #fff7e9; }.action-column { text-align: right; }.link-button { margin-left: 12px; padding: 0; border: 0; background: transparent; color: #1766a6; }.link-button:disabled { cursor: not-allowed; color: #aebbc5; }.danger-link { color: #c4544e; }
.action-message { padding: 10px 20px; border-bottom: 1px solid #e4eaf0; font-size: 13px; }.action-message.success { color: #2f8a60; background: #f2fbf5; }.action-message.error { color: #c4544e; background: #fff5f4; }.state-panel { min-height: 190px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 24px; color: #6f8192; font-size: 14px; }.state-error { color: #c4544e; }.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-top: 1px solid #e4eaf0; color: #6f8192; font-size: 12px; }
.drawer-mask { position: fixed; z-index: 40; inset: 0; display: flex; justify-content: flex-end; background: rgba(32,54,78,.3); }.drawer { width: min(540px,100%); height: 100%; display: flex; flex-direction: column; overflow-y: auto; background: #fff; box-shadow: -8px 0 24px rgba(18,58,99,.16); }.drawer-heading { display: flex; align-items: center; justify-content: space-between; padding: 22px 24px; border-bottom: 1px solid #e4eaf0; }.drawer-heading p { margin-bottom: 6px; color: #6f8192; font-size: 12px; }.close-button { padding: 0; border: 0; background: transparent; color: #6f8192; font-size: 26px; line-height: 1; }.detail-summary { display: flex; align-items: center; gap: 16px; padding: 24px; border-bottom: 1px solid #edf1f4; }.detail-summary img,.detail-image-placeholder { width: 76px; height: 76px; flex: 0 0 76px; object-fit: cover; border: 1px solid #e4eaf0; }.detail-image-placeholder { display: grid; place-items: center; color: #81909d; background: #eef1f3; font-size: 12px; }.detail-summary h3 { color: #20364e; font-size: 18px; }.detail-summary p { margin-top: 8px; color: #1766a6; font-weight: 600; }.detail-list { flex: 1; padding: 8px 24px 20px; }.detail-list>div { display: grid; grid-template-columns: 92px 1fr; gap: 18px; padding: 15px 0; border-bottom: 1px solid #edf1f4; font-size: 13px; }.detail-list>div:last-child { border-bottom: 0; }dt { color: #6f8192; }dd { margin: 0; color: #20364e; word-break: break-word; }.flavor-list { margin: 0; padding-left: 18px; }.flavor-list li+li { margin-top: 6px; }.drawer-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 18px 24px; border-top: 1px solid #e4eaf0; background: #fff; }
@media(max-width:560px){.page-heading { align-items: flex-start; flex-direction: column; }.filter-panel { align-items: stretch; flex-direction: column; }.filter-field,.filter-name,input,select { min-width: 0; width: 100%; }.filter-actions button { flex: 1; }.table-panel { margin-top: 18px; }.drawer-heading,.detail-summary,.detail-list { padding-right: 18px; padding-left: 18px; }.drawer-actions { padding: 16px 18px; }}
</style>
