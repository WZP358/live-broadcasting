<template>
  <div
    class="message-wrapper"
    :class="[levelClass, msgTypeClass, { 'is-self': isSelf }]"
    @mouseenter="showActions = true"
    @mouseleave="showActions = false"
  >
    <span class="guardian-badge" v-if="data.guardianLevel" :class="'guardian-badge-' + guardianBadgeClass">
      {{ guardianLabel }}
    </span>
    <span class="moderator-badge" v-if="data.isModerator">房管</span>
    <span class="name">{{ showName || "观众" }}：</span>
    <span class="msg">{{ data.text || "" }}</span>
    <span class="mod-actions" v-if="isModerator && showActions && data.fromUserId && data.fromUserId !== userId">
      <button type="button" @click.stop="$emit('muteUser', data.fromUserId, 60)">禁言60s</button>
      <button type="button" @click.stop="$emit('muteUser', data.fromUserId, 300)">禁言5m</button>
      <button type="button" class="danger" @click.stop="$emit('kickUser', data.fromUserId)">踢出</button>
    </span>
    <span class="report-actions" v-if="showActions && data.fromUserId && data.fromUserId !== userId">
      <button type="button" @click.stop="$emit('reportMessage', data)">举报</button>
    </span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useStore } from "@/stores"

const props = defineProps({
  data: { type: Object, default: () => ({}) },
  isModerator: { type: Boolean, default: false },
})

defineEmits(['muteUser', 'kickUser', 'reportMessage'])

const showActions = ref(false)

const userInfo = useStore().user().userInfo || {}
const userId = computed(() => userInfo.userId)
const isSelf = computed(() => Boolean(userId.value && props.data.fromUserId && userId.value === props.data.fromUserId))

const showName = computed(() => {
  if (userId.value && props.data.fromUserId) {
    return isSelf.value ? "我" : props.data.nickname
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

const guardianLabel = computed(() => {
  const lv = props.data.guardianLevel || 0
  if (lv >= 3) return "金"
  if (lv >= 2) return "银"
  return "守"
})

const msgTypeClass = computed(() => {
  const text = props.data.text || ""
  if (props.data.isGift || /送出了?|赠送了/.test(text)) return 'chat-gift-message'
  if (props.data.isEnter) return 'chat-enter-message'
  if (props.data.isSystem || props.data.nickname === "系统消息") return 'chat-system-message'
  return ''
})
</script>

<style lang="scss" scoped>
.message-wrapper {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  position: relative;
  width: 100%;
  min-height: 28px;
  padding: 5px 7px;
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.45;
  transition:
    background 0.16s ease,
    box-shadow 0.16s ease;

  &:hover {
    background: var(--bg-card);
    box-shadow: var(--shadow);
  }

  .name {
    max-width: 110px;
    overflow: hidden;
    color: var(--accent);
    font-size: 13px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .msg {
    min-width: 0;
    color: var(--text-primary);
    font-size: 13px;
    overflow-wrap: anywhere;
  }
}

.is-self {
  background: var(--accent-light);
}

.guardian-badge,
.moderator-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  height: 18px;
  padding: 0 5px;
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
  line-height: 18px;
}

.guardian-badge-bronze {
  background: linear-gradient(135deg, #b8753f, #8d4f25);
}

.guardian-badge-silver {
  background: linear-gradient(135deg, #a8b2c0, #6f7b8a);
}

.guardian-badge-gold {
  background: linear-gradient(135deg, #f6c65b, #d58b12);
}

.moderator-badge {
  background: var(--bg-header);
}

.danmaku-level-10 .name { color: var(--accent); }
.danmaku-level-20 .name { color: var(--accent-strong); }
.danmaku-level-30 .name { color: var(--warning); }
.danmaku-level-40 .name { color: color-mix(in srgb, var(--warning) 82%, var(--text-primary)); }
.danmaku-level-50 .name { color: color-mix(in srgb, var(--warning) 72%, var(--text-primary)); }

.chat-system-message {
  color: var(--warning);
  background: color-mix(in srgb, var(--warning) 13%, var(--bg-card));

  .name {
    color: var(--warning);
  }

  .msg {
    color: var(--text-primary);
  }
}

.chat-gift-message {
  border: 1px solid color-mix(in srgb, var(--accent) 24%, var(--border));
  background: linear-gradient(90deg, var(--accent-light), var(--bg-card));

  .name {
    color: var(--accent);
  }

  .msg {
    color: var(--text-primary);
    font-weight: 800;
  }
}

.chat-enter-message {
  color: var(--success);
  background: color-mix(in srgb, var(--success) 12%, var(--bg-card));

  .name,
  .msg {
    color: var(--success);
  }
}

.mod-actions {
  display: inline-flex;
  gap: 4px;
  margin-left: auto;
  white-space: nowrap;
}

.report-actions {
  display: inline-flex;
  margin-left: auto;
  white-space: nowrap;
}

.mod-actions + .report-actions {
  margin-left: 4px;
}

.mod-actions button,
.report-actions button {
  height: 22px;
  padding: 0 6px;
  border: 0;
  border-radius: 4px;
  color: var(--accent);
  background: var(--accent-light);
  font-size: 11px;
  cursor: pointer;
}

.mod-actions button.danger {
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 12%, var(--bg-card));
}

.report-actions button {
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 10%, var(--bg-card));
}
</style>
