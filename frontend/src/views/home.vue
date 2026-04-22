<template>
  <div class="home-page">
    <section class="hero-shell">
      <div class="hero-main">
        <div class="hero-copy">
          <span class="hero-badge">ANT LIVE · LIVE NOW</span>
          <h1>像内容平台一样逛直播，而不只是进入一个房间</h1>
          <p>
            把推荐流、频道分区、最近观看、热度榜和低延迟直播能力收进同一个首页。整体体验向成熟直播平台靠拢，但保留本项目现有的开播、播放和互动链路。
          </p>
          <div class="hero-actions">
            <a-button type="primary" size="large" @click="goLiveCenter">立即开播</a-button>
            <a-button size="large" @click="handleAllClick">浏览全部直播</a-button>
          </div>
          <div class="hero-stats">
            <div class="hero-stat">
              <strong>{{ allLivingRooms.length }}</strong>
              <span>正在直播</span>
            </div>
            <div class="hero-stat">
              <strong>{{ categories.length || 0 }}</strong>
              <span>内容分区</span>
            </div>
            <div class="hero-stat">
              <strong>{{ totalPopularity }}</strong>
              <span>全站热度</span>
            </div>
          </div>
        </div>

        <div class="hero-focus">
          <div class="focus-cover">
            <img :src="heroRoom.cover || fallbackCover" alt="" />
            <div class="focus-overlay">
              <span class="focus-live-tag">直播中</span>
              <span class="focus-latency-tag">WebRTC 低延迟优先</span>
            </div>
          </div>
          <div class="focus-body">
            <div class="focus-category">{{ heroRoom.categoryInfo?.name || "推荐频道" }}</div>
            <h2>{{ heroRoom.title || "发现高互动直播内容" }}</h2>
            <p>{{ heroRoom.introduce || heroRoom.notice || "支持网页直播、实时互动、礼物和观看回退链路。" }}</p>
            <div class="focus-meta">
              <div class="anchor-chip">
                <img :src="heroRoom.userInfo?.avatar || fallbackAvatar" alt="" />
                <span>{{ heroRoom.userInfo?.name || heroRoom.userInfo?.nickName || "官方推荐主播" }}</span>
              </div>
              <span class="focus-hot">热度 {{ formatPopularity(heroRoom.popularity || 0) }}</span>
            </div>
            <div class="focus-actions">
              <a-button type="primary" @click="enterRoom(heroRoom.id)">进入直播间</a-button>
              <a-button @click="scrollToRecommend">看热门推荐</a-button>
            </div>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="panel-card">
          <div class="panel-head">
            <h3>今日热门榜</h3>
            <span>按房间热度排序</span>
          </div>
          <div class="ranking-list">
            <button
              v-for="(room, index) in hotRanking"
              :key="room.id"
              class="ranking-item"
              type="button"
              @click="enterRoom(room.id)"
            >
              <span class="ranking-no" :class="`ranking-no-${index + 1}`">{{ index + 1 }}</span>
              <div class="ranking-body">
                <strong>{{ room.title }}</strong>
                <span>{{ room.userInfo?.name || room.userInfo?.nickName || "主播" }} · {{ room.categoryInfo?.name || "推荐" }}</span>
              </div>
              <span class="ranking-value">{{ formatPopularity(room.popularity || 0) }}</span>
            </button>
          </div>
        </div>

        <div class="panel-card">
          <div class="panel-head">
            <h3>频道导航</h3>
            <span>快速切换内容分区</span>
          </div>
          <div class="category-cloud">
            <button
              class="category-pill"
              :class="{ active: !currentSelectCategory }"
              type="button"
              @click="handleAllClick"
            >
              全部
            </button>
            <button
              v-for="item in categories"
              :key="item.id"
              class="category-pill"
              :class="{ active: currentSelectCategory?.id === item.id }"
              type="button"
              @click="handleCategoryClick(item)"
            >
              {{ item.name }}
            </button>
          </div>
        </div>
      </aside>
    </section>

    <section class="discover-shell">
      <div class="discover-toolbar">
        <div>
          <h2>{{ currentSelectCategory?.name || "推荐直播" }}</h2>
          <p>更像直播平台首页的内容流：支持搜索、筛选、热度排序与分区浏览。</p>
        </div>
        <div class="discover-actions">
          <a-input-search
            v-model:value="keyword"
            class="search-box"
            allow-clear
            placeholder="搜索直播标题 / 主播 / 分区"
          />
          <a-radio-group v-model:value="sortMode" button-style="solid" size="middle">
            <a-radio-button value="recommend">推荐</a-radio-button>
            <a-radio-button value="hot">最热</a-radio-button>
            <a-radio-button value="new">最新</a-radio-button>
          </a-radio-group>
        </div>
      </div>

      <div class="channel-strip">
        <button
          v-for="item in featuredCategories"
          :key="item.id"
          class="channel-card"
          :class="{ active: currentSelectCategory?.id === item.id }"
          type="button"
          @click="handleCategoryClick(item)"
        >
          <strong>{{ item.name }}</strong>
          <span>{{ item.roomCount }} 个直播间</span>
        </button>
      </div>

      <div v-if="historyRooms.length" class="history-block">
        <div class="section-title">
          <h3>继续观看</h3>
          <span>最近访问过的直播间会展示在这里</span>
        </div>
        <div class="history-row">
          <button
            v-for="item in historyRooms"
            :key="item.roomId || item.id"
            class="history-item"
            type="button"
            @click="enterRoom(item.roomId || item.id)"
          >
            <img :src="item.cover || fallbackCover" alt="" />
            <div>
              <strong>{{ item.title || "直播间" }}</strong>
              <span>{{ item.userNickname || item.userInfo?.name || "继续回到直播现场" }}</span>
            </div>
          </button>
        </div>
      </div>
    </section>

    <section ref="recommendRef" class="content-shell">
      <div class="main-column">
        <div class="section-title">
          <h3>推荐直播流</h3>
          <span>根据当前频道和搜索条件实时筛选</span>
        </div>
        <div v-if="displayRooms.length" class="room-grid">
          <LiveRoom v-for="item in displayRooms" :key="item.id" :room="item" />
        </div>
        <a-empty v-else description="当前条件下还没有匹配的直播内容，试试切换频道或清空搜索" />
      </div>

      <aside class="side-column">
        <div class="panel-card compact-panel">
          <div class="panel-head">
            <h3>观众最爱看</h3>
            <span>高互动直播间</span>
          </div>
          <div class="mini-list">
            <button
              v-for="room in sidebarRooms"
              :key="room.id"
              class="mini-room"
              type="button"
              @click="enterRoom(room.id)"
            >
              <img :src="room.cover || fallbackCover" alt="" />
              <div>
                <strong>{{ room.title }}</strong>
                <span>{{ room.categoryInfo?.name || "推荐" }} · {{ formatPopularity(room.popularity || 0) }}</span>
              </div>
            </button>
          </div>
        </div>
      </aside>
    </section>

    <section v-if="categoryFloors.length" class="floors-shell">
      <div
        v-for="floor in categoryFloors"
        :key="floor.id"
        class="floor-block"
      >
        <div class="floor-head">
          <div>
            <h3>{{ floor.name }}</h3>
            <p>围绕 {{ floor.name }} 分区继续往下逛，快速跳转更多内容。</p>
          </div>
          <a-button type="link" @click="handleCategoryClick(floor)">进入频道</a-button>
        </div>
        <div class="floor-grid">
          <LiveRoom v-for="room in floor.rooms" :key="room.id" :room="room" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import liveApi from "@/api/live"
