<template>
  <section class="statistics-page">
    <div class="page-heading">
      <div>
        <p class="section-label">数据中心</p>
        <h1>{{ title }}</h1>
      </div>
      <div class="heading-actions">
        <span class="date-range">{{ rangeText }}</span>
        <el-button v-if="isAdmin" class="ui-action export-button" native-type="button" :disabled="exporting" @click="exportReport" type="primary" :icon="Download" :loading="exporting">
          {{ exporting ? '导出中...' : '导出报表' }}
        </el-button>
      </div>
    </div>

    <div class="toolbar" aria-label="统计时间范围">
      <span class="toolbar-label">统计周期</span>
      <el-radio-group :model-value="selectedRange" :disabled="loading" aria-label="统计周期" @change="changeRange">
        <el-radio-button v-for="item in rangeOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="errorMessage" class="error-panel" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button native-type="button" @click="loadReports" class="ui-action" :icon="Refresh">重新加载</el-button>
    </div>

    <div class="chart-grid" :class="{ loading, merchant: !isAdmin }" aria-live="polite">
        <section class="chart-panel">
          <div class="panel-heading">
            <div>
              <h2>营业额趋势</h2>
              <p>已完成订单每日营业额（元）</p>
            </div>
            <span class="panel-unit">单位：元</span>
          </div>
          <BaseChart :option="turnoverOption" aria-label="营业额趋势图" />
        </section>

        <section v-if="isAdmin" class="chart-panel">
          <div class="panel-heading">
            <div>
              <h2>用户趋势</h2>
              <p>累计用户数与每日新增用户数</p>
            </div>
            <span class="panel-unit">单位：人</span>
          </div>
          <BaseChart :option="userOption" aria-label="用户趋势图" />
        </section>

        <section class="chart-panel">
          <div class="panel-heading">
            <div>
              <h2>订单数量与成交率</h2>
              <p>每日订单量及已完成订单占比</p>
            </div>
            <span class="panel-unit">订单 / %</span>
          </div>
          <BaseChart :option="orderOption" aria-label="订单数量与成交率图" />
        </section>

        <section class="chart-panel">
          <div class="panel-heading">
            <div>
              <h2>菜品销量 TOP10</h2>
              <p>已完成订单中的菜品销售数量排行</p>
            </div>
            <span class="panel-unit">单位：份</span>
          </div>
          <BaseChart :option="salesOption" aria-label="菜品销量前十图" />
        </section>
    </div>
  </section>
</template>

<script setup>
import { Download, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import BaseChart from '../components/BaseChart.vue'
import {
  getOrderReportApi,
  getSalesTop10Api,
  getTurnoverReportApi,
  getUserReportApi,
  exportBusinessDataApi
} from '../api/report.js'
import {
  getMerchantOrderReportApi,
  getMerchantSalesTop10Api,
  getMerchantTurnoverReportApi
} from '../api/workspace.js'

defineProps({
  title: { type: String, required: true }
})

const isAdmin = localStorage.getItem('xiaoximen_role') === 'ADMIN'
const selectedRange = ref('last7')
const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const turnoverData = ref({ dateList: '', turnoverList: '' })
const userData = ref({ dateList: '', userCountList: '', newUserCountList: '' })
const orderData = ref({ dateList: '', orderCountList: '', orderCompletionRateList: '' })
const salesData = ref({ nameList: '', numberList: '' })

const rangeOptions = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '近七天', value: 'last7' },
  { label: '近30天', value: 'last30' }
]

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function resolveRange(type) {
  const end = new Date()
  const begin = new Date(end)
  if (type === 'week') {
    const day = end.getDay() || 7
    begin.setDate(end.getDate() - day + 1)
  } else if (type === 'month') {
    begin.setDate(1)
  } else {
    begin.setDate(end.getDate() - (type === 'last30' ? 29 : 6))
  }
  return { begin: formatDate(begin), end: formatDate(end) }
}

const currentRange = computed(() => resolveRange(selectedRange.value))
const rangeText = computed(() => `${currentRange.value.begin} 至 ${currentRange.value.end}`)

function parseCsv(value, transform = (item) => item) {
  if (value === null || value === undefined || value === '') return []
  return String(value).split(',').map((item) => transform(item.trim()))
}

function getBusinessData(response) {
  if (response.data?.code !== 200) {
    throw new Error(response.data?.msg || response.data?.message || '统计接口返回失败')
  }
  return response.data.data || {}
}

