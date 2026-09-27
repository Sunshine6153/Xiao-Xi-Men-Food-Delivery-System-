const deliveryApi = require('../../utils/delivery-api')
const orderList = require('../../utils/order-list')
const events = require('../../utils/order-events')

Page({
  data: { tab: 'available', orders: [], total: 0, page: 1, hasMore: false, loading: false, loadingMore: false, errorMessage: '', moreError: '' },
  onShow() { this.loadOrders(); events.startRefresh(this, () => this.refreshOrders()) },
  onHide() { events.stopRefresh(this) },
  onUnload() { events.stopRefresh(this); this._listSequence++ },
  onReachBottom() { this.loadMore() },
  onPullDownRefresh() { this.refreshOrders().finally(() => wx.stopPullDownRefresh()) },
  fetchPage(query) { return this.data.tab === 'available' ? deliveryApi.available(query) : deliveryApi.mine(query) },
  loadOrders() { return orderList.load(this, query => this.fetchPage(query)) },
  refreshOrders() { return orderList.load(this, query => this.fetchPage(query), false, true) },
  loadMore() { return orderList.load(this, query => this.fetchPage(query), true) },
  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab, page: 1 }, () => this.loadOrders()) },
  login() { getApp().login().then(() => this.loadOrders()).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) },
  openDetail(e) {
    if (!wx.getStorageSync('xiaoximen_token')) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/order-detail/index?id=${e.currentTarget.dataset.id}&mode=delivery` })
  },
  goHome() { wx.navigateTo({ url: '/pages/index/index' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/index' })},
  goMine() { wx.navigateTo({ url: '/pages/mine/index' }) }
})
