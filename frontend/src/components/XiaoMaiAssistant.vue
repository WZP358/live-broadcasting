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
            <button v-for="item in quickQuestions" :key="item" type="button" @click="askQuick(item)">
              {{ item }}
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
            v-model:value="question"
            allow-clear
            :placeholder="inputPlaceholder"
            :disabled="busy"
            @press-enter="sendQuestion"
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
import { computed, nextTick, onBeforeUnmount, ref } from "vue"
import { CloseOutlined, RobotOutlined, SendOutlined } from "@ant-design/icons-vue"
import agentApi from "@/api/agent"
import $modal from "@/utils/message"
import xiaomaiAvatar from "@/assets/img/xiaomai-avatar.png"

const open = ref(false)
const busy = ref(false)
const dragging = ref(false)
const dragMoved = ref(false)
const question = ref("")
const chatRef = ref(null)
const serviceReady = ref(null)
const position = ref({ right: 18, bottom: 18 })
const dragState = ref(null)
const quickDismissed = ref(false)
const expandedMessages = ref({})
const messages = ref([
  { role: "assistant", content: "你好，我是小脉。你可以问我开播、数据、客服或平台功能。" },
])

const quickQuestions = [
  "这个项目的 AI 智能体怎么体现？",
  "怎么补充演示数据？",
  "个人中心怎么更好看？",
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

const checkHealth = async () => {
  try {
    const res = await agentApi.healthCheck()
    const data = res?.data || {}
    const health = data.llm || data
    serviceReady.value = health?.status === "ok" || health?.status === "ready"
    if (serviceReady.value) {
      messages.value.push({ role: "assistant", content: "小脉已就位，可以直接提问。" })
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

const isLongMessage = (msg) => String(msg?.content || "").length > 80

const toggleMessageExpand = (index) => {
  expandedMessages.value = {
    ...expandedMessages.value,
    [index]: !expandedMessages.value[index],
  }
}

const sendQuestion = async () => {
  const q = question.value.trim()
  if (!q || busy.value) return

  if (serviceReady.value === false) {
    $modal.msgWarning("AI 服务已启动，但大模型暂时不可用")
    return
  }

  messages.value.push({ role: "user", content: q })
  quickDismissed.value = true
  question.value = ""
  busy.value = true
  await scrollToBottom()

  try {
    const res = await agentApi.askHelper(q)
    const answer = res?.data?.answer || res?.data?.message || "小脉暂时没有拿到回复。"
    messages.value.push({ role: "assistant", content: answer })
    serviceReady.value = true
  } catch (error) {
    serviceReady.value = false
    messages.value.push({ role: "assistant", content: "AI 服务已启动，但大模型暂时不可用。" })
  } finally {
    busy.value = false
    await scrollToBottom()
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
