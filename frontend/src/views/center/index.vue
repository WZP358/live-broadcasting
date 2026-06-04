<template>
  <div class="center-page">
    <section class="profile-hero">
      <img draggable="false" :src="userAvatar" class="hero-avatar" @error="onImgError" />
      <div class="hero-info">
        <div class="hero-top">
          <h1>{{ userDisplayName }}</h1>
          <span class="account-pill">{{ isAdmin ? "运营账号" : "直播用户" }}</span>
        </div>
        <p>{{ userSignature }}</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span>账号</span>
            <strong>{{ userInfo.username || "-" }}</strong>
          </div>
          <div class="hero-stat">
            <span>邮箱</span>
            <strong>{{ userInfo.email || "未绑定" }}</strong>
          </div>
          <div class="hero-stat">
            <span>手机号</span>
            <strong>{{ userInfo.mobile || "未绑定" }}</strong>
          </div>
        </div>
        <div class="hero-actions">
          <button class="hero-action hero-action--primary" type="button" @click="route.push('/center/live/live-settings')">我要开播</button>
          <button class="hero-action" type="button" @click="route.push('/center/personnel/follow')">我的关注</button>
        </div>
      </div>
    </section>

    <div class="center-body">
      <aside class="center-menu">
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
import { computed, h, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { useStore } from "@/stores"
import { onImgError } from "@/utils/fallback"

const store = useStore()
const route = useRouter()
const userStore = store.user()
const userInfo = computed(() => userStore.userInfo || {})
const isAdmin = computed(() => userStore.isAdmin)
const userAvatar = computed(() => userInfo.value.avatar || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 160 160'%3E%3Crect fill='%23e2e8f0' width='160' height='160' rx='24'/%3E%3Ccircle fill='%2394a3b8' cx='80' cy='64' r='24'/%3E%3Cellipse fill='%2394a3b8' cx='80' cy='134' rx='40' ry='26'/%3E%3C/svg%3E")
const userDisplayName = computed(() => userInfo.value.nickName || userInfo.value.nickname || userInfo.value.username || "直播用户")
const userSignature = computed(() => userInfo.value.signature || "保持稳定开播，持续打磨内容与直播体验。")

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
      { key: "live-settings", label: "我要开播", title: "我要开播" },
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
      { key: "punishment", label: "运营奖惩", title: "运营奖惩" },
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
</script>

<style lang="scss" scoped>
.center-page { max-width: 1340px; margin: 0 auto; padding: 20px; }
.profile-hero {
  position: relative;
  overflow: hidden;
  display: flex; gap: 20px; align-items: center; padding: 24px;
  border: 1px solid rgba(255, 216, 77, 0.18);
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(255, 153, 0, 0.17), rgba(255, 153, 0, 0.03)),
    #171b24;
  box-shadow: 0 12px 30px rgba(18, 20, 28, 0.12);
}
.profile-hero::after {
  position: absolute;
  right: 28px;
  bottom: -38px;
  width: 190px;
  height: 110px;
  border: 1px solid rgba(255, 216, 77, 0.2);
  border-radius: 8px 8px 0 0;
  background: rgba(255, 255, 255, 0.04);
  content: "";
}
.hero-avatar {
  position: relative;
  z-index: 1;
  width: 80px; height: 80px; border-radius: 50%; object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 3px rgba(255, 216, 77, 0.26);
}
.hero-info { position: relative; z-index: 1; flex: 1; }
.hero-top { display: flex; align-items: center; gap: 10px; }
.hero-top h1 { margin: 0; font-size: 24px; color: #fff; }
.account-pill {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 4px;
  color: #ffd84d;
  background: rgba(255, 216, 77, 0.14);
  font-size: 12px;
  font-weight: 900;
}
.hero-info > p { margin: 6px 0 0; color: rgba(255, 255, 255, 0.66); font-size: 13px; }
.hero-stats {
  display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 16px;
}
.hero-stat {
  padding: 10px 12px; border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 6px; background: rgba(255, 255, 255, 0.07);
  span { display: block; color: rgba(255, 255, 255, 0.58); font-size: 12px; }
  strong { display: block; margin-top: 4px; color: #fff; font-size: 14px; }
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
.hero-action {
  height: 34px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 17px;
  color: rgba(255, 255, 255, 0.82);
  background: rgba(255, 255, 255, 0.08);
  font-weight: 800;
  cursor: pointer;
}
.hero-action--primary {
  border-color: #ffb020;
  color: #171b24;
  background: linear-gradient(135deg, #ffd84d, #ff9900);
}
.center-body {
  display: grid; grid-template-columns: 210px minmax(0, 1fr); gap: 16px;
  margin-top: 16px; align-items: start;
}
.center-menu {
  padding: 8px 0;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
  position: sticky; top: 80px;
}
.center-content {
  min-height: 640px; min-width: 0; overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}
.center-content__inner { padding: 20px; }

:deep(.ant-menu-light.ant-menu-inline) {
  border-inline-end: 0;
}

:deep(.ant-menu-item-selected) {
  color: var(--accent);
  background: var(--accent-light);
  font-weight: 800;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  color: var(--accent);
}
@media (max-width: 960px) {
  .center-page { padding: 14px; }
  .profile-hero { flex-direction: column; align-items: flex-start; }
  .hero-stats, .center-body { grid-template-columns: 1fr; }
  .center-menu { position: static; }
}
</style>
