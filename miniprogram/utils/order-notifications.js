const request = require('./request')
const events = require('./order-events')

function start() {
  const token = wx.getStorageSync('xiaoximen_token')
  let stopped = false
  let socket
  let retryTimer
  let heartbeat
  function retry() {
    clearInterval(heartbeat)
    clearTimeout(retryTimer)
    if (!stopped && token === wx.getStorageSync('xiaoximen_token')) retryTimer = setTimeout(connect, 5000)
  }
  function connect() {
    if (stopped || !token || token !== wx.getStorageSync('xiaoximen_token')) return
    request('/user/websocket/ticket').then(ticket => {
      if (stopped || token !== wx.getStorageSync('xiaoximen_token')) return
      const base = getApp().globalData.apiBaseUrl.replace(/^http/, 'ws').replace(/\/$/, '')
      socket = wx.connectSocket({ url: `${base}/ws?ticket=${encodeURIComponent(ticket)}`, success() {}, fail: retry })
      socket.onOpen(() => {
        events.publish({ event: 'RECONNECTED' })
        heartbeat = setInterval(() => socket.send({ data: 'ping', fail() {} }), 25000)
      })
      socket.onMessage(message => {
        if (message.data === 'pong') return
        try {
          const data = JSON.parse(message.data)
          if (!data.orderId) return
          events.publish(data)
          wx.showToast({ title: data.content || '订单状态已更新', icon: 'none' })
        } catch { /* 忽略非订单消息 */ }
      })
      socket.onClose(retry)
      socket.onError(() => { socket.close({ success() {}, fail() {} }); retry() })
    }).catch(retry)
  }
  connect()
  return () => {
    stopped = true
    clearTimeout(retryTimer)
    clearInterval(heartbeat)
    if (socket) socket.close({ success() {}, fail() {} })
  }
}
module.exports = { start }
