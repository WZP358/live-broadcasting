<template>
  <section class="admin-card">
    <div v-if="title || $slots.extra" class="admin-card__header">
      <div>
        <h3 v-if="title" class="admin-card__title">{{ title }}</h3>
        <p v-if="subtitle" class="admin-card__subtitle">{{ subtitle }}</p>
      </div>
      <slot name="extra" />
    </div>
    <div class="admin-card__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: "",
  },
  subtitle: {
    type: String,
    default: "",
  },
})
</script>
