<template>
  <div class="profile-page">
    <section class="room-profile">
      <header class="room-profile__header">
        <div class="anchor-avatar">
          <a-upload
            v-model:file-list="fileList"
            name="file"
            list-type="picture-card"
            class="avatar-uploader"
            :show-upload-list="false"
            action="/api/v1/upload/avatar"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            @change="handleChange"
          >
            <img class="avatar-img" :src="displayAvatar" alt="avatar" @error="onImgError" />
            <div v-if="loading" class="avatar-loading">
              <loading-outlined />
            </div>
          </a-upload>
        </div>

        <div class="anchor-info">
          <div class="anchor-title">
            <h1>{{ displayName }}</h1>
            <span class="level-badge">{{ accountLevel }}</span>
            <button class="link-action" type="button" @click="openProfileEditor">&#32534;&#36753;</button>
          </div>
          <p class="room-id">&#36134;&#21495;&#65306;{{ accountIdentity }}</p>
          <div class="anchor-badges">
            <span v-for="badge in profileBadges" :key="badge">{{ badge }}</span>
          </div>
        </div>

        <div class="header-actions">
          <button class="recharge-btn" type="button" @click="goTo('/center/dollar/recharge')">&#20805;&#20540;</button>
          <button class="plain-btn" type="button" @click="goTo('/center/dollar/wallet')">&#26126;&#32454;</button>
        </div>
      </header>

      <div class="room-profile__body">
        <main class="room-stage">
          <section class="profile-overview">
            <div class="overview-copy">
              <span class="overview-kicker">&#20010;&#20154;&#36164;&#26009;</span>
              <h2>{{ displayName }}</h2>
              <p>{{ userInfo.signature || "\u8fd8\u6ca1\u6709\u7f16\u8f91\u4e2a\u6027\u7b7e\u540d\uff0c\u5b8c\u5584\u540e\u4f1a\u5c55\u793a\u5728\u4e2a\u4eba\u4e3b\u9875\u548c\u76f4\u64ad\u95f4\u8d44\u6599\u4e2d\u3002" }}</p>
              <div class="overview-meta">
                <span>&#36134;&#21495; {{ accountIdentity }}</span>
                <span>{{ userStore.isAdmin ? "\u8fd0\u8425\u8d26\u53f7" : "\u666e\u901a\u7528\u6237" }}</span>
                <span>{{ userInfo.mobile || userInfo.email ? "\u8054\u7cfb\u65b9\u5f0f\u5df2\u5b8c\u5584" : "\u8054\u7cfb\u65b9\u5f0f\u5f85\u5b8c\u5584" }}</span>
              </div>
            </div>

            <div class="overview-metrics">
              <article class="metric-card metric-card--primary">
                <div class="metric-head">
                  <span>&#36164;&#26009;&#23436;&#21892;&#24230;</span>
                  <strong>{{ profileCompletion }}%</strong>
                </div>
                <a-progress :percent="profileCompletion" :show-info="false" />
              </article>
              <article class="metric-card">
                <div class="metric-head">
                  <span>&#36134;&#21495;&#23433;&#20840;</span>
                  <strong>{{ securityReadyCount }}/3</strong>
                </div>
                <a-progress :percent="securityPercent" :show-info="false" status="active" />
              </article>
              <div class="status-strip">
                <span v-for="item in accountSummary" :key="item.label">
                  <strong>{{ item.value }}</strong>
                  <em>{{ item.label }}</em>
                </span>
              </div>
            </div>
          </section>

          <section class="section-block">
            <div class="section-title">
              <h2>&#22522;&#30784;&#20449;&#24687;</h2>
              <button class="edit-profile-btn" type="button" @click="openProfileEditor">&#32534;&#36753;&#36164;&#26009;</button>
            </div>
            <div class="basic-grid">
              <div v-for="item in basicInfoItems" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="section-block">
            <div class="section-title">
              <h2>&#36134;&#21495;&#26381;&#21153;</h2>
            </div>
            <div class="play-grid">
              <article v-for="item in playItems" :key="item.title" class="play-card">
                <div class="play-icon">
                  <component :is="item.icon" />
                </div>
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <button type="button" @click="goTo(item.path)">{{ item.action }}</button>
              </article>
            </div>
          </section>

          <footer class="room-toolbox">
            <button v-for="tool in toolbox" :key="tool.label" type="button" @click="goTo(tool.path)">
              <span><component :is="tool.icon" /></span>
              {{ tool.label }}
            </button>
          </footer>
        </main>

        <aside class="room-side">
          <div class="side-ad">
            <strong>&#36134;&#21495;&#31649;&#29702;</strong>
            <span>&#23436;&#21892;&#36164;&#26009;&#65292;&#31649;&#29702;&#30452;&#25773;&#12289;&#38065;&#21253;&#21644;&#36134;&#21495;&#23433;&#20840;&#12290;</span>
          </div>
          <div class="side-tabs">
            <button
              v-for="tab in sideTabOptions"
              :key="tab.key"
              type="button"
              :class="{ active: activeSideTab === tab.key }"
              @click="activeSideTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>
          <section class="side-panel">
            <div class="panel-title">
              <strong>{{ activeSideTitle }}</strong>
              <em v-if="activeSideTab === 'security'">{{ securityReadyCount }}/3</em>
              <em v-else-if="activeSideTab === 'account'">{{ profileCompletion }}%</em>
              <em v-else>&#24555;&#25463;</em>
            </div>
            <div v-if="activeSideTab === 'interaction'" class="side-action-list">
              <button v-for="item in playItems" :key="item.title" type="button" @click="goTo(item.path)">
                <span>{{ item.title }}</span>
                <em>{{ item.action }}</em>
              </button>
            </div>
            <div v-else-if="activeSideTab === 'account'" class="side-info-list">
              <span v-for="item in basicInfoItems.slice(0, 5)" :key="item.label">
                <em>{{ item.label }}</em>
                <strong>{{ item.value }}</strong>
              </span>
            </div>
            <SecurityItem v-else />
          </section>
        </aside>
      </div>
    </section>

    <a-modal
      v-model:open="profileEditorVisible"
      title="&#32534;&#36753;&#20010;&#20154;&#36164;&#26009;"
      :confirm-loading="profileSaving"
      width="560px"
      ok-text="&#20445;&#23384;"
      cancel-text="&#21462;&#28040;"
      @ok="saveProfileInfo"
      @cancel="closeProfileEditor"
    >
      <a-form ref="profileFormRef" :model="profileForm" :rules="profileRules" layout="vertical" class="profile-edit-form">
        <a-form-item label="&#26165;&#31216;" name="nickName">
          <a-input v-model:value="profileForm.nickName" :maxlength="16" placeholder="&#35831;&#36755;&#20837;&#26165;&#31216;" show-count />
        </a-form-item>
        <a-form-item label="&#20010;&#24615;&#31614;&#21517;" name="signature">
          <a-textarea v-model:value="profileForm.signature" :maxlength="64" :rows="4" placeholder="&#29992;&#19968;&#21477;&#35805;&#20171;&#32461;&#33258;&#24049;" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import SecurityItem from "./SecurityItem.vue"
