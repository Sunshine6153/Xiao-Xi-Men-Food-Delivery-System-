<template>
  <div ref="chartElement" class="base-chart" role="img" :aria-label="ariaLabel"></div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '../utils/echarts.js'

const props = defineProps({
  option: { type: Object, required: true },
  ariaLabel: { type: String, default: '数据图表' }
})

const chartElement = ref(null)
let chart = null
let resizeObserver = null

function renderChart() {
  if (!chart) return
  chart.setOption(props.option, { notMerge: true })
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartElement.value)
  renderChart()

  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
})

watch(() => props.option, renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.base-chart {
  width: 100%;
  height: 100%;
  min-height: 320px;
}
</style>
