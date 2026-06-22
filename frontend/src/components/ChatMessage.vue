<template>
  <div :class="['chat-message', `chat-message--${type}`]">
    <div class="chat-message__avatar">
      <el-avatar :size="36" :style="avatarStyle">
        {{ type === 'ai' ? 'AI' : '我' }}
      </el-avatar>
    </div>
    <div class="chat-message__bubble">
      <div class="chat-message__text" v-html="formatContent(content)" />
      <div v-if="score" class="chat-message__score">
        <ScoreTags :scores="score" />
      </div>
      <div v-if="feedback" class="chat-message__feedback">
        <el-alert :title="feedback" type="info" show-icon :closable="false" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ScoreTags from './ScoreTags.vue'

const props = defineProps({
  type: { type: String, required: true, validator: v => ['ai', 'user', 'system'].includes(v) },
  content: { type: String, default: '' },
  score: { type: Object, default: null },
  feedback: { type: String, default: '' }
})

const avatarStyle = computed(() => ({
  background: props.type === 'user' ? 'var(--color-primary)' : 'var(--color-secondary)',
  color: '#fff',
  fontSize: '14px',
  fontWeight: 600
}))

const formatContent = (text) => {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  animation: slide-up var(--duration-slow) var(--easing-out) both;
}

.chat-message--user {
  flex-direction: row-reverse;
}

.chat-message--system {
  justify-content: center;
}

.chat-message__avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.chat-message__bubble {
  max-width: 75%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  line-height: var(--leading-normal);
  font-size: var(--text-base);
}

.chat-message--ai .chat-message__bubble {
  background: var(--color-surface);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
  border-top-left-radius: var(--radius-sm);
}

.chat-message--user .chat-message__bubble {
  background: var(--color-primary);
  color: var(--color-text-on-primary);
  border-top-right-radius: var(--radius-sm);
}

.chat-message--system .chat-message__bubble {
  background: var(--color-info-surface);
  color: var(--color-info);
  font-style: italic;
  font-size: var(--text-sm);
  border-radius: var(--radius-md);
  max-width: 90%;
  text-align: center;
}

.chat-message__score {
  margin-top: var(--space-3);
}

.chat-message__feedback {
  margin-top: var(--space-3);
}

@media (max-width: 640px) {
  .chat-message__bubble {
    max-width: 85%;
  }
}
</style>
