<template>
  <div class="center-page">
    <div class="center-body">
      <aside class="center-menu">
        <div class="menu-caption">个人中心</div>
        <a-menu :selectedKeys="current" :openKeys="current" mode="inline" :items="items" @click="handleClick" />
      </aside>
      <section class="center-content">
        <div class="center-content__inner">
          <RouterView />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { BarChartOutlined, MailOutlined, MessageOutlined, PlaySquareOutlined, WalletOutlined } from "@ant-design/icons-vue"
import { h, onMounted, ref } from "vue"
import { useRouter } from "vue-router"

const route = useRouter()

onMounted(() => {
  const path = route.currentRoute.value.path.split("/").slice(-2)
  current.value = path.length === 2 ? path : ["personnel", "profile"]
})

const handleClick = (e) => {
  current.value = e.keyPath
  if (e.keyPath.length === 1) {
    route.push("/center/" + e.keyPath[0])
  } else {
    route.push("/center/" + e.keyPath[0] + "/" + e.keyPath[1])
  }
}

const current = ref(["personnel", "profile"])
const items = ref([
  {
    key: "personnel",
    icon: () => h(MailOutlined),
    label: "账号中心",
    title: "账号中心",
    children: [
      { key: "profile", label: "个人信息", title: "个人信息" },
      { key: "follow", label: "我的关注", title: "我的关注" },
      { key: "history", label: "观看历史", title: "观看历史" },
    ],
  },
  {
    key: "live",
    icon: () => h(PlaySquareOutlined),
    label: "主播中心",
    title: "主播中心",
    children: [
      { key: "live-settings", label: "开播准备", title: "开播准备" },
      { key: "dashboard", label: "数据看板", title: "数据看板" },
      { key: "guardian", label: "守护管理", title: "守护管理" },
    ],
  },
  {
    key: "dollar",
    icon: () => h(WalletOutlined),
    label: "钱包资产",
    title: "钱包资产",
    children: [
      { key: "wallet", label: "我的钱包", title: "我的钱包" },
      { key: "recharge", label: "充值", title: "充值" },
      { key: "settlement", label: "收益结算", title: "收益结算" },
      { key: "bill", label: "交易记录", title: "交易记录" },
    ],
  },
  {
    key: "statistic",
    icon: () => h(BarChartOutlined),
    label: "直播数据",
    title: "直播数据",
    children: [
      { key: "overview", label: "数据总览", title: "数据总览" },
      { key: "gift-list", label: "礼物流水", title: "礼物流水" },
    ],
  },
  {
    key: "messages",
    icon: () => h(MessageOutlined),
    label: "消息中心",
    title: "消息中心",
    children: [
      { key: "pm", label: "私信", title: "私信" },
    ],
  },
])

const serviceMenu = items.value.find((item) => item.key === "messages")
if (serviceMenu && !serviceMenu.children?.some((item) => item.key === "customer-service")) {
  serviceMenu.children = serviceMenu.children || []
  serviceMenu.children.push({ key: "customer-service", label: "联系客服", title: "联系客服" })
}
</script>

<style lang="scss" scoped>
.center-page {
  max-width: 1500px;
  margin: 0 auto;
  padding: 16px 14px 30px;
  background:
    radial-gradient(circle at 12% 0, color-mix(in srgb, var(--accent) 7%, transparent), transparent 28%),
    transparent;
}

.center-body {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  min-height: calc(100vh - 72px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: color-mix(in srgb, var(--bg-card) 86%, var(--bg-primary));
  box-shadow: var(--shadow);
}

.center-menu {
  position: sticky;
  top: 60px;
  align-self: start;
  min-height: calc(100vh - 72px);
  padding: 24px 0;
  border-right: 1px solid var(--border);
  border-radius: 0;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent) 5%, transparent), transparent 190px),
    var(--bg-card);
  box-shadow: none;
}

.menu-caption {
  padding: 0 28px 18px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
}

.center-content {
  min-width: 0;
  overflow: hidden;
  background: var(--bg-card);
}

.center-content__inner {
  padding: 0;
}

:deep(.ant-menu-light.ant-menu-inline) {
  border-inline-end: 0;
}

:deep(.ant-menu-inline) {
  background: transparent !important;
}

:deep(.ant-menu-submenu-title),
:deep(.ant-menu-item) {
  width: calc(100% - 20px);
  height: 48px;
  margin: 3px 10px;
  border-radius: 8px;
  color: var(--text-secondary) !important;
  font-size: 16px;
  font-weight: 700;
}

:deep(.ant-menu-submenu-title .ant-menu-item-icon),
:deep(.ant-menu-item .ant-menu-item-icon) {
  color: var(--text-muted);
  font-size: 20px;
}

:deep(.ant-menu-item-selected) {
  color: var(--accent) !important;
  background: var(--accent-light) !important;
  font-weight: 900;
}

:deep(.ant-menu-item-selected::after) {
  inset-inline-start: 0;
  inset-inline-end: auto;
  border-inline-end: 0;
  border-left: 0;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  color: var(--accent) !important;
  background: var(--accent-light) !important;
}

:deep(.ant-menu-submenu .ant-menu-item) {
  padding-left: 52px !important;
  font-size: 15px;
}

@media (max-width: 960px) {
  .center-page {
    padding: 0;
  }

  .center-body {
    grid-template-columns: 1fr;
    border-inline: 0;
  }

  .center-menu {
    position: static;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }
}
</style>
