<template>
  <section class="admin-page">
    <header v-if="title" class="admin-page-header">
      <div class="admin-page-header__main">
        <span class="admin-page-header__eyebrow">{{ eyebrow }}</span>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <div v-if="$slots['header-extra']" class="admin-page-header__extra">
        <slot name="header-extra" />
      </div>
    </header>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: "ADMIN CONSOLE",
  },
  title: {
    type: String,
    default: "",
  },
  description: {
    type: String,
    default: "",
  },
})
</script>
