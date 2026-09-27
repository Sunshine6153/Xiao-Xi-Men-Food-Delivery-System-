const addressApi = require('../../utils/address-api')
const shoppingCartApi = require('../../utils/shopping-cart-api')
const orderApi = require('../../utils/order-api')
const events = require('../../utils/order-events')

Page({
  data: { cart: [], address: '', addressId: null, requirement: '', fee: '', remark: '', subtotal: '0.00', total: '0.00', showLogin: false, submitting: false, pendingOrder: null },
  onLoad() { this.loadCart() },
  onShow() { this.refreshSession(); events.watchSession(this, () => this.refreshSession()) },
  onHide() { events.stopSession(this) },
  onUnload() { events.stopSession(this) },
  refreshSession() {
    const user = wx.getStorageSync('xiaoximen_user') || {}
    if (this._checkoutUserId !== user.id) this.setData({ pendingOrder: null, submitting: false })
    this._checkoutUserId = user.id
    this.loadAddress()
    if (!this.data.pendingOrder) this.loadCart()
  },
  loadAddress() {
    if (!wx.getStorageSync('xiaoximen_token')) { this.setData({ address: '', addressId: null }); return }
    this.setData({ address: '', addressId: null })
    addressApi.list().then(addresses => {
      const savedId = wx.getStorageSync('xiaoximen_address_id')
      const selected = (addresses || []).find(item => String(item.id) === String(savedId))
        || (addresses || []).find(item => item.isDefault === 1)
      this.setData({ address: selected ? selected.address : '', addressId: selected ? selected.id : null })
      if (selected) {
        wx.setStorageSync('xiaoximen_address_id', selected.id)
        wx.setStorageSync('xiaoximen_address', selected.address)
      } else {
        wx.removeStorageSync('xiaoximen_address_id')
        wx.removeStorageSync('xiaoximen_address')
      }
    }).catch(error => {
      this.setData({ address: '', addressId: null })
      wx.showToast({ title: error.message, icon: 'none' })
    })
  },
  loadCart() { if (!wx.getStorageSync('xiaoximen_token')) { this.setData({ cart: [], subtotal: '0.00', total: '0.00' }); return } shoppingCartApi.list().then(items => { const cart = items.map(item => Object.assign({}, item, { lineTotal: (item.price * item.number).toFixed(2) })); const subtotal = cart.reduce((sum, item) => sum + item.price * item.number, 0); this.setData({ cart, subtotal: subtotal.toFixed(2), total: (subtotal + Number(this.data.fee || 0)).toFixed(2) }) }).catch(error => wx.showToast({ title: error.message || '购物车加载失败', icon: 'none' })) },
  onRequirement(e) { this.setData({ requirement: e.detail.value }) },
  onFee(e) { const fee = e.detail.value; this.setData({ fee, total: (Number(this.data.subtotal) + Number(fee || 0)).toFixed(2) }) },
  onRemark(e) { this.setData({ remark: e.detail.value }) },
  editAddress() { wx.navigateTo({ url: '/pages/address/index?select=1' }) },
  submitOrder() { if (this.data.submitting) return; if (!this.data.addressId) { wx.showToast({ title: '请先填写配送地址', icon: 'none' }); return } if (!this.data.cart.length) { wx.showToast({ title: '购物车为空', icon: 'none' }); return } if (!wx.getStorageSync('xiaoximen_user')) { this.setData({ showLogin: true }); return } if (this.data.pendingOrder) { this.pay(this.data.pendingOrder); return } const remarks = [this.data.requirement ? `配送要求：${this.data.requirement}` : '', this.data.remark].filter(Boolean).join('；'); this.setData({ submitting: true }); orderApi.submit({ addressId: this.data.addressId, remark: remarks, orderDeliveryTime: null, deliveryStatus: 0, tablewareAmount: 0, deliveryFee: Number(this.data.fee || 0), orderAmount: Number(this.data.total) }).then(order => { this.setData({ pendingOrder: order }); return this.pay(order) }).catch(error => { this.setData({ submitting: false }); wx.showToast({ title: error.message || '下单失败', icon: 'none' }) }) },
  wechatLogin() { getApp().login().then(() => { this.setData({ showLogin: false }); this.loadAddress(); this.loadCart(); wx.showToast({ title: '登录成功', icon: 'success' }) }).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) },
  pay(order) { this.setData({ submitting: true }); return orderApi.payment(order.orderNumber).then(payment => { const orderId = payment.orderId || order.id; this.setData({ pendingOrder: null }); wx.showToast({ title: '支付成功', icon: 'success' }); setTimeout(() => wx.redirectTo({ url: `/pages/order-detail/index?id=${orderId}` }), 700) }).catch(error => wx.showToast({ title: `${error.message || '支付失败'}，请重试`, icon: 'none' })).then(() => this.setData({ submitting: false })) },
  closeLogin() { this.setData({ showLogin: false }) },
  stopPropagation() {}
})
