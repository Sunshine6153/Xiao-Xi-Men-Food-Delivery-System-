<template>
  <section class="dish-page">
    <div class="page-heading">
      <div>
        <p class="section-label">菜品管理</p>
        <h1>本店菜品</h1>
      </div>
      <div class="heading-actions">
        <span class="scope-note">仅显示当前商户菜品</span>
        <RouterLink class="primary-button" to="/session/dishes/new">新增菜品</RouterLink>
      </div>
    </div>

    <form class="filter-panel" @submit.prevent="applyFilters">
      <label class="filter-field filter-name">
        <span>菜品名称</span>
        <input v-model.trim="filters.name" type="search" placeholder="请输入菜品名称" />
      </label>
      <label class="filter-field">
        <span>菜品分类</span>
        <select v-model="filters.categoryId">
          <option value="">全部分类</option>
          <option v-for="category in categories" :key="category.id" :value="String(category.id)">
            {{ category.name }}{{ category.status === 0 ? '（已停用）' : '' }}
          </option>
        </select>
      </label>
      <label class="filter-field">
        <span>销售状态</span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="1">已上架</option>
          <option value="0">已下架</option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="primary-button" type="submit">查询</button>
        <button class="secondary-button" type="button" @click="resetFilters">重置</button>
      </div>
    </form>

    <section class="table-panel">
      <div class="table-heading">
        <h2>菜品列表</h2>
        <span v-if="!loading && !errorMessage" class="total-count">共 {{ total }} 项</span>
      </div>
      <div v-if="actionMessage" class="action-message" :class="actionMessageType">{{ actionMessage }}</div>

      <div v-if="loading" class="state-panel">正在加载菜品...</div>
      <div v-else-if="errorMessage" class="state-panel state-error">
        <p>{{ errorMessage }}</p>
        <button class="secondary-button" type="button" @click="loadDishes">重新加载</button>
      </div>
      <div v-else-if="dishes.length === 0" class="state-panel">
        <p>暂无符合条件的菜品</p>
        <button class="secondary-button" type="button" @click="resetFilters">清除筛选</button>
      </div>
      <template v-else>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>菜品</th><th>分类</th><th>价格</th><th>状态</th><th>更新时间</th><th class="action-column">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dish in dishes" :key="dish.id">
                <td>
                  <div class="dish-cell">
                    <img v-if="dish.image" :src="resolveImageUrl(dish.image)" :alt="dish.name" />
                    <span v-else class="image-placeholder">无图</span>
                    <div><strong>{{ dish.name }}</strong><small v-if="dish.description">{{ dish.description }}</small></div>
                  </div>
                </td>
                <td>{{ dish.categoryName || categoryName(dish.categoryId) || '未分类' }}</td>
                <td>¥ {{ formatPrice(dish.price) }}</td>
                <td><span class="status" :class="dish.status === 1 ? 'status-on' : 'status-off'">{{ dish.status === 1 ? '已上架' : '已下架' }}</span></td>
                <td>{{ formatDate(dish.updateTime || dish.createTime) }}</td>
                <td class="action-column">
                  <button class="link-button" type="button" @click="openDetail(dish.id)">查看详情</button>
                  <RouterLink class="link-button edit-link" :to="`/session/dishes/${dish.id}/edit`">编辑</RouterLink>
                  <button class="link-button" type="button" :disabled="statusUpdatingId === dish.id" @click="toggleStatus(dish)">{{ statusUpdatingId === dish.id ? '处理中' : dish.status === 1 ? '下架' : '上架' }}</button>
                  <button class="link-button danger-link" type="button" :disabled="deletingId === dish.id" @click="removeDish(dish)">{{ deletingId === dish.id ? '删除中' : '删除' }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <span>第 {{ page }} / {{ pageCount }} 页</span>
          <div>
            <button class="secondary-button" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
            <button class="secondary-button" type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </template>
    </section>

    <div v-if="detailVisible" class="detail-mask" @click.self="closeDetail">
      <aside class="detail-panel" aria-label="菜品详情">
        <div class="detail-heading"><h2>菜品详情</h2><button class="close-button" type="button" aria-label="关闭详情" @click="closeDetail">×</button></div>
        <div v-if="detailLoading" class="state-panel">正在加载详情...</div>
        <div v-else-if="detailError" class="state-panel state-error"><p>{{ detailError }}</p><button class="secondary-button" type="button" @click="loadDetail(detailDish?.id)">重新加载</button></div>
        <dl v-else-if="detailDish" class="detail-list">
          <div><dt>菜品名称</dt><dd>{{ detailDish.name }}</dd></div>
          <div><dt>所属分类</dt><dd>{{ detailDish.categoryName || categoryName(detailDish.categoryId) || '未分类' }}</dd></div>
          <div><dt>销售价格</dt><dd>¥ {{ formatPrice(detailDish.price) }}</dd></div>
          <div><dt>销售状态</dt><dd>{{ detailDish.status === 1 ? '已上架' : '已下架' }}</dd></div>
          <div><dt>菜品描述</dt><dd>{{ detailDish.description || '暂无描述' }}</dd></div>
          <div><dt>口味数量</dt><dd>{{ detailDish.flavors?.length || 0 }} 项</dd></div>
        </dl>
        <div v-if="detailDish && !detailLoading && !detailError" class="detail-actions">
          <RouterLink class="secondary-button" :to="`/session/dishes/${detailDish.id}/edit`">编辑菜品</RouterLink>
          <button class="secondary-button" type="button" :disabled="statusUpdatingId === detailDish.id" @click="toggleStatus(detailDish)">{{ detailDish.status === 1 ? '下架菜品' : '上架菜品' }}</button>
          <button class="danger-button" type="button" :disabled="deletingId === detailDish.id" @click="removeDish(detailDish)">删除菜品</button>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listCategoriesApi } from '../api/category.js'
