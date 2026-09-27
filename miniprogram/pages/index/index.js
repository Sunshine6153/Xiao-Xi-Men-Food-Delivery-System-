const API_BASE_URL = 'http://localhost:8080'
const shoppingCartApi = require('../../utils/shopping-cart-api')
const apiRequest = require('../../utils/request')
const events = require('../../utils/order-events')

const categoryIcons = {
  '炒饭': '🍳',
  '面食': '🍜',
  '炸串': '🍢',
  '饮品': '🥤',
  '小吃': '🍽'
}

Page({
  data: {
    categories: [{ id: 0, name: '全部', icon: '✦' }],
    activeCategory: 0,
    allDishes: [],
    filteredDishes: [],
    selectedDish: null,
    selectedFlavor: '',
    cartCount: 0,
    cartTotal: '0.00',
    loading: false,
    menuLoaded: false,
    loadError: ''
  },

  onLoad() { this.loadMenu() },

  onShow() {
    events.watchSession(this, () => { this.loadMenu(); this.refreshCart() })
    this.refreshCart()
    if (!wx.getStorageSync('xiaoximen_token') || !this.data.menuLoaded) this.loadMenu()
  },
  onHide() { events.stopSession(this) },
  onUnload() { events.stopSession(this) },

  request(path, options = {}) {
    return apiRequest(path, options.data || {}, options.method || 'GET')
  },

  loadMenu() {
    if (!wx.getStorageSync('xiaoximen_token')) {
      this.setData({ loadError: '请先登录后查看菜单', loading: false, menuLoaded: false, allDishes: [], filteredDishes: [] })
      return
    }
    this.setData({ loading: true, loadError: '' })
    Promise.all([this.request('/user/category/list'), this.request('/user/dish/list')])
      .then(([categories, dishes]) => {
        const normalizedCategories = [{ id: 0, name: '全部', icon: '✦' }].concat(
          (categories || []).map(item => ({ ...item, icon: categoryIcons[item.name] || '🍽' }))
        )
        const normalizedDishes = (dishes || []).map(item => this.normalizeDish(item))
        this.setData({ categories: normalizedCategories, allDishes: normalizedDishes, filteredDishes: normalizedDishes, activeCategory: 0, loading: false, menuLoaded: true, loadError: '' })
      })
      .catch(error => this.setData({ loading: false, loadError: error.message || '菜单加载失败' }))
  },

  normalizeDish(dish) {
    const flavors = (dish.flavors || []).map(item => typeof item === 'string' ? item : item.value).filter(Boolean)
    const image = dish.image && dish.image.indexOf('/') === 0 ? `${API_BASE_URL}${dish.image}` : dish.image
    return { ...dish, image, merchant: dish.merchantName || '小西门商户', location: dish.merchantLocation || '小西门美食街', flavors, sold: dish.sold || 0 }
  },

  refreshCart() {
    if (!wx.getStorageSync('xiaoximen_token')) {
      this.setData({ cartCount: 0, cartTotal: '0.00' })
      return
    }
    shoppingCartApi.list().then(cart => {
      this.setData({ cartCount: cart.reduce((sum, item) => sum + item.number, 0), cartTotal: cart.reduce((sum, item) => sum + item.price * item.number, 0).toFixed(2) })
    }).catch(() => this.setData({ cartCount: 0, cartTotal: '0.00' }))
  },

  selectCategory(e) {
    const id = Number(e.currentTarget.dataset.id)
    this.setData({ activeCategory: id })
    this.applySearch(this.searchKeyword || '')
  },

  onSearchInput(e) {
    this.searchKeyword = e.detail.value.trim()
    this.applySearch(this.searchKeyword)
  },

  applySearch(keyword) {
    const source = this.data.activeCategory === 0 ? this.data.allDishes : this.data.allDishes.filter(item => item.categoryId === this.data.activeCategory)
    const filteredDishes = keyword ? source.filter(item => `${item.name}${item.merchant}`.includes(keyword)) : source
    this.setData({ filteredDishes })
  },

  loginAndLoad() {
    getApp().login().then(() => this.loadMenu()).catch(error => this.setData({ loadError: error.message || '登录失败' }))
  },

  getDish(id) { return this.data.allDishes.find(item => item.id === Number(id)) },

  isDishUnavailable(dish) {
    if (!dish) return true
    if (dish.merchantStatus === 0) { wx.showToast({ title: '该商户暂停售卖', icon: 'none' }); return true }
    if (dish.status === 0) { wx.showToast({ title: '该菜品已下架', icon: 'none' }); return true }
    return false
  },

  openDish(e) {
    const dish = this.getDish(e.currentTarget.dataset.id)
    if (this.isDishUnavailable(dish) || !dish.flavors || !dish.flavors.length) return
    this.setData({ selectedDish: dish, selectedFlavor: dish.flavors[0] })
  },

  handleDishAction(e) {
    const dish = this.getDish(e.currentTarget.dataset.id)
    if (this.isDishUnavailable(dish)) return
    if (dish.flavors && dish.flavors.length) { this.setData({ selectedDish: dish, selectedFlavor: dish.flavors[0] }); return }
    this.quickAdd(dish)
  },

  quickAdd(dish) {
    if (!wx.getStorageSync('xiaoximen_user')) {
      wx.showModal({ title: '需要先登录', content: '登录后才能加入购物车。', confirmText: '微信登录', success: res => { if (res.confirm) getApp().login().then(() => wx.showToast({ title: '登录成功，请再次加购', icon: 'success' })).catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' })) } })
      return
    }
    shoppingCartApi.add({ dishId: dish.id }).then(() => {
      this.refreshCart()
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    }).catch(error => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },

  closeDish() { this.setData({ selectedDish: null }) },
  stopPropagation() {},
  selectFlavor(e) { this.setData({ selectedFlavor: e.currentTarget.dataset.flavor }) },

  addToCart() {
    if (!wx.getStorageSync('xiaoximen_user')) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    const dish = this.data.selectedDish
    const flavor = this.data.selectedFlavor
    shoppingCartApi.add({ dishId: dish.id, dishFlavor: flavor }).then(() => {
      this.setData({ selectedDish: null })
      this.refreshCart()
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    }).catch(error => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },

  openCart() { wx.navigateTo({ url: '/pages/cart/index' }) },
  goDelivery() { wx.navigateTo({ url: '/pages/delivery/index' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/index' }) },
  goMine() { wx.navigateTo({ url: '/pages/mine/index' }) }
})