import {
  AccountBookOutlined,
  AreaChartOutlined,
  CustomerServiceOutlined,
  LoadingOutlined,
  VideoCameraOutlined,
  WalletOutlined,
} from "@ant-design/icons-vue"
import { useStore } from "@/stores"
import userApi from "@/api/user"
import { FALLBACK_AVATAR, onImgError, resolveSafeImageUrl } from "@/utils/fallback"
import $modal from "@/utils/message"
import { computed, onMounted, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { storeToRefs } from "pinia"

const store = useStore()
const router = useRouter()
const userStore = store.user()
const { userInfo } = storeToRefs(userStore)
const fileList = ref([])
const loading = ref(false)
const imageUrl = ref("")
const profileFormRef = ref()
const profileEditorVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({
  nickName: "",
  signature: "",
})
const activeSideTab = ref("interaction")
const sideTabOptions = [
  { key: "interaction", label: "\u4e92\u52a8" },
  { key: "account", label: "\u8d26\u6237" },
  { key: "security", label: "\u5b89\u5168" },
]
const activeSideTitle = computed(() => sideTabOptions.find((item) => item.key === activeSideTab.value)?.label || "\u4e92\u52a8")
const profileRules = {
  nickName: [
    { required: true, message: "\u8bf7\u8f93\u5165\u6635\u79f0", trigger: "blur" },
    { min: 1, max: 16, message: "\u6635\u79f0\u957f\u5ea6\u4e3a 1 \u5230 16 \u4e2a\u5b57\u7b26", trigger: "blur" },
  ],
  signature: [{ max: 64, message: "\u4e2a\u6027\u7b7e\u540d\u6700\u591a 64 \u4e2a\u5b57\u7b26", trigger: "blur" }],
}

const displayAvatar = computed(() => resolveSafeImageUrl(imageUrl.value || userInfo.value.avatar, FALLBACK_AVATAR))
const displayName = computed(() => userInfo.value.nickName || userInfo.value.nickname || userInfo.value.username || "\u76f4\u64ad\u7528\u6237")
const accountIdentity = computed(() => userInfo.value.username || userInfo.value.userId || userInfo.value.id || "-")
const accountLevel = computed(() => {
  const level = userInfo.value.level || userInfo.value.userLevel || userInfo.value.levelName
  return level ? `LV${level}` : "LV1"
})
const securityReadyCount = computed(() => [userInfo.value.mobile, userInfo.value.email, userInfo.value.password].filter(Boolean).length)
const securityPercent = computed(() => Math.round((securityReadyCount.value / 3) * 100))
const profileCompletion = computed(() => {
  const fields = [
    userInfo.value.username,
    userInfo.value.avatar,
    userInfo.value.nickName || userInfo.value.nickname,
    userInfo.value.signature,
    userInfo.value.email,
    userInfo.value.mobile,
  ]
  return Math.round((fields.filter(Boolean).length / fields.length) * 100)
})
const profileBadges = computed(() => {
  const badges = [userStore.isAdmin ? "\u8fd0\u8425\u8d26\u53f7" : "\u76f4\u64ad\u7528\u6237"]
  if (userInfo.value.mobile) badges.push("\u624b\u673a\u5df2\u7ed1\u5b9a")
  if (userInfo.value.email) badges.push("\u90ae\u7bb1\u5df2\u7ed1\u5b9a")
  if (!userInfo.value.mobile && !userInfo.value.email) badges.push("\u8d44\u6599\u5f85\u5b8c\u5584")
  return badges
})
const maskPhone = (value) => {
  if (!value) return "-"
  return String(value).replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2")
}
const maskEmail = (value) => {
  if (!value) return "-"
  const [name, domain] = String(value).split("@")
  if (!name || !domain) return value
  return `${name.slice(0, 2)}***@${domain}`
}
const basicInfoItems = computed(() => [
  { label: "\u8d26\u53f7", value: userInfo.value.username || "-" },
  { label: "\u6635\u79f0", value: displayName.value },
  { label: "\u7528\u6237ID", value: userInfo.value.userId || userInfo.value.id || "-" },
  { label: "\u624b\u673a", value: maskPhone(userInfo.value.mobile) },
  { label: "\u90ae\u7bb1", value: maskEmail(userInfo.value.email) },
  { label: "\u8d26\u53f7\u7c7b\u578b", value: userStore.isAdmin ? "\u8fd0\u8425\u8d26\u53f7" : "\u666e\u901a\u7528\u6237" },
  { label: "\u4e2a\u6027\u7b7e\u540d", value: userInfo.value.signature || "\u4f60\u8fd8\u6ca1\u6709\u7f16\u8f91\u4e2a\u6027\u7b7e\u540d\u3002" },
])
const accountSummary = computed(() => [
  { label: "\u8d44\u6599", value: `${profileCompletion.value}%` },
  { label: "\u5b89\u5168", value: `${securityReadyCount.value}/3` },
  { label: "\u8054\u7cfb", value: userInfo.value.mobile || userInfo.value.email ? "\u5df2\u5b8c\u5584" : "\u5f85\u5b8c\u5584" },
])
const playItems = [
  { icon: VideoCameraOutlined, title: "\u5f00\u64ad\u51c6\u5907", desc: "\u914d\u7f6e\u5c01\u9762\u3001\u5206\u7c7b\u548c\u76f4\u64ad\u65b9\u5f0f", action: "\u53bb\u8bbe\u7f6e", path: "/center/live/live-settings" },
  { icon: CustomerServiceOutlined, title: "\u8054\u7cfb\u5ba2\u670d", desc: "\u5145\u503c\u3001\u5f00\u64ad\u6216\u8d26\u53f7\u95ee\u9898\u53ef\u63d0\u4ea4\u5de5\u5355", action: "\u53bb\u53cd\u9988", path: "/center/messages/customer-service" },
]
const toolbox = [
  { icon: VideoCameraOutlined, label: "\u5f00\u64ad", path: "/center/live/live-settings" },
  { icon: AreaChartOutlined, label: "\u6570\u636e", path: "/center/statistic/overview" },
  { icon: WalletOutlined, label: "\u94b1\u5305", path: "/center/dollar/wallet" },
  { icon: AccountBookOutlined, label: "\u8d26\u5355", path: "/center/dollar/bill" },
  { icon: CustomerServiceOutlined, label: "\u5ba2\u670d", path: "/center/messages/customer-service" },
]

const goTo = (path) => {
  if (!path) return
  router.push(path)
}

const openProfileEditor = () => {
  profileForm.nickName = userInfo.value.nickName || userInfo.value.nickname || userInfo.value.username || ""
  profileForm.signature = userInfo.value.signature || ""
  profileEditorVisible.value = true
}

const closeProfileEditor = () => {
  profileEditorVisible.value = false
  profileFormRef.value?.clearValidate?.()
}

const saveProfileInfo = async () => {
  try {
    await profileFormRef.value?.validate?.()
    profileSaving.value = true
    const response = await userApi.updateUserInfo({
      nickName: profileForm.nickName.trim(),
      signature: profileForm.signature.trim(),
    })
    if (response?.data !== true) {
      throw new Error(response?.msg || "\u4e2a\u4eba\u8d44\u6599\u4fdd\u5b58\u5931\u8d25")
    }
    userStore.updateSecurityInfo({
      nickName: profileForm.nickName.trim(),
      nickname: profileForm.nickName.trim(),
      signature: profileForm.signature.trim(),
    })
    $modal.msgSuccess("\u4e2a\u4eba\u8d44\u6599\u5df2\u4fdd\u5b58")
    closeProfileEditor()
  } catch (error) {
    if (!error?.errorFields) {
      $modal.msgError(error?.message || "\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
    }
  } finally {
    profileSaving.value = false
  }
}

const uploadHeaders = computed(() => ({
  Authorization: store.user().userToken || "",
}))

onMounted(() => {
  imageUrl.value = userInfo.value.avatar
})

const legacyHandleChange = (info) => {
  if (info.file.status === "uploading") {
    loading.value = true
    return
  }
  if (info.file.status === "done") {
    const response = info.file.response || {}
    if (response.code !== 0 || !response.data) {
      loading.value = false
      $modal.msgError(response.msg || "\u5934\u50cf\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
      return
    }
    imageUrl.value = response.data
    userStore.updateAvatar(imageUrl.value)
    loading.value = false
    $modal.msgSuccess("\u5934\u50cf\u4e0a\u4f20\u6210\u529f")
  }
  if (info.file.status === "error") {
    loading.value = false
    const response = info.file.response || {}
    $modal.msgError(response.msg || info.file.error?.message || "\u5934\u50cf\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
  }
}

const legacyBeforeUpload = (file) => {
  const isJpgOrPng = file.type === "image/jpeg" || file.type === "image/png"
  if (!isJpgOrPng) {
    $modal.msgError("\u53ea\u80fd\u4e0a\u4f20 JPG \u6216 PNG \u56fe\u7247")
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    $modal.msgError("图片大小不能超过 2MB")
  }
  return isJpgOrPng && isLt2M
}
const getUploadErrorMessage = (file) => {
  const response = file?.response || file?.xhr?.response
  if (typeof response === "string") {
    try {
      const parsed = JSON.parse(response)
      return parsed?.msg || parsed?.message
    } catch (error) {
      return response
    }
  }
  return response?.msg || response?.message || file?.error?.message
}

const handleChange = (info) => {
  if (info.file.status === "uploading") {
    loading.value = true
    return
  }
  if (info.file.status === "done") {
    const response = info.file.response || {}
    loading.value = false
    if (response.code !== 0 || !response.data) {
      $modal.msgError(response.msg || "\u5934\u50cf\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
      return
    }
    imageUrl.value = response.data
    userStore.updateAvatar(imageUrl.value)
    $modal.msgSuccess("\u5934\u50cf\u4e0a\u4f20\u6210\u529f")
  }
  if (info.file.status === "error") {
    loading.value = false
    $modal.msgError(getUploadErrorMessage(info.file) || "\u5934\u50cf\u4e0a\u4f20\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
  }
}

const beforeUpload = (file) => {
  const allowedTypes = ["image/jpeg", "image/png", "image/webp"]
  const isSupportedImage = allowedTypes.includes(file.type)
  if (!isSupportedImage) {
    $modal.msgError("\u53ea\u80fd\u4e0a\u4f20 JPG\u3001PNG \u6216 WEBP \u56fe\u7247")
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    $modal.msgError("图片大小不能超过 2MB")
  }
  return isSupportedImage && isLt2M
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100%;
  padding: 16px;
  background:
    radial-gradient(circle at 15% 0, color-mix(in srgb, var(--accent) 8%, transparent), transparent 28%),
    var(--bg-primary);
}

.room-profile {
  min-height: calc(100vh - 72px);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--bg-card) 88%, var(--bg-primary));
  box-shadow: var(--shadow);
}

.room-profile__header {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  min-height: 118px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 10%, transparent), transparent 46%),
    var(--bg-card);
}

.anchor-avatar {
  width: 72px;
  height: 72px;
  padding: 2px;
  border-radius: 50%;
  background: var(--accent-gradient);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--accent) 22%, transparent);
}

