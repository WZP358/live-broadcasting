<template>
  <div class="dashboard-page">
    <a-spin :spinning="loading">
      <a-empty v-if="!loading && rooms.length === 0" description="暂未创建直播间" />
      <a-row :gutter="16" v-else>
        <a-col :span="24" v-for="room in rooms" :key="room.id">
          <a-card size="small" class="room-card">
            <template #title>
              <span>{{ room.title || '直播间#' + room.id }}</span>
              <a-tag :color="room.status === 1 ? 'green' : 'default'" style="margin-left:8px">
                {{ room.status === 1 ? '直播中' : '未开播' }}
              </a-tag>
            </template>
            <a-row :gutter="16">
              <a-col :span="6">
                <a-statistic title="关注数" :value="room.watchCount || 0" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="粉丝团" :value="room.fanCount || 0" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="今日礼物收入" :value="room.todayIncome || 0" prefix="¥" :precision="2" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="人气" :value="room.popularity || 0" />
              </a-col>
            </a-row>
            <a-divider style="margin:12px 0" />
            <div class="daily-chart" v-if="room.dailyIncome">
              <span class="chart-title">近7天礼物收入</span>
              <div class="chart-bars">
                <div class="chart-bar-item" v-for="d in room.dailyIncome" :key="d.date">
                  <div class="bar" :style="{ height: maxIncome > 0 ? (d.income / maxIncome * 60) + 'px' : '0' }" :title="'¥' + (d.income || 0)"></div>
                  <span class="bar-label">{{ d.date.slice(5) }}</span>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dashboardApi from '@/api/dashboard'
import $modal from '@/utils/message'

const rooms = ref([])
const loading = ref(false)
const maxIncome = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await dashboardApi.getMyRooms()
    const list = (res.data && Array.isArray(res.data)) ? res.data : []
    // 加载每个房间的详细统计
    for (const room of list) {
      try {
        const statsRes = await dashboardApi.getRoomStats(room.id)
        if (statsRes.data) {
          room.dailyIncome = statsRes.data.dailyIncome || []
        }
      } catch (e) { /* ignore stats errors */ }
    }
    rooms.value = list
    // 计算最大收入用于柱状图
    const allIncome = list.flatMap(r => (r.dailyIncome || []).map(d => d.income || 0))
    maxIncome.value = allIncome.length > 0 ? Math.max(...allIncome, 1) : 1
  } catch (e) {
    $modal.msgError('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.dashboard-page { padding: 8px 0; }
.room-card { margin-bottom: 16px; }
.chart-title { font-size: 13px; color: #666; }
.chart-bars { display: flex; gap: 12px; align-items: flex-end; margin-top: 8px; height: 70px; }
.chart-bar-item { display: flex; flex-direction: column; align-items: center; flex: 1; }
.bar { width: 24px; background: linear-gradient(180deg, #1890ff, #69c0ff); border-radius: 4px 4px 0 0; min-height: 2px; transition: height 0.3s; }
.bar-label { font-size: 10px; color: #999; margin-top: 4px; }
</style>
