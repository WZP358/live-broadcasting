<template>
  <div class="center-page">
    <section class="profile-hero">
      <img draggable="false" :src="userAvatar" class="hero-avatar" />
      <div class="hero-info">
        <div class="hero-top">
          <h1>{{ userDisplayName }}</h1>
          <a-tag color="blue">{{ isAdmin ? "管理员账号" : "用户账号" }}</a-tag>
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

const store = useStore()
const route = useRouter()
const userStore = store.user()
const userInfo = computed(() => userStore.userInfo || {})
const isAdmin = computed(() => userStore.isAdmin)
const userAvatar = computed(() => userInfo.value.avatar || "https://dummyimage.com/160x160/e2e8f0/64748b&text=LIVE")
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
    label: "直播中心",
    title: "直播中心",
    children: [{ key: "live-settings", label: "开播设置", title: "开播设置" }],
  },
  {
    key: "dollar",
    icon: () => h(WalletOutlined),
    label: "钱包资产",
    title: "钱包资产",
    children: [
      { key: "wallet", label: "我的钱包", title: "我的钱包" },
      { key: "recharge", label: "充值", title: "充值" },
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
  },
])
</script>

<style lang="scss" scoped>
.center-page {
  max-width: 1340px;
  margin: 0 auto;
  padding: 20px;
}

.profile-hero,
.center-menu,
.center-content {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
}

.profile-hero {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 28px;
  background:
    radial-gradient(circle at right top, rgba(59, 130, 246, 0.16), transparent 24%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
}

.hero-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.1);
}

.hero-info {
  flex: 1;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-top h1 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
}

.hero-info p {
  margin: 10px 0 0;
  color: #64748b;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.hero-stat {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.hero-stat span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.hero-stat strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 16px;
}

.center-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  margin-top: 20px;
  align-items: start;
}

.center-menu {
  padding: 16px 0;
}

.center-content {
  min-height: 640px;
  min-width: 0;
  overflow: hidden;
}

.center-content__inner {
  padding: 18px;
}

@media (max-width: 960px) {
  .center-page {
    padding: 18px 16px 28px;
  }

  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats,
  .center-body {
    grid-template-columns: 1fr;
  }
}
</style>
