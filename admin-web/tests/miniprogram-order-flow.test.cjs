const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const miniRoot = process.env.XIAOXIMEN_MINI_ROOT || '/Users/a123/WeChatProjects/xiaoximen'

function environment(overrides = {}) {
  const storage = new Map([['xiaoximen_token', 'test-token']])
  const pages = []
  let app = { globalData: { apiBaseUrl: 'http://localhost:8080' }, handleAuthFailure() {} }
  const wx = { getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, value), removeStorageSync: key => storage.delete(key), showToast() {}, ...overrides }
  const cache = new Map()
  const context = vm.createContext({ wx, getApp: () => app, App: value => { app = value }, Page: value => pages.push(value), setInterval, clearInterval, setTimeout, clearTimeout, Date, Set, Promise, console })
  function load(file) {
    const absolute = path.resolve(miniRoot, file)
    if (cache.has(absolute)) return cache.get(absolute).exports
    const module = { exports: {} }
    cache.set(absolute, module)
    const fn = vm.runInContext(`(function(require, module, exports) {${fs.readFileSync(absolute, 'utf8')}\n})`, context, { filename: absolute })
    fn(relative => load(path.relative(miniRoot, path.resolve(path.dirname(absolute), relative + '.js'))), module, module.exports)
    return module.exports
  }
  function page(file) {
    load(file)
    const value = pages.at(-1)
    value.data = JSON.parse(JSON.stringify(value.data))
    value.setData = (data, callback) => { Object.assign(value.data, data); if (callback) callback() }
    return value
  }
  return { load, page, wx, storage, app: () => app }
}

test('每页20条，超过100条仍能全部加载；刷新保留已加载页数', async () => {
  const env = environment()
  const list = env.load('utils/order-list.js')
  const page = { data: { orders: [], page: 1, hasMore: false }, setData(data) { Object.assign(this.data, data) } }
  const all = Array.from({ length: 121 }, (_, id) => ({ id: id + 1 }))
  const fetch = async query => ({ total: all.length, records: all.slice((query.page - 1) * query.pageSize, query.page * query.pageSize) })
  await list.load(page, fetch)
  assert.equal(page.data.orders.length, 20)
  while (page.data.hasMore) await list.load(page, fetch, true)
  assert.equal(page.data.orders.length, 121)
  assert.equal(page.data.page, 7)
  await list.load(page, fetch, false, true)
  assert.equal(page.data.orders.length, 121)
})

test('快速切换列表后，旧请求不覆盖新列表', async () => {
  const env = environment()
  const list = env.load('utils/order-list.js')
  const page = { data: { orders: [], page: 1 }, setData(data) { Object.assign(this.data, data) } }
  let resolveOld
  const pending = list.load(page, () => new Promise(resolve => { resolveOld = resolve }))
  await list.load(page, async () => ({ total: 1, records: [{ id: 2 }] }))
  resolveOld({ total: 1, records: [{ id: 1 }] })
  await pending
  assert.equal(page.data.orders[0].id, 2)
})

test('分页失败保留现有订单，重试仍请求同一页', async () => {
  const env = environment()
  const list = env.load('utils/order-list.js')
  const page = { data: { orders: [{ id: 1 }], page: 1, hasMore: true }, setData(data) { Object.assign(this.data, data) } }
  await list.load(page, async () => { throw new Error('网络错误') }, true)
  assert.equal(page.data.orders.length, 1)
  assert.equal(page.data.page, 1)
  assert.equal(page.data.moreError, '网络错误')
  await list.load(page, async query => { assert.equal(query.page, 2); return { total: 21, records: [{ id: 2 }] } }, true)
  assert.equal(page.data.orders.length, 2)
})

test('商户分组包含取餐位置与最早制作进度', () => {
  const env = environment()
  const api = env.load('utils/order-api.js')
  const result = api.normalizeOrder({ status: 3, dishes: [
    { merchantId: 1, merchantName: 'A', merchantLocation: '北门', status: 3, number: 1, amount: 10 },
    { merchantId: 1, status: 2, number: 2, amount: 5 },
    { merchantId: 2, merchantLocation: '西门', status: 3, number: 1, amount: 10 }
  ] })
  assert.equal(result.merchantGroups[0].location, '北门')
  assert.equal(result.merchantGroups[0].preparationText, '制作中')
  assert.equal(result.merchantGroups[1].preparationText, '已出餐')
})

