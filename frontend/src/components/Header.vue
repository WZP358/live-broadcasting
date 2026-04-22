<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { DownOutlined } from "@ant-design/icons-vue"
import { useStore } from "@/stores"
import liveApi from "@/api/live"

const router = useRouter()
const store = useStore()

const visible = ref(false)
const categories = ref([])

const userStore = store.user()
const userInfo = computed(() => userStore.userInfo || {})
const isLogin = computed(() => userStore.isLogin)
const hasAdminRole = computed(() => userStore.isAdmin)
const currentCategory = computed(() => store.web().category.currentSelect)
const displayName = computed(() => userInfo.value?.nickName || userInfo.value?.nickname || userInfo.value?.username || "用户")

onMounted(async () => {
  const res = await liveApi.listCategories({})
  categories.value = res?.data?.list || []
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

const handleGoLiveCenter = () => {
  router.push("/center/live/live-settings")
}

const handleGoAdmin = () => {
  router.push("/system/dashboard")
}

const handleLogout = () => {
  userStore.logout()
}
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner">
      <div class="site-header__brand" @click="handleGoHome">
        <div class="brand-mark">A</div>
        <div class="brand-copy">
          <strong>Ant Live</strong>
          <span>直播与运营一体平台</span>
        </div>
      </div>

      <nav class="site-header__nav">
        <button class="nav-item" type="button" @click="handleGoHome">发现直播</button>
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
            {{ currentCategory?.name || "频道分类" }}
          </button>
        </a-popover>
        <button v-if="isLogin" class="nav-item" type="button" @click="handleGoLiveCenter">我要开播</button>
      </nav>

      <div class="site-header__actions">
        <template v-if="!isLogin">
          <a-button type="primary" size="large" @click="handleLogin">登录 / 进入系统</a-button>
        </template>
        <template v-else>
          <a-dropdown>
            <button class="user-entry" type="button">
              <img class="header-avatar" :src="userInfo?.avatar" alt="" />
              <span class="user-entry__text">
                <strong>{{ displayName }}</strong>
                <span>{{ hasAdminRole ? "可进入后台" : "普通用户" }}</span>
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
  border-radius: 14px;
  background: linear-gradient(135deg, #1677ff, #0f4cdd);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 21px;
  font-weight: 800;
  box-shadow: 0 16px 30px rgba(22, 119, 255, 0.22);
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
  border-radius: 12px;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s ease;
}

.nav-item:hover,
.nav-item--highlight {
  background: #eef5ff;
  color: #0f4cdd;
}

.site-header__actions {
  display: flex;
  align-items: center;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: 0.2s ease;
}

.user-entry:hover {
  border-color: #bfdbfe;
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
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
  color: #2563eb;
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
