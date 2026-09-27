<template>
  <section class="merchant-page">
    <div class="page-heading">
      <div><p class="section-label">平台管理</p><h1>商户管理</h1></div>
      <el-button class="ui-action primary-button" native-type="button" @click="openCreate" type="primary" :icon="Plus">新增商户</el-button>
    </div>

    <form class="filter-panel" @submit.prevent="applyFilters">
      <label class="filter-field"><span>商户名称</span><input v-model.trim="filters.name" type="search" maxlength="128" placeholder="请输入商户名称" /></label>
      <div class="filter-actions"><el-button class="ui-action primary-button" native-type="submit" type="primary" :icon="Search">查询</el-button><el-button class="ui-action secondary-button" native-type="button" @click="resetFilters">重置</el-button></div>
    </form>

    <section class="table-panel">
      <div class="table-heading"><h2>商户列表</h2><span v-if="!loading && !loadError" class="total-count">共 {{ total }} 项</span></div>
      <div v-if="actionMessage" class="action-message" :class="actionType">{{ actionMessage }}</div>
      <div v-if="loading" class="state-panel">正在加载商户...</div>
      <div v-else-if="loadError" class="state-panel state-error"><p>{{ loadError }}</p><el-button class="ui-action secondary-button" native-type="button" @click="loadMerchants" :icon="Refresh">重新加载</el-button></div>
      <div v-else-if="merchants.length === 0" class="state-panel"><p>暂无符合条件的商户</p><el-button class="ui-action secondary-button" native-type="button" @click="resetFilters">清除筛选</el-button></div>
      <template v-else>
        <div class="table-scroll">
          <table>
            <thead><tr><th>商户名称</th><th>账号</th><th>电话</th><th>摊位位置</th><th>状态</th><th class="action-column">操作</th></tr></thead>
            <tbody>
              <tr v-for="merchant in merchants" :key="merchant.id">
                <td><strong>{{ merchant.merchantName }}</strong></td><td>{{ merchant.username }}</td><td>{{ merchant.phone || '-' }}</td>
                <td><span class="location-text">{{ merchant.location || '-' }}</span></td>
                <td><span class="status" :class="merchant.status === 1 ? 'status-on' : 'status-off'">{{ merchant.status === 1 ? '已启用' : '已禁用' }}</span></td>
                <td class="action-column">
                  <el-button class="ui-action link-button" native-type="button" @click="openDetail(merchant.id)" type="primary" link>查看</el-button>
                  <el-button class="ui-action link-button" native-type="button" @click="openEdit(merchant.id)" type="primary" link>编辑</el-button>
                  <el-button class="ui-action link-button" :class="merchant.status === 1 ? 'danger-link' : ''" native-type="button" :disabled="statusUpdatingId === merchant.id" @click="toggleStatus(merchant)" type="primary" link :loading="statusUpdatingId === merchant.id">{{ statusUpdatingId === merchant.id ? '处理中' : merchant.status === 1 ? '禁用' : '启用' }}</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination"><span>第 {{ page }} / {{ pageCount }} 页</span><el-pagination :current-page="page" :page-size="pageSize" :total="total" :pager-count="5" layout="prev, pager, next" :disabled="loading" @current-change="changePage" /></div>
      </template>
    </section>

    <div v-if="drawerVisible" class="drawer-mask" @click.self="requestCloseDrawer">
      <aside class="drawer" role="dialog" aria-modal="true" :aria-label="drawerTitle">
        <div class="drawer-heading"><div><p>{{ mode === 'view' ? '商户资料' : '信息维护' }}</p><h2>{{ drawerTitle }}</h2></div><button class="close-button" type="button" aria-label="关闭抽屉" @click="requestCloseDrawer">×</button></div>
        <div v-if="detailLoading" class="drawer-state">正在加载商户...</div>
        <div v-else-if="drawerError && mode === 'view'" class="drawer-state state-error"><p>{{ drawerError }}</p><el-button class="ui-action secondary-button" native-type="button" @click="loadMerchant(form.id)" :icon="Refresh">重新加载</el-button></div>
        <template v-else-if="mode === 'view'">
          <dl class="detail-list">
            <div><dt>商户名称</dt><dd>{{ form.merchantName }}</dd></div><div><dt>登录账号</dt><dd>{{ form.username }}</dd></div>
            <div><dt>联系电话</dt><dd>{{ form.phone || '-' }}</dd></div><div><dt>摊位位置</dt><dd>{{ form.location || '-' }}</dd></div>
            <div><dt>账号状态</dt><dd>{{ form.status === 1 ? '已启用' : '已禁用' }}</dd></div><div><dt>创建时间</dt><dd>{{ formatDate(form.createTime) }}</dd></div>
            <div><dt>更新时间</dt><dd>{{ formatDate(form.updateTime) }}</dd></div>
          </dl>
          <div class="drawer-actions"><el-button class="ui-action secondary-button" native-type="button" @click="requestCloseDrawer">关闭</el-button><el-button class="ui-action primary-button" native-type="button" @click="switchToEdit" type="primary">编辑商户</el-button></div>
        </template>
        <form v-else class="drawer-form" @submit.prevent="submitForm">
          <div class="form-content">
            <p v-if="drawerError" class="form-error">{{ drawerError }}</p>
            <label class="form-field"><span>商户名称 <em>*</em></span><input v-model.trim="form.merchantName" type="text" maxlength="128" placeholder="请输入商户名称" /><small v-if="errors.merchantName">{{ errors.merchantName }}</small></label>
            <label class="form-field"><span>登录账号 <em>*</em></span><input v-model.trim="form.username" type="text" maxlength="64" autocomplete="off" placeholder="请输入登录账号" /><small v-if="errors.username">{{ errors.username }}</small></label>
            <label class="form-field"><span>{{ mode === 'create' ? '登录密码' : '新密码' }} <em v-if="mode === 'create'">*</em></span><input v-model="form.password" type="password" maxlength="128" autocomplete="new-password" :placeholder="mode === 'create' ? '请输入登录密码' : '留空则保持原密码'" /><small v-if="errors.password">{{ errors.password }}</small><small v-else-if="mode === 'edit'" class="field-tip">出于安全考虑不回显原密码。</small></label>
            <label class="form-field"><span>联系电话</span><input v-model.trim="form.phone" type="text" maxlength="32" placeholder="请输入联系电话（选填）" /></label>
            <label class="form-field"><span>摊位位置</span><textarea v-model.trim="form.location" maxlength="256" rows="3" placeholder="请输入摊位或档口位置（选填）"></textarea></label>
          </div>
          <div class="drawer-actions"><el-button class="ui-action secondary-button" native-type="button" :disabled="saving" @click="requestCloseDrawer" :loading="saving">取消</el-button><el-button class="ui-action primary-button" native-type="submit" :disabled="saving" type="primary" :loading="saving">{{ saving ? '保存中...' : '保存' }}</el-button></div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { getMerchantApi, pageMerchantsApi, saveMerchantApi, updateMerchantApi, updateMerchantStatusApi } from '../api/merchant.js'

