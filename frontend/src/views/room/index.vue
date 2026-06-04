<template>
  <div class="room-page">
    <section class="room-shell">
      <main class="player-column">
        <div class="room-head">
          <div class="anchor-avatar-wrap">
            <img class="anchor-avatar" :src="roomInfo.userInfo?.avatar || fallbackAvatar" alt="" @error="onImgError" />
            <span class="anchor-live-dot"></span>
          </div>
          <div class="head-copy">
            <div class="head-meta">
              <span>{{ roomInfo.categoryInfo?.name || "推荐" }}</span>
              <span>{{ roomInfo.status === 1 ? "直播中" : "未开播" }}</span>
              <span>{{ roomInfo.browserLive ? "流畅观看" : "高清观看" }}</span>
            </div>
            <h1>{{ roomInfo.title || "直播间" }}</h1>
            <div class="anchor-line">
              <strong>{{ anchorName }}</strong>
              <span>{{ roomInfo.notice || roomInfo.introduce || "欢迎来到直播间，文明互动，理性消费。" }}</span>
            </div>
          </div>
          <div class="head-stats">
            <div>
              <strong>{{ formatCount(roomInfo.popularity || 0) }}</strong>
              <span>热度</span>
            </div>
            <div>
              <strong>{{ roomTags.length || "-" }}</strong>
              <span>标签</span>
            </div>
          </div>
          <div class="head-actions">
            <a-button
              type="primary"
              :ghost="roomExtraInfo.follow"
              @click="handleFollowBtnClick"
            >
              {{ roomExtraInfo.follow ? "已关注" : "关注" }}
            </a-button>
            <a-button v-if="isLogin && roomInfo.userId !== myUserId" @click="showGuardianModal = true">开通守护</a-button>
            <a-button @click="copyRoomLink">分享</a-button>
            <a-button v-if="isLogin && roomInfo.userId !== myUserId" type="text" danger @click="handleReport">举报</a-button>
          </div>
        </div>

        <section class="watch-stage">
          <div class="player-box">
            <Player
              v-if="roomInfo.status === 1 && (roomInfo.browserLive || roomInfo.pullUrl)"
              ref="playChild"
              :room-id="roomId"
              :pull-url="roomInfo.pullUrl"
              :browser-live="Boolean(roomInfo.browserLive)"
            />
            <div v-else class="player-empty">
              <strong>{{ roomInfo.status === 1 ? "直播画面准备中" : "主播暂未开播" }}</strong>
              <span>{{ roomInfo.status === 1 ? "稍等片刻，直播画面马上出现。" : "可以先关注主播，开播后再回来观看。" }}</span>
            </div>
            <div id="svga-wrap"></div>
          </div>

          <PlayerToolbar
            :is-live="roomInfo.status === 1"
            :viewer-count="roomInfo.popularity || 0"
            @line-change="onLineChange"
            @quality-change="onQualityChange"
            @volume-change="onVolumeChange"
            @fullscreen="onFullscreen"
          />

          <div class="watch-underbar">
            <div class="watch-tags">
              <span v-if="!roomTags.length">弹幕互动</span>
              <span v-for="tag in roomTags" :key="tag.id || tag.tagName">{{ tag.tagName }}</span>
            </div>
            <p>{{ roomInfo.notice || "文明发言，理性消费，享受直播。" }}</p>
          </div>
        </section>

        <section class="gift-section">
          <div class="section-head">
            <h2>礼物与互动</h2>
            <span>{{ isLogin ? "送礼会扣除开心果并进入亲密榜" : "登录后可送礼" }}</span>
          </div>
          <GiftList :room-id="roomId" @require-login="goLogin" />
        </section>

        <section class="detail-section">
          <div class="section-head">
            <h2>主播资料</h2>
            <span>{{ anchorName }}</span>
          </div>
          <p>{{ roomInfo.introduce || "主播暂时还没有填写简介。" }}</p>
        </section>
      </main>

      <aside class="chat-column">
        <div class="chat-tabs">
          <button :class="{ active: chatMode === 'chat' }" @click="chatMode = 'chat'">弹幕</button>
          <button :class="{ active: chatMode === 'agent' }" @click="chatMode = 'agent'">互动助手</button>
        </div>
        <ChatList v-if="chatMode === 'chat'" :room-id="roomId" :is-moderator="isModerator" @sendGift="handleSendGift" />
        <AgentPanel
          v-else
          :chat-messages="[]"
          :room-title="roomInfo.title"
          :category-name="roomInfo.categoryInfo?.name"
          :anchor-name="anchorName"
          @send-welcome="handleSendWelcome"
        />
      </aside>
    </section>

    <section class="recommend-section" v-if="recommendRooms.length">
      <div class="section-head">
        <h2>相关推荐</h2>
        <span>为你挑选相似内容</span>
      </div>
      <div class="recommend-grid">
        <LiveRoom v-for="item in recommendRooms" :key="item.id" :room="item" />
      </div>
    </section>

    <!-- 开通守护弹窗 -->
    <a-modal v-model:open="showGuardianModal" title="开通守护" :footer="null" width="420px">
      <div class="guardian-modal">
        <p class="guardian-tip">开通守护支持主播，获得专属徽章和特权</p>
        <a-radio-group v-model:value="guardianLevel" direction="vertical">
          <a-radio :value="1">
            <span>青铜守护 <a-tag color="brown">¥3/月</a-tag></span>
          </a-radio>
          <a-radio :value="2">
            <span>白银守护 <a-tag color="grey">¥6/月</a-tag></span>
          </a-radio>
          <a-radio :value="3">
            <span>黄金守护 <a-tag color="gold">¥12/月</a-tag></span>
          </a-radio>
        </a-radio-group>
        <a-checkbox v-model:checked="guardianAutoRenew" style="margin-top:12px">自动续费</a-checkbox>
        <div style="margin-top:16px;text-align:right">
          <a-button @click="showGuardianModal = false" style="margin-right:8px">取消</a-button>
          <a-button type="primary" :loading="guardianLoading" @click="subscribeGuardian">确认开通</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 举报弹窗 -->
    <a-modal v-model:open="showReportModal" title="举报" :footer="null" width="400px">
      <a-form layout="vertical">
        <a-form-item label="举报原因">
          <a-select v-model:value="reportReason" placeholder="选择举报原因">
            <a-select-option value="违规内容">违规内容</a-select-option>
            <a-select-option value="色情低俗">色情低俗</a-select-option>
            <a-select-option value="欺诈诈骗">欺诈诈骗</a-select-option>
            <a-select-option value="侵权投诉">侵权投诉</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="详细描述">
          <a-textarea v-model:value="reportDesc" placeholder="请描述具体情况（选填）" :maxlength="200" :rows="3" />
        </a-form-item>
        <div style="text-align:right">
          <a-button @click="showReportModal = false" style="margin-right:8px">取消</a-button>
          <a-button type="primary" :loading="reportLoading" @click="submitReport">提交举报</a-button>
        </div>
      </a-form>
    </a-modal>

    <GiftEffects ref="giftEffectsRef" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import $modal from "@/utils/message"
