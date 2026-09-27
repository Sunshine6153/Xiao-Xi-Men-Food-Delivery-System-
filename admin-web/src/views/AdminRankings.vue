<template>
  <section class="ranking-page">
    <div class="page-heading">
      <div><p class="section-label">数据中心</p><h1>菜品销量排名</h1></div>
      <span class="date-range">{{ rangeText }}</span>
    </div>

    <div class="toolbar">
      <span>统计周期</span>
      <el-radio-group :model-value="range" :disabled="loading" aria-label="统计周期" @change="changeRange">
        <el-radio-button v-for="item in rangeOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="errorMessage" class="state-panel state-error"><p>{{ errorMessage }}</p><el-button native-type="button" @click="loadRanking" class="ui-action" :icon="Refresh">重新加载</el-button></div>
    <section v-else class="ranking-panel" :class="{ loading }">
      <div class="panel-heading"><div><h2>菜品销量 TOP10</h2><p>统计已完成订单中的菜品销售数量</p></div><span>单位：份</span></div>
      <BaseChart :option="rankingOption" aria-label="菜品销量排名图" />
    </section>
  </section>
</template>

<script setup>
import { Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import BaseChart from '../components/BaseChart.vue'
import { getSalesTop10Api } from '../api/report.js'

const range = ref('last7')
const loading = ref(false)
const errorMessage = ref('')
const ranking = ref({ nameList: '', numberList: '' })
const rangeOptions = [{ label: '本周', value: 'week' }, { label: '本月', value: 'month' }, { label: '近七天', value: 'last7' }, { label: '近30天', value: 'last30' }]

function formatDate(date) { const year = date.getFullYear(); const month = String(date.getMonth() + 1).padStart(2, '0'); const day = String(date.getDate()).padStart(2, '0'); return `${year}-${month}-${day}` }
function resolveRange(type) { const end = new Date(); const begin = new Date(end); if (type === 'week') { const day = end.getDay() || 7; begin.setDate(end.getDate() - day + 1) } else if (type === 'month') begin.setDate(1); else begin.setDate(end.getDate() - (type === 'last30' ? 29 : 6)); return { begin: formatDate(begin), end: formatDate(end) } }
function csv(value, transform = item => item) { return value ? String(value).split(',').map(item => transform(item.trim())) : [] }
const currentRange = computed(() => resolveRange(range.value))
const rangeText = computed(() => `${currentRange.value.begin} 至 ${currentRange.value.end}`)
const rankingOption = computed(() => {
  const names = csv(ranking.value.nameList)
  const values = csv(ranking.value.numberList, Number)
  return {
    animationDuration: 350, color: ['#1766a6'], tooltip: { trigger: 'axis' },
    grid: { top: 24, right: 42, bottom: 34, left: 92, containLabel: true },
    xAxis: { type: 'value', min: 0, minInterval: 1, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#6f8192' }, splitLine: { lineStyle: { color: '#edf1f4' } } },
    yAxis: { type: 'category', inverse: true, data: names, axisLine: { lineStyle: { color: '#d7e1e9' } }, axisTick: { show: false }, axisLabel: { color: '#40566c' } },
    series: [{ type: 'bar', data: values, barMaxWidth: 24 }],
    graphic: names.length ? [] : [{ type: 'text', left: 'center', top: 'middle', style: { text: loading.value ? '正在加载...' : '当前周期暂无销量数据', fill: '#94a2ae', fontSize: 14 } }]
  }
})

async function loadRanking() { loading.value = true; errorMessage.value = ''; try { const response = await getSalesTop10Api(currentRange.value); if (response.data?.code !== 200) throw new Error(response.data?.message || '销量排名加载失败'); ranking.value = response.data.data || {} } catch (error) { ranking.value = {}; errorMessage.value = error?.response?.data?.message || error.message || '销量排名加载失败' } finally { loading.value = false } }
function changeRange(value) { if (range.value === value) return; range.value = value; loadRanking() }
onMounted(loadRanking)
</script>

<style scoped>
.ranking-page { max-width: 1120px; margin: 0 auto; }.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 20px; }.section-label { margin-bottom: 7px; color: #6f8192; font-size: 12px; }h1 { color: #20364e; font-size: 24px; font-weight: 600; }.date-range { color: #6f8192; font-size: 13px; }.toolbar { display: flex; align-items: center; gap: 18px; margin-top: 24px; padding: 14px 18px; border: 1px solid #e4eaf0; background: #fff; color: #40566c; font-size: 13px; }.range-tabs { display: flex; border: 1px solid #ccd8e2; }.range-tabs button { min-width: 76px; height: 32px; padding: 0 14px; border: 0; border-right: 1px solid #ccd8e2; background: #fff; color: #506579; cursor: pointer; }.range-tabs button:last-child { border-right: 0; }.range-tabs button.active { color: #fff; background: #1766a6; }.ranking-panel { margin-top: 18px; border: 1px solid #e4eaf0; background: #fff; }.ranking-panel.loading { opacity: .65; }.panel-heading { display: flex; align-items: start; justify-content: space-between; padding: 20px 22px 0; }.panel-heading h2 { color: #20364e; font-size: 16px; font-weight: 600; }.panel-heading p { margin-top: 6px; color: #7b8c9b; font-size: 12px; }.panel-heading > span { color: #8b99a5; font-size: 11px; }.ranking-panel :deep(.base-chart) { height: 460px; }.state-panel { min-height: 180px; margin-top: 18px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; border: 1px solid #e4eaf0; background: #fff; color: #6f8192; }.state-panel button { border: 0; background: transparent; color: #1766a6; cursor: pointer; }.state-error { color: #a04444; }@media (max-width: 600px) { .page-heading { align-items: start; flex-direction: column; gap: 8px; }.toolbar { align-items: start; flex-direction: column; }.range-tabs { width: 100%; display: grid; grid-template-columns: repeat(2, 1fr); }.range-tabs button { width: 100%; border-bottom: 1px solid #ccd8e2; }.ranking-panel :deep(.base-chart) { height: 380px; } }
</style>
