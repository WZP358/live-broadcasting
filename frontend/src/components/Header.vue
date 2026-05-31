<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { BellOutlined, DownOutlined } from "@ant-design/icons-vue"
import { useStore } from "@/stores"
import { useNotificationStore } from "@/stores/modules/notification"
import { useNotificationSocket } from "@/composables/useNotificationSocket"
import liveApi from "@/api/live"
import levelApi from "@/api/level"

const router = useRouter()
const store = useStore()

const visible = ref(false)
const categories = ref([])

const userStore = store.user()
const notificationStore = useNotificationStore()
const { connect: connectNotificationWs, disconnect: disconnectNotificationWs } = useNotificationSocket()
const userInfo = computed(() => userStore.userInfo || {})
const isLogin = computed(() => userStore.isLogin)
const hasAdminRole = computed(() => userStore.isAdmin)
const currentCategory = computed(() => store.web().category.currentSelect)
const displayName = computed(() => userInfo.value?.nickName || userInfo.value?.nickname || userInfo.value?.username || "用户")
const userLevel = ref(0)
const unreadCount = computed(() => notificationStore.unreadCount)

const levelName = (lv) => {
  if (!lv) return ''
  if (lv >= 50) return 'Lv.' + lv
  if (lv >= 40) return 'Lv.' + lv
  if (lv >= 30) return 'Lv.' + lv
  if (lv >= 20) return 'Lv.' + lv
  return 'Lv.' + lv
}

const loadLevel = async () => {
  try {
    const res = await levelApi.getMyLevel()
    if (res.data) {
      userLevel.value = res.data.level || (res.data.userLevel && res.data.userLevel.level) || 0
    }
  } catch (e) { /* ignore */ }
}

watch(isLogin, (val) => {
  if (val) {
    connectNotificationWs()
    notificationStore.fetchUnreadCount()
  } else {
    disconnectNotificationWs()
  }
})

onMounted(async () => {
  try {
    const res = await liveApi.listCategories({})
    categories.value = res?.data?.list || []
  } catch (error) {
    categories.value = []
  }
  if (isLogin.value) {
    connectNotificationWs()
    notificationStore.fetchUnreadCount()
    loadLevel()
  }
})

const handleCategoryClick = (item) => {
  store.web().selectCategory(item)
  visible.value = false
  router.push("/")
}

const handleSelectAll = () => {
  store.web().selectCategory(null)
  visible.value = false
  router.push("/")
}

const handleLogin = () => {
  router.push("/login")
}

const handleGoHome = () => {
  router.push("/")
}

const handleGoCenter = () => {
  router.push("/center")
}

const handleGoFollow = () => {
  router.push("/center/personnel/follow")
}

const handleGoHistory = () => {
  router.push("/center/personnel/history")
}

const handleGoLiveCenter = () => {
  router.push("/center/live/live-settings")
}

const handleGoAdmin = () => {
  router.push("/system/dashboard")
}

const handleLogout = () => {
  userStore.logout()
}

const handleGoMessages = () => {
  router.push("/center/messages")
}
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner">
      <div class="site-header__brand" @click="handleGoHome">
        <div class="brand-mark">A</div>
        <div class="brand-copy">
          <strong>PulseLive</strong>
          <span>看直播，上 PulseLive</span>
        </div>
      </div>

      <nav class="site-header__nav">
        <button class="nav-item" type="button" @click="handleGoHome">直播</button>
        <a-popover v-model:open="visible" placement="bottom">
          <template #content>
            <div class="category-panel">
              <button class="category-pill" type="button" @click="handleSelectAll">全部分类</button>
              <button
                v-for="item in categories"
                :key="item.id"
                class="category-pill"
                type="button"
                @click="handleCategoryClick(item)"
              >
                {{ item.name }}
              </button>
            </div>
          </template>
          <button class="nav-item nav-item--highlight" type="button">
            {{ currentCategory?.name || "分类" }}
          </button>
        </a-popover>
        <button class="nav-item" type="button" @click="handleGoFollow">关注</button>
        <button class="nav-item" type="button" @click="handleGoHistory">历史</button>
        <button class="nav-item nav-item--start" type="button" @click="handleGoLiveCenter">开播</button>
      </nav>

      <div class="site-header__actions">
        <template v-if="!isLogin">
          <a-button type="primary" size="large" @click="handleLogin">登录</a-button>
        </template>
        <template v-else>
          <a-badge :count="unreadCount" :overflow-count="99" :number-style="{ backgroundColor: '#ff4d4f' }">
            <button class="notify-bell" type="button" @click="handleGoMessages">
              <BellOutlined :style="{ fontSize: '20px' }" />
            </button>
          </a-badge>
          <a-dropdown>
            <button class="user-entry" type="button">
              <img class="header-avatar" :src="userInfo?.avatar" alt="" />
              <span class="user-entry__text">
                <strong>{{ displayName }}</strong>
                <span>{{ hasAdminRole ? "管理员" : userLevel ? "Lv." + userLevel : "观众" }}</span>
              </span>
              <DownOutlined :style="{ fontSize: '12px' }" />
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="center">
                  <a @click="handleGoCenter">个人中心</a>
                </a-menu-item>
                <a-menu-item key="live">
                  <a @click="handleGoLiveCenter">开播中心</a>
                </a-menu-item>
                <a-menu-item key="admin" v-if="hasAdminRole">
                  <a @click="handleGoAdmin">管理后台</a>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <a @click="handleLogout">退出登录</a>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.site-header {
  position: sticky;
  top: 0;
  z-index: 998;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px);
}

.site-header__inner {
  max-width: 1280px;
  height: 76px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.site-header__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff9d1c, #ff5f00);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 21px;
  font-weight: 800;
  box-shadow: 0 12px 22px rgba(255, 128, 0, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  font-size: 18px;
  color: #0f172a;
}

.brand-copy span {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.site-header__nav {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.nav-item {
  height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s ease;
}

.nav-item:hover,
.nav-item--highlight {
  background: #fff4e5;
  color: #d96c00;
}

.nav-item--start {
  background: #ff8a00;
  color: #fff;
}

.nav-item--start:hover {
  background: #ff7a00;
  color: #fff;
}

.site-header__actions {
  display: flex;
  align-items: center;
}

.notify-bell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-right: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  transition: 0.2s ease;
}

.notify-bell:hover {
  border-color: #ffd199;
  color: #d96c00;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: 0.2s ease;
}

.user-entry:hover {
  border-color: #ffd199;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.header-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  background: #e2e8f0;
}

.user-entry__text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.user-entry__text strong {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-size: 14px;
}

.user-entry__text span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 12px;
}

.category-panel {
  width: 320px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-pill {
  padding: 8px 14px;
  border: 1px solid #ffe0b8;
  border-radius: 999px;
  background: #fffaf2;
  color: #d96c00;
  cursor: pointer;
}

@media (max-width: 960px) {
  .site-header__inner {
    height: auto;
    padding: 14px 16px;
    flex-wrap: wrap;
  }

  .site-header__nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }
}
</style>
