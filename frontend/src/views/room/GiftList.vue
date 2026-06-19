<template>
  <div class="gift-board" :class="{ 'is-disabled': isBoardDisabled }">
    <div class="gift-panel">
      <div class="gift-toolbar">
        <div class="gift-tabs" role="tablist" aria-label="礼物分类">
          <button
            v-for="tier in giftTiers"
            :key="tier.key"
            type="button"
            :class="{ active: selectedTier === tier.key }"
            @click="selectedTier = tier.key"
          >
            {{ tier.label }}
          </button>
        </div>

        <button class="wallet-chip" type="button" @click="handleWalletClick">
          <WalletOutlined />
          <span>{{ walletText }}</span>
        </button>
      </div>

      <div v-if="loading" class="gift-grid gift-grid--loading" aria-busy="true">
        <span v-for="i in 6" :key="i" class="gift-skeleton"></span>
      </div>

      <a-empty v-else-if="!filteredGifts.length" class="gift-empty" description="暂无可送礼物" />

      <div v-else class="gift-grid">
        <button
          v-for="item in filteredGifts"
          :key="item.id"
          type="button"
          class="gift-item"
          :class="[giftLevelClass(item), { active: selectedGift?.id === item.id }]"
          @click="selectGift(item)"
        >
          <span v-if="giftTag(item)" class="gift-tag">{{ giftTag(item) }}</span>
          <span class="gift-icon-wrap">
            <img v-if="item.icon" :src="item.icon" alt="" @error="onGiftIconError" />
            <span v-else class="gift-icon-fallback">{{ giftFallbackLabel(item.name) }}</span>
          </span>
          <strong>{{ item.name }}</strong>
          <span>{{ formatPrice(item.price) }} 开心果</span>
        </button>
      </div>
    </div>

    <aside class="gift-send-panel">
      <div class="selected-gift">
        <span class="selected-icon">
          <img v-if="selectedGift?.icon" :src="selectedGift.icon" alt="" @error="onGiftIconError" />
          <GiftOutlined v-else />
        </span>
        <div>
          <strong>{{ selectedGift?.name || "选择礼物" }}</strong>
          <span>{{ selectedGift ? `${formatPrice(selectedGift.price)} 开心果 / 个` : "从左侧列表选择" }}</span>
        </div>
      </div>

      <div class="quantity-block">
        <div class="field-label">
          <span>数量</span>
          <b v-if="quantity > 1">连送 x{{ quantity }}</b>
        </div>
        <div class="quantity-presets">
          <button
            v-for="preset in quantityPresets"
            :key="preset"
            type="button"
            :class="{ active: quantity === preset }"
            @click="setQuantity(preset)"
          >
            {{ preset }}
          </button>
        </div>
        <a-input-number
          v-model:value="customQuantity"
          class="quantity-input"
          :min="1"
          :max="999"
          :precision="0"
          size="small"
          @change="setQuantity"
        />
      </div>

      <div class="gift-summary">
        <div>
          <span>合计</span>
          <strong>{{ formatPrice(totalPrice) }} 开心果</strong>
        </div>
        <div>
          <span>余额</span>
          <strong :class="{ danger: isLogin && !hasEnoughBalance }">{{ balanceText }}</strong>
        </div>
      </div>

      <p v-if="isBoardDisabled" class="gift-status">{{ disabledReasonText }}</p>
      <p v-else-if="isLogin && !hasEnoughBalance" class="gift-status is-warning">余额不足</p>

      <a-button
        class="send-button"
        type="primary"
        block
        :loading="sending"
        :disabled="!canSubmitGift"
        @click="sendSelectedGift"
      >
        <template #icon><SendOutlined /></template>
        {{ sendButtonText }}
      </a-button>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { GiftOutlined, SendOutlined, WalletOutlined } from "@ant-design/icons-vue"
