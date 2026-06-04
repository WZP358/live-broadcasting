<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  AppstoreOutlined,
  BellOutlined,
  CompassOutlined,
  DownOutlined,
  FireOutlined,
  HeartOutlined,
  HomeOutlined,
  SearchOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons-vue';
import { useStore } from '@/stores';
import { useNotificationStore } from '@/stores/modules/notification';
import { useNotificationSocket } from '@/composables/useNotificationSocket';
import liveApi from '@/api/live';
import levelApi from '@/api/level';
import ThemeSwitcher from '@/components/ThemeSwitcher.vue';
import { FALLBACK_AVATAR, onImgError } from '@/utils/fallback';

const router = useRouter();
const store = useStore();

const visible = ref(false);
const categories = ref([]);
const searchValue = ref('');

const userStore = store.user();
const notificationStore = useNotificationStore();
const { connect: connectNotificationWs, disconnect: disconnectNotificationWs } = useNotificationSocket();
const userInfo = computed(() => userStore.userInfo || {});
const isLogin = computed(() => userStore.isLogin);
const hasAdminRole = computed(() => userStore.isAdmin);
const currentCategory = computed(() => store.web().category.currentSelect);
const displayName = computed(() => userInfo.value?.nickName || userInfo.value?.nickname || userInfo.value?.username || '用户');
const userLevel = ref(0);
const unreadCount = computed(() => notificationStore.unreadCount);

const loadLevel = async () => {
  try {
    const res = await levelApi.getMyLevel();
    if (res.data) {
      userLevel.value = res.data.level || (res.data.userLevel && res.data.userLevel.level) || 0;
    }
  } catch (e) {
    // ignore
  }
};

watch(isLogin, (val) => {
  if (val) {
    connectNotificationWs();
    notificationStore.fetchUnreadCount();
  } else {
    disconnectNotificationWs();
  }
});

onMounted(async () => {
  try {
    const res = await liveApi.listCategories({});
    categories.value = res?.data?.list || [];
  } catch (error) {
    categories.value = [];
  }
  if (isLogin.value) {
    connectNotificationWs();
    notificationStore.fetchUnreadCount();
    loadLevel();
  }
});

const handleSearch = () => {
  const kw = searchValue.value.trim();
  if (kw) {
    router.push({ path: '/search', query: { keyword: kw } });
  }
};

const handleCategoryClick = (item) => {
  store.web().selectCategory(item);
  visible.value = false;
  router.push('/');
};

const handleSelectAll = () => {
  store.web().selectCategory(null);
  visible.value = false;
  router.push('/');
};

const handleLogin = () => {
  router.push('/login');
};

const handleGoHome = () => {
  router.push('/');
};

const handleGoDiscover = () => {
  store.web().selectCategory(null);
  router.push('/');
};

const handleGoCenter = () => {
  router.push('/center');
};

const handleGoFollow = () => {
  router.push('/center/personnel/follow');
};

const handleGoLiveCenter = () => {
  router.push('/live/studio');
};

const handleGoAdmin = () => {
  router.push('/system/dashboard');
};

const handleLogout = () => {
  userStore.logout();
};

const handleGoMessages = () => {
  router.push('/center/messages');
};

