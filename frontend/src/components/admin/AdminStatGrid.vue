<template>
  <section class="admin-stat-grid">
    <article v-for="item in items" :key="item.key" class="admin-stat-card">
      <div class="admin-stat-card__label">{{ item.label }}</div>
      <div class="admin-stat-card__value">{{ item.value }}</div>
      <div class="admin-stat-card__extra">{{ item.extra || "实时聚合数据" }}</div>
    </article>
  </section>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})
</script>
