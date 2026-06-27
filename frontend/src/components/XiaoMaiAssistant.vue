<template>
  <div
    class="xiao-mai-assistant"
    :class="{ 'is-open': open, 'is-dragging': dragging }"
    :style="assistantStyle"
  >
    <button
      v-if="!open"
      class="assistant-fab"
      type="button"
      @click="handleFabClick"
      @pointerdown="startDrag"
    >
      <RobotOutlined />
      <span>小脉</span>
    </button>

    <transition name="assistant-panel">
      <div v-if="open" class="assistant-panel">
        <header class="assistant-header" @pointerdown="startDrag">
          <div class="assistant-title">
            <strong>小脉 AI 助手</strong>
            <span :class="['assistant-status', statusClass]">{{ statusText }}</span>
          </div>
          <button class="assistant-close" type="button" @click="open = false">
            <CloseOutlined />
          </button>
        </header>

        <div class="assistant-body">
          <div v-if="showQuickQuestions" class="assistant-quick">
            <button v-for="item in quickQuestions" :key="item.question" type="button" @click="askQuick(item.question)">
              {{ item.label }}
            </button>
          </div>

          <div class="assistant-chat" ref="chatRef">
            <article v-for="(msg, index) in messages" :key="index" :class="['chat-line', msg.role]">
              <img v-if="msg.role === 'assistant'" class="chat-avatar chat-avatar--image" :src="xiaomaiAvatar" alt="" />
              <div v-else class="chat-avatar chat-avatar--user">{{ userAvatarText }}</div>
              <div class="chat-bubble">
                <p :class="{ collapsed: isLongMessage(msg) && !expandedMessages[index] }">{{ msg.content }}</p>
                <button v-if="isLongMessage(msg)" class="message-expand" type="button" @click="toggleMessageExpand(index)">
                  {{ expandedMessages[index] ? "收起" : "展开" }}
                </button>
              </div>
            </article>
          </div>
        </div>

        <footer class="assistant-footer">
          <a-input
            :key="inputResetKey"
            ref="inputRef"
            v-model:value="question"
            allow-clear
            :placeholder="inputPlaceholder"
            :disabled="busy"
            @press-enter="handlePressEnter"
          />
          <a-button type="primary" :loading="busy" @click="sendQuestion">
            <SendOutlined />
          </a-button>
        </footer>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue"
import { useRoute } from "vue-router"
import { CloseOutlined, RobotOutlined, SendOutlined } from "@ant-design/icons-vue"
import agentApi from "@/api/agent"
import liveApi from "@/api/live"
import roomApi from "@/api/room"
import $modal from "@/utils/message"
import { useStore } from "@/stores"
import xiaomaiAvatar from "@/assets/img/xiaomai-avatar.png"

const route = useRoute()
const store = useStore()
const userStore = store.user()
const CONTEXT_TTL = 15000
const open = ref(false)
const busy = ref(false)
const dragging = ref(false)
const dragMoved = ref(false)
const question = ref("")
const chatRef = ref(null)
const inputRef = ref(null)
const inputResetKey = ref(0)
const serviceReady = ref(null)
const position = ref({ right: 18, bottom: 18 })
const dragState = ref(null)
const quickDismissed = ref(false)
const expandedMessages = ref({})
const pageDetails = ref({
  path: "",
  fetchedAt: 0,
  room: null,
  studioRoom: null,
})
const messages = ref([
  { role: "assistant", content: "你好，我是小脉。我会结合你当前所在页面，帮你处理开播、观看、客服、数据和后台管理问题。" },
])

const quickQuestions = [
  { label: "当前页能做什么", question: "我当前页面有哪些功能，下一步建议做什么？" },
  { label: "AI 能做什么", question: "这个平台里的 AI 助手可以帮我做什么？" },
  { label: "运营动线", question: "帮我规划一条从看直播到后台运营的体验路线。" },
]

const statusText = computed(() => {
  if (serviceReady.value === false) return "AI 服务未启动"
  if (serviceReady.value === true) return "在线"
  return "待检查"
})

const statusClass = computed(() => {
  if (serviceReady.value === false) return "is-offline"
  if (serviceReady.value === true) return "is-online"
  return "is-idle"
})

const inputPlaceholder = computed(() => (serviceReady.value === false ? "AI 服务未启动" : "问点什么吧"))
const showQuickQuestions = computed(() => !quickDismissed.value && !messages.value.some((msg) => msg.role === "user"))
const userAvatarText = computed(() => "我")
const currentRoomId = computed(() => {
  const matched = route.path.match(/^\/room\/(\d+)/)
  return matched ? Number(matched[1]) : null
})
const isStudioPage = computed(() => /^\/live\/studio/.test(route.path) || /\/live-settings/.test(route.path))