import LiveRoom from "@/components/LiveRoom.vue"

const store = useStore()
const router = useRouter()

const allLivingRooms = ref([])
const categories = ref([])
const historyRooms = ref([])
const keyword = ref("")
const sortMode = ref("recommend")
const recommendRef = ref(null)
const fallbackCover = "https://dummyimage.com/960x540/e2e8f0/64748b&text=LIVE"
const fallbackAvatar = "https://dummyimage.com/96x96/e2e8f0/64748b&text=A"

const currentSelectCategory = computed(() => store.web().category.currentSelect)

watch(
  () => currentSelectCategory.value,
  () => {
    getLiveRooms()
  }
)

onMounted(async () => {
  await Promise.all([getLiveRooms(), getCategories(), getHistory()])
})

const normalizePopularity = (room, index = 0) => Number(room?.popularity || room?.heat || 0) + (allLivingRooms.value.length - index)

const sortedRooms = computed(() => {
  const keywordValue = keyword.value.trim().toLowerCase()
  const list = [...allLivingRooms.value]
    .filter((item) => {
      if (!keywordValue) {
        return true
      }
      const bucket = [
        item?.title,
        item?.userInfo?.name,
        item?.userInfo?.nickName,
        item?.categoryInfo?.name,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
      return bucket.includes(keywordValue)
    })

  const byHot = (a, b) => normalizePopularity(b) - normalizePopularity(a)
  const byRecommend = (a, b) => {
    const aScore = normalizePopularity(a) + (a?.browserLive ? 3000 : 0) + (a?.pullUrl ? 1000 : 0)
    const bScore = normalizePopularity(b) + (b?.browserLive ? 3000 : 0) + (b?.pullUrl ? 1000 : 0)
    return bScore - aScore
  }

  if (sortMode.value === "hot") {
    return list.sort(byHot)
  }
  if (sortMode.value === "new") {
    return list.reverse()
  }
  return list.sort(byRecommend)
})

const displayRooms = computed(() => sortedRooms.value)
const heroRoom = computed(() => displayRooms.value[0] || {})
const hotRanking = computed(() => [...allLivingRooms.value].sort((a, b) => normalizePopularity(b) - normalizePopularity(a)).slice(0, 5))
const sidebarRooms = computed(() => [...sortedRooms.value].slice(0, 4))
const totalPopularity = computed(() => {
  const total = allLivingRooms.value.reduce((sum, item, index) => sum + normalizePopularity(item, index), 0)
  return formatPopularity(total)
})

const featuredCategories = computed(() =>
  categories.value.map((item) => ({
    ...item,
    roomCount: allLivingRooms.value.filter((room) => room.categoryId === item.id || room.categoryInfo?.id === item.id).length,
  }))
    .filter((item) => item.roomCount > 0)
    .slice(0, 8)
)

const categoryFloors = computed(() =>
  featuredCategories.value
    .map((item) => ({
      ...item,
      rooms: [...allLivingRooms.value]
        .filter((room) => room.categoryId === item.id || room.categoryInfo?.id === item.id)
        .sort((a, b) => normalizePopularity(b) - normalizePopularity(a))
        .slice(0, 4),
    }))
    .filter((item) => item.rooms.length)
    .slice(0, 3)
)

const getCategories = async () => {
  const res = await liveApi.listCategories({})
  categories.value = res?.data?.list || []
}

const getLiveRooms = async () => {
  const res = await liveApi.listLivingRooms({
    categoryId: currentSelectCategory.value?.id,
  })
  allLivingRooms.value = (res?.data?.list || []).map((item, index) => ({
    ...item,
    popularity: Number(item?.popularity || 0) || (res?.data?.list?.length || 0) - index,
  }))
}

const getHistory = async () => {
  try {
    const res = await liveApi.listHistory({ type: 0, page: 1, limit: 6 })
    historyRooms.value = res?.data?.list || []
  } catch (error) {
    historyRooms.value = []
  }
}

const handleAllClick = () => {
  store.web().selectCategory(null)
}

const handleCategoryClick = (item) => {
  store.web().selectCategory(item)
}

const goLiveCenter = () => {
  router.push("/center/live/live-settings")
}

const enterRoom = (id) => {
  if (!id) {
    return
  }
  router.push(`/room/${id}`)
}

const scrollToRecommend = async () => {
  await nextTick()
  recommendRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

const formatPopularity = (value) => {
  const count = Number(value || 0)
  if (!Number.isFinite(count) || count <= 0) {
    return "0"
  }
  if (count >= 100000000) {
    return `${(count / 100000000).toFixed(1).replace(/\.0$/, "")}亿`
  }
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`
  }
  return `${count}`
}
</script>

<style lang="scss" scoped>
.home-page {
  --page-bg: #f6f7fb;
  --panel-bg: rgba(255, 255, 255, 0.94);
  --border-color: rgba(15, 23, 42, 0.08);
  --text-main: #18191c;
  --text-sub: #61666d;
  --brand-blue: #00aeec;
  --brand-pink: #fb7299;

  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  background:
    radial-gradient(circle at top left, rgba(0, 174, 236, 0.08), transparent 24%),
    radial-gradient(circle at top right, rgba(251, 114, 153, 0.08), transparent 22%),
    var(--page-bg);
}

.hero-shell,
.discover-shell,
.content-shell,
.floors-shell {
  margin-top: 20px;
}

.hero-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  margin-top: 0;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 18px;
}

.hero-copy,
.hero-focus,
.panel-card,
.discover-shell,
.main-column,
.side-column,
.floor-block {
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--panel-bg);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
}

.hero-copy {
  padding: 30px;
  background:
    linear-gradient(135deg, rgba(0, 174, 236, 0.16), transparent 36%),
    linear-gradient(145deg, #ffffff 0%, #fbfdff 60%, #fff7fa 100%);
}

.hero-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(0, 174, 236, 0.1);
  color: #0086bf;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 18px 0 14px;
  color: var(--text-main);
  font-size: 44px;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.hero-copy p {
  margin: 0;
  color: var(--text-sub);
  font-size: 15px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 28px;
}

.hero-stat {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.hero-stat strong {
  display: block;
  color: var(--text-main);
  font-size: 26px;
}

.hero-stat span {
  display: block;
  margin-top: 6px;
  color: var(--text-sub);
  font-size: 13px;
}

.hero-focus {
  overflow: hidden;
}

.focus-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #111827;
}

.focus-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.focus-overlay {
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.focus-live-tag,
.focus-latency-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  backdrop-filter: blur(8px);
}

.focus-live-tag {
  background: rgba(251, 114, 153, 0.92);
}

.focus-latency-tag {
  background: rgba(15, 23, 42, 0.64);
}

.focus-body {
  padding: 22px;
}

.focus-category {
  color: var(--brand-pink);
  font-size: 13px;
  font-weight: 700;
}

.focus-body h2 {
  margin: 10px 0;
  color: var(--text-main);
  font-size: 26px;
}

.focus-body p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.8;
}

.focus-meta,
.focus-actions,
.panel-head,
.discover-toolbar,
.section-title,
.floor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.focus-meta {
  margin-top: 16px;
}

.anchor-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.anchor-chip img {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}

.anchor-chip span,
.focus-hot {
  color: var(--text-sub);
  font-size: 13px;
}

.focus-actions {
  justify-content: flex-start;
  margin-top: 18px;
}

.hero-side {
  display: grid;
  gap: 18px;
}

.panel-card {
  padding: 20px;
}

.panel-head h3,
.section-title h3,
.floor-head h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 20px;
}

.panel-head span,
.section-title span,
.floor-head p,
.discover-toolbar p {
  color: var(--text-sub);
  font-size: 13px;
}

.ranking-list,
.mini-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.ranking-item,
.mini-room,
.history-item,
.channel-card {
  width: 100%;
  border: 0;
  cursor: pointer;
  text-align: left;
}

.ranking-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: #f8fafc;
}

.ranking-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #dbeafe;
  color: #2563eb;
  font-weight: 700;
}

.ranking-no-1 {
  background: #ffe08a;
  color: #8a5b00;
}

.ranking-no-2 {
  background: #e2e8f0;
  color: #475569;
}

.ranking-no-3 {
  background: #ffd7b2;
  color: #9a3412;
}

.ranking-body {
  min-width: 0;
}

.ranking-body strong,
.mini-room strong,
.history-item strong {
  display: block;
  color: var(--text-main);
  line-height: 1.5;
}

.ranking-body span,
.mini-room span,
.history-item span {
  display: block;
  margin-top: 4px;
  color: var(--text-sub);
  font-size: 12px;
}

.ranking-value {
  color: var(--brand-pink);
  font-size: 13px;
  font-weight: 700;
}

.category-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.category-pill {
  padding: 10px 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  background: #fff;
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-pill.active,
.category-pill:hover {
  border-color: rgba(0, 174, 236, 0.25);
  background: rgba(0, 174, 236, 0.08);
  color: #0086bf;
}

.discover-shell {
  padding: 20px 22px 22px;
}

.discover-toolbar h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
}

.discover-toolbar p {
  margin: 8px 0 0;
}

.discover-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  width: 320px;
}

.channel-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.channel-card {
  padding: 18px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(0, 174, 236, 0.12), transparent 70%),
    #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.channel-card:hover,
.channel-card.active {
  transform: translateY(-2px);
  border-color: rgba(0, 174, 236, 0.25);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.channel-card strong {
  display: block;
  color: var(--text-main);
  font-size: 16px;
}

.channel-card span {
  display: block;
  margin-top: 6px;
  color: var(--text-sub);
  font-size: 12px;
}

.history-block {
  margin-top: 20px;
}

.history-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.history-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  padding: 10px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.history-item img,
.mini-room img {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  object-fit: cover;
  background: #e2e8f0;
}

.content-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.main-column,
.side-column {
  padding: 20px;
}

.room-grid,
.floor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.compact-panel {
  padding: 0;
  overflow: hidden;
}

.compact-panel .panel-head {
  padding: 20px 20px 0;
}

.mini-list {
  padding: 16px 20px 20px;
}

.mini-room {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
}

.mini-room img {
  height: 72px;
}

.mini-room div {
  padding: 8px 0;
}

.floors-shell {
  display: grid;
  gap: 20px;
}

.floor-block {
  padding: 22px;
}

@media (max-width: 1280px) {
  .hero-shell,
  .content-shell {
    grid-template-columns: 1fr;
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .channel-strip,
  .history-row,
  .room-grid,
  .floor-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .home-page {
    padding: 16px 16px 32px;
  }

  .hero-copy h1 {
    font-size: 32px;
  }

  .hero-stats,
  .channel-strip,
  .history-row,
  .room-grid,
  .floor-grid {
    grid-template-columns: 1fr;
  }

  .discover-toolbar,
  .discover-actions,
  .section-title,
  .floor-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-box {
    width: 100%;
  }
}
</style>
