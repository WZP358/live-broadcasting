<template>
  <div class="messages-page">
    <section class="messages-header">
      <div>
        <h2>消息中心</h2>
        <p>把系统通知和互动提醒收在统一视图里，支持未读态标记和快速处理。</p>
      </div>
      <a-button type="primary" :disabled="!hasUnreadMessages" @click="markAllAsRead">全部标记已读</a-button>
    </section>

    <section class="messages-panel">
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <a-tab-pane key="system" tab="系统消息">
          <div class="message-list">
            <article
              v-for="item in systemMessages"
              :key="item.id"
              class="message-card"
              :class="{ 'message-card--unread': !item.isRead }"
              @click="markAsRead(item)"
            >
              <div class="message-card__head">
                <h3>{{ item.title }}</h3>
                <a-tag v-if="!item.isRead" color="error">未读</a-tag>
              </div>
              <p>{{ item.content }}</p>
              <span>{{ formatTime(item.createTime) }}</span>
            </article>
          </div>
        </a-tab-pane>

        <a-tab-pane key="notice" tab="通知消息">
          <div class="message-list">
            <article
              v-for="item in noticeMessages"
              :key="item.id"
              class="message-card"
              :class="{ 'message-card--unread': !item.isRead }"
              @click="markAsRead(item)"
            >
              <div class="message-card__head">
                <h3>{{ item.title }}</h3>
                <a-tag v-if="!item.isRead" color="error">未读</a-tag>
              </div>
              <p>{{ item.content }}</p>
              <span>{{ formatTime(item.createTime) }}</span>
            </article>
          </div>
        </a-tab-pane>
      </a-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue"
import { message } from "ant-design-vue"
import dayjs from "dayjs"

const activeTab = ref("system")
const systemMessages = ref([])
const noticeMessages = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
})

const hasUnreadMessages = computed(() => {
  const currentMessages = activeTab.value === "system" ? systemMessages.value : noticeMessages.value
  return currentMessages.some((item) => !item.isRead)
})

const formatTime = (time) => dayjs(time).format("YYYY-MM-DD HH:mm:ss")

const fetchMessages = async () => {
  const now = new Date()
  systemMessages.value = [
    {
      id: 1,
      title: "系统维护通知",
      content: "平台将在今晚 22:00 后进行例行维护，请提前确认直播安排和后台配置。",
      isRead: false,
      createTime: now.toISOString(),
    },
    {
      id: 2,
      title: "功能更新提醒",
      content: "新的统一登录入口和后台界面已上线，后续请从同一登录页进入不同系统。",
      isRead: true,
      createTime: new Date(now.getTime() - 86400000).toISOString(),
    },
  ]

  noticeMessages.value = [
    {
      id: 3,
      title: "直播互动提醒",
      content: "你关注的主播已开播，当前直播间互动热度较高，建议及时查看。",
      isRead: false,
      createTime: now.toISOString(),
    },
    {
      id: 4,
      title: "账号安全提示",
      content: "检测到你的账号在新设备登录，如非本人操作请尽快修改密码。",
      isRead: true,
      createTime: new Date(now.getTime() - 3600000 * 8).toISOString(),
    },
  ]
}

const markAsRead = (item) => {
  if (!item.isRead) {
    item.isRead = true
    message.success("消息已标记为已读")
  }
}

const markAllAsRead = () => {
  const currentMessages = activeTab.value === "system" ? systemMessages.value : noticeMessages.value
  currentMessages.forEach((item) => {
    item.isRead = true
  })
  message.success("已全部标记为已读")
}

const handleTabChange = (key) => {
  activeTab.value = key
  pagination.current = 1
}

onMounted(() => {
  fetchMessages()
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
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
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
  color: #0f172a;
  font-size: 24px;
}

.messages-header p {
  margin: 0;
  color: #64748b;
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
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: #fff;
  cursor: pointer;
  transition: 0.2s ease;
}

.message-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.message-card--unread {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.9), rgba(255, 255, 255, 1));
  border-color: rgba(59, 130, 246, 0.26);
}

.message-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.message-card__head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.message-card p {
  margin: 10px 0 12px;
  color: #475569;
  line-height: 1.75;
}

.message-card span {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 960px) {
  .messages-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