const pageContext = computed(() => {
  const path = route.path
  const titleMap = [
    { test: /^\/home|^\/$/, title: "首页", actions: ["浏览直播", "搜索直播间", "进入直播间"] },
    { test: /^\/room\//, title: "直播间", actions: ["观看直播", "发送弹幕", "送礼", "关注主播", "举报内容"] },
    { test: /^\/live\/studio|\/live-settings/, title: "开播工作台", actions: ["选择开播模式", "开始直播", "开启降噪", "查看互动"] },
    { test: /^\/center\/messages\/customer-service/, title: "客服工单", actions: ["提交问题", "查看处理状态", "关闭工单"] },
    { test: /^\/center/, title: "个人中心", actions: ["管理资料", "查看关注历史", "查看钱包账单", "进入开播准备"] },
    { test: /^\/system\/customer-service/, title: "后台客服处理", actions: ["筛选工单", "回复用户", "更新处理状态"] },
    { test: /^\/system\/content-audit/, title: "内容审核后台", actions: ["查看举报", "裁决违规", "记录处理结果"] },
    { test: /^\/system/, title: "管理后台", actions: ["管理用户", "管理直播间", "查看统计", "维护配置"] },
    { test: /^\/search/, title: "搜索页", actions: ["搜索主播", "筛选直播间", "进入观看"] },
  ]
  const matched = titleMap.find((item) => item.test.test(path)) || {
    title: route.meta?.title || "平台页面",
    actions: ["查看页面信息", "按业务流程继续操作"],
  }
  return {
    path,
    title: matched.title,
    actions: matched.actions,
    loggedIn: userStore.isLogin,
    isAdmin: userStore.isAdmin,
    roomId: currentRoomId.value,
    room: pageDetails.value.room,
    studioRoom: pageDetails.value.studioRoom,
  }
})

const assistantStyle = computed(() => ({
  right: `${position.value.right}px`,
  bottom: `${position.value.bottom}px`,
}))

const clampPosition = (right, bottom) => {
  const margin = 12
  const maxRight = Math.max(margin, window.innerWidth - 72)
  const maxBottom = Math.max(margin, window.innerHeight - 72)
  return {
    right: Math.min(Math.max(right, margin), maxRight),
    bottom: Math.min(Math.max(bottom, margin), maxBottom),
  }
}

const toggleOpen = async () => {
  open.value = !open.value
  if (open.value && serviceReady.value === null) {
    await refreshPageDetails({ force: true })
    await checkHealth()
  }
}

const handleFabClick = async () => {
  if (dragMoved.value) {
    dragMoved.value = false
    return
  }
  await toggleOpen()
}

const startDrag = (event) => {
  if (event.button !== undefined && event.button !== 0) return
  const target = event.target
  if (target?.closest?.(".assistant-close, .assistant-footer, .assistant-quick, .assistant-chat")) return
  event.preventDefault()

  dragState.value = {
    startX: event.clientX,
    startY: event.clientY,
    startRight: position.value.right,
    startBottom: position.value.bottom,
    moved: false,
  }
  event.currentTarget?.setPointerCapture?.(event.pointerId)
  window.addEventListener("pointermove", handleDragMove)
  window.addEventListener("pointerup", stopDrag)
}

const handleDragMove = (event) => {
  const state = dragState.value
  if (!state) return
  const dx = event.clientX - state.startX
  const dy = event.clientY - state.startY
  if (Math.abs(dx) + Math.abs(dy) > 4) {
    state.moved = true
    dragMoved.value = true
    dragging.value = true
  }
  position.value = clampPosition(state.startRight - dx, state.startBottom - dy)
}

const stopDrag = () => {
  window.removeEventListener("pointermove", handleDragMove)
  window.removeEventListener("pointerup", stopDrag)
  dragging.value = false
  dragState.value = null
}

onBeforeUnmount(() => {
  window.removeEventListener("pointermove", handleDragMove)
  window.removeEventListener("pointerup", stopDrag)
})

watch(
  () => route.fullPath,
  () => {
    pageDetails.value = { path: "", fetchedAt: 0, room: null, studioRoom: null }
    if (open.value) {
      refreshPageDetails({ force: true })
    }
  }
)

const statusTextOfRoom = (status) => (Number(status) === 1 ? "直播中" : "未开播")

const getNameFromUser = (info = {}) =>
  info.name || info.nickName || info.nickname || info.username || info.account || "主播"

const normalizeRoomInfo = (room = {}) => ({
  id: room.id || currentRoomId.value || null,
  title: room.title || "直播间",
  anchorName: getNameFromUser(room.userInfo || {}),
  categoryName: room.categoryInfo?.name || room.categoryName || "未分类",
  status: room.status,
  statusText: statusTextOfRoom(room.status),
  notice: room.notice || "",
  introduce: room.introduce || "",
  popularity: room.popularity || 0,
  browserLive: Boolean(room.browserLive),
  hasPlayableStream: Boolean(room.browserLive || room.pullUrl),
})

const refreshPageDetails = async ({ force = false } = {}) => {
  const now = Date.now()
  if (!force && pageDetails.value.path === route.fullPath && now - pageDetails.value.fetchedAt < CONTEXT_TTL) {
    return
  }

  const nextDetails = {
    path: route.fullPath,
    fetchedAt: now,
    room: null,
    studioRoom: null,
  }

  if (currentRoomId.value) {
    try {
      const res = await roomApi.getRoomInfo({ roomId: currentRoomId.value }, { silentError: true })
      nextDetails.room = normalizeRoomInfo(res?.data || {})
    } catch (error) {
      nextDetails.room = {
        id: currentRoomId.value,
        title: "直播间",
        anchorName: "主播",
        categoryName: "未知",
        statusText: "未知",
        notice: "",
        introduce: "",
      }
    }
  }

  if (isStudioPage.value) {
    try {
      const res = await liveApi.getRoomSettingsInfo()
      nextDetails.studioRoom = normalizeRoomInfo(res?.data || {})
    } catch (error) {
      nextDetails.studioRoom = null
    }
  }

  pageDetails.value = nextDetails
}

const checkHealth = async () => {
  try {
    const res = await agentApi.healthCheck()
    const data = res?.data || {}
    const health = data.llm || data
    serviceReady.value = health?.status === "ok" || health?.status === "ready"
    if (serviceReady.value) {
      messages.value.push({ role: "assistant", content: `小脉已就位。我看到你当前在「${pageContext.value.title}」，可以直接问我下一步怎么操作。` })
    } else {
      messages.value.push({ role: "assistant", content: health?.message || data.message || "AI 服务已启动，但大模型暂时不可用。" })
    }
  } catch (error) {
    serviceReady.value = false
    messages.value.push({ role: "assistant", content: "AI 服务已启动，但大模型暂时不可用。" })
  } finally {
    scrollToBottom()
  }
}

const scrollToBottom = async () => {
  await nextTick()
  const el = chatRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

const askQuick = (text) => {
  question.value = text
  sendQuestion()
}

const handlePressEnter = (event) => {
  event?.preventDefault?.()
  sendQuestion()
}

const isLongMessage = (msg) => String(msg?.content || "").length > 80

const toggleMessageExpand = (index) => {
  expandedMessages.value = {
    ...expandedMessages.value,
    [index]: !expandedMessages.value[index],
  }
}

const clearQuestionInput = async () => {
  question.value = ""
  inputResetKey.value += 1
  await nextTick()
}

const focusQuestionInput = () => {
  nextTick(() => inputRef.value?.focus?.())
}

const compactRoomDescription = (room) => {
  const lines = [
    `「${room.title || "这个直播间"}」`,
    room.categoryName ? `分类是「${room.categoryName}」` : "",
    room.anchorName ? `主播是 ${room.anchorName}` : "",
    room.statusText ? `当前${room.statusText}` : "",
  ].filter(Boolean)
  return lines.join("，")
}

const answerFromLocalContext = (q) => {
  const room = pageContext.value.room
  const studioRoom = pageContext.value.studioRoom
  const text = String(q || "")

  if (room && /(主播.*(做什么|干什么|在干嘛|在做啥)|主播在|在干嘛|在做什么)/.test(text)) {
    if (Number(room.status) !== 1) {
      return `${room.anchorName || "主播"} 当前未开播，所以我看不到实时动作。你可以先关注直播间，开播后再回来观看。`
    }
    return `从房间资料看，${room.anchorName || "主播"} 正在直播「${room.title || "直播内容"}」，分类是「${room.categoryName || "未分类"}」。我不会假装识别实时画面；如果需要判断画面里的具体动作，可以结合播放器画面或弹幕继续问我。`
  }

  if (room && /直播间/.test(text) && /(做什么|是什么|介绍|内容|播什么|看什么)/.test(text)) {
    const detail = room.notice || room.introduce
    return `${compactRoomDescription(room)}。${detail ? `房间说明是：${detail}` : "房间暂时没有填写更详细的公告或简介。"}`
  }

  if (studioRoom && /(我的直播间|当前直播间|房间信息|开播)/.test(text)) {
    return `${compactRoomDescription(studioRoom)}。你可以在这里修改标题、分类、公告，准备好后进入开播工作台开始直播。`
  }

  return ""
}

const buildConversationContext = () =>
  messages.value.slice(-8).map((msg) => ({
    role: msg.role,
    content: String(msg.content || "").slice(0, 180),
  }))

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q || busy.value) return

  messages.value.push({ role: "user", content: q })
  quickDismissed.value = true
  await clearQuestionInput()
  busy.value = true
  await scrollToBottom()

  try {
    await refreshPageDetails({ force: true })
    const localAnswer = answerFromLocalContext(q)
    if (localAnswer) {
      messages.value.push({ role: "assistant", content: localAnswer })
      serviceReady.value = serviceReady.value === false ? false : true
      return
    }

    if (serviceReady.value === false) {
      $modal.msgWarning("AI 服务已启动，但大模型暂时不可用")
      messages.value.push({ role: "assistant", content: "AI 服务暂时不可用，我已经记录你的问题。等服务恢复后可以继续问我。" })
      return
    }

    const res = await agentApi.askHelper(q, {
      page: pageContext.value,
      conversation: buildConversationContext(),
      assistant: "xiaomai",
      expected_style: "answer briefly, use page facts, avoid generic platform explanations",
    })
    const answer = res?.data?.answer || res?.data?.message || "小脉暂时没有拿到回复。"
    messages.value.push({ role: "assistant", content: answer })
    serviceReady.value = true
  } catch (error) {
    serviceReady.value = false
    messages.value.push({ role: "assistant", content: "AI 服务已启动，但大模型暂时不可用。" })
  } finally {
    busy.value = false
    await scrollToBottom()
    focusQuestionInput()
  }
}
</script>

