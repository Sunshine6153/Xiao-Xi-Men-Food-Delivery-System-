<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <section class="brand-panel">
      <div class="brand-content">
        <div class="logo">小西门</div>
        <h1>商户管理平台</h1>
        <p class="subtitle">蓝白清爽 · 安全高效 · 一处登录,管理小店</p>
      </div>
    </section>

    <!-- 右侧登录区 -->
    <section class="form-panel">
      <div class="login-card">
        <h2>欢迎回来</h2>
        <p class="desc">请使用商户账号登录</p>

        <form @submit.prevent="handleLogin">
          <label class="field">
            <span class="field-label">账号</span>
            <input
              v-model.trim="form.username"
              type="text"
              placeholder="请输入商户账号"
              autocomplete="username"
              :disabled="loading"
            />
          </label>

          <label class="field">
            <span class="field-label">密码</span>
            <div class="password-wrap">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                :disabled="loading"
              />
              <button
                type="button"
                class="eye-btn"
                :disabled="loading"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
          </label>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
          <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            {{ loading ? '登录中…' : '登 录' }}
          </button>
        </form>

        <div class="card-footer">
          <span>登录即代表同意商户服务协议</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getIdentityApi, loginApi } from '../api/auth.js'
import { useRouter } from 'vue-router'
import { clearSession } from '../utils/request.js'

const form = ref({ username: '', password: '' })
const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

function extractErrorMessage(err) {
  // 后端 Result 风格: { code: 0, message }
  const data = err?.response?.data
  if (data && typeof data.message === 'string' && data.message) return data.message
  if (typeof data === 'string' && data) return data
  if (err?.response?.status) return `登录失败,服务返回 ${err.response.status},请确认后端已启动`
  if (err?.code === 'ECONNABORTED') return '请求超时,请确认后端 http://localhost:8080 已启动'
  if (err?.message === 'Network Error') return '网络异常,请确认后端已启动且代理配置正确'
  return '登录失败,请稍后重试'
}

async function handleLogin() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.value.username) {
    errorMsg.value = '请输入商户账号'
    return
  }
  if (!form.value.password) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    // 后端: POST /admin/merchant/login { username, password } -> Result<MerchantLoginVO>
    const result = await loginApi({
      username: form.value.username,
      password: form.value.password
    })

    if (result && result.code === 200 && result.data?.token) {
      if (!['ADMIN', 'MERCHANT'].includes(result.data.role)) {
        throw new Error('登录响应缺少有效身份，请联系管理员')
      }
      localStorage.setItem('xiaoximen_token', result.data.token)
      localStorage.setItem('xiaoximen_merchant_id', String(result.data.id ?? ''))
      localStorage.setItem('xiaoximen_merchant_name', result.data.name ?? '')
      const identity = await getIdentityApi()
      if (identity.code !== 200 || identity.data?.role !== result.data.role || String(identity.data?.id) !== String(result.data.id)) {
        clearSession()
        throw new Error('登录身份校验失败，请重新登录')
      }
      localStorage.setItem('xiaoximen_role', identity.data.role)
      router.replace(identity.data.role === 'ADMIN' ? '/admin' : '/session')
    } else {
      errorMsg.value = result?.message || '登录失败:账号或密码错误'
    }
  } catch (err) {
    clearSession()
    errorMsg.value = err?.message?.startsWith('登录') ? err.message : extractErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 蓝白主题 */
.login-page {
  min-height: 100%;
  display: flex;
  background: #f2f7ff;
}

/* 左侧品牌区:校门照片 + 蓝色罩衬,保证白色文字可读 */
.brand-panel {
  position: relative;
  flex: 1.1;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(180deg, rgba(29, 78, 216, 0.15) 0%, rgba(30, 58, 95, 0.55) 100%),
    url('../assets/brand-bg.png') center / cover no-repeat;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
  padding: 48px;
  text-shadow: 0 2px 12px rgba(15, 35, 70, 0.55);
}

.logo {
  display: inline-block;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 4px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 14px;
  padding: 10px 22px;
  margin-bottom: 24px;
}

.brand-content h1 {
  font-size: 34px;
  font-weight: 800;
  margin-bottom: 12px;
}

.subtitle {
  font-size: 15px;
  opacity: 0.9;
  margin-bottom: 28px;
}

/* 右侧表单区 */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: #fff;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(37, 99, 235, 0.12);
  padding: 36px 32px;
}

.login-card h2 {
  font-size: 24px;
  color: #1e3a5f;
  margin-bottom: 6px;
}

.desc {
  font-size: 14px;
  color: #7b93b0;
  margin-bottom: 24px;
}

.field {
  display: block;
  margin-bottom: 18px;
}

.field-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e3a5f;
  margin-bottom: 8px;
}

.field input {
  width: 100%;
  height: 44px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  padding: 0 14px;
  font-size: 15px;
  color: #1e3a5f;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #f8fbff;
}

.field input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
  background: #fff;
}

.password-wrap {
  position: relative;
}

.password-wrap input {
  padding-right: 64px;
}

.eye-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}

.error-msg {
  font-size: 13px;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 9px 12px;
  margin-bottom: 14px;
}

.success-msg {
  font-size: 13px;
  color: #15803d;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 9px 12px;
  margin-bottom: 14px;
}

.login-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: opacity 0.2s, transform 0.1s;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.92;
}

.login-btn:active:not(:disabled) {
  transform: scale(0.99);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.card-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #9db1c7;
}

/* 小屏:隐藏品牌区,表单全宽 */
@media (max-width: 820px) {
  .brand-panel {
    display: none;
  }
}
</style>
