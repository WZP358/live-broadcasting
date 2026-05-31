<template>
  <div>
    <div class="player-box">
      <Player
        v-if="canPlay"
        :room-id="roomId"
        :pull-url="roomInfo.pullUrl"
        :browser-live="Boolean(roomInfo.browserLive)"
      />
      <div v-else class="player-empty">
        <strong>{{ roomInfo.status === 1 ? "直播流准备中" : "主播暂未开播" }}</strong>
        <span>{{ roomInfo.status === 1 ? "稍等片刻，播放器会在拉到流后显示画面。" : "可以先关注主播，开播后再回来观看。" }}</span>
      </div>
      <div id="svga-wrap"></div>
    </div>

    <div class="player-bar">
      <div class="bar-tags">
        <span>{{ roomInfo.categoryInfo?.name || "未分类" }}</span>
        <span>{{ roomInfo.pullUrl ? "有回退播放源" : "等待播放源" }}</span>
        <span>{{ isLogin ? "已登录" : "游客观看" }}</span>
      </div>
      <div class="bar-note">弹幕、送礼、关注会写入用户行为；游客只保留观看能力。</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import Player from "../Player.vue";

const props = defineProps({
  isLogin: {
    type: Boolean,
    default: false,
  },
  roomId: {
    type: Number,
    required: true,
  },
  roomInfo: {
    type: Object,
    default: () => ({}),
  },
});

const canPlay = computed(() => props.roomInfo.status === 1 && (props.roomInfo.browserLive || props.roomInfo.pullUrl));
</script>

<style scoped lang="scss">
.player-box {
  position: relative;
  margin-top: 12px;
  min-height: 620px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #050505;
}

.player-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: #e5e7eb;
}

.player-empty strong {
  font-size: 20px;
}

.player-empty span {
  color: #9ca3af;
}

#svga-wrap {
  position: absolute;
  inset: 0;
  z-index: 10;
  pointer-events: none;
}

.player-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.bar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bar-tags span {
  padding: 5px 10px;
  border-radius: 4px;
  background: #fff7ed;
  color: #d96c00;
  font-size: 12px;
}

.bar-note {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 760px) {
  .player-box {
    min-height: 360px;
  }

  .player-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
