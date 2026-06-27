<template>
  <AdminCard
    title="运行环境自检"
    subtitle="检查外网访问、聊天室、直播信令、AI 助手和录播代理是否可用。"
  >
    <template #extra>
      <a-space>
        <a-tag :color="overall.color">{{ overall.text }}</a-tag>
        <a-button size="small" :loading="checking" @click="runCheck">重新检测</a-button>
      </a-space>
    </template>

    <div class="demo-health">
      <article
        v-for="item in visibleItems"
        :key="item.key"
        class="demo-health__item"
        :class="`demo-health__item--${item.status}`"
      >
        <span class="demo-health__dot"></span>
        <div>
          <strong>{{ item.label }}</strong>
          <p>{{ item.detail }}</p>
        </div>
        <a-tag :color="statusMeta[item.status]?.color || 'default'">
          {{ statusMeta[item.status]?.text || item.status }}
        </a-tag>
      </article>
    </div>

    <p v-if="lastCheckedText" class="demo-health__time">最近检测：{{ lastCheckedText }}</p>
  </AdminCard>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import { useStore } from "@/stores"
import { runPublicDemoDiagnostics } from "@/utils/demoDiagnostics"

const store = useStore()
const checking = ref(false)
const items = ref([])
const lastCheckedAt = ref(null)

const statusMeta = {
  ok: { text: "正常", color: "success" },
  warn: { text: "提醒", color: "warning" },
  error: { text: "异常", color: "error" },
}

const placeholderItems = [
  { key: "agent-health", label: "小脉 AI 服务", status: "warn", detail: "尚未检测。" },
  { key: "chat-ws", label: "聊天室 WebSocket", status: "warn", detail: "尚未检测。" },
  { key: "live-signal", label: "直播信令 WebSocket", status: "warn", detail: "尚未检测。" },
  { key: "live-stream-proxy", label: "录播流代理", status: "warn", detail: "尚未检测。" },
]

const visibleItems = computed(() => (items.value.length ? items.value : placeholderItems))

const overall = computed(() => {
  if (checking.value) {
    return { text: "检测中", color: "processing" }
  }
  if (!items.value.length) {
    return { text: "待检测", color: "default" }
  }
  if (items.value.some((item) => item.status === "error")) {
    return { text: "需处理", color: "error" }
  }
  if (items.value.some((item) => item.status === "warn")) {
    return { text: "可演示", color: "warning" }
  }
  return { text: "全部正常", color: "success" }
})

const lastCheckedText = computed(() => {
  if (!lastCheckedAt.value) {
    return ""
  }
  return lastCheckedAt.value.toLocaleTimeString()
})

const runCheck = async () => {
  checking.value = true
  try {
    const result = await runPublicDemoDiagnostics({
      token: store.user().userToken,
    })
    items.value = result.items
    lastCheckedAt.value = result.checkedAt
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  runCheck()
})
</script>

<style scoped lang="scss">
.demo-health {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.demo-health__item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 72px;
  padding: 12px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: var(--bg-secondary);
}

.demo-health__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.demo-health__item--ok .demo-health__dot {
  background: var(--success);
}

.demo-health__item--warn .demo-health__dot {
  background: var(--warning);
}

.demo-health__item--error .demo-health__dot {
  background: var(--danger);
}

.demo-health__item strong {
  display: block;
  color: var(--admin-text);
  font-size: 14px;
}

.demo-health__item p {
  margin: 5px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.demo-health__time {
  margin: 12px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .demo-health {
    grid-template-columns: 1fr;
  }
}
</style>
