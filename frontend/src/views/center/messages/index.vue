<template>
  <div class="messages-page">
    <section class="messages-header">
      <div>
        <h2>消息中心</h2>
        <p>系统通知和互动提醒都会收在这里，未读消息可以快速处理。</p>
      </div>
      <a-button type="primary" :disabled="!hasUnreadMessages" @click="markAllAsRead">全部标记已读</a-button>
    </section>

    <section class="messages-panel">
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <a-tab-pane key="system" tab="系统消息">
          <a-spin :spinning="loading.system">
            <div v-if="systemMessages.length" class="message-list">
              <article
                v-for="item in systemMessages"
                :key="item.id"
                class="message-card"
                :class="{ 'message-card--unread': item.isRead === 0 }"
                @click="markAsRead(item)"
              >
                <div class="message-card__head">
                  <h3>{{ item.title }}</h3>
                  <a-tag v-if="item.isRead === 0" color="error">未读</a-tag>
                </div>
                <p>{{ item.content }}</p>
                <span>{{ formatTime(item.createTime) }}</span>
              </article>
            </div>
            <a-empty v-else description="暂无系统消息" />
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="notice" tab="通知消息">
          <a-spin :spinning="loading.notice">
            <div v-if="noticeMessages.length" class="message-list">
              <article
                v-for="item in noticeMessages"
                :key="item.id"
                class="message-card"
                :class="{ 'message-card--unread': item.isRead === 0 }"
                @click="markAsRead(item)"
              >
                <div class="message-card__head">
                  <h3>{{ item.title }}</h3>
                  <a-tag v-if="item.isRead === 0" color="error">未读</a-tag>
                </div>
                <p>{{ item.content }}</p>
                <span>{{ formatTime(item.createTime) }}</span>
              </article>
            </div>
            <a-empty v-else description="暂无通知消息" />
          </a-spin>
        </a-tab-pane>
      </a-tabs>

      <section class="messages-panel__footer">
        <a-pagination
          :current="pagination[activeTab].current"
          :total="pagination[activeTab].total"
          :page-size="pagination[activeTab].pageSize"
          show-less-items
          @change="handlePageChange"
        />
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue"
import { useRouter } from "vue-router"
import $modal from "@/utils/message"
import notificationApi from "@/api/notification"
import { useNotificationStore } from "@/stores/modules/notification"
import dayjs from "dayjs"

const router = useRouter()
const notificationStore = useNotificationStore()

const activeTab = ref("system")
const systemMessages = ref([])
const noticeMessages = ref([])
const loading = reactive({ system: false, notice: false })
const pagination = reactive({
  system: { current: 1, pageSize: 10, total: 0 },
  notice: { current: 1, pageSize: 10, total: 0 },
})

const currentMessages = computed(() =>
  activeTab.value === "system" ? systemMessages.value : noticeMessages.value
)

const hasUnreadMessages = computed(() =>
  currentMessages.value.some((item) => item.isRead === 0)
)

const formatTime = (time) => dayjs(time).format("YYYY-MM-DD HH:mm:ss")

const fetchMessages = async (tab) => {
  const tabKey = tab || activeTab.value
  loading[tabKey] = true
  try {
    const pg = pagination[tabKey]
    const type = tabKey === "system" ? "system" : "live_started,followed"
    const res = await notificationApi.getNotifications({ type, page: pg.current, limit: pg.pageSize })
    if (res && res.code === 0 && res.data) {
      const data = res.data.list || []
      if (tabKey === "system") {
        systemMessages.value = data
      } else {
        noticeMessages.value = data
      }
      pg.total = res.data.total || 0
    }
  } catch (e) {
    // ignore
  } finally {
    loading[tabKey] = false
  }
}

const markAsRead = async (item) => {
  if (item.isRead === 0) {
    try {
      await notificationApi.markRead({ notificationId: item.id })
      item.isRead = 1
      notificationStore.markRead(item.id)
      $modal.msgSuccess("消息已标记为已读")
    } catch (e) {
      // ignore
    }
  }
  // 点击通知跳转到直播间
  if (item.type === "live_started" && item.relatedId) {
    router.push("/room/" + item.relatedId)
  }
}

const markAllAsRead = async () => {
  try {
    await notificationApi.markAllRead()
    currentMessages.value.forEach((item) => {
      item.isRead = 1
    })
    notificationStore.markAllRead()
    $modal.msgSuccess("已全部标记为已读")
  } catch (e) {
    // ignore
  }
}

const handleTabChange = (key) => {
  activeTab.value = key
  fetchMessages(key)
}

const handlePageChange = (page) => {
  pagination[activeTab.value].current = page
  fetchMessages()
}

onMounted(() => {
  fetchMessages("system")
  fetchMessages("notice")
})
</script>

<style scoped lang="scss">
.messages-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.messages-header,
.messages-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.messages-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 24px 26px;
}

.messages-header h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.messages-header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.messages-panel {
  padding: 18px 22px;
}

.message-list {
  display: grid;
  gap: 14px;
}

.message-card {
  padding: 18px 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  cursor: pointer;
  transition: 0.2s ease;
}

.message-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-hover);
}

.message-card--unread {
  background: linear-gradient(135deg, var(--accent-light), var(--bg-card));
  border-color: color-mix(in srgb, var(--accent) 34%, var(--border));
}

.message-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.message-card__head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
}

.message-card p {
  margin: 10px 0 12px;
  color: var(--text-secondary);
  line-height: 1.75;
}

.messages-panel__footer {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

.message-card span {
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 960px) {
  .messages-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
