<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { FireOutlined, PlayCircleFilled } from '@ant-design/icons-vue';
import ChatApi from '@/api/chat';
import { FALLBACK_AVATAR, FALLBACK_COVER, onImgError, resolveSafeImageUrl } from '@/utils/fallback';

const router = useRouter();
const props = defineProps({
  room: {
    type: Object,
    default: () => ({}),
  },
  enableHeatPolling: {
    type: Boolean,
    default: false,
  },
});

const roomId = computed(() => Number(props.room?.id || 0));
const popularity = ref(Number(props.room?.popularity || props.room?.heat || 0));
const popularityTimer = ref(null);
const fallbackCover = FALLBACK_COVER;
const fallbackAvatar = FALLBACK_AVATAR;
const safeCover = computed(() => resolveSafeImageUrl(props.room?.cover, FALLBACK_COVER));
const safeAnchorAvatar = computed(() => resolveSafeImageUrl(props.room?.userInfo?.avatar, FALLBACK_AVATAR));

const handleItemClick = () => {
  if (props.room?.id) {
    router.push({ path: `/room/${props.room.id}` });
  }
};

const formatPopularity = (value) => {
  const count = Number(value || 0);
  if (!Number.isFinite(count) || count <= 0) {
    return '0';
  }
  if (count >= 100000000) {
    return `${(count / 100000000).toFixed(1).replace(/\.0$/, '')}亿`;
  }
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1).replace(/\.0$/, '')}万`;
  }
  return `${count}`;
};

const anchorName = computed(() => props.room?.userInfo?.name || props.room?.userInfo?.nickName || '主播');
const categoryName = computed(() => props.room?.categoryInfo?.name || props.room?.categoryName || '推荐');
const isBrowserLive = computed(() => Boolean(props.room?.browserLive));
const recommendReason = computed(() => props.room?.recommendReason || props.room?.reason || '');

const loadPopularity = async () => {
  if (!props.enableHeatPolling || document.visibilityState === 'hidden') {
    popularity.value = Number(props.room?.popularity || props.room?.heat || 0);
    return;
  }
  if (!roomId.value) {
    popularity.value = 0;
    return;
  }
  try {
    const res = await ChatApi.getPopularity({ roomId: roomId.value }, { silentError: true });
    popularity.value = Number(res?.data || 0);
  } catch (error) {
    popularity.value = Number(props.room?.popularity || 0);
  }
};

const clearPopularityTimer = () => {
  if (popularityTimer.value) {
    clearInterval(popularityTimer.value);
    popularityTimer.value = null;
  }
};

const startPopularityPolling = () => {
  clearPopularityTimer();
  loadPopularity();
  if (!props.enableHeatPolling) {
    return;
  }
  popularityTimer.value = setInterval(() => {
    loadPopularity();
  }, 12000);
};

onMounted(() => {
  startPopularityPolling();
});

watch(roomId, () => {
  startPopularityPolling();
});

watch(
  () => props.room?.popularity,
  (value) => {
    if (!props.enableHeatPolling) {
      popularity.value = Number(value || props.room?.heat || 0);
    }
  },
);

onBeforeUnmount(() => {
  clearPopularityTimer();
});
</script>

<template>
  <article class="live-card" @click="handleItemClick">
    <div class="live-card__media" :data-category="categoryName">
      <img draggable="false" class="live-card__cover" :src="safeCover" alt="" @error="(e) => onImgError(e, fallbackCover)" />
      <div class="live-card__shade"></div>
      <span class="live-card__status">
        <i></i>
        直播中
      </span>
      <span class="live-card__heat">
        <FireOutlined />
        {{ formatPopularity(popularity) }}
      </span>
      <span class="live-card__line">{{ isBrowserLive ? '蓝光' : '高清' }}</span>
      <span class="live-card__play">
        <PlayCircleFilled />
      </span>
    </div>
    <div class="live-card__body">
      <h3>{{ room.title || '直播间' }}</h3>
      <div class="live-card__meta">
        <span class="live-card__anchor">
          <img :src="safeAnchorAvatar" alt="" @error="onImgError" />
          {{ anchorName }}
        </span>
        <span class="live-card__category">{{ categoryName }}</span>
      </div>
      <p v-if="recommendReason" class="live-card__reason">{{ recommendReason }}</p>
    </div>
  </article>
</template>

<style lang="scss" scoped>
.live-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.live-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--accent) 38%, var(--border));
  box-shadow: var(--shadow-hover);

  .live-card__cover {
    transform: scale(1.05);
  }

  .live-card__play {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.live-card__media {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: #10131a;
}

.live-card__cover {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.32s ease;
}

.live-card__shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 6, 9, 0.08) 0%, rgba(5, 6, 9, 0.02) 46%, rgba(5, 6, 9, 0.72) 100%),
    linear-gradient(90deg, rgba(5, 6, 9, 0.18), transparent 42%);
}

.live-card__status,
.live-card__heat,
.live-card__line {
  position: absolute;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  line-height: 22px;
}

.live-card__status {
  top: 9px;
  left: 9px;
  gap: 4px;
  background: rgba(244, 63, 94, 0.94);
}

.live-card__status i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fff;
}

.live-card__line {
  top: 9px;
  right: 9px;
  color: #ffe7bd;
  background: rgba(5, 6, 9, 0.68);
}

.live-card__heat {
  right: 9px;
  bottom: 9px;
  gap: 4px;
  background: rgba(5, 6, 9, 0.68);
}

.live-card__media::before {
  position: absolute;
  z-index: 2;
  left: 9px;
  bottom: 9px;
  max-width: calc(100% - 100px);
  overflow: hidden;
  color: rgba(255, 255, 255, 0.84);
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  content: attr(data-category);
}

.live-card__play {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 3;
  color: rgba(255, 255, 255, 0.92);
  font-size: 42px;
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.86);
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
  pointer-events: none;
}

.live-card__body {
  padding: 9px 9px 10px;
}

.live-card__body h3 {
  margin: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.42;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
}

.live-card__anchor {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-card__anchor img {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
}

.live-card__category {
  flex: 0 0 auto;
  max-width: 86px;
  overflow: hidden;
  color: var(--accent);
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-card__reason {
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
