import request from '../utils/request.js'

export function pageAdminOrdersApi(params) {
  return request.get('/admin/platform/order/page', { params })
}

export function getAdminOrderApi(id) {
  return request.get(`/admin/platform/order/${id}`)
}
