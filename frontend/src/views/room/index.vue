<template>
  <div class="room-page">
    <section class="room-hero">
      <div class="hero-main">
        <div class="hero-topline">
          <span class="category-chip">{{ roomInfo.categoryInfo?.name || "推荐频道" }}</span>
          <span class="status-chip" :class="{ live: roomInfo.status === 1 }">{{ roomInfo.status === 1 ? "直播中" : "未开播" }}</span>
          <span class="browser-chip">{{ roomInfo.browserLive ? "网页低延迟优先" : "标准播放链路" }}</span>
        </div>

        <h1 class="live-title">{{ roomInfo.title || "直播间" }}</h1>

        <div class="hero-intro">
          {{ roomInfo.introduce || roomInfo.notice || "主播正在调试直播内容，支持聊天、礼物、低延迟观看和播放回退。" }}
        </div>

        <div class="hero-anchor">
          <img class="avatar" :src="roomInfo?.userInfo?.avatar || fallbackAvatar" alt="" />
          <div class="anchor-copy">
            <strong>{{ anchorName }}</strong>
            <span>{{ roomInfo.notice || "欢迎来到直播间，关注主播以便开播提醒。" }}</span>
          </div>
          <div class="hero-actions">
            <a-button class="follow-btn" :class="{ 'followed-btn': roomExtraInfo.follow }" @click="handleFollowBtnClick">
              <template #icon>
                <CheckCircleOutlined v-if="roomExtraInfo.follow" />
                <HeartOutlined v-else />
              </template>
              {{ roomExtraInfo.follow ? "已关注" : "关注主播" }}
            </a-button>
            <a-button @click="copyRoomLink">分享直播间</a-button>
          </div>
        </div>
      </div>

      <aside class="hero-aside">
        <div class="metric-card">
          <span>直播状态</span>
          <strong>{{ roomInfo.status === 1 ? "正在放送" : "等待开播" }}</strong>
        </div>
        <div class="metric-card">
          <span>观看链路</span>
          <strong>{{ roomInfo.browserLive ? "WebRTC 优先" : "HLS / FLV" }}</strong>
        </div>
        <div class="metric-card">
          <span>运营提示</span>
          <strong>{{ roomInfo.notice || "建议保持公告信息完整" }}</strong>
        </div>
      </aside>
    </section>

    <section class="room-main">
      <div class="room-play">
        <div class="room-play-main">
          <Player
            ref="playChild"
            v-if="roomInfo.status === 1 && (roomInfo.browserLive || roomInfo.pullUrl)"
            :room-id="roomId"
            :pull-url="roomInfo.pullUrl"
            :browser-live="Boolean(roomInfo.browserLive)"
          />
          <span v-else-if="roomInfo.status === 1" note>直播流还没有准备好，请先开播</span>
          <span v-else note>主播暂时离开了，稍后再来看看</span>
          <div id="svga-wrap"></div>
        </div>

        <div class="player-actions">
          <div class="action-pill-group">
            <span class="action-pill">直播标签：{{ roomInfo.categoryInfo?.name || "未分类" }}</span>
            <span class="action-pill">{{ roomInfo.browserLive ? "低延迟观看中" : "标准观看模式" }}</span>
            <span class="action-pill">{{ roomInfo.pullUrl ? "具备回退播放" : "等待播放地址" }}</span>
          </div>
          <div class="player-copy">
            <strong>观众体验</strong>
            <span>优先走网页直播低延迟链路，失败后自动切回拉流播放。</span>
          </div>
        </div>

        <div class="room-play-footer">
          <div class="footer-title">
            <h3>礼物互动区</h3>
            <p>保留礼物动画能力，并把直播间观感往成熟平台的“播放器下互动带”靠拢。</p>
          </div>
          <GiftList :room-id="roomId" />
        </div>
      </div>

      <aside class="room-chat">
        <div class="chat-title">
          <h3>直播互动区</h3>
          <p>实时聊天、礼物动态和亲密榜集中展示。</p>
        </div>
        <ChatList :room-id="roomId" @sendGift="handleSendGift" />
      </aside>
    </section>

    <section class="room-bottom">
      <div class="info-column">
        <div class="info-card">
          <div class="card-head">
            <h3>直播间信息</h3>
            <span>更像平台侧的主播信息面板</span>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span>主播昵称</span>
              <strong>{{ anchorName }}</strong>
            </div>
            <div class="info-item">
              <span>所属分区</span>
              <strong>{{ roomInfo.categoryInfo?.name || "未分类" }}</strong>
            </div>
            <div class="info-item">
              <span>房间公告</span>
              <strong>{{ roomInfo.notice || "暂无公告" }}</strong>
            </div>
            <div class="info-item">
              <span>互动能力</span>
              <strong>聊天 / 礼物 / 排行榜 / 历史记录</strong>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="card-head">
            <h3>主播简介</h3>
            <span>直播定位、频道说明与房间运营信息</span>
          </div>
          <p class="long-copy">
            {{ roomInfo.introduce || "主播暂未填写详细简介。你可以在这里展示本场主题、直播节奏、更新安排、观众须知或活动说明，让直播间更像一个长期运营的内容阵地。" }}
          </p>
        </div>
      </div>

      <aside class="recommend-column">
        <div class="info-card">
          <div class="card-head">
            <h3>继续逛同类直播</h3>
            <span>参考内容平台的相关推荐区</span>
          </div>
          <div class="recommend-list">
            <button
              v-for="item in recommendRooms"
              :key="item.id"
              class="recommend-item"
              type="button"
              @click="enterRoom(item.id)"
            >
              <img :src="item.cover || fallbackCover" alt="" />
              <div>
                <strong>{{ item.title }}</strong>
                <span>{{ item.userInfo?.name || item.userInfo?.nickName || "主播" }} · {{ item.categoryInfo?.name || "推荐" }}</span>
              </div>
            </button>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import { message } from "ant-design-vue"
