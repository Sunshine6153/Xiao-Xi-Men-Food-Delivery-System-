import axios from 'axios'

// 后端接口约定(见 sky-server MerchantController):
//   POST {baseURL}/admin/merchant/login
//   请求: { username, password } (明文,后端做 MD5)
//   响应: { code: 200 成功, message, data: { id, name, token, role } }
// 后端业务错误可能以 HTTP 200 返回,调用方需要检查业务 code。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 登录成功后后端返回 JWT,后续请求自动携带
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('xiaoximen_token')
  if (token) {
    config.headers.token = token
  }
  return config
})

function handleExpiredSession(config, code) {
  if (code !== 401 || config?.url?.endsWith('/admin/merchant/login')) return
  if (config?.headers?.token !== localStorage.getItem('xiaoximen_token')) return
  clearSession()
  if (window.location.pathname !== '/login') window.location.assign('/login')
}

request.interceptors.response.use((response) => {
  const code = response.data?.code
  handleExpiredSession(response.config, code)

  return response
}, (error) => {
  handleExpiredSession(error.config, error.response?.status)
  return Promise.reject(error)
})

export function clearSession() {
  for (const key of ['xiaoximen_token', 'xiaoximen_merchant_id', 'xiaoximen_merchant_name', 'xiaoximen_role']) {
    localStorage.removeItem(key)
  }
  window.dispatchEvent(new Event('xiaoximen-session-ended'))
}

export default request
