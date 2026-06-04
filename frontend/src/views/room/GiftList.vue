<template>
  <div class="gift-board">
    <div class="gift-scroller">
      <a-popover v-for="item in giftList" :key="item.id" overlayClassName="gift-popover" placement="topLeft" trigger="hover">
        <template #content>
          <div class="gift-popover__head">
            <img :src="item.icon" alt="" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.price }} 开心果</span>
              <p>{{ item.description || "送礼会增加本房间亲密值" }}</p>
            </div>
          </div>
          <div class="gift-popover__actions">
            <a-button @click="handleItemClick(1, item)">1 个</a-button>
            <a-button @click="handleItemClick(10, item)">10 个</a-button>
            <a-button type="primary" @click="handleItemClick(100, item)">100 个</a-button>
          </div>
        </template>
        <button class="gift-item" type="button">
          <img :src="item.icon" alt="" />
          <strong>{{ item.name }}</strong>
          <span>{{ item.price }}开心果</span>
        </button>
      </a-popover>
    </div>

    <button class="wallet-card" type="button" @click="handleWalletClick">
      <img src="../../../src/assets/img/开心果.png" alt="" />
      <span>{{ isLogin ? `余额 ${wallet.balance || "0"}` : "登录后送礼" }}</span>
    </button>
  </div>
</template>

<script setup>
import giftApi from "@/api/gift"
import walletApi from "@/api/wallet"
import { useStore } from "@/stores"
import { onMounted, ref, defineProps, computed } from "vue"
import { useRouter } from "vue-router"
import $modal from "@/utils/message"

onMounted(async () => {
  getGiftList()
  if (isLogin.value) {
    getWallet()
  }
})

const giftList = ref([])
const wallet = ref({})
const router = useRouter()
const isLogin = computed(() => {
  return useStore().user().isLogin
})

const props = defineProps({
  roomId: {
    type: Number,
    default: undefined,
  },
})

const emits = defineEmits(["requireLogin"])

const handleWalletClick = () => {
  if (isLogin.value) {
    router.push("/center/dollar/wallet")
    return
  }
  emits("requireLogin")
}

const handleItemClick = async (num, item) => {
  if (!isLogin.value) {
    emits("requireLogin")
    return
  }
  try {
    await giftApi.rewardGift({
      presentId: item.id,
      number: num,
      roomId: props.roomId,
    })
    $modal.msgSuccess(`已送出 ${num} 个${item.name}`)
    await getWallet()
  } catch (error) {
    $modal.msgError("送礼失败，请稍后重试")
  }
}

const getGiftList = async () => {
  try {
    const res = await giftApi.getGiftList()
    const { data } = res
    giftList.value = data || []
  } catch (error) {
    giftList.value = []
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
</script>

<style lang="scss" scoped>
.gift-board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 126px;
  gap: 12px;
  align-items: stretch;
}

.gift-scroller {
  display: flex;
  min-width: 0;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: thin;
}

.gift-item,
.wallet-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.gift-item {
  display: grid;
  flex: 0 0 92px;
  min-height: 92px;
  place-items: center;
  gap: 3px;
  padding: 10px 8px;
}

.gift-item:hover,
.wallet-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 153, 0, 0.3);
  background: var(--accent-light);
  box-shadow: var(--shadow);
}

.gift-item img,
.wallet-card img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.gift-item strong {
  max-width: 76px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-item span,
.wallet-card span {
  color: var(--text-muted);
  font-size: 11px;
}

.wallet-card {
  display: grid;
  place-items: center;
  gap: 5px;
  padding: 10px;
}

.wallet-card span {
  color: var(--accent);
  font-weight: 800;
  text-align: center;
}

.gift-popover__head {
  display: flex;
  gap: 10px;
  max-width: 260px;
}

.gift-popover__head img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.gift-popover__head strong,
.gift-popover__head span,
.gift-popover__head p {
  display: block;
}

.gift-popover__head strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 900;
}

.gift-popover__head span {
  margin-top: 3px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 800;
}

.gift-popover__head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.gift-popover__actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

@media (max-width: 640px) {
  .gift-board {
    grid-template-columns: 1fr;
  }

  .wallet-card {
    grid-auto-flow: column;
    justify-content: center;
  }
}
</style>
