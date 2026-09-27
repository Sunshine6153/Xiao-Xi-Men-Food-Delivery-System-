import request from '../utils/request.js'

/**
 * 商户登录
 * @param {{ username: string, password: string }} payload
 * @returns {Promise<{ code: number, message: string, data: { id: number, name: string, token: string, role: 'ADMIN' | 'MERCHANT' } | null }>}
 */
export function loginApi(payload) {
  return request.post('/admin/merchant/login', {
    username: payload.username,
    password: payload.password
  }).then((res) => res.data)
}

export function logoutApi() {
  return request.post('/admin/merchant/logout', {}).then((res) => res.data)
}

export function getIdentityApi() {
  return request.get('/admin/auth/me').then((res) => res.data)
}
