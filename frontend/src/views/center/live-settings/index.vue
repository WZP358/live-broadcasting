<template>
  <div class="live-settings-page">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="eyebrow">Live Console</span>
        <h2>{{ roomInfo.title || "我的直播间" }}</h2>
        <p>你可以直接在网页里开播，观众会优先通过 WebRTC 观看；如果浏览器直播不可用，再回退到拉流播放。</p>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <span class="label">房间号</span>
          <strong>{{ roomInfo.id || "--" }}</strong>
        </div>
        <div class="meta-item">
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
        <h3>兼容推流信息</h3>
        <div class="info-list">
          <div class="info-row">
            <span>推流地址</span>
            <strong>{{ roomLiveInfo.livePushUrl || "--" }}</strong>
          </div>
          <div class="info-row">
            <span>推流密钥</span>
            <strong>{{ roomLiveInfo.livePushSecret || "--" }}</strong>
          </div>
          <div class="info-row">
            <span>开播时间</span>
            <strong>{{ roomLiveInfo.liveStartTime || "--" }}</strong>
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
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
}

.eyebrow {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 12px 0 8px;
  font-size: 30px;
}

.hero-copy p {
  margin: 0;
  max-width: 700px;
  color: #64748b;
  line-height: 1.7;
}

.hero-meta {
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

.label,
.info-row span {
  color: #64748b;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.info-card h3 {
  margin: 0 0 16px;
}

.info-list {
  display: grid;
  gap: 12px;
}

.info-row strong {
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
