import request from './request.js'

// 用一次性票据连接，不将登录 token 放进 WebSocket 地址。
export function startOrderNotifications(onMessage) {
  let socket
  let retryTimer
  let heartbeat
  let stopped = false
  const token = localStorage.getItem('xiaoximen_token')

  async function connect() {
    if (stopped || !token || token !== localStorage.getItem('xiaoximen_token')) return
    try {
      const response = await request.post('/admin/websocket/ticket')
      if (stopped || token !== localStorage.getItem('xiaoximen_token')) return
      if (response.data?.code !== 200) throw new Error('通知连接失败')
      const base = import.meta.env.VITE_API_BASE_URL || '/api'
      const url = new URL(`${base.replace(/\/$/, '')}/ws`, window.location.origin)
      url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
      url.searchParams.set('ticket', response.data.data)
      socket = new WebSocket(url)
      socket.onopen = () => {
        window.dispatchEvent(new Event('xiaoximen-order-updated'))
        heartbeat = setInterval(() => {
          if (socket?.readyState === WebSocket.OPEN) socket.send('ping')
        }, 25000)
      }
      socket.onmessage = (event) => {
        if (event.data === 'pong') return
        try {
          const message = JSON.parse(event.data)
          if (!message.orderId) return
          onMessage(message)
          window.dispatchEvent(new CustomEvent('xiaoximen-order-updated', { detail: message }))
        } catch { /* 忽略非订单消息 */ }
      }
      socket.onclose = scheduleRetry
      socket.onerror = () => socket?.close()
    } catch { scheduleRetry() }
  }

  function scheduleRetry() {
    clearInterval(heartbeat)
    clearTimeout(retryTimer)
    if (!stopped && token === localStorage.getItem('xiaoximen_token')) retryTimer = setTimeout(connect, 5000)
  }
  function stop() {
    stopped = true
    clearTimeout(retryTimer)
    clearInterval(heartbeat)
    socket?.close()
    window.removeEventListener('xiaoximen-session-ended', stop)
  }
  window.addEventListener('xiaoximen-session-ended', stop)
  connect()
  return stop
}
