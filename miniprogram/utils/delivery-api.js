const orderApi = require('./order-api')

function normalizePage(result) {
  return {
    total: Number((result && result.total) || 0),
    records: ((result && result.records) || []).map(orderApi.normalizeOrder)
  }
}

module.exports = {
  available: data => orderApi.request('/user/delivery/available', data, 'GET').then(normalizePage),
  mine: data => orderApi.request('/user/delivery/mine', data, 'GET').then(normalizePage),
  detail: id => orderApi.request(`/user/delivery/${id}`, null, 'GET').then(orderApi.normalizeOrder),
  accept: id => orderApi.request(`/user/delivery/accept/${id}`, null, 'PUT'),
  start: id => orderApi.request(`/user/delivery/start/${id}`, null, 'PUT'),
  complete: id => orderApi.request(`/user/delivery/complete/${id}`, null, 'PUT')
}
