<template>
  <section class="category-page">
    <div class="page-heading">
      <div>
        <p class="section-label">菜品管理</p>
        <h1>分类管理</h1>
      </div>
      <el-button class="ui-action primary-button" native-type="button" @click="openCreate" type="primary" :icon="Plus">新增分类</el-button>
    </div>

    <section class="table-panel">
      <div class="table-heading">
        <div>
          <h2>全部分类</h2>
          <p>分类由系统管理员统一维护，商家仅在菜品中使用。</p>
        </div>
        <span v-if="!loading && !loadError" class="total-count">共 {{ categories.length }} 项</span>
      </div>

      <div v-if="actionMessage" class="action-message" :class="actionType">{{ actionMessage }}</div>
      <div v-if="loading" class="state-panel">正在加载分类...</div>
      <div v-else-if="loadError" class="state-panel state-error">
        <p>{{ loadError }}</p>
        <el-button class="ui-action secondary-button" native-type="button" @click="loadCategories" :icon="Refresh">重新加载</el-button>
      </div>
      <div v-else-if="categories.length === 0" class="state-panel">
        <p>暂无分类</p>
        <el-button class="ui-action secondary-button" native-type="button" @click="openCreate" :icon="Plus">新增第一个分类</el-button>
      </div>
      <div v-else class="table-scroll">
        <table>
          <thead>
            <tr><th>分类名称</th><th>排序</th><th>状态</th><th>更新时间</th><th class="action-column">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="category in categories" :key="category.id">
              <td><strong>{{ category.name }}</strong></td>
              <td>{{ category.sort ?? 0 }}</td>
              <td><span class="status" :class="category.status === 1 ? 'status-on' : 'status-off'">{{ category.status === 1 ? '已启用' : '已停用' }}</span></td>
              <td>{{ formatDate(category.updateTime || category.createTime) }}</td>
              <td class="action-column">
                <el-button class="ui-action link-button" native-type="button" @click="openDetail(category.id)" type="primary" link>查看</el-button>
                <el-button class="ui-action link-button" native-type="button" @click="openEdit(category.id)" type="primary" link>编辑</el-button>
                <el-button class="ui-action link-button" native-type="button" :disabled="statusUpdatingId === category.id" @click="toggleStatus(category)" type="primary" link :loading="statusUpdatingId === category.id">{{ statusUpdatingId === category.id ? '处理中' : category.status === 1 ? '停用' : '启用' }}</el-button>
                <el-button class="ui-action link-button danger-link" native-type="button" :disabled="deletingId === category.id" @click="removeCategory(category)" type="primary" link :loading="deletingId === category.id">{{ deletingId === category.id ? '删除中' : '删除' }}</el-button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="dialogVisible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog" role="dialog" aria-modal="true" :aria-label="dialogTitle">
        <div class="dialog-heading">
          <h2>{{ dialogTitle }}</h2>
          <button class="close-button" type="button" aria-label="关闭弹窗" @click="closeDialog">×</button>
        </div>

        <div v-if="detailLoading" class="dialog-state">正在加载分类...</div>
        <div v-else-if="dialogError && mode === 'view'" class="dialog-state state-error">
          <p>{{ dialogError }}</p>
          <el-button class="ui-action secondary-button" native-type="button" @click="loadCategory(form.id)" :icon="Refresh">重新加载</el-button>
        </div>
        <template v-else-if="mode === 'view'">
          <dl class="detail-list">
            <div><dt>分类名称</dt><dd>{{ form.name }}</dd></div>
            <div><dt>显示排序</dt><dd>{{ form.sort }}</dd></div>
            <div><dt>当前状态</dt><dd>{{ form.status === 1 ? '已启用' : '已停用' }}</dd></div>
            <div><dt>创建时间</dt><dd>{{ formatDate(form.createTime) }}</dd></div>
            <div><dt>更新时间</dt><dd>{{ formatDate(form.updateTime) }}</dd></div>
          </dl>
          <div class="dialog-actions"><el-button class="ui-action secondary-button" native-type="button" @click="closeDialog">关闭</el-button><el-button class="ui-action primary-button" native-type="button" @click="switchToEdit" type="primary">编辑分类</el-button></div>
        </template>
        <form v-else @submit.prevent="submitForm">
          <div class="dialog-body">
            <p v-if="dialogError" class="form-error">{{ dialogError }}</p>
            <label class="form-field">
              <span>分类名称 <em>*</em></span>
              <input v-model.trim="form.name" type="text" maxlength="64" placeholder="请输入分类名称" autofocus />
              <small v-if="errors.name">{{ errors.name }}</small>
            </label>
            <label class="form-field">
              <span>显示排序 <em>*</em></span>
              <input v-model="form.sort" type="number" min="0" max="9999" step="1" placeholder="数字越小越靠前" />
              <small v-if="errors.sort">{{ errors.sort }}</small>
            </label>
            <p class="sort-tip">排序数字越小，在商家菜品筛选和用户端分类中越靠前。</p>
          </div>
          <div class="dialog-actions"><el-button class="ui-action secondary-button" native-type="button" :disabled="saving" @click="closeDialog" :loading="saving">取消</el-button><el-button class="ui-action primary-button" native-type="submit" :disabled="saving" type="primary" :loading="saving">{{ saving ? '保存中...' : '保存' }}</el-button></div>
        </form>
      </section>
    </div>
  </section>
