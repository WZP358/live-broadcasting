<template>
  <AdminPageLayout title="控制台" description="统一查看直播平台的核心运行指标、常用后台入口和当前运营重点。">
    <template #header-extra>
      <a-space>
        <a-button @click="loadSummary">刷新数据</a-button>
        <a-button type="primary" @click="go('/system/live-center/live-room-manage')">进入直播间管理</a-button>
      </a-space>
    </template>

    <AdminStatGrid :items="metricCards" />

    <div class="dashboard-grid">
      <AdminCard title="运营总览" subtitle="把关键指标拆成摘要卡，方便管理员快速判断平台健康状态。">
        <div class="admin-summary-list">
          <div v-for="item in metricCards" :key="item.key" class="admin-summary-item">
            <span class="admin-summary-item__label">{{ item.label }}</span>
            <span class="admin-summary-item__value">{{ item.value }}</span>
          </div>
        </div>
      </AdminCard>

      <AdminCard title="常用入口" subtitle="高频后台动作统一沉淀到工作台，减少多级菜单跳转。">
        <div class="admin-link-grid">
          <article v-for="item in quickLinks" :key="item.path" class="admin-link-card" @click="go(item.path)">
            <div>
              <div class="admin-avatar-meta__title">{{ item.title }}</div>
              <div class="admin-avatar-meta__desc">{{ item.desc }}</div>
            </div>
            <a-button type="link">前往</a-button>
          </article>
        </div>
      </AdminCard>
    </div>

    <div class="dashboard-grid dashboard-grid--secondary">
      <AdminCard title="本轮重点" subtitle="聚焦当前最影响运营效率和用户体验的事项。">
        <div class="focus-list">
          <article v-for="item in focusItems" :key="item.title" class="focus-item">
            <div class="focus-item__meta">
              <h4>{{ item.title }}</h4>
              <p>{{ item.description }}</p>
            </div>
            <a-tag :color="item.color">{{ item.status }}</a-tag>
          </article>
        </div>
      </AdminCard>

      <AdminCard title="运营建议" subtitle="结合当前平台状态，给出继续完善直播运营的方向。">
        <ul class="dashboard-notes">
          <li>保持登录入口清晰，根据账号角色进入对应的观看、开播或管理页面。</li>
          <li>高频管理动作优先放在列表页和工作台，减少运营人员来回跳转。</li>
          <li>开播、观看、降噪和字幕能力要保持稳定，并持续优化主播侧提示。</li>
        </ul>
      </AdminCard>
    </div>
  </AdminPageLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminStatGrid from "@/components/admin/AdminStatGrid.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import dashboardApi from "@/api/system/dashboard"
import { quickLinks } from "./quickLinks"

const router = useRouter()
const metrics = ref([])

const metricCards = computed(() =>
  metrics.value.map((item) => ({
    key: item.code,
    label: item.label,
    value: Number(item.value || 0),
    extra: item.description,
  }))
)

const focusItems = [
  {
    title: "管理列表优化",
    description: "统一筛选、批量操作、结果提示和分页体验，提升运营效率。",
    status: "进行中",
    color: "processing",
  },
  {
    title: "用户端体验优化",
    description: "继续统一个人中心、开播和直播间相关页面的品牌样式。",
    status: "持续优化",
    color: "gold",
  },
  {
    title: "直播能力增强",
    description: "持续完善实时字幕、降噪等主播辅助能力，让开播更省心。",
    status: "已接入",
    color: "success",
  },
]

const loadSummary = async () => {
  const res = await dashboardApi.getSummary()
  metrics.value = res?.data?.metrics || []
}

const go = (path) => {
  router.push(path)
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped lang="scss">
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 16px;
}

.dashboard-grid--secondary {
  grid-template-columns: 1fr 1fr;
}

.focus-list {
  display: grid;
  gap: 12px;
}

.focus-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: var(--bg-secondary);
}

.focus-item__meta h4 {
  margin: 0 0 6px;
  color: var(--admin-text);
  font-size: 15px;
}

.focus-item__meta p {
  margin: 0;
  color: var(--admin-text-secondary);
  line-height: 1.7;
}

.dashboard-notes {
  margin: 0;
  padding-left: 18px;
  color: var(--admin-text-secondary);
  line-height: 1.9;
}

@media (max-width: 1100px) {
  .dashboard-grid,
  .dashboard-grid--secondary {
    grid-template-columns: 1fr;
  }
}
</style>