const getCategoryInitial = (name = '') => {
  const value = String(name).trim();
  return value ? value.slice(0, 1) : '全';
};
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner">
      <button class="site-header__brand" type="button" @click="handleGoHome">
        <span class="brand-mark">PL</span>
        <span class="brand-copy">
          <strong>PulseLive</strong>
          <span>弹幕互动直播平台</span>
        </span>
      </button>

      <nav class="site-header__nav">
        <button class="nav-item" :class="{ active: $route.path === '/' || $route.path === '/home' }" type="button" @click="handleGoHome">
          <HomeOutlined />
          首页
        </button>

        <a-popover v-model:open="visible" placement="bottom" trigger="click">
          <template #content>
            <div class="category-panel">
              <div class="category-panel__head">
                <strong>全部分类</strong>
                <span>{{ categories.length }} 个分区</span>
              </div>
              <div class="category-grid">
                <button class="category-card" type="button" @click="handleSelectAll">
                  <span class="category-card__icon">
                    <AppstoreOutlined />
                  </span>
                  <span>
                    <strong>全部直播</strong>
                    <em>正在热播</em>
                  </span>
                </button>
                <button v-for="item in categories" :key="item.id" class="category-card" type="button" @click="handleCategoryClick(item)">
                  <span class="category-card__icon">{{ getCategoryInitial(item.name) }}</span>
                  <span>
                    <strong>{{ item.name }}</strong>
                    <em>进入分区</em>
                  </span>
                </button>
              </div>
            </div>
          </template>
          <button class="nav-item nav-item--accent" :class="{ active: currentCategory }" type="button">
            <AppstoreOutlined />
            {{ currentCategory?.name || '分类' }}
            <DownOutlined class="nav-caret" />
          </button>
        </a-popover>

        <button class="nav-item" type="button" @click="handleGoFollow">
          <HeartOutlined />
          关注
        </button>
        <button class="nav-item" type="button" @click="handleGoDiscover">
          <CompassOutlined />
          发现
        </button>
      </nav>

      <div class="site-header__search">
        <a-input-search
          v-model:value="searchValue"
          placeholder="搜索主播、房间标题、分区"
          size="large"
          @search="handleSearch"
          @press-enter="handleSearch">
          <template #enterButton>
            <a-button type="primary" size="large">
              <SearchOutlined />
            </a-button>
          </template>
        </a-input-search>
      </div>

      <div class="site-header__actions">
        <button class="start-live" type="button" @click="handleGoLiveCenter">
          <VideoCameraOutlined />
          开播
        </button>
        <ThemeSwitcher />
        <template v-if="!isLogin">
          <a-button class="signin-btn" type="primary" size="large" @click="handleLogin">登录 / 注册</a-button>
        </template>
        <template v-else>
          <a-badge :count="unreadCount" :overflow-count="99" :number-style="{ backgroundColor: 'var(--danger)' }">
            <button class="notify-bell" type="button" @click="handleGoMessages">
              <BellOutlined />
            </button>
          </a-badge>
          <a-dropdown>
            <button class="user-entry" type="button">
              <img class="header-avatar" :src="userInfo?.avatar || FALLBACK_AVATAR" alt="" @error="onImgError" />
              <span class="user-entry__text">
                <strong>{{ displayName }}</strong>
                <span>{{ hasAdminRole ? '管理员' : userLevel ? 'Lv.' + userLevel : '观众' }}</span>
              </span>
              <DownOutlined class="user-caret" />
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="center">
                  <a @click="handleGoCenter">个人中心</a>
                </a-menu-item>
                <a-menu-item key="live">
                  <a @click="handleGoLiveCenter">开播中心</a>
                </a-menu-item>
                <a-menu-item v-if="hasAdminRole" key="admin">
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

      <button class="mobile-start-btn" type="button" @click="handleGoLiveCenter">
        <VideoCameraOutlined />
      </button>
    </div>
  </header>
</template>

<style scoped lang="scss">
.site-header {
  position: sticky;
  top: 0;
  z-index: 998;
  background: var(--bg-header);
  box-shadow: 0 1px 0 color-mix(in srgb, var(--header-text) 12%, transparent);
}

.site-header__inner {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 1480px;
  height: 60px;
  margin: 0 auto;
  padding: 0 20px;
}

.site-header__brand {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  color: var(--header-text-active);
  background: transparent;
  cursor: pointer;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: var(--accent-gradient);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--accent) 26%, transparent);
  font-size: 13px;
  font-weight: 900;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
  text-align: left;
  white-space: nowrap;
}

.brand-copy strong {
  font-size: 16px;
  letter-spacing: 0;
}

.brand-copy span {
  margin-top: 2px;
  color: var(--header-text);
  font-size: 11px;
}

.site-header__nav {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 38px;
  padding: 0 12px;
  border: 0;
  border-radius: 6px;
  color: var(--header-text);
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease;
}

