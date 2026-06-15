<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  BellOutlined,
  ClockCircleOutlined,
  HeartOutlined,
  HomeOutlined,
  SearchOutlined,
  GiftOutlined,
  MessageOutlined,
  UserOutlined,
  WalletOutlined,
  EditOutlined,
  LogoutOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons-vue';
import { useStore } from '@/stores';
import { useNotificationStore } from '@/stores/modules/notification';
import { useNotificationSocket } from '@/composables/useNotificationSocket';
import liveApi from '@/api/live';
import levelApi from '@/api/level';
import walletApi from '@/api/wallet';
import ThemeSwitcher from '@/components/ThemeSwitcher.vue';
import { FALLBACK_AVATAR, onImgError } from '@/utils/fallback';

const router = useRouter();
const store = useStore();

const historyOpen = ref(false);
const headerHistory = ref([]);
const historyLoading = ref(false);
const searchValue = ref('');

const userStore = store.user();
const notificationStore = useNotificationStore();
const { connect: connectNotificationWs, disconnect: disconnectNotificationWs } = useNotificationSocket();
const userInfo = computed(() => userStore.userInfo || {});
const isLogin = computed(() => userStore.isLogin);
const hasAdminRole = computed(() => userStore.isAdmin);
const displayName = computed(() => userInfo.value?.nickName || userInfo.value?.nickname || userInfo.value?.username || '用户');
const userLevel = ref(0);
const walletBalance = ref(0);
const unreadCount = computed(() => notificationStore.unreadCount);
const levelProgress = computed(() => `${Math.min(100, Math.max(0, Math.round(((userLevel.value || 0) % 10) * 10)))}%`);

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