<style scoped lang="scss">
.xiao-mai-assistant {
  position: fixed;
  z-index: 1200;
  touch-action: none;
  user-select: none;
}

.assistant-fab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 16px;
  border: 1px solid color-mix(in srgb, var(--accent) 36%, var(--border));
  border-radius: 999px;
  background: var(--accent-gradient);
  color: var(--accent-text);
  font-weight: 900;
  box-shadow: var(--shadow-hover);
  cursor: grab;
}

.is-dragging .assistant-fab,
.is-dragging .assistant-header {
  cursor: grabbing;
}

.assistant-panel {
  width: min(360px, calc(100vw - 36px));
  margin-bottom: 12px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  box-shadow: var(--shadow-hover);
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 14px 12px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--accent) 8%, var(--bg-card));
  cursor: grab;
}

.assistant-title {
  display: flex;
  align-items: center;
  gap: 10px;

  strong {
    color: var(--text-primary);
    font-size: 15px;
    font-weight: 900;
  }
}

.assistant-status {
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 20px;
  font-weight: 800;

  &.is-online {
    color: var(--success);
    background: color-mix(in srgb, var(--success) 12%, var(--bg-card));
  }

  &.is-offline {
    color: var(--danger);
    background: color-mix(in srgb, var(--danger) 12%, var(--bg-card));
  }

  &.is-idle {
    color: var(--text-muted);
    background: var(--bg-secondary);
  }
}