import giftApi from "@/api/gift"
import walletApi from "@/api/wallet"
import { useStore } from "@/stores"
import $modal from "@/utils/message"
import { FALLBACK_GIFT_ICON, resolveSafeImageUrl } from "@/utils/fallback"

const props = defineProps({
  roomId: {
    type: Number,
    default: undefined,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  disabledReason: {
    type: String,
    default: "",
  },
})

const emits = defineEmits(["requireLogin", "gift-sent"])

const router = useRouter()
const userStore = useStore().user()
const giftList = ref([])
const wallet = ref({})
const loading = ref(false)
const sending = ref(false)
const selectedGiftId = ref(null)
const selectedTier = ref("all")
const quantity = ref(1)
const customQuantity = ref(1)

const quantityPresets = [1, 10, 66, 188]
const giftTiers = [
  { key: "all", label: "全部" },
  { key: "light", label: "轻礼" },
  { key: "highlight", label: "高光" },
]

const isLogin = computed(() => userStore.isLogin)
const currentUserId = computed(() => userStore.userInfo?.userId || userStore.userInfo?.id || 0)
const currentUserName = computed(() => {
  const info = userStore.userInfo || {}
  return info.nickName || info.nickname || info.name || info.username || "我"
})

const selectedGift = computed(() => giftList.value.find((item) => item.id === selectedGiftId.value) || null)
const walletBalance = computed(() => Number(wallet.value?.balance || 0))
const totalPrice = computed(() => Number(selectedGift.value?.price || 0) * Number(quantity.value || 1))
const hasEnoughBalance = computed(() => walletBalance.value >= totalPrice.value)
const isBoardDisabled = computed(() => props.disabled || !props.roomId)
const disabledReasonText = computed(() => props.disabledReason || "直播间暂不可送礼")
const balanceText = computed(() => (isLogin.value ? formatPrice(walletBalance.value) : "--"))
const walletText = computed(() => {
  if (!isLogin.value) return "登录后送礼"
  return `余额 ${formatPrice(walletBalance.value)}`
})

const filteredGifts = computed(() => {
  if (selectedTier.value === "light") {
    return giftList.value.filter((item) => Number(item.price || 0) < 50)
  }
  if (selectedTier.value === "highlight") {
    return giftList.value.filter((item) => Number(item.price || 0) >= 50)
  }
  return giftList.value
})

const canSubmitGift = computed(() => {
  if (!selectedGift.value || sending.value || isBoardDisabled.value) return false
  if (!isLogin.value) return true
  return hasEnoughBalance.value
})

const sendButtonText = computed(() => {
  if (isBoardDisabled.value) return "暂不可送"
  if (!isLogin.value) return "登录后送礼"
  if (!selectedGift.value) return "选择礼物"
  if (!hasEnoughBalance.value) return "余额不足"
  return "送出礼物"
})

onMounted(async () => {
  await getGiftList()
  if (isLogin.value) {
    await getWallet()
  }
})

watch(isLogin, async (loggedIn) => {
  wallet.value = {}
  if (loggedIn) {
    await getWallet()
  }
})

watch(filteredGifts, (list) => {
  if (!list.length) return
  if (!list.some((item) => item.id === selectedGiftId.value)) {
    selectedGiftId.value = list[0].id
  }
})

const normalizeGift = (item) => ({
  ...item,
  icon: resolveSafeImageUrl(item.icon, FALLBACK_GIFT_ICON),
  rawIcon: item.icon || "",
  price: Number(item.price || 0),
})

const selectGift = (item) => {
  selectedGiftId.value = item.id
}

const setQuantity = (value) => {
  const next = Math.max(1, Math.min(999, Number.parseInt(value || 1, 10) || 1))
  quantity.value = next
  customQuantity.value = next
}

const handleWalletClick = () => {
  if (isLogin.value) {
    router.push("/center/dollar/wallet")
    return
  }
  emits("requireLogin")
}

const sendSelectedGift = async () => {
  if (!isLogin.value) {
    emits("requireLogin")
    return
  }
  if (isBoardDisabled.value) {
    $modal.msgWarning(disabledReasonText.value)
    return
  }
  if (!selectedGift.value) {
    $modal.msgWarning("请选择礼物")
    return
  }
  if (!hasEnoughBalance.value) {
    $modal.msgWarning("余额不足，无法送出礼物")
    return
  }

  const gift = selectedGift.value
  const count = Number(quantity.value || 1)
  sending.value = true
  try {
    await giftApi.rewardGift({
      presentId: gift.id,
      number: count,
      roomId: props.roomId,
    })
    emits("gift-sent", {
      giftId: gift.id,
      giftName: gift.name,
      icon: gift.icon,
      count,
      number: count,
      price: gift.price,
      unitPrice: gift.price,
      totalPrice: Number(gift.price || 0) * count,
      senderId: currentUserId.value,
      senderName: currentUserName.value,
    })
    $modal.msgSuccess(`已送出 ${gift.name} x ${count}`)
    await getWallet()
  } catch (error) {
    $modal.msgError(error?.message || "送礼失败，请稍后重试")
  } finally {
    sending.value = false
  }
}

const getGiftList = async () => {
  loading.value = true
  try {
    const res = await giftApi.getGiftList()
    giftList.value = (res.data || []).map(normalizeGift)
    if (!selectedGiftId.value && giftList.value.length) {
      selectedGiftId.value = giftList.value[0].id
    }
  } catch (error) {
    giftList.value = []
  } finally {
    loading.value = false
  }
}

const getWallet = async () => {
  try {
    const res = await walletApi.getBalance()
    wallet.value = res.data || {}
  } catch (error) {
    wallet.value = {}
  }
}

const formatPrice = (value) => {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) return "0"
  return Number.isInteger(num) ? String(num) : num.toFixed(2)
}

