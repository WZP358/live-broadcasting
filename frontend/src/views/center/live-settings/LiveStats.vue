<template>
  <a-modal class="stats-wrapper" v-model:open="modalOpen" @cancel="handleCancel" centered width="600px" :maskClosable="false">
    <template #footer> </template>
    <template #title> </template>
    <h2 title>直播已结束</h2>
    <h3>太棒了，努力就会有收获！</h3>
    <a-row justify="space-evenly">
      <a-col :span="4">
        <a-flex vertical justify="center" class="item">
          <span>{{ presentAmountText }}</span>
          <span>开心果流水</span>
        </a-flex>
      </a-col>
      <a-col :span="4">
        <a-flex vertical justify="center" class="item">
          <span>{{ `${stats.danMuCount || 0}条` }}</span>
          <span>弹幕数</span>
        </a-flex>
      </a-col>
      <a-col :span="4">
        <a-flex vertical justify="center" class="item">
          <span>{{ `${stats.totalViewCount || 0}人` }}</span>
          <span>累计观看</span>
        </a-flex>
      </a-col>
      <a-col :span="4">
        <a-flex vertical justify="center" class="item">
          <span>{{ durationText }}</span>
          <span>直播时长</span>
        </a-flex>
      </a-col>
    </a-row>
    <h3>部分数据有统计延迟，请以结算为准</h3>
  </a-modal>
</template>

<script setup>
import { computed, defineModel } from "vue"
const modalOpen = defineModel()
const props = defineProps({
  stats: {
    type: Object,
    default: () => ({
      presentAmount: 0,
      danMuCount: 0,
      totalViewCount: 0,
      liveDurationSeconds: 0,
    }),
  },
})

const stats = computed(() => props.stats || {})
const presentAmountText = computed(() => {
  const amount = Number(stats.value.presentAmount || 0)
  return `${amount % 1 === 0 ? amount.toFixed(0) : amount.toFixed(2)}开心果`
})
const durationText = computed(() => {
  const seconds = Number(stats.value.liveDurationSeconds || 0)
  if (seconds < 60) {
    return `${seconds}秒`
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainSeconds = seconds % 60
  if (hours > 0) {
    return `${hours}时${minutes}分${remainSeconds}秒`
  }
  return `${minutes}分${remainSeconds}秒`
})

const handleCancel = () => {
  modalOpen.value = false
}
</script>

<style lang="scss" scoped>
.stats-wrapper {
  h2[title] {
    text-align: center;
    color: #666;
  }
  h3 {
    text-align: center;
    color: #666;
    margin: 20px 0px;
  }
  .item {
    height: 60px;
    width: 100px;
    span {
      display: block;
      text-align: center;
      font-size: 16px;
      color: #666;
    }
  }
}
</style>
