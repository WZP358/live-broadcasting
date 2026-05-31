<template>
  <div class="home-page">
    <section class="home-top">
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

      <a-input-search
        v-model:value="keyword"
        allow-clear
        class="search-input"
        placeholder="搜主播、房间标题、分区"
        :loading="searching"
        @search="onKeywordChange"
        @change="onKeywordChange"
      />
    </section>

    <section class="home-layout">
      <main class="feed-column">
        <HomeFocusPanel
          :room="focusRoom"
          :fallback-avatar="fallbackAvatar"
          :fallback-cover="fallbackCover"
          @enter="enterRoom"
        />

        <section class="feed-toolbar">
          <div>
            <h2>{{ currentSelectCategory?.name || "推荐直播" }}</h2>
            <span>{{ displayRooms.length }} 个直播间正在播</span>
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
          :description="hasSearched ? '没有搜到匹配的直播间，换个关键词试试' : '没有找到匹配的直播间，换个关键词或分区试试'"
        />
      </main>

      <HomeSidebar
        :fallback-cover="fallbackCover"
        :history-rooms="historyRooms"
        :hot-ranking="hotRanking"
        @enter="enterRoom"
        @go-live-center="goLiveCenter"
      />
    </section>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import LiveRoom from "@/components/LiveRoom.vue";
import HomeFocusPanel from "@/components/live/HomeFocusPanel.vue";
import HomeSidebar from "@/components/live/HomeSidebar.vue";
import { useHomeFeed } from "@/composables/useHomeFeed";

const router = useRouter();
const {
  currentSelectCategory,
  categories,
  displayRooms,
  feedMode,
  focusRoom,
  historyRooms,
  hotRanking,
  keyword,
  searching,
  hasSearched,
  selectAll,
  selectCategory,
  onKeywordChange,
} = useHomeFeed();

const fallbackCover = "https://dummyimage.com/960x540/111827/ffffff&text=LIVE";
const fallbackAvatar = "https://dummyimage.com/96x96/f3f4f6/9ca3af&text=主播";

const enterRoom = (id) => {
  if (id) {
    router.push(`/room/${id}`);
  }
};

const goLiveCenter = () => {
  router.push("/center/live/live-settings");
};
</script>

<style lang="scss" scoped>
.home-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 18px 24px 42px;
}

.home-top {
  position: sticky;
  top: 76px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  background: rgba(246, 248, 252, 0.92);
  backdrop-filter: blur(12px);
}

.channel-tabs {
  flex: 1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.channel-tabs button {
  height: 36px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: #fff;
  color: #4b5563;
  white-space: nowrap;
  cursor: pointer;
}

.channel-tabs button.active,
.channel-tabs button:hover {
  border-color: #ff8a00;
  color: #d96c00;
  background: #fff7ed;
}

.search-input {
  width: 300px;
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.feed-column {
  min-width: 0;
}

.feed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 22px 0 14px;
}

.feed-toolbar h2 {
  margin: 0;
  color: #1f2937;
}

.feed-toolbar span {
  color: #909399;
  font-size: 13px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.empty-feed {
  padding: 80px 0;
  background: #fff;
  border-radius: 8px;
}

@media (max-width: 1180px) {
  .home-layout {
    grid-template-columns: 1fr;
  }

  .room-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .home-page {
    padding: 12px 14px 30px;
  }

  .home-top,
  .feed-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .room-grid {
    grid-template-columns: 1fr;
  }
}
</style>
