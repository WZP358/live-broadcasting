<template>
  <div class="room-page">
    <section class="room-shell">
      <main class="player-column">
        <div class="room-head">
          <div class="anchor-avatar-wrap">
            <img class="anchor-avatar" :src="safeAnchorAvatar" alt="" @error="onImgError" />
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
              <strong>{{ formatCount(roomExtraInfo.followCount || 0) }}</strong>
              <span>关注</span>
            </div>
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
              v-if="!isOwnRoom"
              type="primary"
              :ghost="roomExtraInfo.follow"
              :loading="followLoading"
              @click="handleFollowBtnClick"
            >
              {{ roomExtraInfo.follow ? "已关注" : "关注" }}
            </a-button>
            <a-button v-if="isLogin && !isOwnRoom" @click="showGuardianModal = true">开通守护</a-button>
            <a-button @click="copyRoomLink">分享</a-button>
            <a-button v-if="isLogin && !isOwnRoom" type="text" danger @click="handleReportRoom">举报</a-button>
          </div>
        </div>

        <section class="watch-stage">
          <div class="player-box">
            <Player
              v-if="hasEnteredRoom && roomInfo.status === 1 && (roomInfo.browserLive || roomInfo.pullUrl)"
              ref="playChild"
              :room-id="roomId"
              :pull-url="roomInfo.pullUrl"
              :browser-live="Boolean(roomInfo.browserLive)"
              :cohost-enabled="isLogin && !isOwnRoom && hasEnteredRoom"
              :applicant-name="viewerName"
              :applicant-avatar="viewerAvatar"
              @require-login="goLogin"
            />
            <div
              v-else-if="roomInfo.status === 1"
              class="room-entry-preview"
              :style="{ backgroundImage: `url(${safeRoomCover})` }"
            >
              <div class="preview-scoreboard">
                <div class="preview-team">
                  <strong>{{ roomInfo.categoryInfo?.name || "推荐直播" }}</strong>
                  <span>{{ formatCount(roomInfo.popularity || 0) }} 热度</span>
                </div>
                <div class="preview-score">
                  <b>20</b>
                  <span>:</span>
                  <b>30</b>
                </div>
                <div class="preview-team preview-team--right">
                  <strong>{{ anchorName }}</strong>
                  <span>{{ roomInfo.browserLive ? "蓝光观看" : "高清观看" }}</span>
                </div>
              </div>

              <div class="preview-rank-panel">
                <span>直播数据</span>
                <strong>{{ formatCount(roomInfo.popularity || 0) }}</strong>
                <i></i>
                <i></i>
                <i></i>
              </div>

              <button class="entry-main-btn" type="button" @click="enterRoom">
                进入直播间
              </button>

              <div class="preview-mini-card">
                <img :src="safeAnchorAvatar" alt="" @error="onImgError" />
                <div>
                  <strong>{{ anchorName }}</strong>
                  <span>{{ roomInfo.title || "精彩直播" }}</span>
                </div>
              </div>

              <div class="preview-control-bar">
                <div class="preview-left-controls">
                  <button type="button">Ⅱ</button>
                  <button type="button">↻</button>
                  <span>{{ roomInfo.title || "直播间" }}</span>
                </div>
                <div class="preview-right-controls">
                  <span>弹幕</span>
                  <span class="volume-track"><i></i></span>
                  <button type="button" @click="enterRoom">进入直播间 ›</button>
                </div>
              </div>
            </div>
            <div v-else class="player-empty">
              <strong>{{ roomInfo.status === 1 ? "直播画面准备中" : "主播暂未开播" }}</strong>
              <span>{{ roomInfo.status === 1 ? "稍等片刻，直播画面马上出现。" : "可以先关注主播，开播后再回来观看。" }}</span>
            </div>
            <DanmakuOverlay ref="danmakuOverlayRef" :enabled="danmakuEnabled" />
            <div id="svga-wrap"></div>
          </div>

          <PlayerToolbar
            :is-live="roomInfo.status === 1"
            :viewer-count="roomInfo.popularity || 0"
            :danmaku-enabled="danmakuEnabled"
            @line-change="onLineChange"
            @quality-change="onQualityChange"
            @volume-change="onVolumeChange"
            @danmaku-toggle="onDanmakuToggle"
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
          <GiftList
            :room-id="roomId"
            :disabled="roomInfo.status !== 1 || isOwnRoom"
            :disabled-reason="isOwnRoom ? '不能给自己的直播间送礼' : roomInfo.status === 1 ? '' : '主播暂未开播，无法送礼'"
            @require-login="goLogin"
            @gift-sent="handleLocalGiftSent"
          />
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
        <ChatList
          v-show="chatMode === 'chat'"
          :room-id="roomId"
          :is-moderator="isModerator"
          @sendGift="handleSendGift"
          @reportMessage="openMessageReport"
          @messagesChange="handleChatMessagesChange"
        />
        <AgentPanel
          v-show="chatMode === 'agent'"
          :chat-messages="chatMessages"
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
    <a-modal v-model:open="showReportModal" :title="reportDialogTitle" :footer="null" width="520px">
      <a-form layout="vertical" class="report-form">
        <a-form-item label="举报原因">
          <a-select v-model:value="reportReason" placeholder="选择举报原因">
            <a-select-option value="违规内容">违规内容</a-select-option>
            <a-select-option value="色情低俗">色情低俗</a-select-option>
            <a-select-option value="欺诈诈骗">欺诈诈骗</a-select-option>
            <a-select-option value="侵权投诉">侵权投诉</a-select-option>
            <a-select-option value="暴力血腥">暴力血腥</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="reportTargetSummary" label="举报对象">
          <a-input :value="reportTargetSummary" disabled />
        </a-form-item>
        <a-form-item label="详细描述">
          <a-textarea v-model:value="reportDesc" placeholder="请描述具体情况（选填）" :maxlength="200" :rows="3" />
        </a-form-item>
        <a-alert v-if="reportTargetType === 'message'" type="warning" show-icon message="该弹幕将提交给管理员审核，不会立即影响当前直播。" />
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
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import $modal from "@/utils/message"
import Player from "./Player.vue"
import DanmakuOverlay from "./DanmakuOverlay.vue"
import PlayerToolbar from "@/components/live/PlayerToolbar.vue"
import ChatList from "./ChatList.vue"
import GiftList from "./GiftList.vue"
import LiveRoom from "@/components/LiveRoom.vue"
import AgentPanel from "@/components/live/AgentPanel.vue"
import roomApi from "@/api/room"
import watchApi from "@/api/watch"
import liveApi from "@/api/live"
import tagApi from "@/api/tag"
import guardianApi from "@/api/guardian"
import recommendApi from "@/api/recommend"
import reportApi from "@/api/report"
import moderatorApi from "@/api/moderator"

