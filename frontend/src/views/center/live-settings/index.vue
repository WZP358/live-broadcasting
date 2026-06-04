<template>
  <div class="live-settings-page">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="eyebrow">主播工作台</span>
        <h2>{{ roomInfo.title || "我的直播间" }}</h2>
        <p>准备好封面、标题和互动工具后，就可以在下方选择画面来源开始直播。</p>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <span class="label">房间号</span>
          <strong>{{ roomInfo.id || "--" }}</strong>
        </div>
        <div class="meta-item" :class="{ active: Number(roomLiveInfo.liveStatus || 0) === 1 }">
          <span class="label">直播状态</span>
          <strong>{{ liveStatusText }}</strong>
        </div>
      </div>
    </section>

    <BrowserLivePanel :room-id="roomInfo.id" :live-status="roomLiveInfo.liveStatus || 0" @status-change="refreshState" />

    <section class="info-grid">
      <article class="info-card">
        <h3>房间信息</h3>
        <div class="info-list">
          <div class="info-row">
            <span>标题</span>
            <strong>{{ roomInfo.title || "--" }}</strong>
          </div>
          <div class="info-row">
            <span>分类</span>
            <strong>{{ roomInfo.categoryId || "--" }}</strong>
          </div>
          <div class="info-row">
            <span>标签</span>
            <div class="tag-edit-row">
              <a-tag v-for="t in tags" :key="t.id" closable @close="removeTag(t)">{{ t.tagName }}</a-tag>
              <a-input v-if="tagInputVisible" ref="tagInputRef" v-model:value="tagInputValue" size="small" style="width:80px" @blur="addTag" @pressEnter="addTag" />
              <a-tag v-else color="processing" @click="showTagInput" style="cursor:pointer">+ 新标签</a-tag>
            </div>
          </div>
          <div class="info-row">
            <span>公告</span>
            <strong>{{ roomInfo.notice || "--" }}</strong>
          </div>
          <div class="info-row">
            <span>简介</span>
            <strong>{{ roomInfo.introduce || "--" }}</strong>
          </div>
        </div>
      </article>

      <article class="info-card">
        <h3>直播状态</h3>
        <div class="info-list">
          <div class="info-row">
            <span>当前状态</span>
            <strong>{{ liveStatusText }}</strong>
          </div>
          <div class="info-row">
            <span>开播时间</span>
            <strong>{{ liveStartTimeText }}</strong>
          </div>
          <div class="info-row">
            <span>观众入口</span>
            <strong>{{ audienceEntryText }}</strong>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue"
import liveAPI from "@/api/live"
import tagApi from "@/api/tag"
import $modal from "@/utils/message"
import BrowserLivePanel from "./BrowserLivePanel.vue"

const roomInfo = ref({})
const roomLiveInfo = ref({})
const tags = ref([])
const tagInputVisible = ref(false)
const tagInputValue = ref('')

const liveStatusText = computed(() => (Number(roomLiveInfo.value?.liveStatus || 0) === 1 ? "直播中" : "未开播"))
const liveStartTimeText = computed(() => roomLiveInfo.value?.liveStartTime || "开播后显示")
const audienceEntryText = computed(() => (Number(roomLiveInfo.value?.liveStatus || 0) === 1 ? "观众可进入直播间观看" : "开播后自动开放"))

const getRoomInfo = async () => {
  const res = await liveAPI.getRoomSettingsInfo()
  roomInfo.value = res.data || {}
  if (roomInfo.value.id) loadTags()
}

const getLiveStatus = async () => {
  const res = await liveAPI.getLiveStatus()
  roomLiveInfo.value = res.data || {}
}

const loadTags = async () => {
  try {
    const res = await tagApi.listByRoom(roomInfo.value.id)
    tags.value = res.data || []
  } catch (e) { tags.value = [] }
}

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    const el = document.querySelector('.tag-edit-row input')
    if (el) el.focus()
  })
}

const addTag = async () => {
  const name = tagInputValue.value.trim()
  tagInputVisible.value = false
  tagInputValue.value = ''
  if (!name) return
  if (tags.value.some(t => t.tagName === name)) { $modal.msgWarning('标签已存在'); return }
  if (tags.value.length >= 5) { $modal.msgWarning('最多5个标签'); return }
  const newTags = [...tags.value.map(t => t.tagName), name]
  try {
    await tagApi.save({ roomId: roomInfo.value.id, tags: newTags })
    loadTags()
    $modal.msgSuccess('标签已更新')
  } catch (e) { $modal.msgError('保存失败') }
}

const removeTag = async (tag) => {
  const newTags = tags.value.filter(t => t.id !== tag.id).map(t => t.tagName)
  try {
    await tagApi.save({ roomId: roomInfo.value.id, tags: newTags })
    loadTags()
    $modal.msgSuccess('标签已删除')
  } catch (e) { $modal.msgError('删除失败') }
}

const refreshState = async () => {
  await Promise.all([getRoomInfo(), getLiveStatus()])
}

onMounted(() => {
  refreshState()
})
</script>

<style scoped lang="scss">
.live-settings-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.info-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 24px;
  box-shadow: var(--shadow);
}

.hero-card {
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  background:
    linear-gradient(90deg, rgba(255, 153, 0, 0.18), rgba(255, 153, 0, 0.04)),
    #171b24;
  border-color: rgba(255, 216, 77, 0.22);
  box-shadow: 0 12px 30px rgba(18, 20, 28, 0.14);
}

.hero-card::after {
  position: absolute;
  right: 24px;
  bottom: -32px;
  width: 170px;
  height: 96px;
  border: 1px solid rgba(255, 216, 77, 0.22);
  border-radius: 8px 8px 0 0;
  background:
    linear-gradient(180deg, rgba(255, 216, 77, 0.12), rgba(255, 255, 255, 0.02)),
    rgba(255, 255, 255, 0.04);
  content: "";
}

.eyebrow {
  position: relative;
  z-index: 1;
  display: inline-block;
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(255, 216, 77, 0.16);
  color: #ffd84d;
  font-size: 12px;
  font-weight: 900;
}

.hero-copy h2 {
  position: relative;
  z-index: 1;
  margin: 12px 0 8px;
  color: #fff;
  font-size: 30px;
  font-weight: 900;
}

.hero-copy p {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 700px;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.7;
}

.hero-meta {
  position: relative;
  z-index: 1;
  min-width: 220px;
  display: grid;
  gap: 14px;
}

.meta-item,
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.meta-item {
  min-height: 52px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.07);
}

.meta-item .label {
  color: rgba(255, 255, 255, 0.62);
}

.meta-item strong {
  color: #fff;
}

.meta-item.active strong {
  color: #ffd84d;
}

.label,
.info-row span {
  color: var(--text-secondary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.info-card h3 {
  margin: 0 0 16px;
  color: var(--text-primary);
  font-weight: 900;
}

.info-list {
  display: grid;
  gap: 12px;
}

.info-row strong {
  color: var(--text-primary);
  text-align: right;
  word-break: break-all;
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-meta,
  .info-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }
}
</style>
