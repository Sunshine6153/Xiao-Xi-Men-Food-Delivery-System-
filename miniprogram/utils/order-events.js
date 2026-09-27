const listeners = new Set()
function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
function publish(message) {
  listeners.forEach(listener => listener(message))
}
function startRefresh(page, refresh) {
  stopRefresh(page)
  const update = () => {
    if (wx.getStorageSync('xiaoximen_token')) refresh()
    else if (page.data.orders) {
      page._listSequence++
      page._listRefreshing = false
      page.setData({ orders: [], total: 0, page: 1, hasMore: false, loading: false, loadingMore: false, errorMessage: '请先登录' })
    } else page.setData({ order: {}, loading: false, errorMessage: '请先登录' })
  }
  page._stopOrderEvents = subscribe(update)
  page._orderTimer = setInterval(update, 15000)
}
function stopRefresh(page) {
  if (page._stopOrderEvents) page._stopOrderEvents()
  clearInterval(page._orderTimer)
  page._stopOrderEvents = null
}
function watchSession(page, refresh) {
  if (page._stopSessionEvents) page._stopSessionEvents()
  page._stopSessionEvents = subscribe(message => {
    if (message.event === 'LOGIN' || message.event === 'LOGOUT') refresh()
  })
}
function stopSession(page) {
  if (page._stopSessionEvents) page._stopSessionEvents()
  page._stopSessionEvents = null
}
module.exports = { subscribe, publish, startRefresh, stopRefresh, watchSession, stopSession }
