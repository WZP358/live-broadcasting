<template>
  <svg class="icon" aria-hidden="true" :style="{ width: size, height: size }">
    <use :xlink:href="`#${icon}`"></use>
  </svg>
</template>

<script setup>
const props = defineProps({
  icon: {
    type: String,
    required: true,
  },
  size: {
    type: String,
    default: "20px",
  },
})
</script>

<style lang="scss" scoped></style>
