<template>
  <aside class="side-column">
    <section class="side-card">
      <div class="side-title">
        <h3>热度榜</h3>
        <span>实时</span>
      </div>
      <button v-for="(room, index) in hotRanking" :key="room.id" class="rank-row" type="button" @click="$emit('enter', room.id)">
        <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
        <span class="rank-copy">
          <strong>{{ room.title || '直播间' }}</strong>
          <em>{{ getAnchorName(room) }} · {{ room.categoryInfo?.name || '推荐' }}</em>
        </span>
        <span class="rank-heat">{{ formatHeat(getRoomHeat(room)) }}</span>
      </button>
    </section>

    <section v-if="giftRank.length" class="side-card">
      <div class="side-title">
        <h3>打赏总榜</h3>
        <span>Top 10</span>
      </div>
      <div v-for="item in giftRank" :key="item.userId" class="rank-row">
        <span class="rank-no" :class="{ top: item.rank <= 3 }">{{ item.rank }}</span>
        <span class="gift-user">
          <img :src="safeAvatar(item.avatar)" class="gift-avatar" @error="onImgError" />
          <strong>{{ item.nickname || '未知用户' }}</strong>
        </span>
        <span class="rank-heat">¥{{ formatAmount(item.amount) }}</span>
      </div>
    </section>

    <section v-if="historyRooms.length" class="side-card">
      <div class="side-title">
        <h3>最近看过</h3>
        <span>继续观看</span>
      </div>
      <button v-for="item in historyRooms" :key="item.roomId || item.id" class="history-row" type="button" @click="$emit('enter', item.roomId || item.id)">
        <img :src="safeCover(item.cover)" alt="" @error="(e) => onImgError(e, fallbackCover)" />
        <span>
          <strong>{{ item.title || '直播间' }}</strong>
          <em>{{ item.userNickname || item.userInfo?.nickName || '继续观看' }}</em>
        </span>
      </button>
    </section>
  </aside>
</template>

<script setup>
import { formatHeat, getAnchorName, getRoomHeat } from '@/utils/liveRoomPresenter';
import { FALLBACK_AVATAR, onImgError, resolveSafeImageUrl } from '@/utils/fallback';

const formatAmount = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
};

const safeAvatar = (url) => resolveSafeImageUrl(url, FALLBACK_AVATAR);
const safeCover = (url) => resolveSafeImageUrl(url, props.fallbackCover);

const props = defineProps({
  fallbackCover: { type: String, required: true },
  historyRooms: { type: Array, default: () => [] },
  hotRanking: { type: Array, default: () => [] },
  giftRank: { type: Array, default: () => [] },
});

defineEmits(['enter']);
</script>

<style scoped lang="scss">
.side-column {
  display: grid;
  min-width: 0;
  gap: 10px;
}

.side-card {
  overflow: hidden;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 5px;
}

.side-card h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 900;
}

.side-title span {
  color: var(--text-muted);
  font-size: 12px;
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
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  transition:
    background 0.16s ease,
    padding 0.16s ease;
}

.rank-row:hover {
  margin: 0 -12px;
  padding-right: 12px;
  padding-left: 12px;
  background: var(--accent-light);
}

.rank-row:last-child {
  border-bottom: 0;
}

.rank-no {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 900;
  text-align: center;
}

.rank-no.top {
  color: var(--accent);
}

.rank-copy,
.gift-user {
  min-width: 0;
}

.rank-copy strong,
.rank-copy em,
.history-row strong,
.history-row em,
.gift-user strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-copy strong,
.history-row strong,
.gift-user strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.rank-copy em,
.history-row em {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.rank-heat {
  color: var(--accent);
  font-size: 12px;
  font-weight: 900;
}

.history-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px 0;
}

.history-row img {
  width: 86px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}

.gift-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gift-avatar {
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
  background: var(--bg-secondary);

  &--placeholder {
    display: grid;
    place-items: center;
    color: var(--text-muted);
    font-size: 12px;
    font-weight: 800;
  }
}
</style>
