<template>
  <div class="dashboard-page">
    <section class="dashboard-header">
      <div>
        <h2>数据看板</h2>
        <p>查看直播间关注、人气、粉丝团和近期礼物收入。</p>
      </div>
      <a-button @click="loadData">刷新</a-button>
    </section>

    <section class="dashboard-content">
      <a-spin :spinning="loading">
        <a-empty v-if="!loading && rooms.length === 0" description="暂未创建直播间" />
        <div v-else class="room-list">
          <article v-for="room in rooms" :key="room.id" class="room-card">
            <header class="room-card__head">
              <div>
                <h3>{{ room.title || "直播间#" + room.id }}</h3>
                <span>房间 {{ room.id || "--" }}</span>
              </div>
              <a-tag :color="room.status === 1 ? 'orange' : 'default'">
                {{ room.status === 1 ? "直播中" : "未开播" }}
              </a-tag>
            </header>

            <div class="metric-grid">
              <div class="metric-item">
                <span>关注数</span>
                <strong>{{ room.watchCount || 0 }}</strong>
              </div>
              <div class="metric-item">
                <span>粉丝团</span>
                <strong>{{ room.fanCount || 0 }}</strong>
              </div>
              <div class="metric-item">
                <span>今日礼物收入</span>
                <strong>¥{{ Number(room.todayIncome || 0).toFixed(2) }}</strong>
              </div>
              <div class="metric-item">
                <span>人气</span>
                <strong>{{ room.popularity || 0 }}</strong>
              </div>
            </div>

            <div v-if="room.dailyIncome?.length" class="daily-chart">
              <span class="chart-title">近7天礼物收入</span>
              <div class="chart-bars">
                <div v-for="d in room.dailyIncome" :key="d.date" class="chart-bar-item">
                  <div
                    class="bar"
                    :style="{ height: maxIncome > 0 ? (d.income / maxIncome * 60) + 'px' : '0' }"
                    :title="'¥' + (d.income || 0)"
                  ></div>
                  <span class="bar-label">{{ d.date.slice(5) }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>
      </a-spin>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue"
import dashboardApi from "@/api/dashboard"
import $modal from "@/utils/message"

const rooms = ref([])
const loading = ref(false)
const maxIncome = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await dashboardApi.getMyRooms()
    const list = res.data && Array.isArray(res.data) ? res.data : []
    for (const room of list) {
      try {
        const statsRes = await dashboardApi.getRoomStats(room.id)
        if (statsRes.data) {
          room.dailyIncome = statsRes.data.dailyIncome || []
        }
      } catch (error) {
        room.dailyIncome = room.dailyIncome || []
      }
    }
    rooms.value = list
    const allIncome = list.flatMap((room) => (room.dailyIncome || []).map((item) => item.income || 0))
    maxIncome.value = allIncome.length > 0 ? Math.max(...allIncome, 1) : 1
  } catch (error) {
    $modal.msgError("数据加载失败")
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  display: grid;
  gap: 16px;
}

.dashboard-header,
.dashboard-content,
.room-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow);
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.dashboard-header h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.dashboard-header p {
  margin: 0;
  color: var(--text-secondary);
}

.dashboard-content {
  padding: 18px;
}

.room-list {
  display: grid;
  gap: 16px;
}

.room-card {
  padding: 18px;
  box-shadow: none;
}

.room-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.room-card__head h3 {
  margin: 0 0 4px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.room-card__head span,
.metric-item span,
.chart-title,
.bar-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  min-height: 72px;
  display: grid;
  align-content: center;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fafbfc;
}

.metric-item strong {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 900;
}

.daily-chart {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.chart-title {
  font-weight: 800;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 70px;
  margin-top: 8px;
}

.chart-bar-item {
  flex: 1;
  display: flex;
  align-items: center;
  flex-direction: column;
}

.bar {
  width: 24px;
  min-height: 2px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #ffd84d, #ff9900);
  transition: height 0.3s;
}

.bar-label {
  margin-top: 4px;
}

@media (max-width: 820px) {
  .dashboard-header,
  .room-card__head {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
