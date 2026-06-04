<template>
  <div class="home-page">
    <section class="home-top">
      <div class="home-top__label">
        <strong>直播分类</strong>
        <span>{{ displayRooms.length }} 个房间</span>
      </div>
      <div class="channel-tabs">
        <button :class="{ active: !currentSelectCategory }" type="button" @click="selectAll">全部</button>
        <button
          v-for="item in categories"
          :key="item.id"
          :class="{ active: currentSelectCategory?.id === item.id }"
          type="button"
          @click="selectCategory(item)"
        >
          {{ item.name }}
        </button>
      </div>
    </section>

    <section class="home-layout">
      <main class="feed-column">
        <BannerCarousel
          :rooms="hotRanking.slice(0, 3)"
          @enter="enterRoom"
        />

        <HomeFocusPanel
          :room="focusRoom"
          :fallback-avatar="fallbackAvatar"
          :fallback-cover="fallbackCover"
          @enter="enterRoom"
        />

        <section class="feed-toolbar">
          <div>
            <h2>{{ currentSelectCategory?.name || "推荐直播" }}</h2>
            <span>{{ currentSelectCategory ? "当前分区正在热播" : "热门房间与关注推荐" }}</span>
          </div>
          <a-radio-group v-model:value="feedMode" button-style="solid">
            <a-radio-button value="recommend">推荐</a-radio-button>
            <a-radio-button value="hot">热门</a-radio-button>
            <a-radio-button value="new">最新</a-radio-button>
            <a-radio-button value="history">继续看</a-radio-button>
          </a-radio-group>
        </section>

        <div v-if="displayRooms.length" class="room-grid">
          <LiveRoom v-for="item in displayRooms" :key="item.id" :room="item" />
        </div>
        <a-empty
          v-else
          class="empty-feed"
          :description="hasSearched ? '没有搜到匹配的直播间，换个关键词试试' : '当前分类暂无直播，换个分区看看吧'"
        >
          <template #children>
            <a-button type="primary" @click="selectAll">查看全部</a-button>
          </template>
        </a-empty>
      </main>

      <HomeSidebar
        :fallback-cover="fallbackCover"
        :history-rooms="historyRooms"
        :hot-ranking="hotRanking"
        :gift-rank="giftRank"
        @enter="enterRoom"
        @go-live-center="goLiveCenter"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import LiveRoom from "@/components/LiveRoom.vue";
import HomeFocusPanel from "@/components/live/HomeFocusPanel.vue";
import HomeSidebar from "@/components/live/HomeSidebar.vue";
import BannerCarousel from "@/components/live/BannerCarousel.vue";
import { useHomeFeed } from "@/composables/useHomeFeed";
import request from "@/utils/request";
import { FALLBACK_AVATAR, FALLBACK_COVER } from "@/utils/fallback";

const router = useRouter();
const {
  currentSelectCategory,
  categories,
  displayRooms,
  feedMode,
  focusRoom,
  historyRooms,
  hotRanking,
  hasSearched,
  selectAll,
  selectCategory,
} = useHomeFeed();

const fallbackCover = FALLBACK_COVER;
const fallbackAvatar = FALLBACK_AVATAR;

const enterRoom = (id) => {
  if (id) {
    router.push(`/room/${id}`);
  }
};

const goLiveCenter = () => {
  router.push("/live/studio");
};

// 打赏总榜
const giftRank = ref([])
onMounted(async () => {
  try {
    const res = await request({ url: "/api/v1/gift-rank/total", method: "get" })
    giftRank.value = res?.data || []
  } catch (e) {}
})
</script>

<style lang="scss" scoped>
.home-page {
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 18px 42px;
}

.home-top {
  position: sticky;
  top: 60px;
  z-index: 19;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
  padding: 10px 0;
  background: linear-gradient(180deg, var(--bg-primary) 82%, color-mix(in srgb, var(--bg-primary) 0%, transparent));
}

.home-top__label {
  display: flex;
  flex: 0 0 auto;
  align-items: baseline;
  gap: 8px;
  padding-right: 18px;
  border-right: 1px solid var(--border);

  strong {
    color: var(--text-primary);
    font-size: 14px;
    font-weight: 900;
  }

  span {
    color: var(--text-muted);
    font-size: 12px;
  }
}

.channel-tabs {
  display: flex;
  gap: 6px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}
.channel-tabs button {
  height: 30px;
  padding: 0 13px;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: var(--accent);
    background: var(--accent-light);
  }

  &.active {
    color: var(--accent-text);
    border-color: var(--accent);
    background: var(--accent);
    font-weight: 800;
  }
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
  margin-top: 8px;
}

.feed-column {
  min-width: 0;
}

.feed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 18px 0 12px;

  h2 {
    margin: 0;
    color: var(--text-primary);
    font-size: 20px;
    font-weight: 900;
  }

  span {
    color: var(--text-muted);
    font-size: 12px;
  }
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px 12px;
}

.empty-feed {
  padding: 60px 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}

@media (max-width: 1380px) {
  .room-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
@media (max-width: 1080px) {
  .home-layout { grid-template-columns: 1fr; }
  .room-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 940px) {
  .home-top {
    top: 116px;
  }
}
@media (max-width: 700px) {
  .home-page { padding: 0 12px 30px; }
  .home-top {
    top: 132px;
    display: block;
  }
  .home-top__label {
    margin-bottom: 8px;
    border-right: 0;
  }
  .feed-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .room-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
}
@media (max-width: 480px) {
  .room-grid { grid-template-columns: 1fr; }
}
</style>
