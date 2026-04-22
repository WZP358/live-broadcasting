<template>
  <div class="message-wrapper">
    <!-- <a-tag :bordered="false" color="magenta">主播</a-tag> -->
    <span class="name"> {{ showName || "观众" }}：</span>
    <span class="msg">{{ data.text || "" }}</span>
  </div>
</template>

<script setup>
import { useStore } from "@/stores"
import { computed } from "vue"

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
})

const userInfo = useStore().user().userInfo
const userId = computed(() => {
  return userInfo.userId
})

/**
 * 获取展示名称
 */
const showName = computed(() => {
  // 用户登陆并且当前用户是消息发送者
  if (userId.value && props.data.fromUserId) {
    return userId.value === props.data.fromUserId ? "我" : props.data.nickname
  }
  return props.data.nickname
})
</script>

<style lang="scss" scoped>
.message-wrapper {
  .name {
    font-size: 14px;
    color: $font-color-light;
  }
  .msg {
    font-size: 14px;
    color: $font-color;
  }
}
</style>
