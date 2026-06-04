<template>
  <AdminPageLayout title="系统管理" description="把角色、菜单、字典和配置入口整理成一个统一的系统管理工作台，减少后台维护时的跳转成本。">
    <template #header-extra>
      <a-space>
        <a-button @click="go('/system/dashboard')">返回控制台</a-button>
        <a-button type="primary" @click="go('/system/system-manage/system-manage-menu')">进入菜单管理</a-button>
      </a-space>
    </template>

    <div class="manage-hero">
      <div>
        <p class="manage-hero__eyebrow">SYSTEM CENTER</p>
        <h3>统一维护后台基础能力</h3>
        <p>
          当前系统管理页作为后台基础设施入口，负责承接菜单、角色、字典、系统配置等能力，
          后续可以继续扩展审计日志、参数管理、租户隔离等企业化功能。
        </p>
      </div>
      <div class="manage-hero__stats">
        <div v-for="item in summaryCards" :key="item.title" class="manage-stat">
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>
    </div>

    <AdminCard title="管理入口" subtitle="统一展示后台基础管理模块，方便快速进入常用功能。">
      <div class="entry-grid">
        <article v-for="entry in entries" :key="entry.path" class="entry-card">
          <div class="entry-card__head">
            <div>
              <h4>{{ entry.title }}</h4>
              <p>{{ entry.description }}</p>
            </div>
            <a-tag :color="entry.available ? 'success' : 'default'">
              {{ entry.available ? "可用" : "规划中" }}
            </a-tag>
          </div>
          <div class="entry-card__foot">
            <span>{{ entry.meta }}</span>
            <a-button :type="entry.available ? 'primary' : 'default'" :disabled="!entry.available" @click="go(entry.path)">
              进入
            </a-button>
          </div>
        </article>
      </div>
    </AdminCard>

    <div class="manage-grid">
      <AdminCard title="推荐动作" subtitle="当前版本更适合优先完成这些基础治理工作。">
        <ul class="manage-list">
          <li>先完善菜单、角色与权限映射，确保后台导航和页面权限一致。</li>
          <li>完善配置保存与复查流程，避免运营设置在切换设备后不一致。</li>
          <li>字典能力收口后，可继续扩展到礼物状态、直播审核状态等公共枚举。</li>
        </ul>
      </AdminCard>

      <AdminCard title="当前状态" subtitle="用于说明哪些模块已经可用，哪些仍在规划。">
        <div class="admin-summary-list">
          <div class="admin-summary-item">
            <span class="admin-summary-item__label">菜单管理</span>
            <span class="admin-summary-item__value">已统一到后台表格框架</span>
          </div>
          <div class="admin-summary-item">
            <span class="admin-summary-item__label">角色管理</span>
            <span class="admin-summary-item__value">已支持统一列表结构</span>
          </div>
          <div class="admin-summary-item">
            <span class="admin-summary-item__label">字典与配置</span>
            <span class="admin-summary-item__value">本轮已完成视觉与交互收口</span>
          </div>
        </div>
      </AdminCard>
    </div>
  </AdminPageLayout>
</template>

<script setup>
import { useRouter } from "vue-router"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"

const router = useRouter()

const summaryCards = [
  { title: "基础模块", value: "4", hint: "菜单、角色、字典、配置" },
  { title: "管理方式", value: "统一维护", hint: "筛选、操作、列表集中处理" },
  { title: "体验目标", value: "清晰高效", hint: "统一后台视觉与交互语言" },
]

const entries = [
  {
    title: "菜单管理",
    description: "维护后台导航结构、图标、排序和可见状态。",
    meta: "用于后台信息架构治理",
    path: "/system/system-manage/system-manage-menu",
    available: true,
  },
  {
    title: "角色管理",
    description: "维护角色名称、状态和后续权限分配入口。",
    meta: "用于账号角色治理",
    path: "/system/system-manage/user-role-manage",
    available: true,
  },
  {
    title: "字典管理",
    description: "维护公共枚举和值映射，供各业务模块复用。",
    meta: "用于公共基础配置",
    path: "/system/system-manage/dictionary-manage",
    available: true,
  },
  {
    title: "系统配置",
    description: "集中管理站点、安全、登录和直播参数。",
    meta: "用于运行参数维护",
    path: "/system/system-manage/system-config",
    available: true,
  },
]

const go = (path) => {
  router.push(path)
}
</script>

<style scoped lang="scss">
.manage-hero {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background:
    linear-gradient(90deg, rgba(64, 158, 255, 0.08), rgba(64, 158, 255, 0.02)),
    #fff;
  box-shadow: var(--admin-shadow);
}

.manage-hero__eyebrow {
  margin: 0;
  color: var(--admin-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.manage-hero h3 {
  margin: 10px 0 8px;
  color: var(--admin-text);
  font-size: 28px;
}

.manage-hero p {
  margin: 0;
  color: var(--admin-text-secondary);
  line-height: 1.8;
}

.manage-hero__stats {
  display: grid;
  gap: 12px;
}

.manage-stat {
  padding: 16px 18px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: #fafcff;
}

.manage-stat span,
.manage-stat small {
  display: block;
  color: var(--admin-text-secondary);
}

.manage-stat strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--admin-text);
  font-size: 26px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.entry-card {
  padding: 18px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: #fff;
}

.entry-card__head,
.entry-card__foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.entry-card__head {
  align-items: flex-start;
}

.entry-card__head h4 {
  margin: 0 0 6px;
  color: var(--admin-text);
}

.entry-card__head p,
.entry-card__foot span {
  margin: 0;
  color: var(--admin-text-secondary);
  line-height: 1.7;
}

.entry-card__foot {
  align-items: center;
  margin-top: 16px;
}

.manage-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.manage-list {
  margin: 0;
  padding-left: 18px;
  color: var(--admin-text-secondary);
  line-height: 1.9;
}

@media (max-width: 1100px) {
  .manage-hero,
  .manage-grid,
  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