import SVGA from "svgaplayerweb"
import Player from "./Player.vue"
import PlayerToolbar from "@/components/live/PlayerToolbar.vue"
import ChatList from "./ChatList.vue"
import GiftList from "./GiftList.vue"
import LiveRoom from "@/components/LiveRoom.vue"
import AgentPanel from "@/components/live/AgentPanel.vue"
import GiftEffects from "@/components/live/GiftEffects.vue"
import roomApi from "@/api/room"
import watchApi from "@/api/watch"
import liveApi from "@/api/live"
import tagApi from "@/api/tag"
import guardianApi from "@/api/guardian"
import recommendApi from "@/api/recommend"
import reportApi from "@/api/report"
import moderatorApi from "@/api/moderator"

const router = useRouter()
const store = useStore()
const roomId = computed(() => Number(router.currentRoute.value.params.id))
const isLogin = computed(() => store.user().isLogin)

const playChild = ref()
const roomInfo = ref({})
const roomExtraInfo = ref({})
const roomTags = ref([])
const recommendRooms = ref([])
const myUserId = ref(store.user().userId || 0)

// 守护相关
const showGuardianModal = ref(false)
const guardianLevel = ref(1)
const guardianAutoRenew = ref(false)
const guardianLoading = ref(false)

// 举报相关
const showReportModal = ref(false)
const reportReason = ref('违规内容')
const reportDesc = ref('')
const reportLoading = ref(false)
const isModerator = ref(false)

