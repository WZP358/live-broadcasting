<template>
  <div class="agent-panel">
    <div class="agent-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        type="button"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
      </button>
    </div>

    <div v-if="activeTab === 'sentiment'" class="agent-body">
      <div class="sentiment-status" :class="sentiment.overall">
        <div class="status-mark">{{ sentiment.overall === 'positive' ? '好' : sentiment.overall === 'negative' ? '警' : '等' }}</div>
        <div class="status-text">
          <strong>弹幕氛围：{{ sentimentLabel }}</strong>
          <span>{{ sentiment.summary || '点击查看当前弹幕氛围' }}</span>
        </div>
      </div>

      <div class="sentiment-score" v-if="sentiment.score !== null">
        <div class="score-bar">
          <div class="score-fill" :style="{ width: scorePercent + '%', background: scoreColor }"></div>
        </div>
        <span class="score-num">{{ sentiment.score > 0 ? '+' : '' }}{{ (sentiment.score * 100).toFixed(0) }}%</span>
      </div>

      <a-button
        type="primary"
        block
        :loading="sentimentLoading"
        :disabled="!analyzableMessages.length"
        @click="analyzeSentiment"
        style="margin-top: 12px"
      >
        {{ sentimentButtonText }}
      </a-button>

      <div v-if="sentiment.flags?.length" class="flag-list">
        <h4>需要留意的消息 ({{ sentiment.flags.length }})</h4>
        <div v-for="item in flaggedMessages" :key="item.index" class="flag-item">
          <a-tag color="warning">{{ item.message?.username || '未知' }}</a-tag>
          <span>{{ item.message?.content?.slice(0, 30) || '...' }}</span>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'brain'" class="agent-body">
      <div v-if="summary.summary" class="brain-card">
        <h4>直播摘要</h4>
        <p>{{ summary.summary }}</p>
      </div>

      <div v-if="summary.welcomeMsg" class="brain-card">
        <h4>欢迎语</h4>
        <p class="welcome-msg">{{ summary.welcomeMsg }}</p>
        <a-button size="small" type="link" @click="$emit('send-welcome', summary.welcomeMsg)">
          发送到弹幕
        </a-button>
      </div>

      <div v-if="summary.tags?.length" class="brain-card">
        <h4>推荐标签</h4>
        <div class="tag-list">
          <a-tag v-for="tag in summary.tags" :key="tag" color="blue">{{ tag }}</a-tag>
        </div>
      </div>

      <a-button
        type="primary"
        block
        :loading="summaryLoading"
        @click="generateSummary"
        style="margin-top: 12px"
      >
        生成直播摘要
      </a-button>
    </div>

    <div v-if="activeTab === 'helper'" class="agent-body">
      <div class="helper-chat">
        <div
          v-for="(msg, i) in helperMessages"
          :key="i"
          :class="['helper-msg', msg.role]"
        >
          <div class="msg-avatar">{{ msg.role === 'user' ? '我' : '脉' }}</div>
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
      </div>

      <div class="helper-input">
        <a-input
          v-model:value="helperQuestion"
          placeholder="问问小脉"
          @press-enter="askHelper"
          :disabled="helperLoading"
        />
        <a-button
          type="primary"
          size="small"
          :loading="helperLoading"
          @click="askHelper"
        >
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue"
import agentApi from "@/api/agent"

defineEmits(["send-welcome"])

const props = defineProps({
  chatMessages: { type: Array, default: () => [] },
  roomTitle: { type: String, default: "" },
  categoryName: { type: String, default: "" },
  anchorName: { type: String, default: "" },
  defaultTab: {
    type: String,
    default: "sentiment",
    validator: (value) => ["sentiment", "brain", "helper"].includes(value),
  },
})

const activeTab = ref(props.defaultTab)
const tabs = [
  { key: "sentiment", label: "弹幕氛围", icon: "弹" },
  { key: "brain", label: "直播摘要", icon: "摘" },
  { key: "helper", label: "小脉问答", icon: "问" },
]

const sentiment = ref({ overall: null, score: null, flags: [], summary: "" })
const sentimentLoading = ref(false)
const lastAnalyzedMessages = ref([])

const sentimentLabel = computed(() => {
  const map = { positive: "积极活跃", neutral: "平静正常", negative: "需要留意" }
  return map[sentiment.value.overall] || "等待分析"
})

const scorePercent = computed(() => {
  if (sentiment.value.score === null) return 50
  return ((sentiment.value.score + 1) / 2) * 100
})

const scoreColor = computed(() => {
  const s = sentiment.value.score
  if (s === null) return "var(--text-muted)"
  if (s > 0.3) return "var(--success)"
  if (s < -0.3) return "var(--danger)"
  return "var(--warning)"
})

const normalizeChatForSentiment = (message = {}) => ({
  username: message.username || message.nickname || message.name || "匿名",
  content: String(message.content ?? message.text ?? "").trim(),
  isSystem: Boolean(message.isSystem || message.isEnter || message.nickname === "系统消息"),
})

const analyzableMessages = computed(() =>
  props.chatMessages
    .map(normalizeChatForSentiment)
    .filter((message) => message.content && !message.isSystem)
)

