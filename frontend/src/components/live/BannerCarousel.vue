<template>
  <section v-if="slides.length" class="banner-carousel">
    <a-carousel autoplay :autoplay-speed="5200" effect="fade" dots-position="bottom">
      <div v-for="slide in slides" :key="slide.id || slide.title" class="banner-slide" @click="handleClick(slide)">
        <img class="banner-cover" :src="slide.cover || fallbackCover" alt="" @error="(e) => onImgError(e, fallbackCover)" />
        <div class="banner-shade"></div>
        <div class="banner-copy">
          <span class="banner-kicker">{{ slide.tag }}</span>
          <h2>{{ slide.title }}</h2>
          <p>{{ slide.desc }}</p>
          <button class="banner-btn" type="button">进入直播间</button>
        </div>
        <div class="banner-side">
          <span class="live-dot">直播中</span>
          <strong>{{ slide.heat }}</strong>
          <span>{{ slide.category }}</span>
        </div>
      </div>
    </a-carousel>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { FALLBACK_COVER, onImgError } from '@/utils/fallback';
import { formatHeat, getAnchorName, getRoomHeat } from '@/utils/liveRoomPresenter';

const props = defineProps({
  rooms: { type: Array, default: () => [] },
});

const emit = defineEmits(['enter']);
const fallbackCover = FALLBACK_COVER;

const tags = ['热门推荐', '正在上升', '本场精选'];

const slides = computed(() =>
  props.rooms.slice(0, Math.min(3, props.rooms.length)).map((room, index) => ({
    ...room,
    tag: tags[index % tags.length],
    title: room.title || '精彩直播',
    desc: `${getAnchorName(room)} · ${room.categoryInfo?.name || '推荐'} · 弹幕互动中`,
    heat: `${formatHeat(getRoomHeat(room))} 热度`,
    category: room.categoryInfo?.name || '推荐',
  })),
);

const handleClick = (slide) => {
  if (slide.id) {
    emit('enter', slide.id);
  }
};
</script>

<style scoped lang="scss">
.banner-carousel {
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--player-bg);
  box-shadow: var(--shadow);
}

.banner-slide {
  position: relative;
  height: 282px;
  overflow: hidden;
  cursor: pointer;
}

.banner-cover {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transform: scale(1.01);
}

.banner-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(5, 6, 9, 0.84) 0%, rgba(5, 6, 9, 0.46) 46%, rgba(5, 6, 9, 0.08) 100%),
    linear-gradient(0deg, rgba(5, 6, 9, 0.42), transparent 44%);
}

.banner-copy {
  position: absolute;
  left: 34px;
  bottom: 32px;
  z-index: 2;
  max-width: min(560px, calc(100% - 220px));
  color: #fff;
}

.banner-kicker {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  color: #1f232b;
  background: rgba(255, 216, 77, 0.92);
  font-size: 12px;
  font-weight: 800;
}

.banner-copy h2 {
  display: -webkit-box;
  margin: 14px 0 8px;
  overflow: hidden;
  color: #fff;
  font-size: 30px;
  font-weight: 900;
  line-height: 1.18;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.banner-copy p {
  margin: 0;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-btn {
  height: 36px;
  margin-top: 18px;
  padding: 0 18px;
  border: 0;
  border-radius: 18px;
  color: #fff;
  background: var(--accent);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.banner-side {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 2;
  display: grid;
  gap: 3px;
  width: 148px;
  padding: 13px 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--radius-md);
  color: #fff;
  background: rgba(5, 6, 9, 0.48);
  backdrop-filter: blur(10px);
}

.banner-side strong {
  font-size: 18px;
}

.banner-side span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.live-dot {
  color: #ffcfb8 !important;
  font-weight: 800;
}

:deep(.ant-carousel .slick-dots) {
  bottom: 12px;
}

:deep(.ant-carousel .slick-dots li) {
  width: 18px;
}

:deep(.ant-carousel .slick-dots li button) {
  height: 3px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.45);
}

:deep(.ant-carousel .slick-dots li.slick-active button) {
  background: #fff;
}

@media (max-width: 780px) {
  .banner-slide {
    height: 210px;
  }

  .banner-copy {
    left: 18px;
    right: 18px;
    bottom: 24px;
    max-width: none;
  }

  .banner-copy h2 {
    font-size: 22px;
  }

  .banner-side {
    display: none;
  }
}
</style>
