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
        @click="analyzeSentiment"
        style="margin-top: 12px"
      >
        分析当前弹幕
      </a-button>

      <div v-if="sentiment.flags?.length" class="flag-list">
        <h4>需要留意的消息 ({{ sentiment.flags.length }})</h4>
        <div v-for="idx in sentiment.flags" :key="idx" class="flag-item">
          <a-tag color="warning">{{ chatMessages[idx]?.username || '未知' }}</a-tag>
          <span>{{ chatMessages[idx]?.content?.slice(0, 30) || '...' }}</span>
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
import { ref, computed } from "vue"
import agentApi from "@/api/agent"

defineEmits(["send-welcome"])

const activeTab = ref("sentiment")
const tabs = [
  { key: "sentiment", label: "弹幕氛围", icon: "弹" },
  { key: "brain", label: "直播摘要", icon: "摘" },
  { key: "helper", label: "小脉问答", icon: "问" },
]

const props = defineProps({
  chatMessages: { type: Array, default: () => [] },
  roomTitle: { type: String, default: "" },
  categoryName: { type: String, default: "" },
  anchorName: { type: String, default: "" },
})

const sentiment = ref({ overall: null, score: null, flags: [], summary: "" })
const sentimentLoading = ref(false)

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
  if (s === null) return "#94a3b8"
  if (s > 0.3) return "#22c55e"
  if (s < -0.3) return "#ef4444"
  return "#f59e0b"
})

const analyzeSentiment = async () => {
  if (!props.chatMessages.length) return
  sentimentLoading.value = true
  try {
    const res = await agentApi.analyzeSentiment(
      props.chatMessages.slice(-20).map(m => ({
        username: m.username || "匿名",
        content: m.content || "",
      }))
    )
    if (res?.data) {
      sentiment.value = res.data
    }
  } catch (e) {
    sentiment.value = { overall: "neutral", score: 0, flags: [], summary: "暂时无法生成弹幕分析" }
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
    const answer = res?.data?.answer || "小脉正在休息，请稍后再问~"
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
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;

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
    color: #64748b;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { background: #f1f5f9; }
    &.active {
      background: #fff7ed;
      color: #ea580c;
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
  background: #f8fafc;
  margin-bottom: 12px;

  &.positive { background: #f0fdf4; }
  &.negative { background: #fef2f2; }

  .status-mark {
    width: 34px;
    height: 34px;
    display: grid;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    color: #fff;
    background: #f59e0b;
    font-size: 14px;
    font-weight: 900;
  }

  strong { display: block; color: #1e293b; font-size: 14px; }
  span { color: #64748b; font-size: 12px; }
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
    background: #e2e8f0;
    overflow: hidden;

    .score-fill {
      height: 100%;
      border-radius: 3px;
      transition: width 0.6s ease;
    }
  }

  .score-num {
    color: #475569;
    font-size: 13px;
    font-weight: 600;
    min-width: 45px;
    text-align: right;
  }
}

.flag-list {
  margin-top: 12px;

  h4 { margin: 0 0 8px; color: #ef4444; font-size: 13px; }

  .flag-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    border-bottom: 1px solid #f1f5f9;
    font-size: 12px;
    color: #64748b;
  }
}

// ─── 直播智囊 ───────────────────────────────

.brain-card {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #f8fafc;

  h4 { margin: 0 0 8px; color: #1e293b; font-size: 13px; }
  p { margin: 0; color: #475569; font-size: 13px; line-height: 1.6; }
  .welcome-msg { color: #ea580c; font-weight: 500; }
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
    background: #f1f5f9;
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
    background: #f1f5f9;
    color: #334155;

    .user & {
      background: #fff7ed;
      color: #1e293b;
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
