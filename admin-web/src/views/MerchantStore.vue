<template>
  <section class="store-page">
    <header class="page-heading">
      <div><p>店铺设置</p><h1>店铺资料</h1></div>
      <span v-if="!loading && !loadError" class="business-badge" :class="form.businessStatus === 1 ? 'open' : 'closed'">
        {{ form.businessStatus === 1 ? '营业中' : '暂停营业' }}
      </span>
    </header>

    <div v-if="loading" class="state-panel">正在加载店铺资料...</div>
    <div v-else-if="loadError" class="state-panel error-panel"><p>{{ loadError }}</p><button type="button" @click="loadProfile">重新加载</button></div>
    <div v-else class="store-grid">
      <form class="profile-card" @submit.prevent="saveProfile">
        <div class="card-heading"><div><h2>基本资料</h2><p>这些信息会展示在小程序菜品页面中</p></div></div>
        <label><span>店铺名称</span><input v-model.trim="form.merchantName" maxlength="128" placeholder="请输入店铺名称" /></label>
        <label><span>联系电话</span><input v-model.trim="form.phone" maxlength="32" placeholder="请输入联系电话" /></label>
        <label><span>店铺位置</span><input v-model.trim="form.location" maxlength="256" placeholder="例如：一食堂一楼 03 号窗口" /></label>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <div class="card-actions"><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中' : '保存资料' }}</button></div>
      </form>

      <section class="status-card">
        <div class="card-heading"><div><h2>营业状态</h2><p>暂停营业后，用户端将不再展示本店菜品</p></div></div>
        <div class="status-control">
          <div><strong>{{ form.businessStatus === 1 ? '当前正常营业' : '当前暂停营业' }}</strong><p>{{ form.businessStatus === 1 ? '用户可以浏览并购买本店菜品' : '商户账号仍可登录和管理数据' }}</p></div>
          <button type="button" :class="form.businessStatus === 1 ? 'stop-button' : 'primary-button'" :disabled="statusSaving" @click="toggleBusinessStatus">
            {{ statusSaving ? '处理中' : form.businessStatus === 1 ? '暂停营业' : '恢复营业' }}
          </button>
        </div>
      </section>
    </div>
    <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getMerchantProfileApi, updateBusinessStatusApi, updateMerchantProfileApi } from '../api/merchant.js'

const form = reactive({ merchantName: '', phone: '', location: '', businessStatus: 1 })
const loading = ref(true), saving = ref(false), statusSaving = ref(false)
const loadError = ref(''), formError = ref(''), successMessage = ref('')

function messageOf(error, fallback) { return error?.response?.data?.message || error?.message || fallback }
function business(response) { if (response.data?.code !== 200) throw new Error(response.data?.message || '请求失败'); return response.data.data }
function showSuccess(message) { successMessage.value = message; window.setTimeout(() => { successMessage.value = '' }, 2200) }

async function loadProfile() {
  loading.value = true; loadError.value = ''
  try {
    const data = business(await getMerchantProfileApi()) || {}
    Object.assign(form, { merchantName: data.merchantName || '', phone: data.phone || '', location: data.location || '', businessStatus: Number(data.businessStatus ?? 1) })
  } catch (error) { loadError.value = messageOf(error, '店铺资料加载失败') }
  finally { loading.value = false }
}

async function saveProfile() {
  formError.value = ''
  if (!form.merchantName) { formError.value = '请输入店铺名称'; return }
  saving.value = true
  try {
    business(await updateMerchantProfileApi({ merchantName: form.merchantName, phone: form.phone || null, location: form.location || null }))
    localStorage.setItem('xiaoximen_merchant_name', form.merchantName)
    window.dispatchEvent(new Event('xiaoximen-merchant-profile-updated'))
    showSuccess('店铺资料已保存')
  } catch (error) { formError.value = messageOf(error, '保存失败') }
  finally { saving.value = false }
}

async function toggleBusinessStatus() {
  const next = form.businessStatus === 1 ? 0 : 1
  if (next === 0 && !window.confirm('暂停营业后，用户端将暂时看不到本店菜品，是否继续？')) return
  statusSaving.value = true
  try { business(await updateBusinessStatusApi(next)); form.businessStatus = next; showSuccess(next === 1 ? '已恢复营业' : '已暂停营业') }
  catch (error) { window.alert(messageOf(error, '营业状态更新失败')) }
  finally { statusSaving.value = false }
}

onMounted(loadProfile)
</script>

<style scoped>
.store-page{max-width:1120px;margin:0 auto;color:#20364e}.page-heading{min-height:52px;display:flex;align-items:end;justify-content:space-between}.page-heading p,.card-heading p,.status-control p{color:#758696;font-size:12px}.page-heading h1{margin-top:7px;font-size:24px;font-weight:600}.business-badge{padding:6px 10px;font-size:12px}.business-badge.open{background:#edf8f2;color:#2f8a60}.business-badge.closed{background:#fff0ef;color:#a34c47}.store-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(300px,.85fr);gap:20px;margin-top:24px}.profile-card,.status-card,.state-panel{border:1px solid #e4eaf0;background:#fff}.profile-card,.status-card{padding:24px}.card-heading{padding-bottom:19px;border-bottom:1px solid #edf1f4}.card-heading h2{font-size:16px}.card-heading p{margin-top:6px}.profile-card label{display:flex;flex-direction:column;gap:8px;margin-top:19px;color:#52677b;font-size:13px}.profile-card input{height:40px;padding:0 12px;border:1px solid #d7e1e9;border-radius:3px;color:#20364e;font:inherit}.card-actions{display:flex;justify-content:flex-end;margin-top:24px}.primary-button,.stop-button,.state-panel button{min-height:38px;padding:0 17px;border-radius:3px;font:inherit;cursor:pointer}.primary-button,.state-panel button{border:1px solid #1766a6;background:#1766a6;color:#fff}.stop-button{border:1px solid #d7aaa7;background:#fff;color:#a34c47}.status-control{display:flex;flex-direction:column;align-items:start;gap:24px;padding-top:23px}.status-control strong{font-size:15px}.status-control p{margin-top:8px;line-height:1.65}.form-error{margin-top:14px;color:#b5534e;font-size:12px}.success-message{position:fixed;right:28px;bottom:28px;padding:11px 16px;background:#20364e;color:#fff;font-size:13px}.state-panel{min-height:240px;margin-top:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;color:#758696}.error-panel{color:#b5534e}button:disabled{cursor:not-allowed;opacity:.55}@media(max-width:760px){.store-grid{grid-template-columns:1fr}.page-heading{align-items:start;flex-direction:column;gap:14px}.profile-card,.status-card{padding:20px}}
</style>
