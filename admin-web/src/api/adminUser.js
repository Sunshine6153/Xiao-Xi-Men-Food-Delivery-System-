import request from '../utils/request.js'

export function pageAdminUsersApi(params) {
  return request.get('/admin/user/page', { params })
}

export function getAdminUserApi(id) {
  return request.get(`/admin/user/${id}`)
}

export function updateAdminUserStatusApi(status, id) {
  return request.post(`/admin/user/status/${status}`, null, { params: { id } })
}
