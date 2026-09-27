<template>
  <section class="overview-page">
    <div class="page-heading"><div><p class="section-label">平台管理</p><h1>平台概览</h1></div><div class="heading-actions"><span>{{ rangeText }}</span><el-button native-type="button" :disabled="loading" @click="loadOverview" class="ui-action" :icon="Refresh" :loading="loading">{{ loading ? '刷新中' : '刷新数据' }}</el-button></div></div>
    <div v-if="errorMessage" class="error-panel"><span>{{ errorMessage }}</span><el-button native-type="button" @click="loadOverview" class="ui-action" :icon="Refresh">重新加载</el-button></div>
    <div class="metric-grid" :class="{ loading }">
      <article><span>近七天营业额</span><strong>¥ {{ money(metrics.turnover) }}</strong><small>已完成订单</small></article>
      <article><span>近七天订单</span><strong>{{ metrics.orders }} 单</strong><small>平台全部业务订单</small></article>
      <article><span>订单完成率</span><strong>{{ rate(metrics.completionRate) }}</strong><small>近七天已完成 / 订单数</small></article>
      <article><span>累计用户</span><strong>{{ metrics.users }} 人</strong><small>截至今日</small></article>
      <article><span>入驻商户</span><strong>{{ metrics.merchants }} 家</strong><small>平台商户总数</small></article>
    </div>
    <div class="chart-grid" :class="{ loading }">
      <section><div class="panel-heading"><div><h2>营业额趋势</h2><p>近七天每日已完成订单营业额</p></div><span>元</span></div><BaseChart :option="turnoverOption" /></section>
      <section><div class="panel-heading"><div><h2>订单趋势</h2><p>近七天每日订单数量</p></div><span>单</span></div><BaseChart :option="orderOption" /></section>
    </div>
  </section>
</template>

