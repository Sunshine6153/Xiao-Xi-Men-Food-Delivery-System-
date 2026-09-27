const orderApi = require('../../utils/order-api')
const deliveryApi = require('../../utils/delivery-api')
const events = require('../../utils/order-events')
const STATUS = ['待支付', '待接单', '制作中', '待取餐', '配送中', '待确认收餐', '已完成']

Page({
  data: { order: {}, steps: [], mode: 'buy', loading: true, processing: false, errorMessage: '', orderId: '', reminderUntil: 0 },
  onLoad(options) {
    const mode = options.mode === 'carry' || options.mode === 'delivery' ? 'delivery' : 'buy'
    this.setData({ mode, orderId: options.id || '' })
  },
  onShow() { this.reloadOrder(); events.startRefresh(this, () => this.reloadOrder(true)) },
  onHide() { events.stopRefresh(this) },
  onUnload() { events.stopRefresh(this); this._detailSequence++ },
  reloadOrder(silent = false) {
    if (silent === true && (this._detailLoading || this.data.processing)) return Promise.resolve()
    if (!wx.getStorageSync('xiaoximen_token')) { this.setData({ loading: false, order: {}, errorMessage: '登录后才能查看订单详情' }); return Promise.resolve() }
    this._detailLoading = true
    const sequence = (this._detailSequence || 0) + 1
    this._detailSequence = sequence
    const token = wx.getStorageSync('xiaoximen_token')
    if (silent !== true) this.setData({ loading: true, errorMessage: '' })
    const api = this.data.mode === 'buy' ? orderApi : deliveryApi
    return api.detail(this.data.orderId).then(order => {
      if (sequence !== this._detailSequence || token !== wx.getStorageSync('xiaoximen_token')) return
      this.setData({ order, steps: this.getSteps(order.status), loading: false, errorMessage: '' })
    }).catch(error => {
      if (sequence === this._detailSequence) this.setData({ loading: false, errorMessage: error.message || '订单详情加载失败' })
    }).finally(() => { if (sequence === this._detailSequence) this._detailLoading = false })
  },
  getSteps(status) {
    if (status === '已取消') return [{ title: '已取消', copy: '订单已取消', done: false, current: true }]
    const current = Math.max(STATUS.indexOf(status), 0)
    return STATUS.map((title, index) => ({ title, copy: index === current ? '当前阶段' : index < current ? '已完成' : '等待中', done: index < current, current: index === current }))
  },
  runAction(action, message) {
    if (this.data.processing) return Promise.resolve()
    this.setData({ processing: true })
    return action().then(() => {
      wx.showToast({ title: message, icon: 'success' })
      return this.reloadOrder()
    }).catch(error => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
      .finally(() => this.setData({ processing: false }))
  },
  acceptDelivery() { return this.runAction(() => deliveryApi.accept(this.data.orderId), '接单成功') },
  startDelivery() { return this.runAction(() => deliveryApi.start(this.data.orderId), '已开始配送') },
  deliverOrder() { return this.runAction(() => deliveryApi.complete(this.data.orderId), '已确认送达') },
  cancelOrder() {
    if (this.data.processing) return
    wx.showModal({ title: '取消订单', content: '确定取消当前订单吗？', success: res => {
      if (res.confirm) this.runAction(() => orderApi.cancel(this.data.orderId), '订单已取消')
    } })
  },
  confirmReceipt() { return this.runAction(() => orderApi.confirmReceipt(this.data.orderId), '已确认收餐') },
  continuePayment() {
    if (this.data.processing || this.data.order.statusValue !== 1) return
    wx.showModal({ title: '模拟支付', content: '确认模拟支付当前订单？不会扣除真实费用。', confirmText: '确认支付', success: res => {
      if (res.confirm) this.runAction(() => orderApi.payment(this.data.order.orderNumber), '模拟支付成功')
    } })
  },
  remindOrder() {
    if (this.data.processing) return
    const seconds = Math.ceil((this.data.reminderUntil - Date.now()) / 1000)
    if (seconds > 0) { wx.showToast({ title: `请${seconds}秒后再催单`, icon: 'none' }); return }
    return this.runAction(() => orderApi.reminder(this.data.orderId).then(() => this.setData({ reminderUntil: Date.now() + 60000 })), '已提醒商户')
  },
  login() { getApp().login().then(() => this.reloadOrder()).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) }
})
