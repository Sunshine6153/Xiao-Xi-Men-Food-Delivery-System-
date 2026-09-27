const orderApi = require('../../utils/order-api')
const deliveryApi = require('../../utils/delivery-api')
const orderList = require('../../utils/order-list')
const events = require('../../utils/order-events')

Page({
  data: { activeTab: 'buy', orders: [], total: 0, page: 1, hasMore: false, loading: false, loadingMore: false, errorMessage: '', moreError: '' },
  onShow() { this.loadOrders(); events.startRefresh(this, () => this.refreshOrders()) },
  onHide() { events.stopRefresh(this) },
  onUnload() { events.stopRefresh(this); this._listSequence++ },
  onReachBottom() { this.loadMore() },
  onPullDownRefresh() { this.refreshOrders().finally(() => wx.stopPullDownRefresh()) },
  fetchPage(query) { return this.data.activeTab === 'carry' ? deliveryApi.mine(query) : orderApi.page(query) },
  loadOrders() { return orderList.load(this, query => this.fetchPage(query)) },
  refreshOrders() { return orderList.load(this, query => this.fetchPage(query), false, true) },
  loadMore() { return orderList.load(this, query => this.fetchPage(query), true) },
  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.tab, page: 1 }, () => this.loadOrders()) },
  login() { getApp().login().then(() => this.loadOrders()).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) },
  openDetail(e) {
    if (!wx.getStorageSync('xiaoximen_token')) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/order-detail/index?id=${e.currentTarget.dataset.id}&mode=${this.data.activeTab}` })
  },
  goHome() { wx.navigateTo({ url: '/pages/index/index' }) },
  goDelivery() { wx.navigateTo({ url: '/pages/delivery/index' }) },
  goMine() { wx.navigateTo({ url: '/pages/mine/index' }) }
})
