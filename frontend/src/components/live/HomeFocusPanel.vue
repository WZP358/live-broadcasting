<template>
  <section class="focus-panel" v-if="room.id">
    <button class="focus-cover" type="button" @click="$emit('enter', room.id)">
      <img :src="room.cover || fallbackCover" alt="" />
      <span class="live-badge">直播中</span>
      <span class="heat-badge">{{ formatHeat(getRoomHeat(room)) }} 热度</span>
    </button>
    <div class="focus-info">
      <div class="focus-meta">
        <span>{{ room.categoryInfo?.name || "推荐" }}</span>
        <span>{{ room.browserLive ? "低延迟线路" : "稳定线路" }}</span>
      </div>
      <h1>{{ room.title || "正在直播" }}</h1>
      <p>{{ room.introduce || room.notice || "主播正在直播，进入房间后可以聊天、关注和送礼。" }}</p>
      <div class="anchor-row">
        <img :src="room.userInfo?.avatar || fallbackAvatar" alt="" />
        <div>
          <strong>{{ getAnchorName(room) }}</strong>
          <span>{{ room.notice || "关注后更容易找到这位主播" }}</span>
        </div>
        <a-button type="primary" @click="$emit('enter', room.id)">进入直播间</a-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { formatHeat, getAnchorName, getRoomHeat } from "@/utils/liveRoomPresenter";

defineProps({
  room: {
    type: Object,
    default: () => ({}),
  },
  fallbackCover: {
    type: String,
    required: true,
  },
  fallbackAvatar: {
    type: String,
    required: true,
  },
});

defineEmits(["enter"]);
</script>

<style scoped lang="scss">
.focus-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.focus-cover {
  position: relative;
  border: 0;
  padding: 0;
  background: #111827;
  cursor: pointer;
}

.focus-cover img {
  width: 100%;
  height: 100%;
  min-height: 360px;
  display: block;
  object-fit: cover;
}

.live-badge,
.heat-badge {
  position: absolute;
  bottom: 14px;
  padding: 6px 10px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
}

.live-badge {
  left: 14px;
  background: #f56c6c;
}

.heat-badge {
  right: 14px;
  background: rgba(0, 0, 0, 0.58);
}

.focus-info {
  padding: 24px;
}

.focus-meta {
  display: flex;
  gap: 8px;
  color: #d96c00;
  font-size: 13px;
}

.focus-info h1 {
  margin: 14px 0 10px;
  color: #1f2937;
  font-size: 28px;
  line-height: 1.25;
}

.focus-info p {
  color: #667085;
  line-height: 1.8;
}

.anchor-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
}

.anchor-row img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.anchor-row div {
  flex: 1;
  min-width: 0;
}

.anchor-row strong,
.anchor-row span {
  display: block;
}

.anchor-row strong {
  color: #1f2937;
}

.anchor-row span {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .focus-panel {
    grid-template-columns: 1fr;
  }
}
</style>
