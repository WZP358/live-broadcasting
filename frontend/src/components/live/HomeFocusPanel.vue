<template>
  <section v-if="room.id" class="focus-panel">
    <button class="focus-cover" type="button" @click="$emit('enter', room.id)">
      <img :src="room.cover || fallbackCover" alt="" @error="(e) => onImgError(e, fallbackCover)" />
      <span class="focus-cover__tag">主推直播</span>
      <span class="focus-cover__heat">{{ formatHeat(getRoomHeat(room)) }} 热度</span>
    </button>
    <div class="focus-info">
      <div class="focus-meta">
        <span>{{ room.categoryInfo?.name || '推荐' }}</span>
        <span>{{ room.browserLive ? '蓝光观看' : '高清观看' }}</span>
      </div>
      <h1>{{ room.title || '正在直播' }}</h1>
      <p>{{ room.introduce || room.notice || '主播正在直播，进入房间后可以聊天、关注和送礼。' }}</p>
      <div class="anchor-row">
        <img :src="room.userInfo?.avatar || fallbackAvatar" alt="" @error="onImgError" />
        <div>
          <strong>{{ getAnchorName(room) }}</strong>
          <span>{{ room.notice || '关注后不错过开播' }}</span>
        </div>
        <a-button type="primary" @click="$emit('enter', room.id)">进入直播间</a-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { formatHeat, getAnchorName, getRoomHeat } from '@/utils/liveRoomPresenter';
import { onImgError } from '@/utils/fallback';

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

defineEmits(['enter']);
</script>

<style scoped lang="scss">
.focus-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid rgba(31, 41, 55, 0.08);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(24, 28, 36, 0.07);
}

.focus-cover {
  position: relative;
  min-height: 236px;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: var(--player-bg);
  cursor: pointer;
}

.focus-cover img {
  width: 100%;
  height: 100%;
  min-height: 236px;
  display: block;
  object-fit: cover;
  transition: transform 0.28s ease;
}

.focus-cover::after {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(0deg, rgba(5, 6, 9, 0.48), rgba(5, 6, 9, 0.02) 58%);
}

.focus-cover:hover img {
  transform: scale(1.04);
}

.focus-cover__tag,
.focus-cover__heat {
  position: absolute;
  z-index: 2;
  bottom: 14px;
  height: 26px;
  padding: 0 10px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  line-height: 26px;
}

.focus-cover__tag {
  left: 14px;
  background: #f43f5e;
}

.focus-cover__heat {
  right: 14px;
  background: rgba(5, 6, 9, 0.66);
}

.focus-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 24px;
}

.focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.focus-meta span {
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  color: #8a5800;
  background: #fff2b8;
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
}

.focus-info h1 {
  display: -webkit-box;
  margin: 14px 0 8px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 23px;
  font-weight: 900;
  line-height: 1.25;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.focus-info p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.anchor-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(31, 41, 55, 0.08);
}

.anchor-row img {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.anchor-row strong {
  color: var(--text-primary);
}

.anchor-row span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .focus-panel {
    grid-template-columns: 1fr;
  }
}
</style>