const pageSize = 10
const page = ref(1), total = ref(0), merchants = ref([]), loading = ref(false), loadError = ref('')
const filters = reactive({ name: '' })
const actionMessage = ref(''), actionType = ref('success'), statusUpdatingId = ref(null)
const drawerVisible = ref(false), detailLoading = ref(false), drawerError = ref(''), saving = ref(false), mode = ref('create')
const form = reactive({ id: null, username: '', password: '', merchantName: '', phone: '', location: '', status: 1, createTime: '', updateTime: '' })
const errors = reactive({ username: '', password: '', merchantName: '' })
const initialSnapshot = ref('')
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const drawerTitle = computed(() => mode.value === 'create' ? '新增商户' : mode.value === 'edit' ? '编辑商户' : '商户详情')
const editableSnapshot = computed(() => JSON.stringify({ username: form.username, password: form.password, merchantName: form.merchantName, phone: form.phone, location: form.location }))
const isDirty = computed(() => mode.value !== 'view' && Boolean(initialSnapshot.value) && editableSnapshot.value !== initialSnapshot.value)

function messageOf(error, fallback) { return error?.response?.data?.message || error?.message || fallback }
function clearForm() { Object.assign(form, { id: null, username: '', password: '', merchantName: '', phone: '', location: '', status: 1, createTime: '', updateTime: '' }); Object.keys(errors).forEach((key) => { errors[key] = '' }); drawerError.value = '' }
function rememberSnapshot() { initialSnapshot.value = editableSnapshot.value }
function queryParams() { const params = { page: page.value, pageSize }; if (filters.name) params.name = filters.name; return params }

