import { onMounted, onUnmounted } from 'vue'

// 通知及时刷新，定时查询用于断线补偿；页面隐藏时不查询。
export function useOrderRefresh(refresh) {
  let timer
  let running = false
  async function update() {
    if (running || document.hidden || !localStorage.getItem('xiaoximen_token')) return
    running = true
    try { await refresh() } finally { running = false }
  }
  onMounted(() => {
    window.addEventListener('xiaoximen-order-updated', update)
    document.addEventListener('visibilitychange', update)
    timer = setInterval(update, 15000)
  })
  onUnmounted(() => {
    clearInterval(timer)
    window.removeEventListener('xiaoximen-order-updated', update)
    document.removeEventListener('visibilitychange', update)
  })
}
