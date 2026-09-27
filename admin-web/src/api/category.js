import request from '../utils/request.js'

export function listCategoriesApi() {
  return request.get('/admin/category/list')
}

export function getCategoryApi(id) {
  return request.get(`/admin/category/${id}`)
}

export function saveCategoryApi(payload) {
  return request.post('/admin/category', payload)
}

export function updateCategoryApi(payload) {
  return request.put('/admin/category', payload)
}

export function updateCategoryStatusApi(status, id) {
  return request.post(`/admin/category/status/${status}`, null, { params: { id } })
}

export function deleteCategoryApi(id) {
  return request.delete(`/admin/category/${id}`)
}