import { CheckCircleOutlined, HeartOutlined } from "@ant-design/icons-vue"
import SVGA from "svgaplayerweb"
import Player from "./Player.vue"
import ChatList from "./ChatList.vue"
import GiftList from "./GiftList.vue"
import roomApi from "@/api/room"
import watchApi from "@/api/watch"
import liveApi from "@/api/live"

const router = useRouter()
const roomId = computed(() => Number(router.currentRoute.value.params.id))
const isLogin = computed(() => useStore().user().isLogin)

const playChild = ref()
const roomInfo = ref({})
const roomExtraInfo = ref({})
const recommendRooms = ref([])
const svgaPlayer = ref(null)
const svgaParser = ref(null)
const fallbackCover = "https://dummyimage.com/640x360/e2e8f0/64748b&text=LIVE"
const fallbackAvatar = "https://dummyimage.com/96x96/e2e8f0/64748b&text=A"

const anchorName = computed(
  () => roomInfo.value?.userInfo?.name || roomInfo.value?.userInfo?.nickName || "主播"
)

onMounted(async () => {
  initSvga()
  await getRoomInfo()
  await getRecommendRooms()
  if (isLogin.value) {
    getRoomExtraInfo()
    saveHistory()
  }
})

const handleFollowBtnClick = () => {
  if (!isLogin.value) {
    router.push("/login")
    return
  }
  if (roomExtraInfo.value.follow) {
    unFollow()
    return
  }
  follow()
}

const saveHistory = () => {
  watchApi.saveHistory({ roomId: roomId.value })
}

const follow = async () => {
  await watchApi.follow({ roomId: roomId.value })
  getRoomExtraInfo()
}

const unFollow = async () => {
  await watchApi.unFollow({ roomId: roomId.value })
  getRoomExtraInfo()
}

const initSvga = () => {
  svgaPlayer.value = new SVGA.Player("#svga-wrap")
  svgaParser.value = new SVGA.Parser("#svga-wrap")
}

const playSvga = (url) => {
  if (!svgaPlayer.value) {
    message.error("礼物动画初始化失败")
    return
  }

  svgaPlayer.value.clearAfterStop = true
  svgaPlayer.value.stopAnimation(true)
  svgaParser.value.load(url, (videoItem) => {
    svgaPlayer.value.loops = 1
    svgaPlayer.value.setVideoItem(videoItem)
    svgaPlayer.value.startAnimation()
  })
}

const getRoomInfo = async () => {
  const res = await roomApi.getRoomInfo({
    roomId: roomId.value,
  })
  roomInfo.value = res.data || {}
}

const getRoomExtraInfo = async () => {
  const res = await roomApi.getRoomExtraInfo({
    roomId: roomId.value,
  })
  roomExtraInfo.value = res.data || {}
}

const getRecommendRooms = async () => {
  try {
    const res = await liveApi.listLivingRooms({})
    recommendRooms.value = (res?.data?.list || [])
      .filter((item) => Number(item.id) !== roomId.value)
      .slice(0, 5)
  } catch (error) {
    recommendRooms.value = []
  }
}

const handleSendGift = () => {
  playSvga("svga/angel.svga")
}

const copyRoomLink = async () => {
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    message.success("直播间链接已复制")
  } catch (error) {
    message.warning("复制失败，请手动复制当前地址")
  }
}

const enterRoom = (id) => {
  if (!id) {
    return
  }
  router.push(`/room/${id}`)
}
</script>

<style scoped lang="scss">
.room-page {
  --panel-bg: rgba(255, 255, 255, 0.94);
  --panel-border: rgba(15, 23, 42, 0.08);
  --text-main: #18191c;
  --text-sub: #61666d;
  --brand-blue: #00aeec;
  --brand-pink: #fb7299;

  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
}