async function loadReports() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = currentRange.value
    if (isAdmin) {
      const [turnover, users, orders, sales] = await Promise.all([
        getTurnoverReportApi(params),
        getUserReportApi(params),
        getOrderReportApi(params),
        getSalesTop10Api(params)
      ])
      turnoverData.value = getBusinessData(turnover)
      userData.value = getBusinessData(users)
      orderData.value = getBusinessData(orders)
      salesData.value = getBusinessData(sales)
    } else {
      const [turnover, orders, sales] = await Promise.all([
        getMerchantTurnoverReportApi(params),
        getMerchantOrderReportApi(params),
        getMerchantSalesTop10Api(params)
      ])
      turnoverData.value = getBusinessData(turnover)
      orderData.value = getBusinessData(orders)
      salesData.value = getBusinessData(sales)
    }
  } catch (error) {
    errorMessage.value = error?.message || '统计数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function changeRange(value) {
  if (selectedRange.value === value) return
  selectedRange.value = value
  loadReports()
}

async function exportReport() {
  exporting.value = true
  errorMessage.value = ''
  try {
    const response = await exportBusinessDataApi(currentRange.value)
    const disposition = response.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename\*=utf-8''([^;]+)/i)
    const fileName = match ? decodeURIComponent(match[1]) : `运营数据报表_${currentRange.value.begin}_${currentRange.value.end}.xlsx`
    const url = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    errorMessage.value = error?.message || '报表导出失败，请稍后重试'
  } finally {
    exporting.value = false
  }
}

function emptyGraphic(hasData) {
  return hasData ? [] : [{
    type: 'text', left: 'center', top: 'middle',
    style: { text: loading.value ? '正在加载...' : '当前周期暂无数据', fill: '#94a2ae', fontSize: 14 }
  }]
}

function categoryAxis(data) {
  return {
    type: 'category', data,
    axisLine: { lineStyle: { color: '#d7e1e9' } },
    axisTick: { show: false },
    axisLabel: { color: '#6f8192', hideOverlap: true }
  }
}

function valueAxis(options = {}) {
  return {
    type: 'value', minInterval: options.minInterval,
    min: options.min, max: options.max,
    axisLine: { show: false }, axisTick: { show: false },
    axisLabel: { color: '#6f8192', formatter: options.formatter },
    splitLine: { lineStyle: { color: '#edf1f4' } }
  }
}

const commonOption = {
  animationDuration: 350,
  color: ['#1766a6', '#6fb4df'],
  tooltip: { trigger: 'axis' },
  grid: { top: 48, right: 28, bottom: 42, left: 54, containLabel: false }
}

const turnoverOption = computed(() => {
  const dates = parseCsv(turnoverData.value.dateList)
  const values = parseCsv(turnoverData.value.turnoverList, Number)
  return {
    ...commonOption,
    xAxis: categoryAxis(dates), yAxis: valueAxis({ min: 0 }),
    series: [{ name: '营业额', type: 'line', smooth: true, symbolSize: 6, data: values, areaStyle: { color: 'rgba(23, 102, 166, 0.08)' } }],
    graphic: emptyGraphic(dates.length > 0)
  }
})

const userOption = computed(() => {
  const dates = parseCsv(userData.value.dateList)
  return {
    ...commonOption,
    legend: { top: 4, right: 12, itemWidth: 14, itemHeight: 8, textStyle: { color: '#6f8192' } },
    xAxis: categoryAxis(dates), yAxis: valueAxis({ min: 0, minInterval: 1 }),
    series: [
      { name: '累计用户', type: 'line', smooth: true, data: parseCsv(userData.value.userCountList, Number) },
      { name: '新增用户', type: 'bar', barMaxWidth: 22, data: parseCsv(userData.value.newUserCountList, Number) }
    ],
    graphic: emptyGraphic(dates.length > 0)
  }
})

const orderOption = computed(() => {
  const dates = parseCsv(orderData.value.dateList)
  const rates = parseCsv(orderData.value.orderCompletionRateList, (value) => Number((Number(value) * 100).toFixed(2)))
  return {
    ...commonOption,
    legend: { top: 4, right: 12, itemWidth: 14, itemHeight: 8, textStyle: { color: '#6f8192' } },
    xAxis: categoryAxis(dates),
    yAxis: [valueAxis({ min: 0, minInterval: 1 }), valueAxis({ min: 0, max: 100, formatter: '{value}%' })],
    series: [
      { name: '订单数量', type: 'bar', barMaxWidth: 24, data: parseCsv(orderData.value.orderCountList, Number) },
      { name: '成交率', type: 'line', yAxisIndex: 1, smooth: true, data: rates }
    ],
    graphic: emptyGraphic(dates.length > 0)
  }
})

