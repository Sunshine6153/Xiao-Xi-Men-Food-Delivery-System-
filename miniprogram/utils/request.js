function request(path, data, method = 'POST') {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('xiaoximen_token')
    if (!token) {
      const error = new Error('请先登录')
      error.authRequired = true
      reject(error)
      return
    }
    wx.request({
      url: `${getApp().globalData.apiBaseUrl}${path}`, method, data,
      header: { token, 'content-type': 'application/json' },
      success: response => {
        if (token !== wx.getStorageSync('xiaoximen_token')) {
          const error = new Error('登录状态已变化，请重新操作')
          error.authRequired = true
          error.authHandled = true
          reject(error)
          return
        }
        const result = response.data || {}
        if (response.statusCode === 200 && result.code === 200) { resolve(result.data); return }
        const error = new Error(result.message || `请求失败（${response.statusCode}）`)
        error.authRequired = response.statusCode === 401 || result.code === 401
        if (error.authRequired) {
          error.authHandled = true
          getApp().handleAuthFailure(token, error.message)
        }
        reject(error)
      },
      fail: error => reject(new Error(error.errMsg || '无法连接服务，请稍后重试'))
    })
  })
}
module.exports = request