.room-hero,
.room-play,
.room-chat,
.info-card {
  border: 1px solid var(--panel-border);
  border-radius: 24px;
  background: var(--panel-bg);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
}

.room-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  padding: 26px;
  background:
    radial-gradient(circle at top left, rgba(0, 174, 236, 0.12), transparent 20%),
    radial-gradient(circle at top right, rgba(251, 114, 153, 0.12), transparent 18%),
    linear-gradient(135deg, #ffffff 0%, #fafcff 100%);
}

.hero-topline,
.hero-anchor,
.hero-actions,
.room-main,
.player-actions,
.action-pill-group,
.room-bottom,
.info-grid,
.card-head {
  display: flex;
  gap: 12px;
}

.hero-topline,
.player-actions,
.room-bottom {
  justify-content: space-between;
}

.hero-topline {
  flex-wrap: wrap;
}

.category-chip,
.status-chip,
.browser-chip,
.action-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
}

.category-chip {
  background: rgba(0, 174, 236, 0.1);
  color: #0086bf;
}

.status-chip {
  background: #eef2f7;
  color: #64748b;
}

.status-chip.live {
  background: rgba(251, 114, 153, 0.12);
  color: #d6336c;
}

.browser-chip,
.action-pill {
  background: rgba(15, 23, 42, 0.05);
  color: #475569;
}

.live-title {
  margin: 16px 0 10px;
  color: var(--text-main);
  font-size: 34px;
  line-height: 1.18;
}

.hero-intro {
  color: var(--text-sub);
  font-size: 15px;
  line-height: 1.85;
}

.hero-anchor {
  align-items: center;
  margin-top: 22px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.anchor-copy {
  flex: 1;
  min-width: 0;
}

.anchor-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 20px;
}

.anchor-copy span {
  display: block;
  margin-top: 6px;
  color: var(--text-sub);
  line-height: 1.6;
}

.hero-actions {
  align-items: center;
}

.hero-aside {
  display: grid;
  gap: 12px;
}

.metric-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.metric-card span {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: var(--text-main);
  font-size: 18px;
  line-height: 1.5;
}

.room-main {
  align-items: start;
  margin-top: 20px;
}

.room-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
}

.room-play {
  overflow: hidden;
}

.room-play-main {
  position: relative;
  min-height: 600px;
  background: #020617;
}

span[note] {
  position: absolute;
  top: 50%;
  left: 50%;
  color: #cbd5e1;
  font-size: 16px;
  transform: translate(-50%, -50%);
}

.player-actions {
  align-items: center;
  padding: 18px 20px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.action-pill-group {
  flex-wrap: wrap;
}

.player-copy strong {
  display: block;
  color: var(--text-main);
}

.player-copy span {
  display: block;
  margin-top: 6px;
  color: var(--text-sub);
  font-size: 13px;
}

.room-play-footer,
.room-chat {
  padding: 20px;
}

.footer-title h3,
.chat-title h3,
.card-head h3 {
  margin: 0;
  color: var(--text-main);
}

.footer-title p,
.chat-title p,
.card-head span {
  margin: 8px 0 0;
  color: var(--text-sub);
}

.chat-title {
  margin-bottom: 14px;
}

.follow-btn {
  min-width: 104px;
}

.followed-btn {
  background-color: #f5f5f5;
  color: #666;
  border-color: #d9d9d9;
}

#svga-wrap {
  position: absolute;
  inset: 0;
  z-index: 999;
}

.room-bottom {
  align-items: start;
  margin-top: 20px;
}

.room-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
}

.info-column,
.recommend-column {
  display: grid;
  gap: 18px;
}

.info-card {
  padding: 20px;
}

.card-head {
  align-items: flex-start;
  justify-content: space-between;
}

.info-grid {
  flex-wrap: wrap;
  margin-top: 16px;
}

.info-item {
  flex: 1 1 calc(50% - 12px);
  min-width: 220px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.info-item span {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.info-item strong {
  display: block;
  margin-top: 8px;
  color: var(--text-main);
  line-height: 1.6;
}

.long-copy {
  margin: 16px 0 0;
  color: var(--text-sub);
  line-height: 1.9;
}

.recommend-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.recommend-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.recommend-item img {
  width: 100%;
  height: 76px;
  border-radius: 16px;
  object-fit: cover;
  background: #e2e8f0;
}

.recommend-item strong {
  display: block;
  color: var(--text-main);
  line-height: 1.5;
}

.recommend-item span {
  display: block;
  margin-top: 6px;
  color: var(--text-sub);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .room-hero,
  .room-main,
  .room-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .room-page {
    padding: 16px;
  }

  .live-title {
    font-size: 28px;
  }

  .hero-anchor,
  .hero-actions,
  .player-actions,
  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .room-play-main {
    min-height: 360px;
  }

  .info-item {
    min-width: 100%;
  }
}
</style>
