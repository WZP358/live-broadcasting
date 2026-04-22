<template>
  <article class="history-item" @click="itemClick">
    <div class="history-item__cover-wrap">
      <span v-if="liveStatus === 1" class="history-item__badge">
        <PlayCircleOutlined />
        直播中
      </span>
      <img class="history-item__cover" :src="cover" alt="" />
    </div>

    <div class="history-item__info">
      <img class="history-item__avatar" :src="cover" alt="" />
      <div class="history-item__meta">
        <strong>{{ name || "未知主播" }}</strong>
        <span>点击进入直播间查看详情</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { PlayCircleOutlined } from "@ant-design/icons-vue"
import { useRouter } from "vue-router"

const router = useRouter()

const propsObj = defineProps({
  name: String,
  cover: String,
  roomId: Number,
  liveStatus: Number,
})

const itemClick = () => {
  router.push("/room/" + propsObj.roomId)
}
</script>

<style lang="scss" scoped>
.history-item {
  width: 248px;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.history-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 24px 44px rgba(15, 23, 42, 0.1);
}

.history-item__cover-wrap {
  position: relative;
}

.history-item__badge {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.history-item__cover {
  display: block;
  width: 100%;
  height: 152px;
  object-fit: cover;
}

.history-item__info {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px 16px 16px;
}

.history-item__avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.history-item__meta {
  min-width: 0;
}

.history-item__meta strong,
.history-item__meta span {
  display: block;
}

.history-item__meta strong {
  color: #0f172a;
  font-size: 15px;
}

.history-item__meta span {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}
</style>
