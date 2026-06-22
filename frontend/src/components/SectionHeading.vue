<template>
  <h3 class="section-heading">
    <span class="section-heading__text">{{ title }}</span>
  </h3>
</template>

<script setup>
defineProps({
  title: { type: String, required: true }
})
</script>

<style scoped>
.section-heading {
  font-size: clamp(var(--text-lg), 2.5vw, var(--text-xl));
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  padding-bottom: var(--space-3);
  margin-bottom: var(--space-5);
  border-bottom: 2px solid var(--color-primary);
  display: inline-block;
}
</style>
