<template>
  <div class="search-page">
    <section class="search-header">
      <div class="search-header__bar">
        <a-input-search
          v-model:value="keyword"
          size="large"
          placeholder="搜索主播、房间标题、分区..."
          :loading="loading"
          @search="doSearch"
        >
          <template #enterButton>
            <a-button type="primary" size="large">搜索</a-button>
          </template>
        </a-input-search>
      </div>

      <div class="search-header__filters" v-if="categories.length">
        <button
          :class="{ active: !activeCategoryId }"
          type="button"
          @click="selectCategory(null)"
        >全部</button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="{ active: activeCategoryId === cat.id }"
          type="button"
          @click="selectCategory(cat.id)"
        >{{ cat.name }}</button>
      </div>
    </section>

    <!-- 搜索历史 -->
    <section class="search-history" v-if="!hasSearched && searchHistory.length">
      <div class="section-head">
        <h2>搜索历史</h2>
        <a-button type="link" size="small" @click="clearHistory">清空</a-button>
      </div>
      <div class="history-tags">
        <a-tag
          v-for="(kw, i) in searchHistory"
          :key="i"
          closable
          @click="keyword = kw; doSearch()"
          @close="removeHistory(i)"
        >{{ kw }}</a-tag>
      </div>
    </section>

    <!-- 热门搜索 -->
    <section class="search-hot" v-if="!hasSearched">
      <h2>热门搜索</h2>
      <div class="hot-tags">
        <a-tag
          v-for="item in hotKeywords"
          :key="item"
          :color="item.color"
          @click="keyword = item.text; doSearch()"
        >{{ item.text }}</a-tag>
      </div>
    </section>

    <!-- 搜索结果 -->
    <section class="search-results" v-if="hasSearched">
      <div class="section-head">
        <h2>
          <template v-if="loading">搜索中...</template>
          <template v-else-if="results.length">{{ keyword }} — 找到 {{ totalResults }} 个直播间</template>
          <template v-else>未找到与「{{ keyword }}」相关的直播间</template>
        </h2>
        <a-radio-group v-if="results.length" v-model:value="sortMode" button-style="solid" size="small">
          <a-radio-button value="hot">最热</a-radio-button>
          <a-radio-button value="new">最新</a-radio-button>
        </a-radio-group>
      </div>

      <div v-if="results.length" class="room-grid">
        <LiveRoom v-for="item in displayResults" :key="item.id" :room="item" />
      </div>

      <a-empty
        v-if="!loading && !results.length"
        class="empty-search"
        description="换个关键词试试？"
      />

      <div v-if="results.length < totalResults" class="load-more">
        <a-button :loading="loadingMore" @click="loadMore" block>加载更多</a-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import LiveRoom from "@/components/LiveRoom.vue";
import searchApi from "@/api/search";
import liveApi from "@/api/live";
import request from "@/utils/request";
import { normalizeLivingRooms, sortRoomsByMode } from "@/utils/liveRoomPresenter";
import { normalizeCategories, normalizeSearchRooms } from "@/utils/categoryPresenter";

const route = useRoute();
const keyword = ref("");
const loading = ref(false);
const loadingMore = ref(false);
const hasSearched = ref(false);
const results = ref([]);
const totalResults = ref(0);
const page = ref(1);
const categories = ref([]);
const activeCategoryId = ref(null);
const sortMode = ref("hot");

const searchHistory = ref(JSON.parse(localStorage.getItem("search_history") || "[]"));

const hotKeywords = ref([]);
const TAG_COLORS = ["orange", "blue", "purple", "green", "magenta", "cyan", "red", "gold"];

const loadHotKeywords = async () => {
  try {
    const res = await request({ url: "/api/v1/search-trend/hot", method: "get" });
    hotKeywords.value = (res?.data || []).map((item, i) => ({
      text: item.keyword,
      color: TAG_COLORS[i % TAG_COLORS.length],
    }));
  } catch (e) {
    hotKeywords.value = [];
  }
};

const displayResults = computed(() => {
  if (!results.value.length) return [];
  return sortRoomsByMode(results.value, sortMode.value);
});

const saveToHistory = (kw) => {
  if (!kw.trim()) return;
  let list = JSON.parse(localStorage.getItem("search_history") || "[]");
  list = list.filter((item) => item !== kw);
  list.unshift(kw);
  if (list.length > 10) list = list.slice(0, 10);
  localStorage.setItem("search_history", JSON.stringify(list));
  searchHistory.value = list;
};

const clearHistory = () => {
  localStorage.removeItem("search_history");
  searchHistory.value = [];
};

const removeHistory = (index) => {
  searchHistory.value.splice(index, 1);
  localStorage.setItem("search_history", JSON.stringify(searchHistory.value));
};

