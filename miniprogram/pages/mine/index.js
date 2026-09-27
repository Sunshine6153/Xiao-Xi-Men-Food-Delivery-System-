const addressApi = require('../../utils/address-api')
const orderApi = require('../../utils/order-api')
const deliveryApi = require('../../utils/delivery-api')
const events = require('../../utils/order-events')

Page({
  data: { user: null, orderCount: 0, carryCount: 0, addressCount: 0 },
  onShow() { this.refresh(); this._unsubscribe = events.subscribe(() => this.refresh()) },
  onHide() { if (this._unsubscribe) this._unsubscribe(); this._unsubscribe = null },
  onUnload() { if (this._unsubscribe) this._unsubscribe() },
  refresh() {
    const user = wx.getStorageSync('xiaoximen_user') || null
    this.setData({ user, orderCount: 0, carryCount: 0, addressCount: 0 })
    if (user && wx.getStorageSync('xiaoximen_token')) {
      const token = wx.getStorageSync('xiaoximen_token')
      const update = data => { if (token === wx.getStorageSync('xiaoximen_token')) this.setData(data) }
      addressApi.list().then(addresses => update({ addressCount: (addresses || []).length })).catch(() => {})
      orderApi.page({ page: 1, pageSize: 1 }).then(result => update({ orderCount: result.total })).catch(() => {})
      deliveryApi.mine({ page: 1, pageSize: 1 }).then(result => update({ carryCount: result.total })).catch(() => {})
    }
  },
  login() { if (this.data.user) return; getApp().login().then(user => { if (wx.getUserProfile) { wx.getUserProfile({ desc: '用于展示头像和昵称', success: profile => { const updated = Object.assign({}, user, { name: profile.userInfo.nickName || user.name, avatarUrl: profile.userInfo.avatarUrl || '' }); wx.setStorageSync('xiaoximen_user', updated); getApp().globalData.user = updated }, complete: () => { this.refresh(); wx.showToast({ title: '登录成功', icon: 'success' }) } }) } else { this.refresh(); wx.showToast({ title: '登录成功', icon: 'success' }) } }).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) },
  logout() { getApp().logout(); this.refresh(); wx.showToast({ title: '已退出登录', icon: 'none' }) },
  goHome() { wx.navigateTo({ url: '/pages/index/index' }) },
  goDelivery() { wx.navigateTo({ url: '/pages/delivery/index' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/index' }) },
  goAddress() { wx.navigateTo({ url: '/pages/address/index' }) }
})