</template>

<script setup>
import { Plus, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteCategoryApi, getCategoryApi, listCategoriesApi, saveCategoryApi, updateCategoryApi, updateCategoryStatusApi } from '../api/category.js'

const categories = ref([])
const loading = ref(false)
const loadError = ref('')
const actionMessage = ref('')
const actionType = ref('success')
const dialogVisible = ref(false)
const detailLoading = ref(false)
const dialogError = ref('')
const saving = ref(false)
const mode = ref('create')
const statusUpdatingId = ref(null)
const deletingId = ref(null)
const form = reactive({ id: null, name: '', sort: 0, status: 1, createTime: '', updateTime: '' })
const errors = reactive({ name: '', sort: '' })
const dialogTitle = computed(() => mode.value === 'create' ? '新增分类' : mode.value === 'edit' ? '编辑分类' : '分类详情')

function responseMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function resetForm() {
  Object.assign(form, { id: null, name: '', sort: 0, status: 1, createTime: '', updateTime: '' })
  errors.name = ''
  errors.sort = ''
  dialogError.value = ''
}

async function loadCategories() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await listCategoriesApi()
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类加载失败')
    categories.value = response.data.data || []
  } catch (error) {
    categories.value = []
    loadError.value = responseMessage(error, '分类加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function showAction(message, type = 'success') {
  actionMessage.value = message
  actionType.value = type
  window.setTimeout(() => { actionMessage.value = '' }, 4000)
}

function openCreate() {
  resetForm()
  mode.value = 'create'
  dialogVisible.value = true
}

async function openDetail(id) {
  resetForm()
  form.id = id
  mode.value = 'view'
  dialogVisible.value = true
  await loadCategory(id)
}

async function openEdit(id) {
  resetForm()
  form.id = id
  mode.value = 'edit'
  dialogVisible.value = true
  await loadCategory(id)
}

async function loadCategory(id) {
  detailLoading.value = true
  dialogError.value = ''
  try {
    const response = await getCategoryApi(id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类详情加载失败')
    Object.assign(form, response.data.data)
  } catch (error) {
    dialogError.value = responseMessage(error, '分类详情加载失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

function switchToEdit() {
  dialogError.value = ''
  mode.value = 'edit'
}

function closeDialog() {
  if (saving.value) return
  dialogVisible.value = false
}

function validate() {
  errors.name = form.name ? '' : '请输入分类名称'
  const sort = Number(form.sort)
  errors.sort = Number.isInteger(sort) && sort >= 0 && sort <= 9999 ? '' : '请输入 0–9999 的整数'
  return !errors.name && !errors.sort
}

async function submitForm() {
  dialogError.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const payload = { name: form.name, sort: Number(form.sort) }
    if (mode.value === 'edit') payload.id = form.id
    const response = mode.value === 'edit' ? await updateCategoryApi(payload) : await saveCategoryApi(payload)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类保存失败')
    const message = mode.value === 'edit' ? '分类已更新' : '分类已新增'
    dialogVisible.value = false
    showAction(message)
    await loadCategories()
  } catch (error) {
    dialogError.value = responseMessage(error, '分类保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(category) {
  statusUpdatingId.value = category.id
  actionMessage.value = ''
  try {
    const nextStatus = category.status === 1 ? 0 : 1
    const response = await updateCategoryStatusApi(nextStatus, category.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类状态更新失败')
    showAction(`“${category.name}”已${nextStatus === 1 ? '启用' : '停用'}`)
    await loadCategories()
  } catch (error) {
    showAction(responseMessage(error, '分类状态更新失败，请稍后重试'), 'error')
  } finally {
    statusUpdatingId.value = null
  }
}

async function removeCategory(category) {
  if (!window.confirm(`确认删除“${category.name}”吗？如果分类下仍有菜品，系统会拒绝删除。`)) return
  deletingId.value = category.id
  actionMessage.value = ''
  try {
    const response = await deleteCategoryApi(category.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '分类删除失败')
    showAction('分类已删除')
    await loadCategories()
  } catch (error) {
    showAction(responseMessage(error, '分类删除失败，请稍后重试'), 'error')
  } finally {
    deletingId.value = null
  }
}

function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(loadCategories)
</script>

<style scoped>
.category-page { max-width: 1200px; margin: 0 auto; }
.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 16px; }
.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; }
h2 { color: #20364e; font-size: 16px; font-weight: 600; }
button, input { font: inherit; }
button { cursor: pointer; }
.primary-button, .secondary-button { min-height: 36px; padding: 0 15px; border-radius: 3px; }
.primary-button { border: 1px solid #1766a6; background: #1766a6; color: #fff; }
.secondary-button { border: 1px solid #cbd8e2; background: #fff; color: #1766a6; }
.primary-button:disabled, .secondary-button:disabled { cursor: not-allowed; opacity: .6; }
.table-panel { margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }
.table-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 20px; border-bottom: 1px solid #e4eaf0; }
.table-heading p { margin-top: 6px; color: #6f8192; font-size: 12px; }
.total-count { flex: none; color: #6f8192; font-size: 12px; }
.table-scroll { overflow-x: auto; }
table { width: 100%; min-width: 720px; border-collapse: collapse; font-size: 13px; }
th, td { height: 58px; padding: 8px 20px; border-bottom: 1px solid #edf1f4; text-align: left; white-space: nowrap; }
th { height: 42px; color: #6f8192; background: #fbfcfd; font-size: 12px; font-weight: 500; }
tr:last-child td { border-bottom: 0; }
td strong { color: #20364e; font-weight: 600; }
.status { display: inline-block; padding: 4px 8px; font-size: 12px; }
.status-on { color: #2f8a60; background: #edf8f2; }
.status-off { color: #9a6b2f; background: #fff7e9; }
.action-column { text-align: right; }
.link-button { margin-left: 12px; padding: 0; border: 0; background: transparent; color: #1766a6; }
.link-button:disabled { cursor: not-allowed; color: #aebbc5; }
.danger-link { color: #c4544e; }
.action-message { padding: 10px 20px; border-bottom: 1px solid #e4eaf0; font-size: 13px; }
.action-message.success { color: #2f8a60; background: #f2fbf5; }
.action-message.error { color: #c4544e; background: #fff5f4; }
.state-panel { min-height: 190px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; padding: 24px; color: #6f8192; font-size: 14px; }
.state-error { color: #c4544e; }
.dialog-mask { position: fixed; z-index: 40; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(32, 54, 78, .32); }
.dialog { width: min(440px, 100%); max-height: calc(100vh - 40px); overflow-y: auto; border: 1px solid #d9e2ea; background: #fff; box-shadow: 0 14px 36px rgba(18, 58, 99, .16); }
.dialog-heading { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid #e4eaf0; }
.close-button { padding: 0; border: 0; background: transparent; color: #6f8192; font-size: 26px; line-height: 1; }
.dialog-body { padding: 20px; }
.form-field { display: flex; flex-direction: column; gap: 8px; margin-bottom: 18px; color: #6f8192; font-size: 13px; }
.form-field input { width: 100%; height: 38px; padding: 0 10px; border: 1px solid #d7e1e9; border-radius: 3px; color: #20364e; outline: none; }
.form-field input:focus { border-color: #1766a6; }
.form-field em, .form-field small, .form-error { color: #c4544e; font-style: normal; }
.form-field small { font-size: 12px; }
.form-error { margin-bottom: 16px; padding: 10px; border: 1px solid #efc7c4; background: #fff5f4; font-size: 13px; }
.sort-tip { color: #8b9ba8; font-size: 12px; line-height: 1.6; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 20px; border-top: 1px solid #e4eaf0; }
.dialog-state { min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 24px; color: #6f8192; }
.detail-list { padding: 6px 20px 14px; }
.detail-list > div { display: grid; grid-template-columns: 88px 1fr; gap: 16px; padding: 14px 0; border-bottom: 1px solid #edf1f4; font-size: 13px; }
.detail-list > div:last-child { border-bottom: 0; }
dt { color: #6f8192; }
dd { margin: 0; color: #20364e; word-break: break-word; }
@media (max-width: 560px) {
  .page-heading { align-items: stretch; flex-direction: column; }
  .page-heading .primary-button { align-self: flex-start; }
  h1 { font-size: 21px; }
  .table-panel { margin-top: 18px; }
  .table-heading { align-items: start; }
  th, td { padding-right: 14px; padding-left: 14px; }
  .dialog-mask { align-items: flex-start; padding: 14px; }
}
</style>
