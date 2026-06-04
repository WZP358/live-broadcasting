<template>
  <div class="room-head">
    <img class="anchor-avatar" :src="roomInfo.userInfo?.avatar || fallbackAvatar" alt="" @error="onImgError" />
    <div class="head-copy">
      <div class="head-meta">
        <span>{{ roomInfo.categoryInfo?.name || "推荐" }}</span>
        <span>{{ roomInfo.status === 1 ? "直播中" : "未开播" }}</span>
        <span>{{ roomInfo.browserLive ? "流畅观看" : "高清观看" }}</span>
      </div>
      <h1>{{ roomInfo.title || "直播间" }}</h1>
      <p>{{ roomInfo.notice || roomInfo.introduce || "欢迎来到直播间，文明互动，理性消费。" }}</p>
    </div>
    <div class="head-actions">
      <a-button type="primary" :ghost="followed" @click="$emit('toggle-follow')">
        {{ followed ? "已关注" : "关注" }}
      </a-button>
      <a-button @click="$emit('copy')">分享</a-button>
    </div>
  </div>
</template>

<script setup>
import { onImgError } from "@/utils/fallback"
defineProps({
  fallbackAvatar: {
    type: String,
    required: true,
  },
  followed: {
    type: Boolean,
    default: false,
  },
  roomInfo: {
    type: Object,
    default: () => ({}),
  },
});

defineEmits(["copy", "toggle-follow"]);
</script>

<style scoped lang="scss">
.room-head {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.anchor-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  object-fit: cover;
}

.head-copy {
  flex: 1;
  min-width: 0;
}

.head-meta {
  display: flex;
  gap: 8px;
  color: var(--accent);
  font-size: 12px;
}

.head-copy h1 {
  margin: 8px 0 4px;
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1.3;
}

.head-copy p {
  margin: 0;
  overflow: hidden;
  color: var(--text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .room-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
