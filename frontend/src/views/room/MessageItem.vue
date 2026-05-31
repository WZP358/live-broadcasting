<template>
  <div class="message-wrapper" :class="[levelClass, msgTypeClass]" @mouseenter="showActions = true" @mouseleave="showActions = false">
    <span class="guardian-badge" v-if="data.guardianLevel" :class="'guardian-badge-' + guardianBadgeClass"></span>
    <span class="moderator-badge" v-if="data.isModerator">房管</span>
    <span class="name"> {{ showName || "观众" }}：</span>
    <span class="msg">{{ data.text || "" }}</span>
    <span class="mod-actions" v-if="isModerator && showActions && data.fromUserId && data.fromUserId !== userId">
      <a-button type="link" size="small" @click.stop="$emit('muteUser', data.fromUserId, 60)">禁言60s</a-button>
      <a-button type="link" size="small" @click.stop="$emit('muteUser', data.fromUserId, 300)">禁言5m</a-button>
      <a-button type="link" size="small" danger @click.stop="$emit('kickUser', data.fromUserId)">踢出</a-button>
    </span>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useStore } from "@/stores"
import { computed } from "vue"

const props = defineProps({
  data: { type: Object, default: () => ({}) },
  isModerator: { type: Boolean, default: false },
})

defineEmits(['muteUser', 'kickUser'])

const showActions = ref(false)

const userInfo = useStore().user().userInfo
const userId = computed(() => userInfo.userId)

const showName = computed(() => {
  if (userId.value && props.data.fromUserId) {
    return userId.value === props.data.fromUserId ? "我" : props.data.nickname
  }
  return props.data.nickname
})

const userLevel = computed(() => props.data.userLevel || 0)

const levelClass = computed(() => {
  const lv = userLevel.value
  if (lv >= 50) return 'danmaku-level-50'
  if (lv >= 40) return 'danmaku-level-40'
  if (lv >= 30) return 'danmaku-level-30'
  if (lv >= 20) return 'danmaku-level-20'
  if (lv >= 10) return 'danmaku-level-10'
  return ''
})

const guardianBadgeClass = computed(() => {
  const lv = props.data.guardianLevel || 0
  if (lv >= 3) return 'gold'
  if (lv >= 2) return 'silver'
  return 'bronze'
})

const msgTypeClass = computed(() => {
  if (props.data.isGift) return 'chat-gift-message'
  if (props.data.isEnter) return 'chat-enter-message'
  if (props.data.isSystem) return 'chat-system-message'
  return ''
})
</script>

<style lang="scss" scoped>
.message-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  position: relative;
  .name { font-size: 14px; color: $font-color-light; }
  .msg { font-size: 14px; color: $font-color; }
}
.mod-actions {
  margin-left: 8px;
  white-space: nowrap;
  opacity: 0.8;
}
</style>