const doSearch = async () => {
  const kw = keyword.value.trim();
  if (!kw) return;

  saveToHistory(kw);
  request({ url: "/api/v1/search-trend/record", method: "post", data: { keyword: kw } }).catch(() => {});
  loading.value = true;
  hasSearched.value = true;
  page.value = 1;

  try {
    const params = { keyword: kw, page: 1, limit: 24 };
    if (activeCategoryId.value) params.categoryId = activeCategoryId.value;
    const res = await searchApi.searchRooms(params);
    results.value = normalizeLivingRooms(normalizeSearchRooms(res?.data?.list || []));
    totalResults.value = res?.data?.total || 0;
  } catch (e) {
    results.value = [];
    totalResults.value = 0;
  } finally {
    loading.value = false;
  }
};

const loadMore = async () => {
  loadingMore.value = true;
  try {
    page.value++;
    const params = { keyword: keyword.value.trim(), page: page.value, limit: 24 };
    if (activeCategoryId.value) params.categoryId = activeCategoryId.value;
    const res = await searchApi.searchRooms(params);
    const more = normalizeLivingRooms(normalizeSearchRooms(res?.data?.list || []));
    results.value = [...results.value, ...more];
    totalResults.value = res?.data?.total || totalResults.value;
  } catch (e) {
    page.value--;
  } finally {
    loadingMore.value = false;
  }
};

const selectCategory = (catId) => {
  activeCategoryId.value = catId;
  if (hasSearched.value && keyword.value.trim()) {
    doSearch();
  }
};

onMounted(async () => {
  loadHotKeywords();
  try {
    const res = await liveApi.listCategories({});
    categories.value = normalizeCategories(res?.data?.list || []);
    if (activeCategoryId.value && !categories.value.some((item) => item.id === activeCategoryId.value)) {
      activeCategoryId.value = null;
    }
  } catch (e) {
    categories.value = [];
  }

  const q = route.query.keyword;
  if (q) {
    keyword.value = q;
    doSearch();
  }
});

watch(() => route.query.keyword, (val) => {
  if (val && val !== keyword.value) {
    keyword.value = val;
    doSearch();
  }
});

</script>

<style lang="scss" scoped>
.search-page {
  max-width: 1500px;
  margin: 0 auto;
  padding: 18px 18px 48px;
}

.search-header {
  margin-bottom: 18px;
  padding: 28px;
  border: 1px solid color-mix(in srgb, var(--accent) 32%, var(--border));
  border-radius: 8px;
  background: var(--accent-gradient);
  box-shadow: var(--shadow-hover);
}

.search-header__bar {
  max-width: 720px;
  margin: 0 auto 20px;

  :deep(.ant-input-search) {
    .ant-input {
      height: 48px;
      font-size: 16px;
      border-radius: 24px 0 0 24px;
      border-color: var(--border);
      padding-left: 20px;

      &:focus {
        border-color: var(--accent);
        box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 16%, transparent);
      }
    }

    .ant-input-search-button {
      height: 48px;
      border-radius: 0 24px 24px 0 !important;
      padding: 0 28px;
      font-size: 15px;
      background: var(--accent);
      border-color: var(--accent);
      color: var(--accent-text);
      font-weight: 900;

      &:hover {
        background: var(--accent-strong);
        border-color: var(--accent-strong);
      }
    }
  }
}

.search-header__filters {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;

  button {
    height: 34px;
    padding: 0 18px;
    border: 1px solid color-mix(in srgb, var(--accent-text) 22%, transparent);
    border-radius: 4px;
    background: color-mix(in srgb, var(--bg-card) 86%, transparent);
    color: var(--text-primary);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: color-mix(in srgb, var(--accent-text) 44%, transparent);
      color: var(--accent);
    }

    &.active {
      background: var(--bg-header);
      border-color: var(--bg-header);
      color: var(--header-text-active);
      font-weight: 800;
    }
  }
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 18px;
    color: var(--text-primary);
    font-weight: 900;
  }
}

.search-hot h2 {
  color: var(--text-primary);
  font-size: 16px;
  margin-bottom: 16px;
}

.search-history,
.search-hot,
.search-results {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.search-history + .search-hot {
  margin-top: 14px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .ant-tag {
    padding: 6px 16px;
    font-size: 14px;
    border-radius: 16px;
    cursor: pointer;
    transition: transform 0.15s;

    &:hover {
      transform: scale(1.06);
    }
  }
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .ant-tag {
    padding: 6px 14px;
    font-size: 13px;
    border-radius: 14px;
    cursor: pointer;
  }
}

.search-results {
  margin-top: 8px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px 12px;
}

.empty-search {
  padding: 80px 0;
  background: var(--bg-card);
  border-radius: var(--radius-md);
}

.load-more {
  margin-top: 24px;
  text-align: center;
}

@media (max-width: 1380px) {
  .room-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1024px) {
  .room-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .room-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .room-grid {
    grid-template-columns: 1fr;
  }
}
</style>