.nav-item:hover,
.nav-item.active {
  color: var(--header-text-active);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
}

.nav-item--accent {
  color: var(--accent);
}

.nav-caret,
.user-caret {
  font-size: 10px;
}

.site-header__search {
  flex: 1 1 360px;
  max-width: 440px;
  min-width: 220px;

  :deep(.ant-input-search) {
    .ant-input-group {
      border-radius: 19px;
      overflow: hidden;
      background: color-mix(in srgb, var(--header-text) 10%, transparent);
    }

    .ant-input {
      height: 38px;
      border: 0;
      border-radius: 19px 0 0 19px;
      color: var(--header-text-active) !important;
      background: color-mix(in srgb, var(--header-text) 10%, transparent) !important;
      font-size: 13px;

      &::placeholder {
        color: var(--header-text) !important;
      }

      &:hover,
      &:focus {
        background: color-mix(in srgb, var(--header-text) 16%, transparent) !important;
        box-shadow: none !important;
      }
    }

    .ant-input-search-button {
      height: 38px;
      border: 0;
      border-radius: 0 19px 19px 0 !important;
      background: var(--accent);
    }
  }
}

.site-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.start-live,
.notify-bell,
.user-entry {
  border: 0;
  cursor: pointer;
  transition:
    background 0.16s ease,
    color 0.16s ease,
    border-color 0.16s ease;
}

.start-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--accent) 46%, transparent);
  border-radius: 18px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 16%, transparent);
  font-weight: 700;
}

.start-live:hover {
  color: var(--header-text-active);
  border-color: color-mix(in srgb, var(--accent) 76%, transparent);
  background: color-mix(in srgb, var(--accent) 28%, transparent);
}

.signin-btn {
  height: 36px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 700;
}

.notify-bell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: var(--header-text);
  background: transparent;
  font-size: 18px;
}

.notify-bell:hover {
  color: var(--header-text-active);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 10px 0 6px;
  border-radius: 19px;
  color: var(--header-text-active);
  background: color-mix(in srgb, var(--header-text) 10%, transparent);
}

.user-entry:hover {
  background: color-mix(in srgb, var(--header-text) 16%, transparent);
}

.header-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}

.user-entry__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  text-align: left;
}

.user-entry__text strong {
  max-width: 94px;
  overflow: hidden;
  color: var(--header-text-active);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-entry__text span {
  color: var(--header-text);
  font-size: 11px;
}

.category-panel {
  width: 430px;
  max-height: 390px;
  overflow-y: auto;
  padding: 4px;
  background: var(--bg-card);
}

.category-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.category-panel__head strong {
  color: var(--text-primary);
  font-size: 15px;
}

.category-panel__head span {
  color: var(--text-muted);
  font-size: 12px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--bg-card);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    border-color 0.16s ease,
    background 0.16s ease;
}

.category-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border));
  background: var(--accent-light);
}

.category-card__icon {
  display: grid;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  color: var(--accent);
  background: var(--accent-soft);
  font-size: 16px;
  font-weight: 800;
}

.category-card span:last-child {
  min-width: 0;
}

.category-card strong,
.category-card em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-card strong {
  font-size: 13px;
}

.category-card em {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
}

.mobile-start-btn {
  display: none;
}

@media (max-width: 1180px) {
  .brand-copy span,
  .start-live {
    display: none;
  }
}

@media (max-width: 940px) {
  .site-header__inner {
    height: auto;
    min-height: 60px;
    padding: 9px 14px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .site-header__nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .site-header__nav::-webkit-scrollbar {
    display: none;
  }

  .site-header__search {
    order: 4;
    flex-basis: 100%;
    max-width: none;
  }

  .mobile-start-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: 0;
    border-radius: 50%;
    color: var(--accent-text);
    background: var(--accent);
  }
}

@media (max-width: 620px) {
  .site-header__actions {
    margin-left: auto;
  }

  .user-entry__text,
  .signin-btn {
    display: none;
  }

  .category-panel {
    width: min(86vw, 430px);
  }

  .category-grid {
    grid-template-columns: 1fr;
  }
}
</style>
