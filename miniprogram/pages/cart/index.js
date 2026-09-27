const shoppingCartApi = require('../../utils/shopping-cart-api')
const events = require('../../utils/order-events')

Page({
  data: { cart: [], groups: [], cartCount: 0, subtotal: '0.00', loading: false },
  onShow() { this.loadCart(); events.watchSession(this, () => this.loadCart()) },
  onHide() { events.stopSession(this) },
  onUnload() { events.stopSession(this) },
  loadCart() { if (!wx.getStorageSync('xiaoximen_token')) { this.setData({ cart: [], groups: [], cartCount: 0, subtotal: '0.00' }); return } this.setData({ loading: true }); shoppingCartApi.list().then(cart => { const map = {}; cart.forEach(item => { const groupKey = String(item.merchantId); if (!map[groupKey]) map[groupKey] = { merchant: item.merchant, location: item.location, items: [] }; map[groupKey].items.push(item) }); this.setData({ cart, groups: Object.keys(map).map(key => map[key]), cartCount: cart.reduce((sum, item) => sum + item.number, 0), subtotal: cart.reduce((sum, item) => sum + item.price * item.number, 0).toFixed(2) }) }).catch(error => wx.showToast({ title: error.message || '购物车加载失败', icon: 'none' })).then(() => this.setData({ loading: false })) },
  changeQuantity(e) { if (this.data.loading) return; const key = String(e.currentTarget.dataset.key); const change = Number(e.currentTarget.dataset.change); const item = this.data.cart.find(row => String(row.key) === key); if (!item) return; const number = item.number + change; this.setData({ loading: true }); const action = number <= 0 ? shoppingCartApi.remove(item.id) : shoppingCartApi.update({ id: item.id, number }); action.then(() => this.loadCart()).catch(error => { this.setData({ loading: false }); wx.showToast({ title: error.message || '数量修改失败', icon: 'none' }) }) },
  clearCart() { wx.showModal({ title: '清空购物车', content: '确定清空全部餐品吗？', success: result => { if (!result.confirm) return; shoppingCartApi.clear().then(() => this.loadCart()).catch(error => wx.showToast({ title: error.message || '清空失败', icon: 'none' })) } }) },
  goCheckout() { wx.navigateTo({ url: '/pages/checkout/index' }) },
  goHome() { wx.navigateBack({ delta: 1 }) }
})
