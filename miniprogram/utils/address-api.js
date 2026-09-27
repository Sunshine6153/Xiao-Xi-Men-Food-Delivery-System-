const apiRequest = require('./request')
function request(path, method = 'GET', data) {
  return apiRequest(`/user/addressBook${path}`, data, method)
}

module.exports = {
  list: () => request('/list'),
  add: data => request('', 'POST', data),
  update: data => request('', 'PUT', data),
  remove: id => request(`/${id}`, 'DELETE'),
  setDefault: id => request(`/${id}/default`, 'PUT')
}
