import request from '../utils/request.js'

export function pageDishesApi(params) {
  return request.get('/admin/dish/page', { params })
}

export function getDishApi(id) {
  return request.get(`/admin/dish/${id}`)
}

export function saveDishApi(payload) {
  return request.post('/admin/dish', payload)
}

export function updateDishApi(payload) {
  return request.put('/admin/dish', payload)
}

export function uploadDishImageApi(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/dish/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateDishStatusApi(status, id) {
  return request.post(`/admin/dish/status/${status}`, null, { params: { id } })
}

export function deleteDishApi(id) {
  return request.delete(`/admin/dish/${id}`)
}
