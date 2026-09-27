const API_BASE_URL = 'http://localhost:8080'
const notifications = require('./utils/order-notifications')
const orderEvents = require('./utils/order-events')

App({
  globalData: { user: null, apiBaseUrl: API_BASE_URL },
  onLaunch() { this.globalData.user = wx.getStorageSync('xiaoximen_user') || null },
  onShow() { this._visible = true; this.startNotifications() },
  onHide() { this._visible = false; this.stopNotifications() },
  startNotifications() {
    if (this._visible && !this._stopNotifications && wx.getStorageSync('xiaoximen_token')) this._stopNotifications = notifications.start()
  },
  stopNotifications() {
    if (this._stopNotifications) this._stopNotifications()
    this._stopNotifications = null
  },
  handleAuthFailure(token, message) {
    if (token !== wx.getStorageSync('xiaoximen_token') || this._authPrompt) return
    this._authPrompt = true
    this.logout()
    wx.showModal({
      title: '登录状态已失效', content: message || '请重新登录后继续操作。', confirmText: '重新登录',
      success: result => {
        if (result.confirm) this.login().catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' }))
      },
      complete: () => { this._authPrompt = false }
    })
  },
  login() {
    if (this._loginPromise) return this._loginPromise
    this._loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: loginResult => {
          if (!loginResult.code) { reject(new Error('微信登录未获取到 code')); return }
          wx.request({
            url: `${this.globalData.apiBaseUrl}/user/login`,
            method: 'POST',
            header: { 'content-type': 'application/json' },
            data: { code: loginResult.code },
            success: response => {
              const result = response.data || {}
              if (response.statusCode === 200 && result.code === 200 && result.data && result.data.token) {
                const storedUser = wx.getStorageSync('xiaoximen_user') || {}
                const previousUser = storedUser.id === result.data.id ? storedUser : {}
                const user = { id: result.data.id, openid: result.data.openid, token: result.data.token, name: previousUser.name || '小西门同学', phone: previousUser.phone || '', avatarUrl: previousUser.avatarUrl || '' }
                wx.setStorageSync('xiaoximen_user', user)
                wx.setStorageSync('xiaoximen_token', result.data.token)
                this.globalData.user = user
                this.stopNotifications()
                this.startNotifications()
                orderEvents.publish({ event: 'LOGIN' })
                resolve(user)
              } else {
                reject(new Error(result.message || `登录失败（${response.statusCode}）`))
              }
            },
            fail: error => reject(new Error(error.errMsg || '无法连接登录服务'))
          })
        },
        fail: error => reject(new Error(error.errMsg || '微信登录调用失败'))
      })
    }).finally(() => { this._loginPromise = null })
    return this._loginPromise
  },
  logout() {
    this.stopNotifications()
    wx.removeStorageSync('xiaoximen_user')
    wx.removeStorageSync('xiaoximen_token')
    wx.removeStorageSync('xiaoximen_address_id')
    wx.removeStorageSync('xiaoximen_address')
    this.globalData.user = null
    orderEvents.publish({ event: 'LOGOUT' })
  }
})
