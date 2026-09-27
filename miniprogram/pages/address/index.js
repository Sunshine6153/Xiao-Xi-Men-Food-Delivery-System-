const addressApi = require('../../utils/address-api')
const events = require('../../utils/order-events')

Page({
  data: {
    addresses: [], selecting: false, loading: false, saving: false, error: '', needsLogin: false,
    editing: false, editId: null, consignee: '', phone: '', address: ''
  },
  onLoad(options) { this.setData({ selecting: options.select === '1' }) },
  onShow() { this.loadAddresses(); events.watchSession(this, () => this.loadAddresses()) },
  onHide() { events.stopSession(this) },
  onUnload() { events.stopSession(this) },
  loadAddresses() {
    const user = wx.getStorageSync('xiaoximen_user') || {}
    if (this._addressUserId !== user.id) this.setData({ editing: false, editId: null, consignee: '', phone: '', address: '' })
    this._addressUserId = user.id
    if (!wx.getStorageSync('xiaoximen_token')) {
      this.setData({ addresses: [], loading: false, error: '登录后可管理配送地址', needsLogin: true })
      return
    }
    this.setData({ loading: true, error: '', needsLogin: false })
    addressApi.list().then(addresses => {
      this.setData({ addresses: addresses || [], loading: false })
    }).catch(error => {
      this.setData({ addresses: [], loading: false, error: error.message, needsLogin: !!error.authRequired })
    })
  },
  login() {
    getApp().login().then(() => this.loadAddresses())
      .catch(error => wx.showToast({ title: error.message || '登录失败', icon: 'none' }))
  },
  addAddress() {
    if (!wx.getStorageSync('xiaoximen_token')) { this.login(); return }
    this.setData({ editing: true, editId: null, consignee: '', phone: '', address: '' })
  },
  editAddress(e) {
    const item = this.data.addresses.find(address => String(address.id) === String(e.currentTarget.dataset.id))
    if (!item) return
    this.setData({ editing: true, editId: item.id, consignee: item.consignee, phone: item.phone, address: item.address })
  },
  onConsignee(e) { this.setData({ consignee: e.detail.value }) },
  onPhone(e) { this.setData({ phone: e.detail.value }) },
  onAddress(e) { this.setData({ address: e.detail.value }) },
  cancelEdit() { this.setData({ editing: false }) },
  saveAddress() {
    if (this.data.saving) return
    const consignee = this.data.consignee.trim()
    const phone = this.data.phone.trim()
    const address = this.data.address.trim()
    if (!consignee || !/^1\d{10}$/.test(phone) || !address) {
      wx.showToast({ title: '请填写姓名、11位手机号和地址', icon: 'none' })
      return
    }
    const current = this.data.addresses.find(item => item.id === this.data.editId)
    const data = { consignee, phone, address }
    if (current) { data.id = current.id; data.isDefault = current.isDefault }
    this.setData({ saving: true })
    const action = current ? addressApi.update(data) : addressApi.add(data)
    action.then(() => {
      this.setData({ editing: false })
      wx.showToast({ title: '已保存', icon: 'success' })
      this.loadAddresses()
    }).catch(error => wx.showToast({ title: error.message, icon: 'none' }))
      .then(() => this.setData({ saving: false }))
  },
  setDefault(e) {
    const id = e.currentTarget.dataset.id
    const current = this.data.addresses.find(item => String(item.id) === String(id))
    if (!current || current.isDefault === 1) return
    addressApi.setDefault(id).then(() => {
      wx.showToast({ title: '已设为默认', icon: 'success' })
      this.loadAddresses()
    }).catch(error => wx.showToast({ title: error.message, icon: 'none' }))
  },
  deleteAddress(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({ title: '删除地址', content: '确定删除这个配送地址吗？', success: res => {
      if (!res.confirm) return
      addressApi.remove(id).then(() => {
        if (String(wx.getStorageSync('xiaoximen_address_id')) === String(id)) {
          wx.removeStorageSync('xiaoximen_address_id')
          wx.removeStorageSync('xiaoximen_address')
        }
        wx.showToast({ title: '已删除', icon: 'success' })
        this.loadAddresses()
      }).catch(error => wx.showToast({ title: error.message, icon: 'none' }))
    } })
  },
  selectAddress(e) {
    if (!this.data.selecting) return
    const selected = this.data.addresses.find(item => String(item.id) === String(e.currentTarget.dataset.id))
    if (!selected) return
    wx.setStorageSync('xiaoximen_address_id', selected.id)
    wx.setStorageSync('xiaoximen_address', selected.address)
    wx.navigateBack({ delta: 1 })
  }
})