import { deleteDishApi, getDishApi, pageDishesApi, updateDishStatusApi } from '../api/dish.js'

const pageSize = 10
const page = ref(1)
const total = ref(0)
const dishes = ref([])
const categories = ref([])
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({ name: '', categoryId: '', status: '' })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailDish = ref(null)
const actionMessage = ref('')
const actionMessageType = ref('')
const statusUpdatingId = ref(null)
const deletingId = ref(null)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function queryParams() {
  const params = { page: page.value, pageSize }
  if (filters.name) params.name = filters.name
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.status !== '') params.status = filters.status
  return params
}

async function loadCategories() {
  try {
    const response = await listCategoriesApi()
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类加载失败')
    categories.value = response.data.data || []
  } catch { categories.value = [] }
}

async function loadDishes() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await pageDishesApi(queryParams())
    if (response.data?.code !== 200) throw new Error(response.data?.message || '菜品加载失败')
    total.value = Number(response.data.data?.total || 0)
    dishes.value = response.data.data?.records || []
  } catch (error) {
    dishes.value = []
    total.value = 0
    errorMessage.value = error.response?.data?.message || error.message || '菜品加载失败，请稍后重试'
  } finally { loading.value = false }
}

function applyFilters() { page.value = 1; loadDishes() }
function resetFilters() { filters.name = ''; filters.categoryId = ''; filters.status = ''; page.value = 1; loadDishes() }
function changePage(nextPage) { if (nextPage >= 1 && nextPage <= pageCount.value && nextPage !== page.value) { page.value = nextPage; loadDishes() } }

function showActionMessage(message, type = 'success') {
  actionMessage.value = message
  actionMessageType.value = type
  window.setTimeout(() => { actionMessage.value = '' }, 3500)
}

async function toggleStatus(dish) {
  statusUpdatingId.value = dish.id
  actionMessage.value = ''
  try {
    const nextStatus = dish.status === 1 ? 0 : 1
    const response = await updateDishStatusApi(nextStatus, dish.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '状态更新失败')
    showActionMessage(`${dish.name}已${nextStatus === 1 ? '上架' : '下架'}`)
    detailVisible.value = false
    await loadDishes()
  } catch (error) {
    showActionMessage(error.response?.data?.message || error.message || '状态更新失败，请稍后重试', 'error')
  } finally { statusUpdatingId.value = null }
}

async function removeDish(dish) {
  if (!window.confirm(`确认删除“${dish.name}”吗？删除后不可恢复。`)) return
  deletingId.value = dish.id
  actionMessage.value = ''
  try {
    const response = await deleteDishApi(dish.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '删除失败')
    showActionMessage('菜品已删除')
    detailVisible.value = false
    if (dishes.value.length === 1 && page.value > 1) page.value -= 1
    await loadDishes()
  } catch (error) {
    showActionMessage(error.response?.data?.message || error.message || '删除失败，请稍后重试', 'error')
  } finally { deletingId.value = null }
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailDish.value = null
  await loadDetail(id)
}

async function loadDetail(id) {
  if (!id) return
  detailLoading.value = true
  detailError.value = ''
  try {
    const response = await getDishApi(id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '详情加载失败')
    detailDish.value = response.data.data
  } catch (error) { detailError.value = error.response?.data?.message || error.message || '详情加载失败，请稍后重试' }
  finally { detailLoading.value = false }
}