.assistant-close {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.assistant-body {
  padding: 12px 12px 8px;
}

.assistant-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;

  button {
    border: 1px solid var(--border);
    border-radius: 999px;
    background: var(--bg-secondary);
    color: var(--text-secondary);
    font-size: 12px;
    font-weight: 700;
    padding: 6px 10px;
    cursor: pointer;
  }
}

.assistant-chat {
  max-height: 300px;
  overflow-y: auto;
  display: grid;
  gap: 10px;
  padding-right: 2px;
}

.chat-line {
  display: flex;
  gap: 8px;

  &.user {
    flex-direction: row-reverse;
  }
}

.chat-avatar {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.chat-avatar--image {
  object-fit: cover;
  padding: 2px;
  border: 1px solid var(--border);
  background: var(--bg-card);
}

.chat-avatar--user {
  color: var(--accent-text);
  background: var(--accent);
}

.chat-bubble {
  max-width: calc(100% - 40px);
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-secondary);

  p {
    margin: 0;
    color: var(--text-primary);
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;

    &.collapsed {
      display: -webkit-box;
      overflow: hidden;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
    }
  }
}

.message-expand {
  margin-top: 6px;
  padding: 0;
  border: 0;
  color: var(--accent);
  background: transparent;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.user .chat-bubble {
  background: color-mix(in srgb, var(--accent) 12%, var(--bg-card));
  border-color: color-mix(in srgb, var(--accent) 24%, var(--border));
}

.assistant-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--border);
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.is-open .assistant-fab {
  display: none;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

@media (max-width: 640px) {
  .assistant-panel {
    width: min(360px, calc(100vw - 24px));
  }
}
</style>