<script setup>
import { Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import BaseChart from '../components/BaseChart.vue'
import { getOrderReportApi, getTurnoverReportApi, getUserReportApi } from '../api/report.js'
import { pageMerchantsApi } from '../api/merchant.js'

const loading = ref(false), errorMessage = ref('')
const turnover = ref({}), orders = ref({}), users = ref({})
const metrics = reactive({ turnover: 0, orders: 0, completionRate: 0, users: 0, merchants: 0 })
function formatDate(date) { const y = date.getFullYear(); const m = String(date.getMonth() + 1).padStart(2, '0'); const d = String(date.getDate()).padStart(2, '0'); return `${y}-${m}-${d}` }
const range = computed(() => { const end = new Date(); const begin = new Date(end); begin.setDate(end.getDate() - 6); return { begin: formatDate(begin), end: formatDate(end) } })
const rangeText = computed(() => `${range.value.begin} 至 ${range.value.end}`)
function csv(value, transform = item => item) { return value ? String(value).split(',').map(item => transform(item.trim())) : [] }
function business(response) { if (response.data?.code !== 200) throw new Error(response.data?.message || '平台数据加载失败'); return response.data.data || {} }
function money(value) { return Number(value || 0).toFixed(2) }
function rate(value) { return `${(Number(value || 0) * 100).toFixed(1)}%` }
function axis(data) { return { type: 'category', data, axisLine: { lineStyle: { color: '#d7e1e9' } }, axisTick: { show: false }, axisLabel: { color: '#6f8192' } } }
function valueAxis() { return { type: 'value', min: 0, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#6f8192' }, splitLine: { lineStyle: { color: '#edf1f4' } } } }
const turnoverOption = computed(() => ({ tooltip: { trigger: 'axis' }, grid: { top: 28, right: 24, bottom: 38, left: 54 }, xAxis: axis(csv(turnover.value.dateList)), yAxis: valueAxis(), series: [{ type: 'line', smooth: true, symbolSize: 6, data: csv(turnover.value.turnoverList, Number), lineStyle: { color: '#1766a6' }, areaStyle: { color: 'rgba(23,102,166,.08)' } }] }))
const orderOption = computed(() => ({ tooltip: { trigger: 'axis' }, grid: { top: 28, right: 24, bottom: 38, left: 48 }, xAxis: axis(csv(orders.value.dateList)), yAxis: { ...valueAxis(), minInterval: 1 }, series: [{ type: 'bar', barMaxWidth: 24, data: csv(orders.value.orderCountList, Number), itemStyle: { color: '#1766a6' } }] }))

async function loadOverview() {
  loading.value = true; errorMessage.value = ''
  try {
    const [turnoverResponse, orderResponse, userResponse, merchantResponse] = await Promise.all([getTurnoverReportApi(range.value), getOrderReportApi(range.value), getUserReportApi(range.value), pageMerchantsApi({ page: 1, pageSize: 1 })])
    turnover.value = business(turnoverResponse); orders.value = business(orderResponse); users.value = business(userResponse); const merchantPage = business(merchantResponse)
    const turnoverValues = csv(turnover.value.turnoverList, Number), orderValues = csv(orders.value.orderCountList, Number), rates = csv(orders.value.orderCompletionRateList, Number), userValues = csv(users.value.userCountList, Number)
    const orderTotal = orderValues.reduce((sum, value) => sum + value, 0); const completedTotal = orderValues.reduce((sum, value, index) => sum + Math.round(value * Number(rates[index] || 0)), 0)
    Object.assign(metrics, { turnover: turnoverValues.reduce((sum, value) => sum + value, 0), orders: orderTotal, completionRate: orderTotal ? completedTotal / orderTotal : 0, users: userValues[userValues.length - 1] || 0, merchants: Number(merchantPage.total || 0) })
  } catch (error) { errorMessage.value = error?.response?.data?.message || error.message || '平台概览加载失败' } finally { loading.value = false }
}
onMounted(loadOverview)
</script>

<style scoped>
.overview-page { max-width: 1240px; margin: 0 auto; }.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 20px; }.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }h1 { color: #20364e; font-size: 24px; font-weight: 600; }.heading-actions { display: flex; align-items: center; gap: 14px; color: #6f8192; font-size: 13px; }.heading-actions button { min-height: 34px; padding: 0 14px; border: 1px solid #cbd8e2; border-radius: 3px; background: #fff; color: #1766a6; cursor: pointer; }.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); margin-top: 24px; border: 1px solid #e4eaf0; background: #fff; }.metric-grid article { min-width: 0; padding: 22px; border-right: 1px solid #edf1f4; }.metric-grid article:last-child { border-right: 0; }.metric-grid span { color: #6f8192; font-size: 13px; }.metric-grid strong { display: block; margin-top: 12px; color: #173f68; font-size: 23px; }.metric-grid small { display: block; margin-top: 8px; color: #95a2ad; font-size: 11px; }.chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; margin-top: 18px; }.chart-grid section { min-width: 0; border: 1px solid #e4eaf0; background: #fff; }.panel-heading { display: flex; justify-content: space-between; padding: 18px 22px 0; }.panel-heading h2 { color: #20364e; font-size: 16px; }.panel-heading p { margin-top: 6px; color: #7b8c9b; font-size: 12px; }.panel-heading > span { color: #8b99a5; font-size: 11px; }.chart-grid :deep(.base-chart) { height: 320px; }.loading { opacity: .6; }.error-panel { display: flex; justify-content: space-between; margin-top: 18px; padding: 14px 18px; border: 1px solid #e2c8c8; background: #fff7f7; color: #a04444; font-size: 13px; }.error-panel button { border: 0; background: transparent; color: #1766a6; cursor: pointer; }@media (max-width: 1000px) { .metric-grid { grid-template-columns: repeat(2, 1fr); }.metric-grid article { border-bottom: 1px solid #edf1f4; }.chart-grid { grid-template-columns: 1fr; } }@media (max-width: 600px) { .page-heading { align-items: start; flex-direction: column; }.metric-grid { grid-template-columns: 1fr; }.metric-grid article { border-right: 0; }.heading-actions { width: 100%; justify-content: space-between; } }
</style>
