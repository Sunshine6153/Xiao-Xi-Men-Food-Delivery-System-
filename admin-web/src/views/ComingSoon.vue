<template>
  <section class="pending-page">
    <div class="page-heading">
      <div>
        <p class="section-label">功能状态</p>
        <h1>{{ title }}</h1>
      </div>
    </div>

    <div class="pending-panel">
      <span class="pending-mark">待接入</span>
      <h2>{{ title }}尚未接入后端接口</h2>
      <p>{{ description }}</p>
      <p class="pending-note">当前页面保留导航位置，不提供虚构数据或无效操作。</p>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true }
})
</script>

<style scoped>
.pending-page { max-width: 1120px; margin: 0 auto; }
.page-heading { display: flex; align-items: center; justify-content: space-between; min-height: 52px; }
.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }
h1 { color: #20364e; font-size: 24px; font-weight: 600; }
.pending-panel { margin-top: 24px; padding: 36px; border: 1px solid #e4eaf0; background: #fff; }
.pending-mark { display: inline-block; padding: 5px 9px; border: 1px solid #d9e2ea; color: #6f8192; font-size: 12px; }
h2 { margin-top: 22px; color: #20364e; font-size: 18px; font-weight: 600; }
p { color: #6f8192; font-size: 14px; line-height: 1.7; }
.pending-panel > p { margin-top: 12px; }
.pending-note { padding-top: 16px; border-top: 1px solid #e4eaf0; }

@media (max-width: 560px) {
  h1 { font-size: 21px; }
  .pending-panel { margin-top: 18px; padding: 24px 18px; }
}
</style>
