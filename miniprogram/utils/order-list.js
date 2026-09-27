const PAGE_SIZE = 20

// 已加载多页时刷新相同页数，不会把历史记录截回第一页。
function load(page, fetchPage, more = false, silent = false) {
  if ((more || silent) && (page.data.loading || page.data.loadingMore || page._listRefreshing)) return Promise.resolve()
  if (more && !page.data.hasMore) return Promise.resolve()
  const token = wx.getStorageSync('xiaoximen_token')
  const sequence = (page._listSequence || 0) + 1
  page._listSequence = sequence
  if (!token) {
    page._listRefreshing = false
    page.setData({ orders: [], total: 0, hasMore: false, loading: false, loadingMore: false, errorMessage: '登录后才能查看订单' })
    return Promise.resolve()
  }
  page._listRefreshing = true
  if (more) page.setData({ loadingMore: true, moreError: '' })
  else if (!silent) page.setData({ loading: true, loadingMore: false, errorMessage: '', moreError: '' })
  const lastPage = more ? page.data.page + 1 : silent ? Math.max(page.data.page, 1) : 1
  const queries = []
  for (let current = more ? lastPage : 1; current <= lastPage; current++) {
    queries.push(fetchPage({ page: current, pageSize: PAGE_SIZE }))
  }
  return Promise.all(queries).then(results => {
    if (sequence !== page._listSequence || token !== wx.getStorageSync('xiaoximen_token')) return
    const records = results.reduce((all, result) => all.concat(result.records || []), [])
    const orders = more ? page.data.orders.concat(records) : records
    const seen = new Set()
    const unique = orders.filter(order => { if (seen.has(order.id)) return false; seen.add(order.id); return true })
    const total = Number(results[0].total || 0)
    page.setData({ orders: unique, total, page: lastPage, hasMore: lastPage * PAGE_SIZE < total, loading: false, loadingMore: false, errorMessage: '', moreError: '' })
  }).catch(error => {
    if (sequence !== page._listSequence || token !== wx.getStorageSync('xiaoximen_token')) return
    if (more || silent) page.setData({ loadingMore: false, moreError: error.message || '刷新失败，请重试' })
    else page.setData({ orders: [], total: 0, hasMore: false, loading: false, errorMessage: error.message || '订单加载失败' })
  }).finally(() => { if (sequence === page._listSequence) page._listRefreshing = false })
}
module.exports = { load }
