<template>
  <div class="score-tags">
    <el-tag
      v-for="(value, key) in scores"
      :key="key"
      :type="getTagType(value)"
      size="small"
      class="score-tags__item"
    >
      {{ getLabel(key) }}: {{ value }}
    </el-tag>
  </div>
</template>

<script setup>
defineProps({
  scores: { type: Object, default: () => ({}) },
  compact: { type: Boolean, default: false }
})

const labels = {
  accuracy: '准确性',
  clarity: '清晰度',
  logic: '逻辑性',
  depth: '深度',
  practice: '实践'
}

const getLabel = (key) => labels[key] || key

const getTagType = (value) => {
  if (value >= 8) return 'success'
  if (value >= 6) return ''
  if (value >= 4) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.score-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.score-tags__item {
  font-size: var(--text-xs);
}
</style>