const GiftEffects = defineAsyncComponent(() => import("@/components/live/GiftEffects.vue"))
let svgaModulePromise = null

const router = useRouter()
const store = useStore()
const roomId = computed(() => Number(router.currentRoute.value.params.id))
const isLogin = computed(() => store.user().isLogin)

const playChild = ref()
const roomInfo = ref({})
const roomExtraInfo = ref({})
const roomTags = ref([])
const recommendRooms = ref([])
const myUserId = computed(() => store.user().userInfo?.userId || 0)
const isOwnRoom = computed(() => Boolean(myUserId.value && roomInfo.value?.userId === myUserId.value))
const followLoading = ref(false)

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
const reportTargetType = ref("room")
const reportTargetId = ref("")
const reportTargetUserId = ref(0)
const reportTargetSummary = ref("")
const isModerator = ref(false)

const checkModerator = async () => {
  try {
    const res = await moderatorApi.check(roomId.value)
    isModerator.value = res?.data === true
  } catch (e) { isModerator.value = false }
}
const svgaPlayer = ref(null)
const svgaParser = ref(null)
import { FALLBACK_AVATAR, FALLBACK_COVER, onImgError, resolveSafeImageUrl } from "@/utils/fallback"
const fallbackAvatar = FALLBACK_AVATAR
const fallbackCover = FALLBACK_COVER

const chatMode = ref("chat")
const hasEnteredRoom = ref(false)
const chatMessages = ref([])
const danmakuEnabled = ref(true)
const danmakuOverlayRef = ref(null)

const anchorName = computed(() => roomInfo.value?.userInfo?.name || roomInfo.value?.userInfo?.nickName || "主播")
const safeAnchorAvatar = computed(() => resolveSafeImageUrl(roomInfo.value?.userInfo?.avatar, FALLBACK_AVATAR))
const safeRoomCover = computed(() => resolveSafeImageUrl(roomInfo.value?.cover, FALLBACK_COVER))
const viewerName = computed(() => {
  const info = store.user().userInfo || {}
  return info.nickName || info.nickname || info.name || info.username || "观众"
})
const viewerAvatar = computed(() => resolveSafeImageUrl(store.user().userInfo?.avatar, ""))
const reportDialogTitle = computed(() => (reportTargetType.value === "message" ? "举报弹幕" : "举报直播间"))