const loadWalletBalance = async () => {
  try {
    const res = await walletApi.getBalance();
    walletBalance.value = Number(res?.data?.balance || 0);
  } catch (e) {
    walletBalance.value = 0;
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

onMounted(() => {
  if (isLogin.value) {
    connectNotificationWs();
    notificationStore.fetchUnreadCount();
    loadLevel();
    loadWalletBalance();
  }
});

const handleSearch = () => {
  const kw = searchValue.value.trim();
  if (kw) {
    router.push({ path: '/search', query: { keyword: kw } });
  }
};

const handleLogin = () => {
  router.push('/login');
};

const handleGoHome = () => {
  router.push('/');
};

const handleGoCenter = () => {
  router.push('/center');
};

const handleGoFollow = () => {
  router.push('/center/personnel/follow');
};

const handleGoHistory = () => {
  router.push('/center/personnel/history');
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

const handleGoRecharge = () => {
  router.push('/center/dollar/recharge');
};

const handleGoWallet = () => {
  router.push('/center/dollar/wallet');
};

const loadHeaderHistory = async () => {
  if (!isLogin.value || historyLoading.value) return;
  historyLoading.value = true;
  try {
    const res = await liveApi.listHistory({ type: 0, page: 1, limit: 5 });
    headerHistory.value = res?.data?.list || [];
  } catch (e) {
    headerHistory.value = [];
  } finally {
    historyLoading.value = false;
  }
};

const handleHistoryOpenChange = (open) => {
  historyOpen.value = open;
  if (open) {
    loadHeaderHistory();
  }
};

const handleEnterHistoryRoom = (item) => {
  const roomId = item?.roomId || item?.id;
  if (!roomId) return;
  historyOpen.value = false;
  router.push('/room/' + roomId);
};

const getHistoryTitle = (item = {}) => item.title || item.name || item.roomTitle || '直播间';

const getHistoryCategory = (item = {}) =>
  item.categoryName || item.categoryInfo?.name || item.userNickname || item.userInfo?.nickName || '继续观看';
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

        <button class="nav-item" type="button" @click="handleGoFollow">
          <HeartOutlined />
          关注
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

      <div class="site-header__quick">
        <a-popover placement="bottom" trigger="hover" :open="historyOpen" @openChange="handleHistoryOpenChange" overlayClassName="history-popover">
          <template #content>
            <div class="history-panel">
              <div class="history-panel__head">
                <strong>今天</strong>
              </div>
              <a-spin :spinning="historyLoading">
                <div v-if="headerHistory.length" class="history-list">
                  <button
                    v-for="item in headerHistory"
                    :key="item.id || item.roomId"
                    class="history-entry"
                    type="button"
                    @click="handleEnterHistoryRoom(item)"
                  >
                    <span class="history-entry__dot">
                      <ClockCircleOutlined />
                    </span>
                    <span class="history-entry__copy">
                      <strong>{{ getHistoryTitle(item) }}</strong>
                    </span>
                    <span class="history-entry__room">{{ getHistoryCategory(item) }}</span>
                  </button>
                </div>
                <a-empty v-else description="暂无观看历史" />
              </a-spin>
              <button class="history-more" type="button" @click="handleGoHistory">更多 ></button>
            </div>
          </template>
          <button class="quick-entry quick-entry--history" :class="{ active: historyOpen || $route.path.includes('/center/personnel/history') }" type="button">
            <ClockCircleOutlined />
            <span>历史</span>
          </button>
        </a-popover>
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
          <a-popover placement="bottomRight" trigger="click" overlayClassName="user-card-popover">
            <button class="user-entry user-entry--avatar-only" type="button">
              <img class="header-avatar header-avatar--large" :src="userInfo?.avatar || FALLBACK_AVATAR" alt="" @error="onImgError" />
            </button>
            <template #content>
              <div class="user-card-panel">
                <button class="user-card-logout" type="button" @click="handleLogout">
                  <LogoutOutlined />
                  退出
                </button>

                <div class="user-card-avatar">
                  <img :src="userInfo?.avatar || FALLBACK_AVATAR" alt="" @error="onImgError" />
                </div>

                <div class="user-card-name">
                  <strong>{{ displayName }}</strong>
                  <span>♀</span>
                  <em>LV{{ userLevel || 1 }}</em>
                  <button type="button">
                    <EditOutlined />
                  </button>
                </div>

                <p class="user-card-signature">
                  <EditOutlined />
                  {{ userInfo?.signature || '点击编辑个性签名' }}
                </p>

                <div class="level-row">
                  <span>LV-{{ userLevel || 1 }}</span>
                  <div class="level-track">
                    <i :style="{ width: levelProgress }"></i>
                    <b>{{ userLevel ? (userLevel % 10) * 20 : 0 }}/200</b>
                  </div>
                  <span>LV-{{ (userLevel || 1) + 1 }}</span>
                </div>

                <div class="asset-row">
                  <span>资产</span>
                  <strong>
                    <WalletOutlined />
                    {{ walletBalance }}
                  </strong>
                  <strong>
                    <GiftOutlined />
                    0
                  </strong>
                  <button type="button" @click="handleGoRecharge">充值</button>
                </div>

                <div class="user-shortcuts">
                  <button type="button" @click="handleGoCenter">
                    <UserOutlined />
                    <span>个人中心</span>
                  </button>
                  <button type="button" @click="handleGoMessages">
                    <MessageOutlined />
                    <span>我的消息</span>
                  </button>
                  <button type="button" @click="handleGoWallet">
                    <WalletOutlined />
                    <span>我的钱包</span>
                  </button>
                  <button type="button" @click="handleGoLiveCenter">
                    <VideoCameraOutlined />
                    <span>创作中心</span>
                  </button>
                </div>

                <button v-if="hasAdminRole" class="admin-shortcut" type="button" @click="handleGoAdmin">
                  管理后台
                </button>
              </div>
            </template>
          </a-popover>
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

.site-header__quick {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}

.quick-entry {
  position: relative;
  display: grid;
  min-width: 52px;
  height: 52px;
  place-items: center;
  padding: 5px 7px 4px;
  border: 0;
  border-radius: 8px;
  color: #6f737b;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background 0.16s ease,
    transform 0.16s ease;
}

.quick-entry:hover {
  color: #ff8a00;
  background: #fff5e6;
  transform: translateY(-1px);
}

.quick-entry svg {
  font-size: 25px;
}

.quick-entry span {
  color: inherit;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.quick-entry--history,
.quick-entry--history.active {
  color: #ff8a00;
}

.quick-entry--history.active::after {
  position: absolute;
  right: 13px;
  bottom: -7px;
  left: 13px;
  height: 12px;
  content: "";
  border-top: 3px solid #ff8a00;
  border-radius: 12px 12px 0 0;
}

:deep(.history-popover .ant-popover-inner) {
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(31, 35, 41, 0.18) !important;
}

:deep(.history-popover .ant-popover-arrow) {
  display: none;
}

.history-panel {
  position: relative;
  width: 420px;
  padding: 18px 22px 18px;
  border-top: 3px solid #ff8a00;
  border-radius: 8px;
  background: #fff;
}

.history-panel::before {
  position: absolute;
  top: -9px;
  left: 50%;
  width: 16px;
  height: 16px;
  content: "";
  background: #fff;
  border-top: 3px solid #ff8a00;
  border-left: 3px solid #ff8a00;
  transform: translateX(-50%) rotate(45deg);
}

.history-panel__head {
  position: relative;
  margin-bottom: 10px;
  padding-left: 14px;
}

.history-panel__head::before {
  position: absolute;
  top: 5px;
  bottom: -244px;
  left: 0;
  width: 1px;
  content: "";
  background: #d9d9d9;
}

.history-panel__head strong {
  color: #555;
  font-size: 17px;
  font-weight: 500;
}

.history-list {
  position: relative;
  display: grid;
  gap: 2px;
  max-height: 250px;
  overflow-y: auto;
  padding-right: 8px;
}

.history-entry {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 96px;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 44px;
  border: 0;
  color: #4a4d52;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.history-entry:hover .history-entry__copy strong {
  color: #ff8a00;
}

.history-entry__dot {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #ff8a00;
  font-size: 15px;
}

.history-entry__dot svg {
  font-size: 15px;
}

.history-entry__copy,
.history-entry__room {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-entry__copy strong {
  color: #3f4248;
  font-size: 16px;
  font-weight: 500;
}

.history-entry__room {
  color: #8f9298;
  font-size: 16px;
}

.history-more {
  display: block;
  width: calc(100% - 16px);
  height: 44px;
  margin: 12px auto 0;
  border: 0;
  border-radius: 22px;
  color: #555;
  background: #f0f1f3;
  font-size: 16px;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background 0.16s ease;
}

.history-more:hover {
  color: #ff8a00;
  background: #fff1df;
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

.user-entry--avatar-only {
  padding: 0;
  width: 38px;
  justify-content: center;
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

.header-avatar--large {
  width: 38px;
  height: 38px;
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

.mobile-start-btn {
  display: none;
}

:deep(.user-card-popover .ant-popover-inner) {
  padding: 0;
  border-radius: 10px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.2) !important;
}

:deep(.user-card-popover .ant-popover-arrow) {
  display: none;
}

.user-card-panel {
  position: relative;
  width: 420px;
  padding: 22px 18px 16px;
  border-top: 4px solid #ff8a00;
  border-radius: 10px;
  background: #fff;
}

.user-card-panel::before {
  position: absolute;
  top: -9px;
  right: 28px;
  width: 18px;
  height: 18px;
  content: "";
  background: #fff;
  border-top: 4px solid #ff8a00;
  border-left: 4px solid #ff8a00;
  transform: rotate(45deg);
}

.user-card-logout {
  position: absolute;
  top: 16px;
  right: 18px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  color: #444;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
}

.user-card-avatar {
  display: grid;
  place-items: center;
}

.user-card-avatar img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.user-card-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.user-card-name strong {
  color: #333;
  font-size: 20px;
  font-weight: 800;
}

.user-card-name span {
  color: #ff5b8f;
  font-weight: 900;
}

.user-card-name em {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  color: #fff;
  background: #5ac8fa;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
}

.user-card-name button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  color: #24b9ff;
  background: transparent;
  cursor: pointer;
}

.user-card-signature {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 8px 0 0;
  color: #9aa1ad;
  font-size: 14px;
}

.level-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-top: 18px;
  color: #333;
}

.level-track {
  position: relative;
  height: 22px;
  border-radius: 11px;
  background: #ececec;
}

.level-track i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #ffd966, #ff9900);
}

.level-track b {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #666;
  font-size: 13px;
}

.asset-row {
  display: grid;
  grid-template-columns: auto auto auto auto;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
  color: #333;
}

.asset-row span {
  font-size: 16px;
  font-weight: 700;
}

.asset-row strong {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #333;
  font-size: 15px;
  font-weight: 800;
}

.asset-row button {
  justify-self: end;
  height: 34px;
  padding: 0 18px;
  border: 0;
  border-radius: 17px;
  color: #fff;
  background: #ff8a00;
  font-weight: 900;
  cursor: pointer;
}

.user-shortcuts {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.user-shortcuts button {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 10px 4px 6px;
  border: 0;
  border-radius: 8px;
  color: #666;
  background: #fff;
  cursor: pointer;
}

.user-shortcuts button:hover {
  background: #fff4e3;
  color: #ff8a00;
}

.user-shortcuts button svg {
  font-size: 26px;
}

.user-shortcuts span {
  font-size: 12px;
  font-weight: 700;
}

.admin-shortcut {
  width: 100%;
  height: 36px;
  margin-top: 10px;
  border: 1px solid #e8ebf0;
  border-radius: 8px;
  color: #666;
  background: #fafafa;
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 1180px) {
  .brand-copy span,
  .start-live {
    display: none;
  }

  .quick-entry {
    min-width: 46px;
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

  .site-header__quick {
    order: 3;
    margin-left: auto;
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

  .history-panel {
    width: min(88vw, 420px);
  }

  .user-card-panel {
    width: min(86vw, 420px);
  }

  .user-shortcuts {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
