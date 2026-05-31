<template>
  <aside class="side-column">
    <section class="side-card start-card">
      <h3>开播中心</h3>
      <p>观众可以直接观看；开播、关注、送礼和个人资产需要登录。</p>
      <a-button type="primary" block @click="$emit('go-live-center')">我要开播</a-button>
    </section>

    <section class="side-card">
      <div class="side-title">
        <h3>站内热榜</h3>
        <span>按热度排序</span>
      </div>
      <button
        v-for="(room, index) in hotRanking"
        :key="room.id"
        class="rank-row"
        type="button"
        @click="$emit('enter', room.id)"
      >
        <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
        <div>
          <strong>{{ room.title || "直播间" }}</strong>
          <span>{{ getAnchorName(room) }} · {{ room.categoryInfo?.name || "推荐" }}</span>
        </div>
        <em>{{ formatHeat(getRoomHeat(room)) }}</em>
      </button>
    </section>

    <section class="side-card" v-if="historyRooms.length">
      <div class="side-title">
        <h3>最近看过</h3>
        <span>最多显示 5 条</span>
      </div>
      <button
        v-for="item in historyRooms"
        :key="item.roomId || item.id"
        class="history-row"
        type="button"
        @click="$emit('enter', item.roomId || item.id)"
      >
        <img :src="item.cover || fallbackCover" alt="" />
        <div>
          <strong>{{ item.title || "直播间" }}</strong>
          <span>{{ item.userNickname || item.userInfo?.nickName || "继续观看" }}</span>
        </div>
      </button>
    </section>
  </aside>
</template>

<script setup>
import { formatHeat, getAnchorName, getRoomHeat } from "@/utils/liveRoomPresenter";

defineProps({
  fallbackCover: {
    type: String,
    required: true,
  },
  historyRooms: {
    type: Array,
    default: () => [],
  },
  hotRanking: {
    type: Array,
    default: () => [],
  },
});

defineEmits(["enter", "go-live-center"]);
</script>

<style scoped lang="scss">
.side-column {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.side-card {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.side-card h3 {
  margin: 0;
  color: #1f2937;
}

.side-title span,
.start-card p {
  color: #909399;
  font-size: 13px;
}

.start-card p {
  line-height: 1.7;
}

.rank-row,
.history-row {
  width: 100%;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.rank-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}

.rank-row:last-child {
  border-bottom: 0;
}

.rank-no {
  color: #a8abb2;
  font-weight: 700;
}

.rank-no.top {
  color: #ff8a00;
}

.rank-row strong,
.history-row strong {
  display: block;
  overflow: hidden;
  color: #303133;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row span,
.history-row span {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.rank-row em {
  color: #ff8a00;
  font-style: normal;
  font-size: 12px;
}

.history-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 0;
}

.history-row img {
  width: 88px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}
</style>
