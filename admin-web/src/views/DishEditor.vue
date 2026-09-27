<template>
  <section class="editor-page">
    <div class="page-heading">
      <div>
        <p class="section-label">菜品管理 / {{ isEdit ? '编辑菜品' : '新增菜品' }}</p>
        <h1>{{ isEdit ? '编辑菜品' : '新增菜品' }}</h1>
      </div>
      <RouterLink class="back-link" to="/session/dishes">返回菜品列表</RouterLink>
    </div>

    <form class="editor-panel" @submit.prevent="submitForm">
      <div v-if="loadError" class="form-alert error-alert">{{ loadError }}</div>
      <div v-if="submitError" class="form-alert error-alert">{{ submitError }}</div>
      <div v-if="successMessage" class="form-alert success-alert">{{ successMessage }}</div>

      <section class="form-section">
        <h2>基本信息</h2>
        <div class="form-grid">
          <label class="form-field">
            <span>菜品名称 <em>*</em></span>
            <input v-model.trim="form.name" type="text" maxlength="50" placeholder="请输入菜品名称" />
            <small v-if="errors.name">{{ errors.name }}</small>
          </label>
          <label class="form-field">
            <span>菜品分类 <em>*</em></span>
            <select v-model="form.categoryId">
              <option value="">请选择菜品分类</option>
              <option v-for="category in activeCategories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
            </select>
            <small v-if="errors.categoryId">{{ errors.categoryId }}</small>
          </label>
          <label class="form-field">
            <span>价格 <em>*</em></span>
            <div class="price-input"><span>¥</span><input v-model="form.price" type="number" min="0.01" max="99999.99" step="0.01" placeholder="请输入价格" /></div>
            <small v-if="errors.price">{{ errors.price }}</small>
          </label>
          <label class="form-field form-field-wide">
            <span>菜品描述</span>
            <textarea v-model.trim="form.description" maxlength="200" rows="3" placeholder="请输入菜品描述（选填）"></textarea>
          </label>
        </div>
      </section>

      <section class="form-section">
        <h2>菜品图片</h2>
        <div class="image-upload-row">
          <div v-if="imagePreview" class="image-preview"><img :src="imagePreview" alt="菜品预览" /><button type="button" class="remove-image" @click="removeImage">移除图片</button></div>
          <label v-else class="upload-box">
            <input type="file" accept="image/*" @change="handleImageChange" />
            <strong>选择图片</strong>
            <span>支持 JPG、PNG 等图片，大小不超过 5MB</span>
          </label>
          <div class="upload-tip">图片会在保存时上传。上传失败时会保留表单内容。</div>
        </div>
        <small v-if="errors.image" class="field-error">{{ errors.image }}</small>
      </section>

      <section class="form-section">
        <div class="section-title-row"><h2>口味设置</h2><button class="secondary-button" type="button" @click="addFlavor">新增口味</button></div>
        <div v-if="form.flavors.length === 0" class="empty-flavors">暂无口味，可按需新增。</div>
        <div v-for="(flavor, index) in form.flavors" :key="flavor.key" class="flavor-row">
          <input v-model.trim="flavor.name" type="text" placeholder="口味名称，如辣度" />
          <input v-model.trim="flavor.value" type="text" placeholder="口味值，如微辣" />
          <button class="link-button danger-link" type="button" @click="removeFlavor(index)">删除</button>
        </div>
        <small v-if="errors.flavors" class="field-error">{{ errors.flavors }}</small>
      </section>

      <div class="form-footer"><RouterLink class="secondary-button footer-button" to="/session/dishes">取消</RouterLink><button class="primary-button footer-button" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存菜品' }}</button></div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCategoriesApi } from '../api/category.js'
import { getDishApi, saveDishApi, updateDishApi, uploadDishImageApi } from '../api/dish.js'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.id))
const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const submitError = ref('')
const successMessage = ref('')
const imageFile = ref(null)
const imagePreview = ref('')
const form = reactive({ id: null, name: '', categoryId: '', price: '', image: '', description: '', flavors: [] })
const errors = reactive({ name: '', categoryId: '', price: '', image: '', flavors: '' })
const activeCategories = computed(() => categories.value.filter((category) => category.status === 1 || category.id === Number(form.categoryId)))

function newFlavor() { return { key: `${Date.now()}-${Math.random()}`, name: '', value: '' } }
function addFlavor() { form.flavors.push(newFlavor()) }
function removeFlavor(index) { form.flavors.splice(index, 1) }

async function loadCategories() {
  const response = await listCategoriesApi()
  if (response.data?.code !== 200) throw new Error(response.data?.message || '分类加载失败')
  categories.value = response.data.data || []
}

async function loadDish() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const response = await getDishApi(route.params.id)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '菜品详情加载失败')
    const dish = response.data.data
    form.id = dish.id
    form.name = dish.name || ''
    form.categoryId = dish.categoryId ? String(dish.categoryId) : ''
    form.price = dish.price ?? ''
    form.image = dish.image || ''
    form.description = dish.description || ''
    form.flavors = (dish.flavors || []).map((flavor) => ({ key: `${flavor.id || Date.now()}-${Math.random()}`, name: flavor.name || '', value: flavor.value || '' }))
    imagePreview.value = form.image ? resolveImageUrl(form.image) : ''
  } catch (error) { loadError.value = error.response?.data?.message || error.message || '菜品详情加载失败，请返回重试' }
  finally { loading.value = false }
}

function handleImageChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  errors.image = ''
  if (!file.type.startsWith('image/')) { errors.image = '请选择图片文件'; return }
  if (file.size > 5 * 1024 * 1024) { errors.image = '图片大小不能超过 5MB'; return }
  imageFile.value = file
  imagePreview.value = URL.createObjectURL(file)
}