.avatar-uploader {
  display: block;
  width: 68px;
  height: 68px;

  :deep(.ant-upload-wrapper),
  :deep(.ant-upload-list),
  :deep(.ant-upload-list-item-container) {
    width: 68px;
    height: 68px;
  }

  :deep(.ant-upload-select),
  :deep(.ant-upload) {
    width: 68px !important;
    height: 68px !important;
    margin: 0 !important;
    border: 0 !important;
    border-radius: 50% !important;
    overflow: hidden;
    background: transparent !important;
  }
}

.avatar-img {
  width: 68px;
  height: 68px;
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

.anchor-info {
  min-width: 0;
}

.anchor-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.anchor-title h1 {
  margin: 0;
  font-size: 31px;
  font-weight: 900;
  color: var(--text-primary);
  line-height: 1.1;
}

.level-badge {
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  color: var(--accent-text);
  background: var(--accent);
  font-size: 12px;
  font-weight: 900;
  line-height: 22px;
}

.link-action {
  border: 0;
  color: var(--accent);
  background: transparent;
  font-weight: 800;
  cursor: pointer;
  transition:
    color 0.2s ease,
    transform 0.2s ease;
}

.link-action:hover {
  color: var(--accent-strong);
  transform: translateY(-1px);
}

.room-id {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 15px;
}

.anchor-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.anchor-badges span {
  height: 24px;
  padding: 0 11px;
  border: 1px solid color-mix(in srgb, var(--border) 82%, transparent);
  border-radius: 999px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--bg-secondary) 88%, transparent);
  font-size: 13px;
  line-height: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.recharge-btn,
