<template>
  <div class="center-panel">
    <section class="center-panel__header">
      <div>
        <h2>我的关注</h2>
        <p>已关注的主播会集中在这里，开播后可以快速回到常看的直播间。</p>
      </div>
      <span class="center-panel__count">共 {{ total }} 个关注</span>
    </section>

    <section class="center-panel__body">
      <a-spin :spinning="spinning">
        <div v-if="list.length" class="follow-list">
          <article v-for="item in list" :key="item.id" class="follow-card">
            <div class="follow-card__cover" @click="goRoom(item.roomId)">
              <img :src="safeCover(item.cover)" alt="" @error="(event) => onImgError(event, fallbackCover)" />
              <span :class="['follow-card__status', { 'is-live': item.liveStatus === 1 }]">
                {{ item.liveStatus === 1 ? "直播中" : "未开播" }}
              </span>
            </div>

            <div class="follow-card__body">
              <img class="follow-card__avatar" :src="safeAvatar(item.avatar)" alt="" @error="onImgError" />
              <div class="follow-card__main">
                <strong>{{ item.name || "主播" }}</strong>
                <span>{{ item.title || "直播间" }}</span>
              </div>
              <div class="follow-card__actions">
                <a-button type="primary" @click="goRoom(item.roomId)">
                  进入直播间
                </a-button>
                <a-button :loading="removingId === item.roomId" @click="cancelFollow(item)">
                  取消关注
                </a-button>
              </div>
            </div>
          </article>
        </div>

        <a-empty v-else class="center-panel__empty">
          <template #description>
            <span class="empty-title">还没有关注任何主播</span>
            <p class="empty-desc">去首页看看正在热播的内容，关注喜欢的主播后会出现在这里。</p>
          </template>
          <template #children>
            <a-button type="primary" size="large" @click="$router.push('/home')">
              去首页
            </a-button>
          </template>
        </a-empty>
      </a-spin>
    </section>

    <section v-if="total > pageSize" class="center-panel__footer">
      <a-pagination :current="current" :total="total" :page-size="pageSize" show-less-items @change="onChange" />
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import $modal from "@/utils/message"
import watchApi from "@/api/watch"
import { FALLBACK_AVATAR, FALLBACK_COVER, onImgError, resolveSafeImageUrl } from "@/utils/fallback"

const router = useRouter()
const pageSize = 12
const current = ref(1)
const total = ref(0)
const list = reactive([])
const spinning = ref(false)
const removingId = ref(null)
const fallbackAvatar = FALLBACK_AVATAR
const fallbackCover = FALLBACK_COVER
const safeAvatar = (url) => resolveSafeImageUrl(url, FALLBACK_AVATAR)
const safeCover = (url) => resolveSafeImageUrl(url, FALLBACK_COVER)

onMounted(() => {
  listData()
})

const listData = async () => {
  spinning.value = true
  try {
    const res = await watchApi.list({ type: 1, page: current.value, limit: pageSize })
    const { list: data = [], total: totalNum = 0 } = res.data || {}
    list.splice(0, list.length, ...data)
    total.value = totalNum
    if (!data.length && current.value > 1) {
      current.value -= 1
      await listData()
    }
  } finally {
    spinning.value = false
  }
}

const cancelFollow = async (item) => {
  try {
    await $modal.confirm(`确定不再关注「${item.name || "该主播"}」吗？`, {
      title: "取消关注",
      okText: "取消关注",
    })
  } catch (error) {
    return
  }

  removingId.value = item.roomId
  try {
    await watchApi.unFollow({ roomId: item.roomId })
    $modal.msgSuccess("已取消关注")
    await listData()
  } catch (error) {
    $modal.msgError(error?.message || "取消关注失败")
  } finally {
    removingId.value = null
  }
}

const goRoom = (roomId) => {
  if (!roomId) {
    return
  }
  router.push(`/room/${roomId}`)
}

const onChange = (currentPageNo) => {
  current.value = currentPageNo
  listData()
}
</script>

<style scoped lang="scss">
.center-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.center-panel__header,
.center-panel__body,
.center-panel__footer {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.center-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 26px;
}

.center-panel__header h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.center-panel__header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.center-panel__count {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  padding: 8px 14px;
  border-radius: 999px;
  background: var(--accent-light);
  color: var(--accent);
  font-weight: 800;
}

.center-panel__body {
  min-height: 380px;
  padding: 22px;
}

.follow-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.follow-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 92%, var(--bg-secondary));
  box-shadow: var(--shadow);
}

.follow-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  cursor: pointer;
  background: var(--bg-secondary);
}

.follow-card__cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.follow-card:hover .follow-card__cover img {
  transform: scale(1.04);
}

.follow-card__status {
  position: absolute;
  top: 10px;
  right: 10px;
  height: 26px;
  padding: 0 10px;
  border-radius: 4px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--bg-card) 86%, transparent);
  font-size: 12px;
  font-weight: 800;
  line-height: 26px;
  backdrop-filter: blur(10px);
}

.follow-card__status.is-live {
  color: var(--accent-text);
  background: var(--accent);
}

.follow-card__body {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
}

.follow-card__avatar {
  width: 48px;
  height: 48px;
  border: 2px solid var(--bg-card);
  border-radius: 50%;
  object-fit: cover;
  box-shadow: var(--shadow);
}

.follow-card__main {
  min-width: 0;
}

.follow-card__main strong,
.follow-card__main span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-card__main strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
}

.follow-card__main span {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 13px;
}

.follow-card__actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 4px;
}

.center-panel__empty {
  padding: 48px 0;
}

.empty-title {
  display: block;
  margin-bottom: 8px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 800;
}

.empty-desc {
  margin: 0 0 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.center-panel__footer {
  display: flex;
  justify-content: center;
  padding: 18px 20px;
}

@media (max-width: 960px) {
  .center-panel__header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .center-panel__body {
    padding: 14px;
  }

  .follow-list {
    grid-template-columns: 1fr;
  }

  .follow-card__actions {
    flex-direction: column;
  }
}
</style>
