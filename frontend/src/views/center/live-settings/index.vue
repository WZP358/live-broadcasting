<template>
  <div class="live-settings-page">
    <section class="room-hero">
      <div class="hero-copy">
        <span class="eyebrow">开播准备</span>
        <h2>{{ roomInfo.title || "我的直播间" }}</h2>
        <p>先确认房间资料和互动标签，准备好后进入开播工作台管理画面、声音和弹幕。</p>
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

    <section class="room-card-section">
      <div class="section-heading">
        <div>
          <span>当前直播间</span>
          <h3>直播间列表卡片预览</h3>
        </div>
        <a-button v-if="roomInfo.id" @click="goRoom">打开观众页</a-button>
      </div>
      <article class="room-preview-card">
        <div class="room-preview-card__cover" :style="{ backgroundImage: `url(${safeCover})` }">
          <span class="room-preview-card__status" :class="{ active: isLiving }">{{ liveStatusText }}</span>
          <span class="room-preview-card__category">{{ categoryName }}</span>
        </div>
        <div class="room-preview-card__body">
          <div>
            <h3>{{ roomInfo.title || "未命名直播间" }}</h3>
            <p>{{ roomInfo.introduce || "填写简介后，会同步展示在直播间卡片和观众页。" }}</p>
          </div>
          <div class="room-preview-card__meta">
            <span>房间号 {{ roomInfo.id || "--" }}</span>
            <span>{{ tags.length ? `${tags.length} 个标签` : "未添加标签" }}</span>
          </div>
        </div>
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
            <strong>{{ categoryName }}</strong>
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

    <section class="config-panel">
      <article class="info-card">
        <div class="panel-head">
          <div>
            <h3>直播间创建 / 资料表单</h3>
            <p>系统会为每个账号初始化一个直播间；这里保存标题、分类、封面、公告和简介后，首页卡片与观众页会同步更新。</p>
          </div>
          <a-button type="primary" :loading="savingInfo" @click="saveRoomProfile">保存资料</a-button>
        </div>
        <a-form class="room-form" layout="vertical">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="直播标题">
                <a-input v-model:value="profileForm.title" :maxlength="60" placeholder="请输入直播标题" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="直播分类">
                <a-select v-model:value="profileForm.cid" placeholder="请选择分类" :options="categoryOptions" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="直播公告">
                <a-input v-model:value="profileForm.notice" :maxlength="120" placeholder="告诉观众本场直播安排" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="封面地址">
                <a-input v-model:value="profileForm.cover" placeholder="可粘贴封面图片 URL" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="直播简介">
                <a-textarea v-model:value="profileForm.introduce" :rows="3" :maxlength="240" placeholder="简单介绍直播内容" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
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
import { normalizeCategories } from "@/utils/categoryPresenter"
import { FALLBACK_COVER, resolveSafeImageUrl } from "@/utils/fallback"

const router = useRouter()
const roomInfo = ref({})
const roomLiveInfo = ref({})
const categories = ref([])
const tags = ref([])
const tagInputVisible = ref(false)
const tagInputValue = ref("")
const savingInfo = ref(false)
const profileForm = ref({
  title: "",
  cover: "",
  cid: undefined,
  notice: "",
  introduce: "",
})

const isLiving = computed(() => Number(roomLiveInfo.value?.liveStatus || 0) === 1)
const liveStatusText = computed(() => (isLiving.value ? "直播中" : "未开播"))
const liveStartTimeText = computed(() => roomLiveInfo.value?.liveStartTime || "开播后显示")
const audienceEntryText = computed(() => (isLiving.value ? "观众可进入直播间观看" : "开播后自动开放"))
const categoryOptions = computed(() => categories.value.map((item) => ({ label: item.name, value: item.id })))
const categoryName = computed(() => {
  const categoryId = Number(roomInfo.value.categoryId || profileForm.value.cid)
  return categories.value.find((item) => item.id === categoryId)?.name || "--"
})
const safeCover = computed(() => resolveSafeImageUrl(profileForm.value.cover || roomInfo.value.cover, FALLBACK_COVER))

const syncProfileForm = () => {
  profileForm.value = {
    title: roomInfo.value.title || "",
    cover: roomInfo.value.cover || "",
    cid: roomInfo.value.categoryId || undefined,
    notice: roomInfo.value.notice || "",
    introduce: roomInfo.value.introduce || "",
  }
}

const getRoomInfo = async () => {
  const res = await liveAPI.getRoomSettingsInfo()
  roomInfo.value = res.data || {}
  syncProfileForm()
  if (roomInfo.value.id) {
    loadTags()
  }
}

const loadCategories = async () => {
  try {
    const res = await liveAPI.listCategories({ limit: 100 })
    categories.value = normalizeCategories(res?.data?.list || [])
  } catch (e) {
    categories.value = []
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
  await Promise.all([loadCategories(), getRoomInfo(), getLiveStatus()])
}

const saveRoomProfile = async () => {
  if (!profileForm.value.title.trim()) {
    $modal.msgWarning("请填写直播标题")
    return
  }
  if (!profileForm.value.cid) {
    $modal.msgWarning("请选择直播分类")
    return
  }
  savingInfo.value = true
  try {
    const res = await liveAPI.saveRoomInfo({
      title: profileForm.value.title.trim(),
      cover: profileForm.value.cover?.trim(),
      cid: profileForm.value.cid,
      notice: profileForm.value.notice?.trim(),
      introduce: profileForm.value.introduce?.trim(),
    })
    if (res?.data === false) {
      $modal.msgError("分类不可用或资料保存失败")
      return
    }
    $modal.msgSuccess("开播资料已保存")
    await getRoomInfo()
  } catch (e) {
    $modal.msgError("保存失败")
  } finally {
    savingInfo.value = false
  }
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

.room-card-section {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.section-heading span {
  color: var(--accent);
  font-size: 12px;
  font-weight: 900;
}

.section-heading h3 {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.room-preview-card {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  min-height: 180px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 84%, var(--bg-secondary));
}

.room-preview-card__cover {
  position: relative;
  min-height: 180px;
  background-color: #11141b;
  background-position: center;
  background-size: cover;
}

.room-preview-card__cover::after {
  position: absolute;
  inset: 0;
  content: "";
  background: linear-gradient(180deg, rgba(5, 6, 9, 0.08), rgba(5, 6, 9, 0.66));
}

.room-preview-card__status,
.room-preview-card__category {
  position: absolute;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.room-preview-card__status {
  top: 12px;
  left: 12px;
  background: rgba(107, 114, 128, 0.86);
}

.room-preview-card__status.active {
  background: rgba(244, 63, 94, 0.94);
}

.room-preview-card__category {
  right: 12px;
  bottom: 12px;
  background: rgba(5, 6, 9, 0.68);
}

.room-preview-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
}

.room-preview-card__body h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.3;
}

.room-preview-card__body p {
  max-width: 64ch;
  margin: 8px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.room-preview-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-preview-card__meta span {
  height: 28px;
  padding: 0 11px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.info-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 14px;
}

.config-panel {
  display: block;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin-bottom: 6px;
}

.panel-head p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.room-form :deep(.ant-form-item) {
  margin-bottom: 12px;
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
  .info-grid,
  .room-preview-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .room-hero,
  .quick-card,
  .info-card,
  .room-card-section {
    padding: 16px;
  }

  .hero-actions :deep(.ant-btn) {
    width: 100%;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .section-heading :deep(.ant-btn) {
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

  .panel-head {
    flex-direction: column;
  }

  .panel-head :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
