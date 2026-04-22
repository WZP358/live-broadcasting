<template>
  <AdminPageLayout title="控制台" description="统一查看直播平台的核心运行指标、常用后台入口和当前治理重点，整体布局向若依后台的工作台体验收口。">
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
      <AdminCard title="本轮重点" subtitle="先把当前项目最影响可用性的部分收进标准后台结构中。">
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

      <AdminCard title="治理建议" subtitle="结合当前项目状态，给出继续完善后台和前台的落地方向。">
        <ul class="dashboard-notes">
          <li>统一登录入口已经合并，后续只保留按角色分流，不再维护双登录页。</li>
          <li>后台列表页优先复用同一套查询区、工具栏、表格容器和分页组件。</li>
          <li>直播链路相关页面尽量只做外层重构，不破坏推流、播放、降噪和字幕能力。</li>
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
    title: "后台列表标准化",
    description: "统一查询卡、工具栏、结果提示和分页结构，提升维护效率。",
    status: "进行中",
    color: "processing",
  },
  {
    title: "用户端视觉重做",
    description: "继续把个人中心内页和直播相关页面改成统一品牌样式。",
    status: "持续优化",
    color: "gold",
  },
  {
    title: "直播能力增强",
    description: "在现有开播链路上兼容字幕、实时降噪等增强能力。",
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
  background: #fafcff;
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
