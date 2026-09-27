<template>
  <main class="pending-shell">
    <header class="pending-header">
      <span>小西门管理端</span>
      <button type="button" @click="handleLogout">退出登录</button>
    </header>
    <section class="pending-panel">
      <p>系统管理员</p>
      <h1>身份验证成功</h1>
      <span>{{ accountName || '管理员' }}，管理员页面待接入。</span>
    </section>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { logoutApi } from '../api/auth.js'
import { clearSession } from '../utils/request.js'

const router = useRouter()
const accountName = computed(() => localStorage.getItem('xiaoximen_merchant_name'))

async function handleLogout() {
  try {
    await logoutApi()
  } finally {
    clearSession()
    router.replace('/login')
  }
}
</script>

<style scoped>
.pending-shell { min-height: 100%; background: #f4f7fa; color: #20364e; }
.pending-header { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 32px; border-bottom: 1px solid #e4eaf0; background: #fff; font-size: 16px; font-weight: 600; }
.pending-header button { padding: 0; border: 0; background: transparent; color: #6f8192; font: inherit; font-size: 13px; cursor: pointer; }
.pending-panel { max-width: 760px; margin: 32px auto; padding: 32px; border: 1px solid #e4eaf0; background: #fff; }
.pending-panel p { color: #6f8192; font-size: 12px; }
h1 { margin-top: 8px; font-size: 24px; font-weight: 600; }
.pending-panel span { display: block; margin-top: 20px; color: #6f8192; font-size: 14px; }
@media (max-width: 560px) { .pending-header { padding: 0 16px; } .pending-panel { margin: 18px 14px; padding: 22px; } }
</style>