async function loadMerchants() {
  loading.value = true; loadError.value = ''
  try { const response = await pageMerchantsApi(queryParams()); if (response.data?.code !== 200) throw new Error(response.data?.message || '商户加载失败'); total.value = Number(response.data.data?.total || 0); merchants.value = response.data.data?.records || [] }
  catch (error) { merchants.value = []; total.value = 0; loadError.value = messageOf(error, '商户加载失败，请稍后重试') }
  finally { loading.value = false }
}
function applyFilters() { page.value = 1; loadMerchants() }
function resetFilters() { filters.name = ''; page.value = 1; loadMerchants() }
function changePage(next) { if (next >= 1 && next <= pageCount.value && next !== page.value) { page.value = next; loadMerchants() } }
function showAction(message, type = 'success') { actionMessage.value = message; actionType.value = type; window.setTimeout(() => { actionMessage.value = '' }, 4000) }

function openCreate() { clearForm(); mode.value = 'create'; rememberSnapshot(); drawerVisible.value = true }
async function openDetail(id) { clearForm(); form.id = id; mode.value = 'view'; drawerVisible.value = true; await loadMerchant(id) }
async function openEdit(id) { clearForm(); form.id = id; mode.value = 'edit'; drawerVisible.value = true; await loadMerchant(id); rememberSnapshot() }
async function loadMerchant(id) {
  detailLoading.value = true; drawerError.value = ''
  try { const response = await getMerchantApi(id); if (response.data?.code !== 200) throw new Error(response.data?.message || '商户详情加载失败'); Object.assign(form, response.data.data, { password: '' }) }
  catch (error) { drawerError.value = messageOf(error, '商户详情加载失败，请稍后重试') }
  finally { detailLoading.value = false }
}
function switchToEdit() { mode.value = 'edit'; form.password = ''; drawerError.value = ''; rememberSnapshot() }
function canCloseDrawer() { return !isDirty.value || window.confirm('当前修改尚未保存，确认关闭吗？') }
function requestCloseDrawer() { if (saving.value || !canCloseDrawer()) return; drawerVisible.value = false }

function validate() {
  errors.merchantName = form.merchantName ? '' : '请输入商户名称'
  errors.username = form.username ? '' : '请输入登录账号'
  errors.password = mode.value === 'create' && !form.password ? '请输入登录密码' : ''
  return !Object.values(errors).some(Boolean)
}
async function submitForm() {
  drawerError.value = ''; if (!validate()) return; saving.value = true
  try {
    const payload = { username: form.username, password: form.password, merchantName: form.merchantName, phone: form.phone || null, location: form.location || null }
    if (mode.value === 'edit') payload.id = form.id
    const response = mode.value === 'edit' ? await updateMerchantApi(payload) : await saveMerchantApi(payload)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '商户保存失败')
    drawerVisible.value = false; initialSnapshot.value = ''; showAction(mode.value === 'edit' ? '商户资料已更新' : '商户已新增'); page.value = 1; await loadMerchants()
  } catch (error) { drawerError.value = messageOf(error, '商户保存失败，请稍后重试') }
  finally { saving.value = false }
}
async function toggleStatus(merchant) {
  const next = merchant.status === 1 ? 0 : 1
  if (next === 0 && !window.confirm(`确认禁用“${merchant.merchantName}”吗？禁用后该账号无法登录。`)) return
  statusUpdatingId.value = merchant.id; actionMessage.value = ''
  try { const response = await updateMerchantStatusApi(next, merchant.id); if (response.data?.code !== 200) throw new Error(response.data?.message || '状态更新失败'); showAction(`“${merchant.merchantName}”已${next === 1 ? '启用' : '禁用'}`); await loadMerchants() }
  catch (error) { showAction(messageOf(error, '状态更新失败，请稍后重试'), 'error') }
  finally { statusUpdatingId.value = null }
}
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function beforeUnload(event) { if (!drawerVisible.value || !isDirty.value) return; event.preventDefault(); event.returnValue = '' }
onBeforeRouteLeave(() => (!drawerVisible.value || canCloseDrawer()))
onMounted(() => { window.addEventListener('beforeunload', beforeUnload); loadMerchants() })
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
</script>

