<template>
  <div class="pm-page">
    <a-row :gutter="16" class="pm-layout">
      <a-col :xs="24" :md="8">
        <div class="pm-list-panel">
          <div class="pm-list-header">私信列表</div>
          <a-list v-if="contacts.length" :data-source="contacts" size="small" class="pm-contact-list">
            <template #renderItem="{ item }">
              <a-list-item class="pm-contact-item" :class="{ active: activeContact && activeContact.userId === item.userId }" @click="openConversation(item)">
                <a-list-item-meta>
                  <template #avatar>
                    <a-badge :count="item.unread" :number-style="{ fontSize: '10px' }">
                      <a-avatar :src="item.avatar || 'https://dummyimage.com/40x40/e2e8f0/64748b&text=U'" />
                    </a-badge>
                  </template>
                  <template #title>{{ item.nickname || '用户' + item.userId }}</template>
                  <template #description>{{ item.lastMsg }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="暂无私信" />
        </div>
      </a-col>
      <a-col :xs="24" :md="16">
        <div class="pm-chat-panel" v-if="activeContact">
          <div class="pm-chat-header">
            <span>{{ activeContact.nickname || '用户' + activeContact.userId }}</span>
          </div>
          <div class="pm-chat-messages" ref="msgContainer">
            <div v-for="msg in messages" :key="msg.id" class="pm-msg-row" :class="{ 'pm-msg-mine': msg.fromUserId === myId }">
              <div class="pm-msg-bubble">{{ msg.content }}</div>
              <div class="pm-msg-time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <a-empty v-if="messages.length === 0" description="暂无消息，发送第一条私信吧" />
          </div>
          <div class="pm-chat-input">
            <a-textarea v-model:value="inputText" placeholder="输入私信内容..." :auto-size="{ minRows: 2, maxRows: 3 }" :maxlength="200" />
            <a-button type="primary" size="small" @click="sendMessage" :disabled="!inputText.trim()">发送</a-button>
          </div>
        </div>
        <div class="pm-chat-panel pm-empty-chat" v-else>
          <a-empty description="选择一位联系人开始私信" />
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useStore } from '@/stores'
import pmApi from '@/api/pm'
import $modal from '@/utils/message'

const store = useStore()
const myId = ref(store.user().userId || 0)
const contacts = ref([])
const activeContact = ref(null)
const messages = ref([])
const inputText = ref('')
const msgContainer = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

const loadContacts = async () => {
  try {
    const res = await pmApi.getContacts()
    contacts.value = (res.data && Array.isArray(res.data)) ? res.data : []
  } catch (e) {
    // contacts stay empty
  }
}

const openConversation = async (contact) => {
  activeContact.value = contact
  contact.unread = 0
  try {
    const res = await pmApi.getConversation(contact.userId)
    const data = res.data
    if (data && data.list) {
      messages.value = data.list.reverse()
    }
    scrollToBottom()
    await pmApi.markRead({ fromUserId: contact.userId })
  } catch (e) {
    $modal.msgError('加载对话失败')
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim() || !activeContact.value) return
  try {
    await pmApi.send({ toUserId: activeContact.value.userId, content: inputText.value.trim() })
    messages.value.push({
      id: Date.now(),
      fromUserId: myId.value,
      toUserId: activeContact.value.userId,
      content: inputText.value.trim(),
      createTime: new Date().toISOString()
    })
    const idx = contacts.value.findIndex(c => c.userId === activeContact.value.userId)
    if (idx >= 0) contacts.value[idx].lastMsg = inputText.value.trim()
    inputText.value = ''
    scrollToBottom()
  } catch (e) {
    $modal.msgError('发送失败')
  }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadContacts()
})
</script>

<style scoped>
.pm-page { min-height: 520px; }
.pm-layout { min-height: 520px; }
.pm-list-panel { border: 1px solid var(--border); border-radius: 8px; padding: 12px; min-height: 520px; height: 100%; display: flex; flex-direction: column; background: var(--bg-card); box-shadow: var(--shadow); }
.pm-list-header { font-weight: 900; margin-bottom: 8px; color: var(--text-primary); }
.pm-contact-list { flex: 1; overflow-y: auto; margin-top: 8px; }
.pm-contact-item { cursor: pointer; border-radius: 6px; }
.pm-contact-item.active { background: var(--accent-light); }
.pm-chat-panel { border: 1px solid var(--border); border-radius: 8px; min-height: 520px; height: 100%; display: flex; flex-direction: column; background: var(--bg-card); box-shadow: var(--shadow); }
.pm-chat-header { padding: 12px 14px; border-bottom: 1px solid var(--border); font-weight: 900; color: var(--text-primary); }
.pm-chat-messages { flex: 1; overflow-y: auto; padding: 14px; background: var(--bg-secondary); }
.pm-msg-row { margin-bottom: 12px; display: flex; flex-direction: column; align-items: flex-start; }
.pm-msg-mine { align-items: flex-end; }
.pm-msg-bubble { max-width: 70%; padding: 8px 12px; border-radius: 8px; background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border); word-break: break-all; }
.pm-msg-mine .pm-msg-bubble { background: var(--accent); color: var(--accent-text); border-color: var(--accent); font-weight: 700; }
.pm-msg-time { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.pm-chat-input { padding: 12px; border-top: 1px solid var(--border); display: grid; gap: 8px; }
.pm-chat-input .ant-btn { width: fit-content; justify-self: end; }
.pm-empty-chat { display: flex; align-items: center; justify-content: center; }
@media (max-width: 767px) {
  .pm-list-panel,
  .pm-chat-panel { min-height: 360px; }
}
</style>
