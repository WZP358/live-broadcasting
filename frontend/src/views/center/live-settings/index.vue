<template>
  <div class="live-settings-page">
    <section class="room-hero">
      <div class="hero-copy">
        <span class="eyebrow">开播准备</span>
        <h2>{{ roomInfo.title || "我的直播间" }}</h2>
        <p>先确认房间资料和互动标签，准备好后进入开播工作台管理画面、声音、字幕和弹幕。</p>
        <div class="hero-actions">
          <a-button type="primary" size="large" @click="enterStudio">进入开播工作台</a-button>
          <a-button size="large" @click="refreshState">刷新状态</a-button>
          <a-button v-if="roomInfo.id" size="large" @click="goRoom">查看直播间</a-button>
        </div>
      </div>

      <div class="hero-meta">
        <div class="meta-item">
          <span>房间号</span>
          <strong>{{ roomInfo.id || "--" }}</strong>
        </div>
        <div class="meta-item" :class="{ active: isLiving }">
          <span>直播状态</span>
          <strong>{{ liveStatusText }}</strong>
        </div>
        <div class="meta-item">
          <span>开播时间</span>
          <strong>{{ liveStartTimeText }}</strong>
        </div>
      </div>
    </section>

    <section class="quick-grid">
      <article class="quick-card quick-card--accent">
        <span>工作台</span>
        <strong>独立页面开播</strong>
        <p>画面预览、开播方式和互动消息在宽屏工作台中管理。</p>
        <button type="button" @click="enterStudio">打开工作台</button>
      </article>
      <article class="quick-card">
        <span>观众入口</span>
        <strong>{{ audienceEntryText }}</strong>
        <p>开播后观众可以从直播间页面进入观看。</p>
      </article>
      <article class="quick-card">
        <span>互动准备</span>
        <strong>{{ tags.length ? `${tags.length} 个标签` : "未添加标签" }}</strong>
        <p>标签会帮助观众更快找到你的直播间。</p>
      </article>
    </section>

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
          <div class="info-row info-row--tags">
            <span>标签</span>
            <div class="tag-edit-row">
              <a-tag v-for="t in tags" :key="t.id" closable @close="removeTag(t)">{{ t.tagName }}</a-tag>
              <a-input
                v-if="tagInputVisible"
                ref="tagInputRef"
                v-model:value="tagInputValue"
                size="small"
                class="tag-input"
                @blur="addTag"
                @pressEnter="addTag"
              />
              <a-tag v-else color="processing" class="add-tag" @click="showTagInput">+ 新标签</a-tag>
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
import { useRouter } from "vue-router"
import liveAPI from "@/api/live"
import tagApi from "@/api/tag"
import $modal from "@/utils/message"

const router = useRouter()
const roomInfo = ref({})
const roomLiveInfo = ref({})
const tags = ref([])
const tagInputVisible = ref(false)
const tagInputValue = ref("")

const isLiving = computed(() => Number(roomLiveInfo.value?.liveStatus || 0) === 1)
const liveStatusText = computed(() => (isLiving.value ? "直播中" : "未开播"))
const liveStartTimeText = computed(() => roomLiveInfo.value?.liveStartTime || "开播后显示")
const audienceEntryText = computed(() => (isLiving.value ? "观众可进入直播间观看" : "开播后自动开放"))

const getRoomInfo = async () => {
  const res = await liveAPI.getRoomSettingsInfo()
  roomInfo.value = res.data || {}
  if (roomInfo.value.id) {
    loadTags()
  }
}

const getLiveStatus = async () => {
  const res = await liveAPI.getLiveStatus()
  roomLiveInfo.value = res.data || {}
}

const loadTags = async () => {
  try {
    const res = await tagApi.listByRoom(roomInfo.value.id)
    tags.value = res.data || []
  } catch (e) {
    tags.value = []
  }
}

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    const el = document.querySelector(".tag-edit-row input")
    if (el) el.focus()
  })
}

const addTag = async () => {
  const name = tagInputValue.value.trim()
  tagInputVisible.value = false
  tagInputValue.value = ""
  if (!name) return
  if (tags.value.some((t) => t.tagName === name)) {
    $modal.msgWarning("标签已存在")
    return
  }
  if (tags.value.length >= 5) {
    $modal.msgWarning("最多5个标签")
    return
  }

  const newTags = [...tags.value.map((t) => t.tagName), name]
  try {
    await tagApi.save({ roomId: roomInfo.value.id, tags: newTags })
    loadTags()
    $modal.msgSuccess("标签已更新")
  } catch (e) {
    $modal.msgError("保存失败")
  }
}

const removeTag = async (tag) => {
  const newTags = tags.value.filter((t) => t.id !== tag.id).map((t) => t.tagName)
  try {
    await tagApi.save({ roomId: roomInfo.value.id, tags: newTags })
    loadTags()
    $modal.msgSuccess("标签已删除")
  } catch (e) {
    $modal.msgError("删除失败")
  }
}

const refreshState = async () => {
  await Promise.all([getRoomInfo(), getLiveStatus()])
}

const enterStudio = () => {
  router.push("/live/studio")
}

const goRoom = () => {
  router.push(`/room/${roomInfo.value.id}`)
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

.room-hero,
.quick-card,
.info-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.room-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 22px;
  align-items: stretch;
  padding: 24px;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 4px;
  color: var(--accent-strong);
  background: var(--accent-light);
  font-size: 12px;
  font-weight: 900;
}

.hero-copy h2 {
  margin: 12px 0 8px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 900;
  line-height: 1.25;
}

.hero-copy p {
  max-width: 720px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.hero-meta {
  display: grid;
  gap: 10px;
}

.meta-item {
  display: grid;
  align-content: center;
  gap: 4px;
  min-height: 70px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.meta-item span,
.quick-card span,
.info-row span {
  color: var(--text-muted);
  font-size: 12px;
}

.meta-item strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
}

.meta-item.active strong {
  color: var(--danger);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  min-width: 0;
  padding: 18px;
}

.quick-card strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 900;
}

.quick-card p {
  min-height: 42px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.quick-card button {
  height: 34px;
  margin-top: 14px;
  padding: 0 14px;
  border: 0;
  border-radius: 6px;
  color: var(--accent-text);
  background: var(--accent);
  font-weight: 900;
  cursor: pointer;
}

.quick-card--accent {
  border-color: color-mix(in srgb, var(--accent) 32%, var(--border));
  background: linear-gradient(135deg, var(--accent-light), var(--bg-card) 58%);
}

.info-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 14px;
}

.info-card {
  padding: 20px;
}

.info-card h3 {
  margin: 0 0 16px;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 900;
}

.info-list {
  display: grid;
  gap: 14px;
}

.info-row {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.info-row strong {
  color: var(--text-primary);
  text-align: right;
  word-break: break-word;
}

.info-row--tags {
  align-items: center;
}

.tag-edit-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.tag-input {
  width: 92px;
}

.add-tag {
  cursor: pointer;
}

@media (max-width: 980px) {
  .room-hero,
  .quick-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .room-hero,
  .quick-card,
  .info-card {
    padding: 16px;
  }

  .hero-actions :deep(.ant-btn) {
    width: 100%;
  }

  .info-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .info-row strong,
  .tag-edit-row {
    justify-content: flex-start;
    text-align: left;
  }
}
</style>
