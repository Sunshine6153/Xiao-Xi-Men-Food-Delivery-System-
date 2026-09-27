import request from '../utils/request.js'

export function pageMerchantOrdersApi(params) {
  return request.get('/admin/order/page', { params })
}

export function getMerchantOrderApi(id) {
  return request.get(`/admin/order/${id}`)
}

export function confirmMerchantOrderApi(id) {
  return request.put(`/admin/order/confirm/${id}`)
}

export function completeMerchantOrderApi(id) {
  return request.put(`/admin/order/complete/${id}`)
}
