<template>
  <div class="job-card" @click="$router.push(`/job/${job.id}`)">
    <div class="job-card__header">
      <h3 class="job-card__title">{{ job.title }}</h3>
      <span v-if="job.isUrgent" class="job-card__urgent">急聘</span>
    </div>
    <div class="job-card__company">{{ job.companyName }}</div>
    <div class="job-card__salary" v-if="job.salaryMin || job.salaryMax">
      {{ formatSalary(job.salaryMin, job.salaryMax) }}
    </div>
    <div class="job-card__tags">
      <span class="job-card__tag" v-if="job.city">{{ job.city }}</span>
      <span class="job-card__tag" v-if="job.experience">{{ job.experience }}</span>
      <span class="job-card__tag" v-if="job.education">{{ job.education }}</span>
      <span class="job-card__tag job-card__tag--type">{{ formatJobType(job.jobType) }}</span>
    </div>
    <div class="job-card__footer">
      <span class="job-card__time">{{ formatTime(job.publishedAt) }}</span>
      <span class="job-card__views">{{ job.viewCount || 0 }}人浏览</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  job: {
    type: Object,
    required: true
  }
})

const formatSalary = (min, max) => {
  if (min && max) {
    return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  } else if (min) {
    return `${(min / 1000).toFixed(0)}K起`
  } else if (max) {
    return `最高${(max / 1000).toFixed(0)}K`
  }
  return '面议'
}

const formatJobType = (type) => {
  const map = {
    full_time: '全职',
    part_time: '兼职',
    intern: '实习'
  }
  return map[type] || type
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return `${Math.floor(days / 30)}月前`
}
</script>

<style scoped>
.job-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e5e7eb;
}

.job-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #10b981;
}

.job-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.job-card__title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.job-card__urgent {
  background: #fef2f2;
  color: #dc2626;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.job-card__company {
  font-size: 14px;
  color: #475569;
  margin-bottom: 8px;
}

.job-card__salary {
  font-size: 20px;
  font-weight: 700;
  color: #10b981;
  margin-bottom: 12px;
}

.job-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.job-card__tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.job-card__tag--type {
  background: #ecfdf5;
  color: #059669;
}

.job-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .job-card {
    padding: 14px;
    border-radius: 10px;
  }

  .job-card__title {
    font-size: 15px;
  }

  .job-card__company {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .job-card__salary {
    font-size: 17px;
    margin-bottom: 8px;
  }

  .job-card__tags {
    gap: 6px;
    margin-bottom: 8px;
  }

  .job-card__tag {
    font-size: 11px;
    padding: 3px 8px;
  }

  .job-card__footer {
    font-size: 11px;
    padding-top: 8px;
  }
}
</style>
