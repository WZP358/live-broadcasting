<script setup></script>

<template>
  <a-config-provider :theme="{
    token: {
      // colorPrimary: '#00b96b',
      borderRadius: 4,
    },
  }">
    <RouterView />
  </a-config-provider>
</template>

<style scoped lang="scss"></style>