const giftEffectsRef = ref(null)
const recentGiftEffects = new Map()
let danmakuSeenMessages = new WeakSet()

const formatCount = (value) => {
  const count = Number(value || 0)
  if (!Number.isFinite(count) || count <= 0) return "0"
  if (count >= 100000000) return `${(count / 100000000).toFixed(1).replace(/\.0$/, "")}亿`
  if (count >= 10000) return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`
  return `${count}`
}

onMounted(async () => {
  await refreshRoom()
})

onBeforeUnmount(() => {
  svgaPlayer.value?.stopAnimation?.(true)
})

watch(roomId, async () => {
  hasEnteredRoom.value = false
  chatMessages.value = []
  danmakuSeenMessages = new WeakSet()
  danmakuOverlayRef.value?.clear?.()
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
    await getRoomExtraInfo()
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

const handleReportRoom = () => {
  reportTargetType.value = "room"
  reportTargetId.value = String(roomId.value)
  reportTargetUserId.value = roomInfo.value.userId || 0
  reportTargetSummary.value = `${roomInfo.value.title || "直播间"} · ${anchorName.value}`
  reportReason.value = "违规内容"
  reportDesc.value = ""
  showReportModal.value = true
}

const openMessageReport = (message) => {
  reportTargetType.value = "message"
  reportTargetId.value = String(message?.id || `${roomId.value}-${Date.now()}`)
  reportTargetUserId.value = message?.fromUserId || 0
  reportTargetSummary.value = `${message?.nickname || "观众"}：${message?.text || "弹幕内容"}`
  reportReason.value = "违规内容"
  reportDesc.value = message?.text ? `弹幕内容：${message.text}` : ""
  showReportModal.value = true
}

const submitReport = async () => {
  if (!reportReason.value) { $modal.msgWarning('请选择举报原因'); return }
  reportLoading.value = true
  try {
    const evidence = {
      type: reportTargetType.value,
      summary: reportTargetSummary.value,
      description: reportDesc.value,
      roomTitle: roomInfo.value.title || "",
      anchorName: anchorName.value,
      submittedAt: new Date().toLocaleString(),
    }
    await reportApi.submit({
      targetUserId: reportTargetUserId.value || roomInfo.value.userId,
      roomId: roomId.value,
      targetType: reportTargetType.value,
      targetId: reportTargetId.value || String(roomId.value),
      reason: reportReason.value,
      description: JSON.stringify(evidence)
    })
    $modal.msgSuccess('举报已提交')
    showReportModal.value = false
    reportDesc.value = ''
    reportTargetSummary.value = ''
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
  if (isOwnRoom.value || followLoading.value) {
    return
  }
  followLoading.value = true
  try {
    if (roomExtraInfo.value.follow) {
      const res = await watchApi.unFollow({ roomId: roomId.value })
      if (res?.data) {
        $modal.msgSuccess("已取消关注")
      } else {
        $modal.msgWarning("当前没有关注该直播间")
      }
    } else {
      const res = await watchApi.follow({ roomId: roomId.value })
      if (res?.data) {
        $modal.msgSuccess("关注成功")
      } else {
        $modal.msgWarning("暂时无法关注该直播间")
      }
    }
    await getRoomExtraInfo()
  } catch (error) {
    $modal.msgError(error?.message || "操作失败，请稍后重试")
  } finally {
    followLoading.value = false
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

const onDanmakuToggle = (enabled) => {
  danmakuEnabled.value = enabled
  if (!enabled) {
    danmakuOverlayRef.value?.clear?.()
  }
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

const loadSvga = () => {
  if (!svgaModulePromise) {
    svgaModulePromise = import("svgaplayerweb").then((module) => module.default || module)
  }
  return svgaModulePromise
}

const initSvga = async () => {
  if (svgaPlayer.value && svgaParser.value) {
    return true
  }
  const wrap = document.querySelector("#svga-wrap")
  if (!wrap) {
    return false
  }
  const SVGA = await loadSvga()
  svgaPlayer.value = new SVGA.Player("#svga-wrap")
  svgaParser.value = new SVGA.Parser("#svga-wrap")
  return true
}

const playSvga = async (url) => {
  const ready = await initSvga()
  if (!ready || !svgaPlayer.value || !svgaParser.value) return
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
    $modal.msgError(error?.message || "直播间加载失败，请稍后重试")
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

const extractGiftCount = (text = "") => {
  const normalized = String(text || "").trim()
  const sentMatch = normalized.match(/(?:送出了?|赠送了)\s*.+?(?:\s*x\s*(\d+)|\s*\*\s*(\d+)|\s+(\d+)\s*个?|$)/)
  const matched = sentMatch?.slice(1).find(Boolean)
  return Number.parseInt(matched || "1", 10) || 1
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
      count: Number(payload.count || payload.number || extractGiftCount(text) || 1),
      giftId: payload.giftId || payload.presentId || payload.id || 0,
      senderId: payload.senderId || payload.fromUserId || 0,
      text,
      source: payload.source || "ws",
    }
  }
  return {
    giftName: payload || "小心心",
    senderName: fallbackSender || "",
    count: 1,
    giftId: 0,
    senderId: 0,
    text: "",
    source: "ws",
  }
}

const getGiftEffectSignature = (gift) => {
  return [
    gift.giftId || gift.giftName || "",
    gift.giftName || "",
    gift.senderId || gift.senderName || "",
    gift.count || 1,
  ].join("|")
}

const rememberGiftEffect = (gift, ttl = 2600) => {
  const signature = getGiftEffectSignature(gift)
  recentGiftEffects.set(signature, Date.now() + ttl)
  return signature
}

const consumeGiftEffect = (gift) => {
  const signature = getGiftEffectSignature(gift)
  const expiry = recentGiftEffects.get(signature)
  if (!expiry) return false
  if (Date.now() > expiry) {
    recentGiftEffects.delete(signature)
    return false
  }
  recentGiftEffects.delete(signature)
  return true
}

const handleSendGift = (payload, senderName, source = "ws") => {
  const gift = resolveGiftPayload(
    typeof payload === "object" ? { ...payload, senderName: payload.senderName || senderName, source } : payload,
    senderName
  )

  if (source !== "local" && consumeGiftEffect(gift)) {
    return
  }

  if (source === "local") {
    rememberGiftEffect(gift)
  }

  giftEffectsRef.value?.playGiftEffect(gift.giftName, gift.senderName, {
    count: gift.count,
    giftId: gift.giftId,
    text: gift.text,
  })
  void playSvga("svga/angel.svga")
}

const handleLocalGiftSent = (payload) => {
  handleSendGift(payload, payload?.senderName, "local")
}

const handleChatMessagesChange = (messages) => {
  const nextMessages = messages || []
  const incomingMessages = nextMessages.filter((item) => {
    if (!item || item.isSystem || item.isGift || item.isEnter) return false
    if (danmakuSeenMessages.has(item)) return false
    danmakuSeenMessages.add(item)
    return true
  })
  chatMessages.value = nextMessages
  incomingMessages.forEach((message) => {
    danmakuOverlayRef.value?.push?.({
      ...message,
      isSelf: Boolean(message.isSelf || (message.fromUserId && message.fromUserId === myUserId.value)),
    })
  })
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

const enterRoom = () => {
  hasEnteredRoom.value = true
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

.player-box:fullscreen {
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  background: #050609;
}

.player-box:fullscreen :deep(.player-shell),
.player-box:fullscreen :deep(#videoElement) {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.player-box:fullscreen :deep(#videoElement) {
  object-fit: contain;
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

.room-entry-preview {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background-color: #11141b;
  background-position: center;
  background-size: cover;
}

.room-entry-preview::before {
  position: absolute;
  inset: 0;
  content: "";
  background:
    linear-gradient(180deg, rgba(5, 6, 9, 0.28), rgba(5, 6, 9, 0.06) 34%, rgba(5, 6, 9, 0.72)),
    radial-gradient(circle at 50% 48%, rgba(255, 122, 0, 0.1), transparent 32%),
    rgba(5, 6, 9, 0.22);
}

.room-entry-preview::after {
  position: absolute;
  inset: 0;
  content: "";
  background:
    linear-gradient(90deg, rgba(5, 6, 9, 0.28), transparent 24%, transparent 72%, rgba(5, 6, 9, 0.36)),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.035) 0 1px, transparent 1px 3px);
  pointer-events: none;
}

.preview-scoreboard {
  position: absolute;
  top: 0;
  left: 16%;
  right: 16%;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px minmax(0, 1fr);
  align-items: center;
  min-height: 54px;
  color: #e5e7eb;
  background: linear-gradient(180deg, rgba(15, 18, 26, 0.9), rgba(15, 18, 26, 0.58));
  clip-path: polygon(0 0, 100% 0, 96% 100%, 4% 100%);
}

.preview-team {
  min-width: 0;
  padding: 8px 28px;
}

.preview-team--right {
  text-align: right;
}

.preview-team strong,
.preview-team span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-team strong {
  font-size: 13px;
  font-weight: 900;
}

.preview-team span {
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 11px;
}

.preview-score {
  display: flex;
  justify-content: center;
  gap: 16px;
  color: #fff;
  font-size: 20px;
  font-weight: 900;
}

.preview-score span {
  color: #ffb020;
}

.preview-rank-panel {
  position: absolute;
  top: 72px;
  left: 16px;
  z-index: 2;
  width: 230px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
  background: rgba(5, 6, 9, 0.46);
  backdrop-filter: blur(8px);
}

.preview-rank-panel span {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.preview-rank-panel strong {
  display: block;
  margin-bottom: 10px;
  color: #ffb020;
  font-size: 26px;
  font-weight: 900;
}

.preview-rank-panel i {
  display: block;
  height: 8px;
  margin-top: 8px;
  border-radius: 8px;
  background: linear-gradient(90deg, #ff7a00, rgba(255, 122, 0, 0.12));
}

.preview-rank-panel i:nth-child(4) {
  width: 74%;
  background: linear-gradient(90deg, #22c55e, rgba(34, 197, 94, 0.12));
}

.preview-rank-panel i:nth-child(5) {
  width: 58%;
}

.entry-main-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 4;
  min-width: 300px;
  height: 84px;
  border: 1px solid rgba(255, 153, 0, 0.92);
  border-radius: 42px;
  color: #ff9900;
  background: rgba(5, 6, 9, 0.66);
  box-shadow:
    0 0 0 1px rgba(255, 153, 0, 0.2) inset,
    0 16px 44px rgba(0, 0, 0, 0.38);
  font-size: 28px;
  font-weight: 900;
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition:
    background 0.18s ease,
    transform 0.18s ease,
    color 0.18s ease;
}

.entry-main-btn:hover {
  color: #11141b;
  background: #ff9900;
  transform: translate(-50%, -50%) scale(1.03);
}

.preview-mini-card {
  position: absolute;
  right: 22px;
  bottom: 86px;
  z-index: 3;
  display: grid;
  grid-template-columns: 52px minmax(0, 170px);
  gap: 10px;
  align-items: center;
  padding: 9px;
  color: #fff;
  background: rgba(5, 6, 9, 0.58);
}

.preview-mini-card img {
  width: 52px;
  height: 52px;
  object-fit: cover;
}

.preview-mini-card strong,
.preview-mini-card span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-mini-card span {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
}

.preview-control-bar {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 72px;
  padding: 0 18px;
  color: rgba(255, 255, 255, 0.82);
  background: linear-gradient(180deg, rgba(5, 6, 9, 0.18), rgba(5, 6, 9, 0.86));
}

.preview-left-controls,
.preview-right-controls {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 14px;
}

.preview-left-controls button,
.preview-right-controls button {
  border: 0;
  color: #fff;
  background: transparent;
  font-weight: 900;
  cursor: pointer;
}

.preview-left-controls button {
  width: 34px;
  height: 34px;
  font-size: 22px;
}

.preview-left-controls span {
  overflow: hidden;
  max-width: 520px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.volume-track {
  position: relative;
  width: 134px;
  height: 5px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.24);
}

.volume-track i {
  position: absolute;
  inset: 0 auto 0 0;
  width: 68%;
  border-radius: inherit;
  background: #ff9900;
}

.preview-right-controls button {
  height: 46px;
  padding: 0 26px;
  border-radius: 23px;
  color: #fff;
  background: #ff9900;
  font-size: 16px;
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

.report-form :deep(.ant-form-item-label > label) {
  color: var(--text-secondary);
  font-weight: 700;
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

  .preview-scoreboard,
  .preview-rank-panel,
  .preview-mini-card {
    display: none;
  }

  .entry-main-btn {
    min-width: 220px;
    height: 62px;
    font-size: 22px;
  }

  .preview-control-bar {
    height: 58px;
  }

  .preview-left-controls span,
  .volume-track,
  .preview-right-controls span {
    display: none;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

}
</style>