const giftTag = (item) => {
  const price = Number(item.price || 0)
  const name = item.name || ""
  if (/火箭|飞机|冠军|奖杯|钻石/.test(name) || price >= 100) return "全屏"
  if (price >= 50) return "高光"
  if (price <= 10) return "轻礼"
  return ""
}

const giftLevelClass = (item) => {
  const price = Number(item.price || 0)
  if (price >= 100 || /火箭|飞机|冠军|奖杯|钻石/.test(item.name || "")) return "gift-item--premium"
  if (price >= 50) return "gift-item--highlight"
  return "gift-item--basic"
}

const giftFallbackLabel = (name = "") => String(name || "礼").slice(0, 1)

const onGiftIconError = (event) => {
  const img = event.target || event.currentTarget
  if (img && img.tagName === "IMG") {
    img.src = FALLBACK_GIFT_ICON
  }
}
</script>

<style lang="scss" scoped>
.gift-board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 12px;
  align-items: stretch;
}

.gift-panel,
.gift-send-panel {
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 94%, var(--bg-secondary));
}

.gift-panel {
  padding: 10px;
}

.gift-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.gift-tabs {
  display: inline-flex;
  min-width: 0;
  gap: 6px;
  padding: 3px;
  border-radius: 7px;
  background: var(--bg-secondary);
}

.gift-tabs button,
.quantity-presets button {
  height: 28px;
  border: 0;
  border-radius: 5px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.gift-tabs button {
  min-width: 52px;
  padding: 0 10px;
}

.gift-tabs button.active,
.quantity-presets button.active {
  color: var(--accent);
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.wallet-chip {
  display: inline-flex;
  align-items: center;
  max-width: 150px;
  height: 34px;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid color-mix(in srgb, var(--accent) 22%, var(--border));
  border-radius: 17px;
  color: var(--accent);
  background: var(--accent-light);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.wallet-chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 9px;
  min-height: 112px;
  max-height: 246px;
  overflow-y: auto;
  padding-right: 2px;
}

.gift-item {
  position: relative;
  display: grid;
  grid-template-rows: 42px auto auto;
  gap: 4px;
  align-items: center;
  justify-items: center;
  min-height: 108px;
  padding: 10px 7px 8px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-primary);
  background: var(--bg-card);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.gift-item::after {
  position: absolute;
  inset: 0;
  content: "";
  opacity: 0;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent) 16%, transparent), transparent 54%);
  transition: opacity 0.18s ease;
}

