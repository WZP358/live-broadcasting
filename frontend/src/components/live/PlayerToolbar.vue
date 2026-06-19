<template>
  <div class="player-toolbar" v-if="visible">
    <div class="toolbar-left">
      <span class="toolbar-status" :class="{ live: isLive }">
        <i class="dot"></i> {{ isLive ? "直播中" : "未开播" }}
      </span>
      <span class="toolbar-viewers" v-if="viewerCount > 0">
        热度 {{ formatCount(viewerCount) }}
      </span>
      <span class="toolbar-note">流畅播放</span>
    </div>

    <div class="toolbar-right">
      <a-dropdown v-if="lines.length > 1" trigger="click">
        <button class="toolbar-btn" type="button">
          {{ currentLine }}线 <DownOutlined />
        </button>
        <template #overlay>
          <a-menu @click="handleLineChange">
            <a-menu-item v-for="line in lines" :key="line">
              {{ line }}线 {{ line === 1 ? '(推荐)' : '' }}
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <a-dropdown trigger="click">
        <button class="toolbar-btn" type="button">
          {{ qualityLabel }} <DownOutlined />
        </button>
        <template #overlay>
          <a-menu @click="handleQualityChange">
            <a-menu-item key="auto">自动</a-menu-item>
            <a-menu-item key="1080p">蓝光 1080p</a-menu-item>
            <a-menu-item key="720p">高清 720p</a-menu-item>
            <a-menu-item key="480p">标清 480p</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <div class="toolbar-volume">
        <a-tooltip :title="muted || volume === 0 ? '恢复声音' : '静音'">
          <button class="toolbar-icon-btn" type="button" @click="toggleMute">
            <AudioMutedOutlined v-if="muted || volume === 0" />
            <SoundOutlined v-else />
          </button>
        </a-tooltip>
        <a-slider
          v-model:value="volume"
          :min="0"
          :max="100"
          :step="1"
          class="volume-slider"
          @change="onVolumeChange"
        />
      </div>

      <a-tooltip :title="danmakuEnabled ? '关闭弹幕' : '开启弹幕'">
        <button class="toolbar-icon-btn" :class="{ active: danmakuEnabled }" type="button" @click="toggleDanmaku">
          <MessageOutlined />
        </button>
      </a-tooltip>

      <a-tooltip title="全屏观看">
        <button class="toolbar-icon-btn toolbar-icon-btn--strong" type="button" @click="$emit('fullscreen')">
          <FullscreenOutlined />
        </button>
      </a-tooltip>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { AudioMutedOutlined, SoundOutlined, FullscreenOutlined, DownOutlined, MessageOutlined } from "@ant-design/icons-vue";

const props = defineProps({
  isLive: { type: Boolean, default: false },
  viewerCount: { type: Number, default: 0 },
  lines: { type: Array, default: () => [1, 2, 3] },
  danmakuEnabled: { type: Boolean, default: true },
});

const emit = defineEmits(["line-change", "quality-change", "volume-change", "danmaku-toggle", "fullscreen"]);

const visible = ref(true);
const currentLine = ref(1);
const currentQuality = ref("auto");
const volume = ref(80);
const muted = ref(false);
const prevVolume = ref(80);
const qualityMap = {
  auto: "自动",
  "1080p": "蓝光 1080p",
  "720p": "高清 720p",
  "480p": "标清 480p",
};
const qualityLabel = computed(() => qualityMap[currentQuality.value] || currentQuality.value);

const handleLineChange = ({ key }) => {
  currentLine.value = Number(key);
  emit("line-change", currentLine.value);
};

const handleQualityChange = ({ key }) => {
  currentQuality.value = key;
  emit("quality-change", key);
};

const toggleMute = () => {
  if (muted.value) {
    volume.value = prevVolume.value;
    muted.value = false;
  } else {
    prevVolume.value = volume.value;
    volume.value = 0;
    muted.value = true;
  }
  emit("volume-change", volume.value);
};

const onVolumeChange = (val) => {
  if (val > 0) muted.value = false;
  emit("volume-change", val);
};

const toggleDanmaku = () => {
  emit("danmaku-toggle", !props.danmakuEnabled);
};

const formatCount = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + "万";
  if (n >= 1000) return (n / 1000).toFixed(1) + "k";
  return String(n);
};
</script>

<style scoped lang="scss">
.player-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 46px;
  padding: 7px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: #10131a;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.toolbar-status {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  color: #fff;
  font-weight: 800;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #94a3b8;
  }

  &.live .dot {
    background: var(--danger);
    animation: blink 1.5s infinite;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.toolbar-viewers {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.toolbar-note {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.42);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  min-width: 0;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 30px;
  min-width: 58px;
  padding: 0 9px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.08);
  }
}

.toolbar-icon-btn {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.74);
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.toolbar-icon-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.toolbar-icon-btn.active {
  color: #ffb020;
  background: rgba(255, 176, 32, 0.13);
}

.toolbar-icon-btn--strong:hover {
  color: #ffb27a;
}

.toolbar-volume {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 112px;
  padding: 0 2px;
  color: rgba(255, 255, 255, 0.74);
}

.volume-slider {
  width: 74px;

  :deep(.ant-slider-rail) {
    background: rgba(255, 255, 255, 0.15);
  }

  :deep(.ant-slider-track) {
    background: var(--accent);
  }

  :deep(.ant-slider-handle) {
    border-color: var(--accent);
  }
}

@media (max-width: 640px) {
  .toolbar-viewers,
  .toolbar-volume,
  .toolbar-note {
    display: none;
  }

  .player-toolbar {
    padding: 7px 10px;
  }

  .toolbar-left {
    gap: 8px;
  }

  .toolbar-btn {
    min-width: auto;
    padding: 0 7px;
  }
}
</style>