<style scoped>
.merchant-page { max-width: 1320px; margin: 0 auto; }.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 16px; }.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; } h2 { color: #20364e; font-size: 16px; font-weight: 600; } button, input, textarea { font: inherit; } button { cursor: pointer; }
.primary-button, .secondary-button { min-height: 36px; padding: 0 15px; border-radius: 3px; }.primary-button { border: 1px solid #1766a6; background: #1766a6; color: #fff; }.secondary-button { border: 1px solid #cbd8e2; background: #fff; color: #1766a6; }.primary-button:disabled, .secondary-button:disabled { cursor: not-allowed; opacity: .6; }
.filter-panel, .table-panel { margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }.filter-panel { display: flex; align-items: end; gap: 16px; padding: 20px; }.filter-field, .form-field { display: flex; flex-direction: column; gap: 8px; color: #6f8192; font-size: 13px; }.filter-field { min-width: 280px; }
input, textarea { width: 100%; padding: 9px 10px; border: 1px solid #d7e1e9; border-radius: 3px; background: #fff; color: #20364e; outline: none; } input { height: 38px; } textarea { resize: vertical; min-height: 78px; } input:focus, textarea:focus { border-color: #1766a6; }.filter-actions, .pagination > div { display: flex; gap: 8px; }
.table-heading { display: flex; align-items: center; justify-content: space-between; padding: 20px; border-bottom: 1px solid #e4eaf0; }.total-count { color: #6f8192; font-size: 12px; }.table-scroll { overflow-x: auto; } table { width: 100%; min-width: 880px; border-collapse: collapse; font-size: 13px; }
th, td { height: 58px; padding: 8px 20px; border-bottom: 1px solid #edf1f4; text-align: left; white-space: nowrap; } th { height: 42px; color: #6f8192; background: #fbfcfd; font-size: 12px; font-weight: 500; } tr:last-child td { border-bottom: 0; } td strong { color: #20364e; font-weight: 600; }.location-text { display: block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; }
.status { display: inline-block; padding: 4px 8px; font-size: 12px; }.status-on { color: #2f8a60; background: #edf8f2; }.status-off { color: #9a6b2f; background: #fff7e9; }.action-column { text-align: right; }.link-button { margin-left: 12px; padding: 0; border: 0; background: transparent; color: #1766a6; }.link-button:disabled { cursor: not-allowed; color: #aebbc5; }.danger-link { color: #c4544e; }
.action-message { padding: 10px 20px; border-bottom: 1px solid #e4eaf0; font-size: 13px; }.action-message.success { color: #2f8a60; background: #f2fbf5; }.action-message.error { color: #c4544e; background: #fff5f4; }.state-panel, .drawer-state { min-height: 190px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 24px; color: #6f8192; font-size: 14px; }.state-error { color: #c4544e; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-top: 1px solid #e4eaf0; color: #6f8192; font-size: 12px; }.drawer-mask { position: fixed; z-index: 40; inset: 0; display: flex; justify-content: flex-end; background: rgba(32, 54, 78, .3); }.drawer { width: min(520px, 100%); height: 100%; display: flex; flex-direction: column; overflow-y: auto; background: #fff; box-shadow: -8px 0 24px rgba(18, 58, 99, .16); }
.drawer-heading { display: flex; align-items: center; justify-content: space-between; padding: 22px 24px; border-bottom: 1px solid #e4eaf0; }.drawer-heading p { margin-bottom: 6px; color: #6f8192; font-size: 12px; }.close-button { padding: 0; border: 0; background: transparent; color: #6f8192; font-size: 26px; line-height: 1; }.drawer-form { min-height: 0; display: flex; flex: 1; flex-direction: column; }.form-content { flex: 1; padding: 24px; }.form-field { margin-bottom: 18px; }
.form-field em, .form-field small, .form-error { color: #c4544e; font-style: normal; }.form-field small { font-size: 12px; }.form-field .field-tip { color: #8b9ba8; }.form-error { margin-bottom: 18px; padding: 10px; border: 1px solid #efc7c4; background: #fff5f4; font-size: 13px; }.drawer-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 18px 24px; border-top: 1px solid #e4eaf0; background: #fff; }
.detail-list { flex: 1; padding: 8px 24px 20px; }.detail-list > div { display: grid; grid-template-columns: 92px 1fr; gap: 18px; padding: 16px 0; border-bottom: 1px solid #edf1f4; font-size: 13px; }.detail-list > div:last-child { border-bottom: 0; } dt { color: #6f8192; } dd { margin: 0; color: #20364e; word-break: break-word; }
@media (max-width: 700px) { .filter-field { min-width: 220px; } }
@media (max-width: 560px) { .page-heading { align-items: stretch; flex-direction: column; }.page-heading .primary-button { align-self: flex-start; } h1 { font-size: 21px; }.filter-panel { align-items: stretch; flex-direction: column; }.filter-field { min-width: 0; }.filter-actions button { flex: 1; }.table-panel { margin-top: 18px; } th, td { padding-right: 14px; padding-left: 14px; }.drawer-heading, .form-content { padding-right: 18px; padding-left: 18px; }.drawer-actions { padding: 16px 18px; } }
</style>
