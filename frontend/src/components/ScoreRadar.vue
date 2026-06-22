<template>
  <div ref="chartRef" class="score-radar" />
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount, shallowRef, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([RadarChart, CanvasRenderer])

const props = defineProps({
  scores: { type: Object, default: () => ({}) }
})

const chartRef = ref(null)
const chartInstance = shallowRef(null)

const getOption = (scores) => ({
  radar: {
    indicator: [
      { name: '准确性', max: 10 },
      { name: '清晰度', max: 10 },
      { name: '逻辑性', max: 10 },
      { name: '深度', max: 10 },
      { name: '实践', max: 10 }
    ],
    shape: 'polygon',
    radius: '60%',
    center: ['50%', '52%'],
    axisName: {
      color: '#475569',
      fontSize: 12,
      fontWeight: 500
    },
    splitArea: {
      areaStyle: {
        color: ['#ffffff', '#f8fafc', '#ffffff', '#f8fafc', '#ffffff']
      }
    },
    splitLine: { lineStyle: { color: '#e2e8f0' } },
    axisLine: { lineStyle: { color: '#e2e8f0' } }
  },
  series: [{
    type: 'radar',
    symbol: 'circle',
    symbolSize: 6,
    data: [{
      value: [
        scores.accuracy || 0,
        scores.clarity || 0,
        scores.logic || 0,
        scores.depth || 0,
        scores.practice || 0
      ],
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(16, 185, 129, 0.35)' },
          { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
        ])
      },
      lineStyle: { color: '#10b981', width: 2 },
      itemStyle: { color: '#10b981', borderColor: '#fff', borderWidth: 2 }
    }]
  }],
  tooltip: {
    trigger: 'item',
    formatter: (params) => {
      const labels = ['准确性', '清晰度', '逻辑性', '深度', '实践']
      return params.value.map((v, i) => `${labels[i]}: ${v}`).join('<br/>')
    }
  }
})

onMounted(() => {
  nextTick(() => {
    if (chartRef.value) {
      chartInstance.value = echarts.init(chartRef.value)
      chartInstance.value.setOption(getOption(props.scores))

      const observer = new ResizeObserver(() => {
        chartInstance.value?.resize()
      })
      observer.observe(chartRef.value)
    }
  })
})

watch(() => props.scores, (newScores) => {
  chartInstance.value?.setOption(getOption(newScores))
}, { deep: true })

onBeforeUnmount(() => {
  chartInstance.value?.dispose()
})
</script>

<style scoped>
.score-radar {
  width: 100%;
  height: 260px;
}

@media (max-width: 640px) {
  .score-radar {
    height: 200px;
  }
}
</style>