.plain-btn,
.edit-profile-btn,
.play-card button {
  height: 36px;
  padding: 0 28px;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.plain-btn:hover,
.edit-profile-btn:hover,
.play-card button:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border));
  color: var(--accent);
  box-shadow: var(--shadow);
}

.recharge-btn {
  color: var(--accent-text);
  border-color: var(--accent);
  background: var(--accent);
  box-shadow: 0 10px 20px color-mix(in srgb, var(--accent) 20%, transparent);
}

.recharge-btn:hover {
  transform: translateY(-1px);
  background: var(--accent-strong);
  border-color: var(--accent-strong);
}

.room-profile__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 410px);
  gap: 16px;
  padding: 16px;
}

.room-stage {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.profile-overview {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 24px;
  min-height: 320px;
  padding: 30px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent) 16%, transparent), transparent 30%),
    linear-gradient(180deg, var(--bg-card) 0%, var(--bg-secondary) 100%);
}

.overview-copy {
  display: grid;
  align-content: start;
  gap: 12px;
}

.overview-kicker {
  color: var(--accent);
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.overview-copy h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 36px;
  font-weight: 900;
  line-height: 1.1;
}

.overview-copy p {
  max-width: 56ch;
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.8;
}

.overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}

.overview-meta span {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--bg-card) 78%, var(--bg-secondary));
  font-size: 13px;
  line-height: 28px;
}