test('HTTP401和业务401统一处理，正常403不清登录', async () => {
  let response = { statusCode: 200, data: { code: 403, message: '无权限' } }
  const env = environment({ request: options => options.success(response) })
  const errors = []
  env.app().handleAuthFailure = (token, message) => errors.push({ token, message })
  const request = env.load('utils/request.js')
  await assert.rejects(request('/user/order/page'), error => !error.authRequired)
  assert.equal(errors.length, 0)
  response = { statusCode: 200, data: { code: 401, message: '登录过期' } }
  await assert.rejects(request('/user/order/page'), error => error.authHandled)
  response = { statusCode: 401, data: {} }
  await assert.rejects(request('/user/order/page'), error => error.authHandled)
  assert.equal(errors.length, 2)
})

test('多个401只提示一次，旧请求不能清除新登录', () => {
  let prompts = 0
  const env = environment({ showModal: () => { prompts++ } })
  env.load('app.js')
  const app = env.app()
  app.handleAuthFailure('test-token', '过期')
  app.handleAuthFailure('test-token', '过期')
  assert.equal(prompts, 1)
  assert.equal(env.storage.has('xiaoximen_token'), false)
  env.storage.set('xiaoximen_token', 'new-token')
  app.handleAuthFailure('test-token', '过期')
  assert.equal(env.storage.get('xiaoximen_token'), 'new-token')
})

test('继续模拟支付使用已有订单号，不重新下单，重复点击只支付一次', async () => {
  let payments = 0
  let submits = 0
  let releasePayment
  const env = environment({ showModal: options => options.success({ confirm: true }) })
  const api = env.load('utils/order-api.js')
  api.submit = () => { submits++; return Promise.resolve() }
  api.payment = number => { assert.equal(number, 'existing-order'); payments++; return new Promise(resolve => { releasePayment = resolve }) }
  api.detail = async () => api.normalizeOrder({ status: 2, number: 'existing-order' })
  const page = env.page('pages/order-detail/index.js')
  page.setData({ orderId: '10', order: { orderNumber: 'existing-order', statusValue: 1 } })
  page.continuePayment()
  page.continuePayment()
  assert.equal(payments, 1)
  assert.equal(submits, 0)
  releasePayment({ status: 2 })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(page.data.order.statusValue, 2)
  assert.equal(page.data.processing, false)
})

test('催单调用真实路径，成功后短时间内不能重复发送', async () => {
  const calls = []
  const env = environment({ request: options => { calls.push(options.url); options.success({ statusCode: 200, data: { code: 200 } }) } })
  const api = env.load('utils/order-api.js')
  api.detail = async () => api.normalizeOrder({ status: 3 })
  const page = env.page('pages/order-detail/index.js')
  page.setData({ orderId: '10' })
  await page.remindOrder()
  await page.remindOrder()
  assert.equal(calls.length, 1)
  assert.equal(calls[0], 'http://localhost:8080/user/dish/reminder/10')
})

test('切换账号后，旧请求的成功结果也不会泄漏到新账号页面', async () => {
  let respond
  const env = environment({ request: options => { respond = options.success } })
  const promise = env.load('utils/request.js')('/user/addressBook/list')
  env.storage.set('xiaoximen_token', 'new-token')
  respond({ statusCode: 200, data: { code: 200, data: [{ address: '旧账号地址' }] } })
  await assert.rejects(promise, error => error.authHandled)
})

test('结算页切换账号时丢弃旧待支付订单，避免向新账号继续支付', () => {
  const env = environment()
  const page = env.page('pages/checkout/index.js')
  page.loadAddress = () => {}
  page.loadCart = () => {}
  page._checkoutUserId = 1
  page.setData({ pendingOrder: { id: 10, orderNumber: 'old-order' } })
  env.storage.set('xiaoximen_user', { id: 2 })
  page.refreshSession()
  assert.equal(page.data.pendingOrder, null)
})