const salesOption = computed(() => {
  const names = parseCsv(salesData.value.nameList)
  const numbers = parseCsv(salesData.value.numberList, Number)
  return {
    ...commonOption,
    grid: { top: 22, right: 36, bottom: 36, left: 88, containLabel: true },
    xAxis: valueAxis({ min: 0, minInterval: 1 }),
    yAxis: { ...categoryAxis(names), inverse: true },
    series: [{ name: '销量', type: 'bar', barMaxWidth: 20, data: numbers, itemStyle: { color: '#1766a6' } }],
    graphic: emptyGraphic(names.length > 0)
  }
})

onMounted(loadReports)
</script>

<style scoped>
.statistics-page { max-width: 1240px; margin: 0 auto; }
.page-heading { min-height: 52px; display: flex; align-items: end; justify-content: space-between; gap: 24px; }
.section-label { margin: 0 0 7px; color: #6f8192; font-size: 12px; }
h1 { margin: 0; color: #20364e; font-size: 24px; font-weight: 600; }
.date-range { color: #6f8192; font-size: 13px; }
.heading-actions { display: flex; align-items: center; gap: 14px; }
.export-button { min-height: 34px; padding: 0 14px; border: 1px solid #1766a6; border-radius: 3px; background: #1766a6; color: #fff; cursor: pointer; font: inherit; font-size: 13px; }
.export-button:disabled { cursor: wait; opacity: .65; }
.toolbar { display: flex; align-items: center; gap: 18px; margin-top: 24px; padding: 14px 18px; border: 1px solid #e4eaf0; background: #fff; }
.toolbar-label { flex: 0 0 auto; color: #40566c; font-size: 13px; font-weight: 600; }
.range-tabs { display: flex; flex-wrap: wrap; gap: 0; border: 1px solid #ccd8e2; }
.range-tabs button { min-width: 76px; height: 32px; padding: 0 14px; border: 0; border-right: 1px solid #ccd8e2; background: #fff; color: #506579; cursor: pointer; font: inherit; font-size: 13px; }
.range-tabs button:last-child { border-right: 0; }
.range-tabs button:hover:not(:disabled) { color: #1766a6; background: #f1f7fb; }
.range-tabs button.active { color: #fff; background: #1766a6; }
.range-tabs button:disabled { cursor: wait; }
.chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; margin-top: 18px; transition: opacity 150ms ease; }
.chart-grid.merchant .chart-panel:last-child { grid-column: 1 / -1; }
.chart-grid.loading { opacity: 0.68; }
.chart-panel { min-width: 0; border: 1px solid #e4eaf0; background: #fff; }
.panel-heading { min-height: 69px; display: flex; align-items: start; justify-content: space-between; gap: 16px; padding: 18px 22px 0; }
h2 { margin: 0; color: #20364e; font-size: 16px; font-weight: 600; }
.panel-heading p { margin: 6px 0 0; color: #7b8c9b; font-size: 12px; }
.panel-unit { flex: 0 0 auto; color: #8b99a5; font-size: 11px; }
.chart-panel :deep(.base-chart) { height: 330px; min-height: 330px; }
.error-panel, .notice-panel { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 18px; padding: 14px 18px; border: 1px solid #e2c8c8; background: #fff7f7; color: #a04444; font-size: 13px; }
.error-panel button { border: 0; background: transparent; color: #1766a6; cursor: pointer; font: inherit; font-weight: 600; }
.notice-panel { border-color: #d7e1e9; background: #fff; color: #6f8192; }

@media (max-width: 1000px) {
  .chart-grid { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .page-heading { align-items: start; flex-direction: column; gap: 9px; }
  .heading-actions { width: 100%; justify-content: space-between; }
  h1 { font-size: 21px; }
  .toolbar { align-items: start; flex-direction: column; gap: 10px; margin-top: 18px; }
  .range-tabs { width: 100%; display: grid; grid-template-columns: repeat(2, 1fr); }
  .range-tabs button { width: 100%; border-bottom: 1px solid #ccd8e2; }
  .range-tabs button:nth-child(2) { border-right: 0; }
  .range-tabs button:nth-last-child(-n + 2) { border-bottom: 0; }
  .chart-panel :deep(.base-chart) { height: 300px; min-height: 300px; }
  .panel-heading { padding: 16px 17px 0; }
}
</style>