.overview-metrics {
  display: grid;
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 86%, var(--bg-secondary));
  box-shadow: var(--shadow);
}

.metric-card--primary {
  border-color: color-mix(in srgb, var(--accent) 28%, var(--border));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 9%, transparent), transparent),
    color-mix(in srgb, var(--bg-card) 88%, var(--bg-secondary));
}

.metric-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.metric-head span {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
}

.metric-head strong {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 900;
}

.overview-metrics :deep(.ant-progress-outer) {
  margin-right: 0 !important;
  padding-right: 0 !important;
}

.overview-metrics :deep(.ant-progress-inner) {
  height: 10px;
  border-radius: 999px;
  background: var(--bg-secondary);
}

.overview-metrics :deep(.ant-progress-bg) {
  border-radius: 999px;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.status-strip span {
  min-width: 0;
  padding: 12px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 76%, var(--bg-secondary));
}

.status-strip strong,
.status-strip em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-strip strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
}

.status-strip em {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.section-block {
  padding: 28px 30px 30px;
  border-top: 1px solid var(--border);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.section-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.section-title::after {
  content: "";
  height: 1px;
  flex: 1;
  background: var(--border);
}

.edit-profile-btn {
  position: relative;
  z-index: 1;
  padding: 0 16px;
  color: var(--accent);
  border: 0;
}

.basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.basic-grid div {
  min-width: 0;
  display: grid;
  gap: 7px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 78%, var(--bg-secondary));
  font-size: 15px;
  line-height: 1.45;
}

.basic-grid span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.basic-grid strong {
  color: var(--text-primary);
  font-weight: 800;
  word-break: break-word;
}

.play-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.play-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 82%, var(--bg-secondary));
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.play-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 32%, var(--border));
  box-shadow: var(--shadow-hover);
}

