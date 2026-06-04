<template>
  <div class="profile-page">
    <section class="profile-grid">
      <a-card class="profile-card profile-card--summary" :bordered="false">
        <div class="summary-header">
          <div class="summary-avatar">
            <a-upload
              v-model:file-list="fileList"
              name="file"
              list-type="picture-card"
              class="avatar-uploader"
              :show-upload-list="false"
              action="/api/v1/upload/avatar"
              :headers="userToken"
              :before-upload="beforeUpload"
              @change="handleChange"
            >
              <img class="avatar-img" :src="displayAvatar" alt="avatar" @error="onImgError" />
              <div v-if="loading" class="avatar-loading">
                <loading-outlined />
              </div>
            </a-upload>
          </div>

          <div class="summary-copy">
            <h2>{{ userInfo.nickName || userInfo.nickname || userInfo.username || "直播用户" }}</h2>
            <p>{{ userInfo.signature || "这个人很懒，什么都没留下。" }}</p>
            <div class="summary-tags">
              <a-tag color="gold">UID {{ userInfo.userId || userInfo.id || "-" }}</a-tag>
              <a-tag color="green">{{ userInfo.email ? "已绑定邮箱" : "未绑定邮箱" }}</a-tag>
              <a-tag color="gold">{{ userInfo.mobile ? "已绑定手机" : "未绑定手机" }}</a-tag>
            </div>
          </div>
        </div>

        <div class="summary-list">
          <div class="summary-item">
            <span>账号</span>
            <strong>{{ userInfo.username || "-" }}</strong>
          </div>
          <div class="summary-item">
            <span>昵称</span>
            <strong>{{ userInfo.nickName || userInfo.nickname || "-" }}</strong>
          </div>
          <div class="summary-item">
            <span>签名</span>
            <strong>{{ userInfo.signature || "暂未设置" }}</strong>
          </div>
          <div class="summary-item">
            <span>邮箱</span>
            <strong>{{ userInfo.email || "未绑定" }}</strong>
          </div>
          <div class="summary-item">
            <span>手机</span>
            <strong>{{ userInfo.mobile || "未绑定" }}</strong>
          </div>
          <div class="summary-item">
            <span>身份</span>
            <strong>{{ userStore.isAdmin ? "运营账号" : "直播用户" }}</strong>
          </div>
        </div>
      </a-card>

      <div class="profile-side">
        <a-card title="账号安全" class="profile-card" :bordered="false">
          <SecurityItem />
        </a-card>
        <a-card title="信息认证" class="profile-card" :bordered="false">
          <Authentication />
        </a-card>
      </div>
    </section>
  </div>
</template>

<script setup>
import SecurityItem from "./SecurityItem.vue"
import Authentication from "./Authentication.vue"
import { LoadingOutlined } from "@ant-design/icons-vue"
import { useStore } from "@/stores"
import { FALLBACK_AVATAR, onImgError } from "@/utils/fallback"
import $modal from "@/utils/message"
import { computed, onMounted, ref } from "vue"
import { storeToRefs } from "pinia"

const store = useStore()
const userStore = store.user()
const { userInfo } = storeToRefs(userStore)
const fileList = ref([])
const loading = ref(false)
const imageUrl = ref("")

const displayAvatar = computed(() => imageUrl.value || FALLBACK_AVATAR)

const userToken = computed(() => ({
  Authorization: `${store.user().userToken}`,
}))

onMounted(() => {
  imageUrl.value = userInfo.value.avatar
})

const handleChange = (info) => {
  if (info.file.status === "uploading") {
    loading.value = true
    return
  }
  if (info.file.status === "done") {
    imageUrl.value = info.file.response.data
    store.user().updateAvatar(imageUrl.value)
    loading.value = false
  }
  if (info.file.status === "error") {
    loading.value = false
    $modal.msgError("头像上传失败")
  }
}

const beforeUpload = (file) => {
  const isJpgOrPng = file.type === "image/jpeg" || file.type === "image/png"
  if (!isJpgOrPng) {
    $modal.msgError("只能上传 JPG 或 PNG 图片")
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    $modal.msgError("上传文件大小不能超过 2MB")
  }
  return isJpgOrPng && isLt2M
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100%;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(540px, 620px) minmax(360px, 1fr);
  gap: 18px;
  min-width: 0;
  align-items: start;
}

.profile-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  min-width: 0;
}

.profile-card--summary {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 13%, transparent), transparent),
    var(--bg-card);
}

.summary-header {
  display: flex;
  gap: 22px;
  align-items: center;
}

.summary-copy h2 {
  margin: 0;
  font-size: 28px;
  color: var(--text-primary);
  font-weight: 900;
}

.summary-copy p {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
  max-width: 540px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--bg-card) 92%, var(--bg-secondary));
  border: 1px solid var(--border);
}

.summary-item span {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
}

.profile-side {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.summary-avatar {
  width: 108px;
  height: 108px;
  flex: 0 0 108px;
}

.avatar-uploader {
  display: block;
  width: 108px;
  height: 108px;

  :deep(.ant-upload-wrapper),
  :deep(.ant-upload-list),
  :deep(.ant-upload-list-item-container) {
    width: 108px;
    height: 108px;
  }

  :deep(.ant-upload-select) {
    width: 108px !important;
    height: 108px !important;
    margin: 0 !important;
    border: 0 !important;
    border-radius: 50% !important;
    overflow: hidden;
    background: transparent !important;
  }

  :deep(.ant-upload) {
    width: 108px !important;
    height: 108px !important;
    position: relative;
    border-radius: 50% !important;
    overflow: hidden;
  }
}

.avatar-img {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-loading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--accent);
  background: color-mix(in srgb, var(--bg-card) 72%, transparent);
}

@media (max-width: 960px) {
  .profile-grid,
  .summary-list {
    grid-template-columns: 1fr;
  }

  .summary-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