.gift-item:hover,
.gift-item.active {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 52%, var(--border));
  box-shadow: var(--shadow-hover);
}

.gift-item:hover::after,
.gift-item.active::after {
  opacity: 1;
}

.gift-item.active {
  background: color-mix(in srgb, var(--accent-light) 62%, var(--bg-card));
}

.gift-item--premium {
  border-color: color-mix(in srgb, var(--warning) 34%, var(--border));
}

.gift-icon-wrap {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent-light) 72%, var(--bg-card));
}

.gift-icon-wrap img,
.selected-icon img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.gift-icon-fallback {
  color: var(--accent);
  font-size: 18px;
  font-weight: 900;
}

.gift-item strong,
.gift-item span:not(.gift-tag):not(.gift-icon-wrap):not(.gift-icon-fallback) {
  position: relative;
  z-index: 1;
  max-width: 82px;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-item strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}

.gift-item span:not(.gift-tag):not(.gift-icon-wrap):not(.gift-icon-fallback) {
  color: var(--text-muted);
  font-size: 11px;
}

.gift-tag {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
  height: 18px;
  padding: 0 5px;
  border-radius: 4px;
  color: var(--accent-text);
  background: var(--accent);
  font-size: 10px;
  font-weight: 900;
  line-height: 18px;
}

.gift-send-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.selected-gift {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 54px;
}

.selected-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: var(--accent);
  background:
    radial-gradient(circle at 30% 20%, color-mix(in srgb, var(--accent) 24%, transparent), transparent 44%),
    var(--accent-light);
}

.selected-gift strong,
.selected-gift span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-gift strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 900;
}

.selected-gift span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.field-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.field-label b {
  color: var(--accent);
  font-size: 12px;
}

.quantity-presets {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 8px;
}

.quantity-presets button {
  border: 1px solid var(--border);
  background: var(--bg-card);
}

.quantity-input {
  width: 100%;
}

.gift-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.gift-summary div {
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.gift-summary span,
.gift-summary strong {
  display: block;
}

.gift-summary span {
  color: var(--text-muted);
  font-size: 11px;
}

.gift-summary strong {
  overflow: hidden;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-summary strong.danger,
.gift-status.is-warning {
  color: var(--danger);
}

.gift-status {
  min-height: 18px;
  margin: -2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.send-button {
  height: 38px;
  border-radius: 19px;
  font-weight: 900;
}

.gift-empty {
  display: grid;
  min-height: 112px;
  place-items: center;
}

.gift-skeleton {
  min-height: 108px;
  border-radius: 8px;
  background:
    linear-gradient(90deg, transparent, color-mix(in srgb, var(--accent) 8%, var(--bg-card)), transparent),
    var(--bg-secondary);
  background-size: 220% 100%;
  animation: giftSkeleton 1.2s ease-in-out infinite;
}

@keyframes giftSkeleton {
  0% { background-position: 120% 0; }
  100% { background-position: -120% 0; }
}

@media (max-width: 860px) {
  .gift-board {
    grid-template-columns: 1fr;
  }

  .gift-send-panel {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(180px, 240px);
    align-items: center;
  }

  .gift-summary,
  .quantity-block,
  .send-button,
  .gift-status {
    grid-column: auto;
  }
}

@media (max-width: 640px) {
  .gift-toolbar,
  .gift-send-panel {
    grid-template-columns: 1fr;
  }

  .gift-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .wallet-chip {
    justify-content: center;
    max-width: none;
  }

  .gift-send-panel {
    display: flex;
  }
}
</style>
