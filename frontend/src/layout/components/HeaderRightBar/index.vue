<template>
  <div class="header-right-bar">
    <a-dropdown v-if="userStore.isLogin" placement="bottomRight" :trigger="['hover']">
      <div class="user-info">
        <a-avatar :size="34" :src="userStore.userInfo.avatar" :style="{ backgroundColor: '#ff9900' }">
          {{ userStore.userInfo.username?.charAt(0)?.toUpperCase() }}
        </a-avatar>
        <div class="user-meta">
          <span class="username">{{ userStore.userInfo.nickName || userStore.userInfo.nickname || userStore.userInfo.username }}</span>
          <span class="role-text">{{ userStore.isAdmin ? "系统管理员" : "平台用户" }}</span>
        </div>
        <DownOutlined class="dropdown-icon" />
      </div>
      <template #overlay>
        <a-menu>
          <a-menu-item key="profile" @click="goToProfile">
            <UserOutlined />
            <span>个人中心</span>
          </a-menu-item>
          <a-menu-item key="messages" @click="goToMessages">
            <MessageOutlined />
            <span>消息中心</span>
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item key="logout" @click="handleLogout">
            <LogoutOutlined />
            <span>退出登录</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <div v-else class="login-actions">
      <a-button type="primary" @click="goToLogin">登录</a-button>
    </div>
  </div>
</template>

<script setup>
import { DownOutlined, LogoutOutlined, MessageOutlined, UserOutlined } from "@ant-design/icons-vue"
import { Modal } from "ant-design-vue"
import { useRouter } from "vue-router"
import { useUserStore } from "@/stores/modules/user"

const userStore = useUserStore()
const router = useRouter()

const goToProfile = () => router.push("/center")
const goToMessages = () => router.push("/center/messages")
const goToLogin = () => router.push("/login")

const handleLogout = () => {
  Modal.confirm({
    title: "确认退出登录？",
    content: "退出后将返回登录页。",
    okText: "确认",
    cancelText: "取消",
    onOk() {
      userStore.logout()
      router.push("/login")
    },
  })
}
</script>

<style scoped lang="scss">
.header-right-bar {
  display: flex;
  align-items: center;
  height: 100%;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.username {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.role-text {
  font-size: 12px;
  color: #909399;
}

.dropdown-icon {
  color: #909399;
  font-size: 12px;
}

.login-actions {
  display: flex;
  align-items: center;
}
</style>