function removeImage() { imageFile.value = null; form.image = ''; imagePreview.value = '' }

function validate() {
  errors.name = form.name ? '' : '请输入菜品名称'
  errors.categoryId = form.categoryId ? '' : '请选择菜品分类'
  errors.price = Number(form.price) > 0 ? '' : '请输入大于 0 的价格'
  errors.flavors = form.flavors.some((flavor) => !flavor.name || !flavor.value) ? '请完整填写口味名称和值，或删除空行' : ''
  return !Object.values(errors).some(Boolean)
}

async function submitForm() {
  submitError.value = ''
  successMessage.value = ''
  if (!validate()) return
  saving.value = true
  try {
    if (imageFile.value) {
      const uploadResponse = await uploadDishImageApi(imageFile.value)
      if (uploadResponse.data?.code !== 200) throw new Error(uploadResponse.data?.message || '图片上传失败')
      form.image = uploadResponse.data.data || ''
    }
    const payload = { id: form.id, name: form.name, categoryId: Number(form.categoryId), price: Number(form.price), image: form.image || null, description: form.description || null, flavors: form.flavors.map(({ name, value }) => ({ name, value })) }
    const response = isEdit.value ? await updateDishApi(payload) : await saveDishApi(payload)
    if (response.data?.code !== 200) throw new Error(response.data?.message || '保存失败')
    successMessage.value = '保存成功，正在返回菜品列表...'
    setTimeout(() => router.replace('/session/dishes'), 500)
  } catch (error) { submitError.value = error.response?.data?.message || error.message || '保存失败，请检查后重试' }
  finally { saving.value = false }
}

function resolveImageUrl(image) { if (/^https?:\/\//.test(image)) return image; return `${import.meta.env.VITE_API_BASE_URL || '/api'}${image.startsWith('/') ? image : `/${image}`}` }

onMounted(async () => {
  try { await loadCategories(); await loadDish() }
  catch (error) { loadError.value = error.response?.data?.message || error.message || '页面数据加载失败，请返回重试' }
})
</script>

<style scoped>
.editor-page { max-width: 980px; margin: 0 auto; }
.page-heading { display: flex; align-items: end; justify-content: space-between; min-height: 52px; }
.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; }
.back-link { color: #1766a6; font-size: 13px; text-decoration: none; }
.editor-panel { margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }
.form-section { padding: 24px; border-bottom: 1px solid #e4eaf0; }
h2 { color: #20364e; font-size: 16px; font-weight: 600; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px 24px; margin-top: 20px; }
.form-field { display: flex; flex-direction: column; gap: 8px; color: #6f8192; font-size: 13px; }
.form-field-wide { grid-column: 1 / -1; }
em { color: #c4544e; font-style: normal; }
input, select, textarea { width: 100%; padding: 9px 10px; border: 1px solid #d7e1e9; border-radius: 3px; background: #fff; color: #20364e; font: inherit; outline: none; }
input, select { height: 38px; }
textarea { resize: vertical; min-height: 76px; }
input:focus, select:focus, textarea:focus { border-color: #1766a6; }
.form-field small, .field-error { color: #c4544e; font-size: 12px; }
.price-input { position: relative; }
.price-input > span { position: absolute; top: 10px; left: 10px; color: #6f8192; }
.price-input input { padding-left: 28px; }
.image-upload-row { display: flex; align-items: center; gap: 24px; margin-top: 18px; }
.upload-box { width: 220px; height: 132px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1px dashed #b8c8d5; color: #1766a6; cursor: pointer; }
.upload-box input { display: none; }
.upload-box span, .upload-tip { color: #8b9ba8; font-size: 12px; }
.image-preview { position: relative; width: 220px; height: 132px; }
.image-preview img { width: 100%; height: 100%; object-fit: contain; border: 1px solid #e4eaf0; }
.remove-image { position: absolute; right: 0; bottom: 0; padding: 5px 8px; border: 0; background: rgba(32, 54, 78, 0.8); color: #fff; font-size: 12px; }
.section-title-row { display: flex; align-items: center; justify-content: space-between; }
.secondary-button, .primary-button { min-height: 36px; display: inline-flex; align-items: center; justify-content: center; padding: 0 15px; border-radius: 3px; font: inherit; text-decoration: none; cursor: pointer; }
.secondary-button { border: 1px solid #cbd8e2; background: #fff; color: #1766a6; }
.primary-button { border: 1px solid #1766a6; background: #1766a6; color: #fff; }
.empty-flavors { margin-top: 18px; color: #8b9ba8; font-size: 13px; }
.flavor-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 12px; margin-top: 12px; }
.link-button { padding: 0; border: 0; background: transparent; color: #1766a6; font: inherit; cursor: pointer; }
.danger-link { color: #c4544e; }
.form-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 20px 24px; }
.footer-button { min-width: 92px; }
.footer-button:disabled { opacity: .6; cursor: not-allowed; }
.form-alert { margin: 20px 24px 0; padding: 11px 12px; border: 1px solid; font-size: 13px; }
.error-alert { border-color: #efc7c4; background: #fff5f4; color: #c4544e; }
.success-alert { border-color: #c8e4d5; background: #f2fbf5; color: #2f8a60; }
@media (max-width: 700px) { .form-grid { grid-template-columns: 1fr; } .form-field-wide { grid-column: auto; } }
@media (max-width: 560px) { .page-heading { align-items: start; flex-direction: column; gap: 8px; } h1 { font-size: 21px; } .form-section { padding: 18px; } .image-upload-row { align-items: start; flex-direction: column; gap: 12px; } .flavor-row { grid-template-columns: 1fr; } .flavor-row .link-button { justify-self: start; } .form-footer { padding: 16px 18px; } }
</style>
