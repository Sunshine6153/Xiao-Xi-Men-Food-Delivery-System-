import request from '../utils/request.js'

export function pageMerchantsApi(params) {
  return request.post('/admin/merchant/page', null, { params })
}

export function getMerchantApi(id) {
  return request.get(`/admin/merchant/${id}`)
}

export function saveMerchantApi(payload) {
  return request.post('/admin/merchant/save', payload)
}

export function updateMerchantApi(payload) {
  return request.post('/admin/merchant/update', payload)
}

export function updateMerchantStatusApi(status, id) {
  return request.post(`/admin/merchant/status/${status}`, null, { params: { id } })
}

export function getMerchantProfileApi() {
  return request.get('/admin/merchant/profile')
}

export function updateMerchantProfileApi(payload) {
  return request.put('/admin/merchant/profile', payload)
}

export function updateBusinessStatusApi(status) {
  return request.put(`/admin/merchant/business-status/${status}`)
}
