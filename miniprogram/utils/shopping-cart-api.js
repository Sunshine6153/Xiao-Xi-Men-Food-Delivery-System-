const apiRequest = require('./request')
function request(path, method = 'GET', data) {
  return apiRequest(`/user/shoppingCart${path}`, data, method)
}

function normalize(item) {
  const baseUrl = getApp().globalData.apiBaseUrl
  const image = item.image && item.image.indexOf('/') === 0 ? `${baseUrl}${item.image}` : item.image
  return {
    ...item,
    key: String(item.id),
    merchant: item.merchantName || `商户 ${item.merchantId}`,
    location: item.merchantLocation || '小西门美食街',
    flavor: item.dishFlavor || '默认',
    image,
    price: Number(item.price || 0),
    number: Number(item.number || 0)
  }
}

module.exports = {
  add: data => request('/add', 'POST', data),
  list: () => request('/list').then(items => (items || []).map(normalize)),
  update: data => request('/update', 'PUT', data),
  remove: id => request(`/deleteItem?id=${id}`, 'DELETE'),
  clear: () => request('/delete', 'DELETE')
}
