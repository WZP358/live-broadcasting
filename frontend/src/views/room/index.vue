<template>
  <div class="room-page">
    <section class="room-shell">
      <main class="player-column">
        <div class="room-head">
          <img class="anchor-avatar" :src="roomInfo.userInfo?.avatar || fallbackAvatar" alt="" />
          <div class="head-copy">
            <div class="head-meta">
              <span>{{ roomInfo.categoryInfo?.name || "推荐" }}</span>
              <span>{{ roomInfo.status === 1 ? "直播中" : "未开播" }}</span>
              <span>{{ roomInfo.browserLive ? "低延迟" : "标准线路" }}</span>
            </div>
            <h1>{{ roomInfo.title || "直播间" }}</h1>
            <p>{{ roomInfo.notice || roomInfo.introduce || "欢迎来到直播间，文明互动，理性消费。" }}</p>
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

        <div class="player-box">
          <Player
            v-if="roomInfo.status === 1 && (roomInfo.browserLive || roomInfo.pullUrl)"
            ref="playChild"
            :room-id="roomId"
            :pull-url="roomInfo.pullUrl"
            :browser-live="Boolean(roomInfo.browserLive)"
          />
          <div v-else class="player-empty">
            <strong>{{ roomInfo.status === 1 ? "直播流准备中" : "主播暂未开播" }}</strong>
            <span>{{ roomInfo.status === 1 ? "稍等片刻，播放器会在拉到流后显示画面。" : "可以先关注主播，开播后再回来观看。" }}</span>
          </div>
          <div id="svga-wrap"></div>
        </div>

        <div class="player-bar">
          <div class="bar-tags">
            <span>{{ roomInfo.categoryInfo?.name || "未分类" }}</span>
            <span>{{ roomInfo.pullUrl ? "有回退播放源" : "等待播放源" }}</span>
            <a-tag v-for="t in roomTags" :key="t.id" size="small" color="blue">{{ t.tagName }}</a-tag>
            <span>{{ isLogin ? "已登录" : "游客观看" }}</span>
          </div>
          <div class="bar-note">弹幕、送礼、关注会写入用户行为；游客只保留观看能力。</div>
        </div>

        <section class="gift-section">
          <div class="section-head">
            <h2>礼物</h2>
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
        <ChatList :room-id="roomId" :is-moderator="isModerator" @sendGift="handleSendGift" />
      </aside>
    </section>

    <section class="recommend-section" v-if="recommendRooms.length">
      <div class="section-head">
        <h2>相关推荐</h2>
        <span>同分区和热门直播优先展示</span>
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import $modal from "@/utils/message"
import SVGA from "svgaplayerweb"
import Player from "./Player.vue"
import ChatList from "./ChatList.vue"
import GiftList from "./GiftList.vue"
import LiveRoom from "@/components/LiveRoom.vue"
import roomApi from "@/api/room"
import watchApi from "@/api/watch"
import liveApi from "@/api/live"
import tagApi from "@/api/tag"
import guardianApi from "@/api/guardian"
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
const fallbackAvatar = "https://dummyimage.com/96x96/f3f4f6/9ca3af&text=主播"

const anchorName = computed(() => roomInfo.value?.userInfo?.name || roomInfo.value?.userInfo?.nickName || "主播")

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
    $modal.msgError('开通失败，请确认余额充足')
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

const saveHistory = () => watchApi.saveHistory({ roomId: roomId.value })

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
  } catch (error) {
    recommendRooms.value = []
  }
}

const handleSendGift = () => {
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

const goLogin = () => {
  router.push("/login")
}
</script>

<style scoped lang="scss">
.room-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 18px 24px 42px;
}

.room-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.room-head,
.player-box,
.player-bar,
.gift-section,
.detail-section,
.chat-column,
.recommend-section {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.room-head {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px;
}

.anchor-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  object-fit: cover;
}

.head-copy {
  flex: 1;
  min-width: 0;
}

.head-meta {
  display: flex;
  gap: 8px;
  color: #d96c00;
  font-size: 12px;
}

.head-copy h1 {
  margin: 8px 0 4px;
  color: #1f2937;
  font-size: 22px;
  line-height: 1.3;
}

.head-copy p {
  margin: 0;
  overflow: hidden;
  color: #909399;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.player-box {
  position: relative;
  margin-top: 12px;
  min-height: 620px;
  overflow: hidden;
  background: #050505;
}

.player-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: #e5e7eb;
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

.player-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 12px 16px;
}

.bar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bar-tags span {
  padding: 5px 10px;
  border-radius: 4px;
  background: #fff7ed;
  color: #d96c00;
  font-size: 12px;
}

.bar-note {
  color: #909399;
  font-size: 12px;
}

.gift-section,
.detail-section,
.recommend-section {
  margin-top: 12px;
  padding: 16px;
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
  color: #1f2937;
  font-size: 18px;
}

.section-head span,
.detail-section p {
  color: #909399;
}

.detail-section p {
  margin: 0;
  line-height: 1.8;
}

.chat-column {
  position: sticky;
  top: 94px;
  overflow: hidden;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
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
  .player-bar,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .player-box {
    min-height: 360px;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