const sentimentButtonText = computed(() => (analyzableMessages.value.length ? "分析当前弹幕" : "暂无弹幕可分析"))
const getSentimentErrorText = (error) => {
  const message = String(error?.message || "")
  if (/消息列表不能为空/.test(message)) {
    return "还没有可分析的观众弹幕，先等观众发言。"
  }
  if (/invalid JSON|schema|empty response|AI model/i.test(message)) {
    return "AI 模型返回格式不稳定，已收到弹幕但暂时无法生成分析。"
  }
  if (/unavailable|timeout|暂不可用|服务/.test(message)) {
    return "AI Agent 或本地大模型暂不可用，请检查服务状态。"
  }
  return "暂时无法生成弹幕分析，请稍后重试。"
}

const flaggedMessages = computed(() =>
  (sentiment.value.flags || [])
    .map((idx) => ({
      index: idx,
      message: lastAnalyzedMessages.value[idx],
    }))
    .filter((item) => item.message)
)

const analyzeSentiment = async () => {
  const messages = analyzableMessages.value.slice(-20)
  if (!messages.length) {
    sentiment.value = {
      overall: null,
      score: null,
      flags: [],
      summary: "还没有可分析的观众弹幕，先等观众发言。",
    }
    return
  }

  sentimentLoading.value = true
  lastAnalyzedMessages.value = messages
  try {
    const res = await agentApi.analyzeSentiment(messages.map(({ username, content }) => ({ username, content })))
    if (res?.data) {
      sentiment.value = res.data
    }
  } catch (e) {
    sentiment.value = {
      overall: null,
      score: null,
      flags: [],
      summary: getSentimentErrorText(e),
    }
  } finally {
    sentimentLoading.value = false
  }
}

const summary = ref({ summary: "", tags: [], welcomeMsg: "" })
const summaryLoading = ref(false)

const generateSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await agentApi.generateSummary({
      title: props.roomTitle,
      category: props.categoryName,
      anchorName: props.anchorName,
      highlights: [],
    })
    if (res?.data) {
      summary.value = res.data
    }
  } catch (e) {
    summary.value = { summary: "暂时无法生成直播摘要", tags: [], welcomeMsg: "" }
  } finally {
    summaryLoading.value = false
  }
}

const helperQuestion = ref("")
const helperLoading = ref(false)
const helperMessages = ref([
  { role: "assistant", content: "你好！我是 PulseLive 小助手「小脉」，有什么可以帮你的吗？" },
])

const askHelper = async () => {
  const q = helperQuestion.value.trim()
  if (!q || helperLoading.value) return

  helperMessages.value.push({ role: "user", content: q })
  helperQuestion.value = ""
  helperLoading.value = true

  try {
    const res = await agentApi.askHelper(q)
    const answer = res?.data?.answer || "AI 服务未返回有效回复，请检查智能体服务状态。"
    helperMessages.value.push({ role: "assistant", content: answer })
  } catch (e) {
    helperMessages.value.push({ role: "assistant", content: "小脉暂时无法回复，请稍后再试。" })
  } finally {
    helperLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.agent-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.agent-tabs {
  display: flex;
  gap: 4px;
  padding: 8px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);

  button {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 36px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: var(--text-secondary);
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { background: var(--accent-light); }
    &.active {
      background: var(--accent-light);
      color: var(--accent);
      font-weight: 600;
    }
  }

  .tab-icon { font-size: 14px; }
}

.agent-body {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
}

// ─── 情感哨兵 ───────────────────────────────

.sentiment-status {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 10px;
  background: var(--bg-secondary);
  margin-bottom: 12px;

  &.positive { background: color-mix(in srgb, var(--success) 12%, var(--bg-card)); }
  &.negative { background: color-mix(in srgb, var(--danger) 12%, var(--bg-card)); }

  .status-mark {
    width: 34px;
    height: 34px;
    display: grid;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    color: #fff;
    background: var(--warning);
    font-size: 14px;
    font-weight: 900;
  }

  strong { display: block; color: var(--text-primary); font-size: 14px; }
  span { color: var(--text-secondary); font-size: 12px; }
}

.sentiment-score {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;

  .score-bar {
    flex: 1;
    height: 6px;
    border-radius: 3px;
    background: var(--border);
    overflow: hidden;

    .score-fill {
      height: 100%;
      border-radius: 3px;
      transition: width 0.6s ease;
    }
  }

  .score-num {
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 600;
    min-width: 45px;
    text-align: right;
  }
}

.flag-list {
  margin-top: 12px;

  h4 { margin: 0 0 8px; color: var(--danger); font-size: 13px; }

  .flag-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    border-bottom: 1px solid var(--border);
    font-size: 12px;
    color: var(--text-secondary);
  }
}

// ─── 直播智囊 ───────────────────────────────

.brain-card {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: var(--bg-secondary);

  h4 { margin: 0 0 8px; color: var(--text-primary); font-size: 13px; }
  p { margin: 0; color: var(--text-secondary); font-size: 13px; line-height: 1.6; }
  .welcome-msg { color: var(--accent); font-weight: 500; }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

// ─── 平台小助手 ───────────────────────────────

.helper-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.helper-msg {
  display: flex;
  gap: 8px;

  &.user { flex-direction: row-reverse; }

  .msg-avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--bg-secondary);
    color: var(--text-primary);
    display: grid;
    place-items: center;
    font-size: 16px;
    flex-shrink: 0;
  }

  .msg-bubble {
    max-width: 75%;
    padding: 8px 12px;
    border-radius: 10px;
    font-size: 13px;
    line-height: 1.5;
    background: var(--bg-secondary);
    color: var(--text-primary);

    .user & {
      background: var(--accent-light);
      color: var(--text-primary);
    }
  }
}

.helper-input {
  display: flex;
  gap: 8px;

  :deep(.ant-input) {
    border-radius: 16px;
  }
}
</style>