const checkModerator = async () => {
  try {
    const res = await moderatorApi.check(roomId.value)
    isModerator.value = res?.data === true
  } catch (e) { isModerator.value = false }
}
const svgaPlayer = ref(null)
const svgaParser = ref(null)
import { FALLBACK_AVATAR, onImgError } from "@/utils/fallback"
const fallbackAvatar = FALLBACK_AVATAR

const chatMode = ref("chat")

const anchorName = computed(() => roomInfo.value?.userInfo?.name || roomInfo.value?.userInfo?.nickName || "主播")

const giftEffectsRef = ref(null)

const formatCount = (value) => {
  const count = Number(value || 0)
  if (!Number.isFinite(count) || count <= 0) return "0"
  if (count >= 100000000) return `${(count / 100000000).toFixed(1).replace(/\.0$/, "")}亿`
  if (count >= 10000) return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`
  return `${count}`
}

onMounted(async () => {
  initSvga()
  await refreshRoom()
})

onBeforeUnmount(() => {
  svgaPlayer.value?.stopAnimation?.(true)
})

watch(roomId, async () => {
  await refreshRoom()
})

const loadRoomTags = async () => {
  try {
    const res = await tagApi.listByRoom(roomId.value)
    roomTags.value = res.data || []
  } catch (e) { roomTags.value = [] }
}

const refreshRoom = async () => {
  await getRoomInfo()
  await Promise.all([getRecommendRooms(), loadRoomTags(), checkModerator()])
  if (isLogin.value) {
    await Promise.all([getRoomExtraInfo(), saveHistory()])
  } else {
    roomExtraInfo.value = {}
  }
}

const subscribeGuardian = async () => {
  guardianLoading.value = true
  try {
    await guardianApi.subscribe({
      targetUserId: roomInfo.value.userId,
      level: guardianLevel.value,
      autoRenew: guardianAutoRenew.value
    })
    $modal.msgSuccess('守护开通成功')
    showGuardianModal.value = false
  } catch (e) {
    $modal.msgError(e?.message || '开通失败，请确认余额充足')
  } finally {
    guardianLoading.value = false
  }
}

const handleReport = () => { showReportModal.value = true }

const submitReport = async () => {
  if (!reportReason.value) { $modal.msgWarning('请选择举报原因'); return }
  reportLoading.value = true
  try {
    await reportApi.submit({
      targetUserId: roomInfo.value.userId,
      roomId: roomId.value,
      targetType: 'room',
      targetId: String(roomId.value),
      reason: reportReason.value,
      description: reportDesc.value
    })
    $modal.msgSuccess('举报已提交')
    showReportModal.value = false
    reportDesc.value = ''
  } catch (e) {
    $modal.msgError('提交失败')
  } finally {
    reportLoading.value = false
  }
}

const handleFollowBtnClick = async () => {
  if (!isLogin.value) {
    goLogin()
    return
  }
  try {
    if (roomExtraInfo.value.follow) {
      await watchApi.unFollow({ roomId: roomId.value })
      $modal.msgSuccess("已取消关注")
    } else {
      await watchApi.follow({ roomId: roomId.value })
      $modal.msgSuccess("关注成功")
    }
    await getRoomExtraInfo()
  } catch (error) {
    $modal.msgError("操作失败，请稍后重试")
  }
}

const saveHistory = () => watchApi.saveHistory({ roomId: roomId.value }).catch(() => {})

// 播放器工具栏事件
const onLineChange = (line) => {
  void line
}

const onQualityChange = (quality) => {
  void quality
}

const onVolumeChange = (vol) => {
  playChild.value?.setVolume?.(vol)
}

const onFullscreen = () => {
  const playerEl = document.querySelector(".player-box")
  if (playerEl) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      playerEl.requestFullscreen()
    }
  }
}

const initSvga = () => {
  svgaPlayer.value = new SVGA.Player("#svga-wrap")
  svgaParser.value = new SVGA.Parser("#svga-wrap")
}

const playSvga = (url) => {
  if (!svgaPlayer.value || !svgaParser.value) return
  svgaPlayer.value.clearAfterStop = true
  svgaPlayer.value.stopAnimation(true)
  svgaParser.value.load(url, (videoItem) => {
    svgaPlayer.value.loops = 1
    svgaPlayer.value.setVideoItem(videoItem)
    svgaPlayer.value.startAnimation()
  })
}

const getRoomInfo = async () => {
  try {
    const res = await roomApi.getRoomInfo({ roomId: roomId.value })
    roomInfo.value = res.data || {}
  } catch (error) {
    roomInfo.value = {}
  }
}

const getRoomExtraInfo = async () => {
  try {
    const res = await roomApi.getRoomExtraInfo({ roomId: roomId.value })
    roomExtraInfo.value = res.data || {}
  } catch (error) {
    roomExtraInfo.value = {}
  }
}

const getRecommendRooms = async () => {
  try {
    const res = await recommendApi.getSimilarRooms(roomId.value, 4)
    const data = res?.data
    recommendRooms.value = Array.isArray(data) ? data : (data?.list || [])
  } catch (error) {
    try {
      const res = await liveApi.listLivingRooms({})
      const categoryId = roomInfo.value?.categoryInfo?.id || roomInfo.value?.categoryId
      recommendRooms.value = (res?.data?.list || [])
        .filter((item) => Number(item.id) !== roomId.value)
        .sort((a, b) => {
          const aSame = (a.categoryInfo?.id || a.categoryId) === categoryId ? 1 : 0
          const bSame = (b.categoryInfo?.id || b.categoryId) === categoryId ? 1 : 0
          return bSame - aSame || Number(b.popularity || 0) - Number(a.popularity || 0)
        })
        .slice(0, 4)
    } catch (fallbackError) {
      recommendRooms.value = []
    }
  }
}

const extractGiftName = (text = "") => {
  const normalized = String(text || "").trim()
  const sentMatch = normalized.match(/送出了?\s*(.+?)(?:\s*x\s*\d+|\s*\*\s*\d+|$)/)
  if (sentMatch?.[1]) return sentMatch[1].trim()
  const rewardMatch = normalized.match(/赠送了\s*(.+?)(?:\s*x\s*\d+|\s*\*\s*\d+|$)/)
  return rewardMatch?.[1]?.trim() || ""
}

const extractSenderName = (text = "") => {
  const normalized = String(text || "").trim()
  const match = normalized.match(/^(.+?)(?:\s*送出了?|\s*赠送了)/)
  return match?.[1]?.trim() || ""
}

const resolveGiftPayload = (payload, fallbackSender = "") => {
  if (payload && typeof payload === "object") {
    const text = payload.text || payload.content || ""
    return {
      giftName: payload.giftName || extractGiftName(text) || "小心心",
      senderName: payload.senderName || payload.nickname || extractSenderName(text) || fallbackSender || "",
    }
  }
  return {
    giftName: payload || "小心心",
    senderName: fallbackSender || "",
  }
}

const handleSendGift = (payload, senderName) => {
  const gift = resolveGiftPayload(payload, senderName)
  giftEffectsRef.value?.playGiftEffect(gift.giftName, gift.senderName)
  playSvga("svga/angel.svga")
}

const copyRoomLink = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    $modal.msgSuccess("直播间链接已复制")
  } catch (error) {
    $modal.msgWarning("复制失败，请手动复制浏览器地址")
  }
}

const handleSendWelcome = (msg) => {
  navigator.clipboard.writeText(msg)
  $modal.msgSuccess("欢迎语已复制，请在弹幕框粘贴发送")
}

const goLogin = () => {
  router.push("/login")
}
</script>

<style scoped lang="scss">
.room-page {
  max-width: 1500px;
  margin: 0 auto;
  padding: 14px 20px 42px;
}

.room-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 14px;
  align-items: start;
}

.player-column {
  min-width: 0;
}

.room-head,
.watch-stage,
.gift-section,
.detail-section,
.chat-column,
.recommend-section {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.room-head {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 78px;
  padding: 12px 14px;
  border-color: color-mix(in srgb, var(--accent) 18%, var(--border));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 10%, transparent), transparent 44%),
    var(--bg-card);
}

.anchor-avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.anchor-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--bg-secondary);
  box-shadow: 0 0 0 3px var(--accent-light);
}

.anchor-live-dot {
  position: absolute;
  right: 0;
  bottom: 2px;
  width: 13px;
  height: 13px;
  border: 2px solid var(--bg-card);
  border-radius: 50%;
  background: var(--danger);
}

.head-copy {
  flex: 1;
  min-width: 0;
}

.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
}

.head-meta span {
  height: 21px;
  padding: 0 7px;
  border-radius: 4px;
  color: var(--accent);
  background: var(--accent-light);
  font-weight: 700;
  line-height: 22px;
}

.head-copy h1 {
  margin: 7px 0 4px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 21px;
  font-weight: 900;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.anchor-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.anchor-line strong {
  flex: 0 0 auto;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}

.anchor-line span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-stats {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  padding: 0 4px;
}

.head-stats div {
  min-width: 58px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--bg-card) 80%, var(--bg-secondary));
  text-align: center;
}

.head-stats strong,
.head-stats span {
  display: block;
}

.head-stats strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 900;
}

.head-stats span {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 11px;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.watch-stage {
  margin-top: 12px;
  overflow: hidden;
  border-color: #11141b;
  background: #080a0f;
  box-shadow: 0 18px 46px rgba(5, 6, 9, 0.18);
}

.player-box {
  position: relative;
  min-height: 552px;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: #050609;
  box-shadow: none;
}

.player-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: #e5e7eb;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 14%, transparent), transparent 34%),
    #050609;
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

.watch-underbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.72);
  background: #11141b;
}

.watch-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  min-width: 0;
}

.watch-tags span {
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 18%, transparent);
  font-size: 12px;
  font-weight: 800;
  line-height: 24px;
}

.watch-underbar p {
  margin: 0;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-section,
.detail-section,
.recommend-section {
  margin-top: 12px;
  padding: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.section-head span,
.detail-section p {
  color: var(--text-muted);
}

.detail-section p {
  margin: 0;
  line-height: 1.8;
}

.chat-column {
  position: sticky;
  top: 74px;
  overflow: hidden;
  border-color: var(--border);
  background: var(--bg-card);
  box-shadow: var(--shadow-hover);
}

.chat-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  padding: 8px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
}

.chat-tabs button {
  height: 34px;
  border: 0;
  border-radius: 6px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.chat-tabs button.active {
  color: var(--accent);
  background: var(--accent-light);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1180px) {
  .room-shell {
    grid-template-columns: 1fr;
  }

  .chat-column {
    position: static;
  }

  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .room-page {
    padding: 12px 14px 30px;
  }

  .room-head,
  .watch-underbar,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-stats {
    display: none;
  }

  .player-box {
    min-height: 360px;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