function closeDetail() { detailVisible.value = false }
function categoryName(id) { return categories.value.find((category) => category.id === id)?.name || '' }
function formatPrice(price) { return Number(price || 0).toFixed(2) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function resolveImageUrl(image) { if (/^https?:\/\//.test(image)) return image; return `${import.meta.env.VITE_API_BASE_URL || '/api'}${image.startsWith('/') ? image : `/${image}`}` }

onMounted(() => { loadCategories(); loadDishes() })
</script>

<style scoped>
.dish-page { max-width: 1320px; margin: 0 auto; }
.page-heading { display: flex; align-items: end; justify-content: space-between; min-height: 52px; }
.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; }
.scope-note { color: #6f8192; font-size: 13px; }
.heading-actions { display: flex; align-items: center; gap: 16px; }
.filter-panel, .table-panel { margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }
.filter-panel { display: flex; flex-wrap: wrap; align-items: end; gap: 16px; padding: 20px; }
.filter-field { display: flex; min-width: 150px; flex-direction: column; gap: 8px; color: #6f8192; font-size: 12px; }
.filter-name { min-width: 230px; }
input, select { height: 36px; min-width: 150px; padding: 0 10px; border: 1px solid #d7e1e9; border-radius: 3px; background: #fff; color: #20364e; font: inherit; outline: none; }
input:focus, select:focus { border-color: #1766a6; }
.filter-actions, .pagination > div { display: flex; gap: 8px; }
button { font: inherit; cursor: pointer; }
.primary-button, .secondary-button { min-height: 36px; padding: 0 15px; border-radius: 3px; }
.primary-button { border: 1px solid #1766a6; background: #1766a6; color: #fff; }
.secondary-button { border: 1px solid #cbd8e2; background: #fff; color: #1766a6; }
.secondary-button:disabled { cursor: not-allowed; color: #aebbc5; border-color: #e4eaf0; }
.table-heading { display: flex; justify-content: space-between; align-items: center; padding: 20px; border-bottom: 1px solid #e4eaf0; }
h2 { color: #20364e; font-size: 16px; font-weight: 600; }
.total-count { color: #6f8192; font-size: 12px; }
.table-scroll { overflow-x: auto; }
table { width: 100%; min-width: 760px; border-collapse: collapse; font-size: 13px; }
th, td { height: 58px; padding: 8px 20px; border-bottom: 1px solid #edf1f4; text-align: left; white-space: nowrap; }
th { height: 42px; color: #6f8192; background: #fbfcfd; font-size: 12px; font-weight: 500; }
tr:last-child td { border-bottom: 0; }
.dish-cell { display: flex; align-items: center; gap: 10px; min-width: 180px; }
.dish-cell img, .image-placeholder { width: 38px; height: 38px; object-fit: cover; border: 1px solid #e4eaf0; }
.image-placeholder { display: inline-flex; align-items: center; justify-content: center; color: #9aaab7; font-size: 11px; }
.dish-cell strong { display: block; color: #20364e; font-weight: 600; }
.dish-cell small { display: block; max-width: 220px; margin-top: 3px; overflow: hidden; color: #94a2ae; text-overflow: ellipsis; font-size: 11px; }
.status { display: inline-block; padding: 4px 8px; font-size: 12px; }
.status-on { color: #2f8a60; background: #edf8f2; }
.status-off { color: #9a6b2f; background: #fff7e9; }
.link-button { padding: 0; border: 0; background: transparent; color: #1766a6; }
.link-button:disabled { cursor: not-allowed; color: #aebbc5; }
.edit-link { margin-left: 10px; }
.danger-link { margin-left: 10px; color: #c4544e; }
.action-column { text-align: right; }
.action-message { padding: 10px 20px; border-bottom: 1px solid #e4eaf0; font-size: 13px; }
.action-message.success { color: #2f8a60; background: #f2fbf5; }
.action-message.error { color: #c4544e; background: #fff5f4; }
.state-panel { min-height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 24px; color: #6f8192; font-size: 14px; }
.state-error { color: #c4544e; }
.state-panel p { margin: 0; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-top: 1px solid #e4eaf0; color: #6f8192; font-size: 12px; }
.detail-mask { position: fixed; z-index: 30; inset: 0; display: flex; justify-content: flex-end; background: rgba(32, 54, 78, 0.28); }
.detail-panel { width: min(460px, 100%); height: 100%; overflow-y: auto; padding: 26px; background: #fff; box-shadow: -8px 0 24px rgba(18, 58, 99, 0.15); }
.detail-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid #e4eaf0; }
.close-button { border: 0; background: transparent; color: #6f8192; font-size: 26px; line-height: 1; }
.detail-list { margin-top: 12px; }
.detail-list > div { display: grid; grid-template-columns: 92px 1fr; gap: 18px; padding: 16px 0; border-bottom: 1px solid #edf1f4; font-size: 14px; }
dt { color: #6f8192; } dd { margin: 0; color: #20364e; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 24px; }
.detail-actions button, .detail-actions a { font-size: 13px; }
.danger-button { min-height: 36px; padding: 0 15px; border: 1px solid #e4bbb8; border-radius: 3px; background: #fff; color: #c4544e; font: inherit; cursor: pointer; }
.danger-button:disabled { cursor: not-allowed; opacity: .6; }
@media (max-width: 800px) { .filter-name { min-width: 200px; } .filter-panel { gap: 12px; } }
@media (max-width: 560px) { .page-heading { align-items: start; flex-direction: column; gap: 8px; } .heading-actions { align-items: start; flex-direction: column; gap: 10px; } h1 { font-size: 21px; } .filter-panel { align-items: stretch; flex-direction: column; } .filter-field, .filter-name, input, select { width: 100%; min-width: 0; } .filter-actions { margin-top: 4px; } .filter-actions button { flex: 1; } .table-panel { margin-top: 18px; } th, td { padding-right: 14px; padding-left: 14px; } .detail-panel { padding: 20px 16px; } }
</style>
