import { computed, onMounted, ref, watch } from "vue";
import { useStore } from "@/stores";
import liveApi from "@/api/live";
import searchApi from "@/api/search";
import recommendApi from "@/api/recommend";
import {
  buildHotRanking,
  filterRoomsByKeyword,
  normalizeLivingRooms,
  sortRoomsByMode,
} from "@/utils/liveRoomPresenter";
import { normalizeCategories, normalizeSearchRooms } from "@/utils/categoryPresenter";

export function useHomeFeed() {
  const store = useStore();
  const allLivingRooms = ref([]);
  const recommendedRooms = ref([]);
  const categories = ref([]);
  const historyRooms = ref([]);
  const keyword = ref("");
  const feedMode = ref("recommend");
  const searchResults = ref([]);
  const searching = ref(false);
  const hasSearched = ref(false);

  const currentSelectCategory = computed(() => store.web().category.currentSelect);

  const loadCategories = async () => {
    try {
      const res = await liveApi.listCategories({});
      categories.value = normalizeCategories(res?.data?.list || []);
      if (currentSelectCategory.value && !categories.value.some((item) => item.id === currentSelectCategory.value.id)) {
        store.web().selectCategory(null);
      }
    } catch (error) {
      categories.value = [];
    }
  };

  const loadLivingRooms = async () => {
    try {
      const res = await liveApi.listLivingRooms({
        categoryId: currentSelectCategory.value?.id,
      });
      allLivingRooms.value = normalizeLivingRooms(res?.data?.list || []);
    } catch (error) {
      allLivingRooms.value = [];
    }
  };

  const loadHistory = async () => {
    try {
      const res = await liveApi.listHistory({ type: 0, page: 1, limit: 5 });
      historyRooms.value = res?.data?.list || [];
    } catch (error) {
      historyRooms.value = [];
    }
  };

  const loadRecommendations = async () => {
    try {
      const res = await recommendApi.getRecommendedRooms(12);
      recommendedRooms.value = normalizeLivingRooms(res?.data || []);
    } catch (error) {
      recommendedRooms.value = [];
    }
  };

  const doSearch = async () => {
    const kw = keyword.value.trim();
    if (!kw) {
      searchResults.value = [];
      hasSearched.value = false;
      return;
    }
    searching.value = true;
    hasSearched.value = true;
    try {
      const res = await searchApi.searchRooms({ keyword: kw, page: 1, limit: 24 });
      searchResults.value = normalizeLivingRooms(normalizeSearchRooms(res?.data?.list || []));
    } catch (error) {
      searchResults.value = [];
    } finally {
      searching.value = false;
    }
  };

  let searchTimer = null;
  const onKeywordChange = () => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      doSearch();
    }, 350);
  };

  const searchedRooms = computed(() => {
    if (hasSearched.value && keyword.value.trim()) {
      return searchResults.value;
    }
    return filterRoomsByKeyword(allLivingRooms.value, keyword.value);
  });
  const displayRooms = computed(() => {
    if (feedMode.value === "recommend" && !currentSelectCategory.value && !keyword.value.trim() && recommendedRooms.value.length > 0) {
      const ids = new Set(allLivingRooms.value.map(r => r.id));
      const freshRecommended = recommendedRooms.value.filter(r => ids.has(r.id));
      if (freshRecommended.length > 0) return freshRecommended;
    }
    return sortRoomsByMode(searchedRooms.value, feedMode.value, historyRooms.value);
  });
  const focusRoom = computed(() => displayRooms.value[0] || {});
  const hotRanking = computed(() => buildHotRanking(allLivingRooms.value, 8));

  const selectAll = () => {
    store.web().selectCategory(null);
    keyword.value = "";
    searchResults.value = [];
    hasSearched.value = false;
  };

  const selectCategory = (item) => {
    store.web().selectCategory(item);
    keyword.value = "";
    searchResults.value = [];
    hasSearched.value = false;
  };

  onMounted(() => {
    loadCategories();
    loadLivingRooms();
    loadHistory();
    loadRecommendations();
  });

  watch(currentSelectCategory, () => {
    loadLivingRooms();
  });

  return {
    allLivingRooms,
    categories,
    currentSelectCategory,
    displayRooms,
    feedMode,
    focusRoom,
    historyRooms,
    hotRanking,
    keyword,
    searching,
    hasSearched,
    searchResults,
    loadHistory,
    loadLivingRooms,
    onKeywordChange,
    selectAll,
    selectCategory,
  };
}
