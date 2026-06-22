<template>
  <div class="typing-indicator" aria-label="面试官正在输入">
    <div class="typing-indicator__avatar">
      <el-avatar :size="36" style="background: var(--color-secondary); color: #fff; font-size: 14px; font-weight: 600;">
        AI
      </el-avatar>
    </div>
    <div class="typing-indicator__content">
      <span class="typing-indicator__text">{{ text }}</span>
      <div class="typing-indicator__dots">
        <span /><span /><span />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '面试官正在输入...' }
})
</script>

<style scoped>
.typing-indicator {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  animation: slide-up var(--duration-slow) var(--easing-out) both;
}

.typing-indicator__content {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border-top-left-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

.typing-indicator__text {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

.typing-indicator__dots {
  display: flex;
  gap: 3px;
}

.typing-indicator__dots span {
  width: 6px;
  height: 6px;
  background: var(--color-text-tertiary);
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-indicator__dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator__dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
