<template>
  <div class="company-card" @click="$router.push(`/company/${company.id}`)">
    <div class="company-card__logo">
      <img v-if="company.logo" :src="company.logo" :alt="company.name" />
      <span v-else class="company-card__logo-text">{{ company.name?.charAt(0) }}</span>
    </div>
    <div class="company-card__info">
      <h3 class="company-card__name">{{ company.name }}</h3>
      <div class="company-card__tags">
        <span class="company-card__tag" v-if="company.industry">{{ company.industry }}</span>
        <span class="company-card__tag" v-if="company.scale">{{ company.scale }}</span>
        <span class="company-card__tag" v-if="company.city">{{ company.city }}</span>
      </div>
      <div class="company-card__jobs" v-if="company.jobCount">
        {{ company.jobCount }}个在招职位
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  company: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.company-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 16px;
}

.company-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #10b981;
}

.company-card__logo {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.company-card__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.company-card__logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.company-card__info {
  flex: 1;
  min-width: 0;
}

.company-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.company-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.company-card__tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.company-card__jobs {
  font-size: 13px;
  color: #10b981;
  font-weight: 500;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .company-card {
    padding: 14px;
    gap: 12px;
    border-radius: 10px;
  }

  .company-card__logo {
    width: 48px;
    height: 48px;
    border-radius: 10px;
  }

  .company-card__logo-text {
    font-size: 20px;
  }

  .company-card__name {
    font-size: 14px;
    margin-bottom: 6px;
  }

  .company-card__tags {
    gap: 4px;
    margin-bottom: 6px;
  }

  .company-card__tag {
    font-size: 11px;
    padding: 2px 6px;
  }

  .company-card__jobs {
    font-size: 12px;
  }
}
</style>
