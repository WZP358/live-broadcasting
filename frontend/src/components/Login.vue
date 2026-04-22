<script setup>
import { ref } from "vue"
const inputTexts = ref("123")
const onSearch = () => {
  inputTexts.value = "aaa"
}
</script>

<template>
  <a-alert :message="inputTexts" type="success" />

  <a-input-search :value="inputTexts" placeholder="input search text" enter-button @search="onSearch" />
</template>

<style scoped></style>