.play-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: var(--accent);
  background: var(--accent-light);
  font-size: 22px;
}

.play-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.play-card p {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 14px;
}

.play-card button {
  padding: 0 20px;
  white-space: nowrap;
}

.room-toolbox {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 2px;
  padding: 10px 12px;
  border-top: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-card) 88%, var(--bg-secondary));
}

.room-toolbox button {
  height: 62px;
  border: 0;
  border-radius: 8px;
  color: var(--text-secondary);
  background: transparent;
  font-weight: 800;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.room-toolbox button:hover {
  background: var(--accent-light);
  color: var(--accent);
  transform: translateY(-1px);
}

.room-toolbox span {
  display: grid;
  place-items: center;
  margin: 0 auto 5px;
  font-size: 21px;
}

.room-side {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.side-ad {
  display: grid;
  gap: 6px;
  padding: 20px;
  color: var(--accent-text);
  background: var(--accent-gradient);
}

.side-ad strong {
  font-size: 21px;
  line-height: 1.25;
}

.side-ad span {
  font-weight: 700;
  opacity: 0.82;
}

.side-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--border);
}

.side-tabs button {
  height: 34px;
  border: 0;
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  text-align: center;
  font-weight: 800;
  line-height: 34px;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.side-tabs button:hover {
  color: var(--accent);
  background: var(--accent-light);
}

.side-tabs button.active {
  color: var(--accent);
  background: var(--accent-light);
  font-weight: 900;
}

.side-panel {
  padding: 16px 18px;
  border-bottom: 1px solid var(--border);
}

.side-action-list,
.side-info-list {
  display: grid;
  gap: 10px;
}

.side-action-list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-primary);
  background: var(--bg-secondary);
  text-align: left;
  cursor: pointer;
}

.side-action-list button:hover {
  border-color: color-mix(in srgb, var(--accent) 36%, var(--border));
  background: var(--accent-light);
}

.side-action-list span {
  font-weight: 800;
}

.side-action-list em {
  flex: 0 0 auto;
  color: var(--accent);
  font-style: normal;
  font-weight: 800;
}

.side-info-list span {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.side-info-list em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.side-info-list strong {
  overflow: hidden;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.panel-title strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.panel-title em {
  color: var(--text-muted);
  font-style: normal;
  font-weight: 800;
}

@media (max-width: 960px) {
  .room-profile__header,
  .room-profile__body,
  .basic-grid,
  .play-grid,
  .room-toolbox {
    grid-template-columns: 1fr;
  }

  .profile-overview {
    grid-template-columns: 1fr;
  }

  .room-profile__header {
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .profile-overview {
    min-height: auto;
    padding: 22px;
  }

  .overview-copy h2 {
    font-size: 28px;
  }
}
</style>
