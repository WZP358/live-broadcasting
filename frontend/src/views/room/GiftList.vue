<template>
  <div class="gift-wrapper">
    <a-popover overlayClassName="gift-popover" placement="topLeft" trigger="hover" v-for="(item, index) in giftList"
      :key="item.id">
      <template #content>
        <a-flex>
          <img :src="item.icon" alt="" />
          <a-flex vertical>
            <div>
              <span name>{{ item.name }}</span>
              <span price>{{ item.price }}开心果</span>
            </div>
            <span describe>{{ item.description || "投喂即可加入主播的粉丝团" }}</span>
          </a-flex>
        </a-flex>
        <a-divider />
        <a-flex>
          <a-button @click="handleItemClick(1, item)">1个</a-button>
          <a-button @click="handleItemClick(10, item)">10个</a-button>
          <a-button @click="handleItemClick(100, item)">100个</a-button>
        </a-flex>
      </template>
      <div class="footer-item" vertical align="center">
        <img :src="item.icon" alt="" />
        <span name>{{ item.name }}</span>
        <span price>{{ item.price }}开心果</span>
      </div>
    </a-popover>
  </div>
  <a-divider type="vertical" style="height: 100%" />
  <div class="wallet-wrapper">
    <div class="footer-item" vertical align="center" @click="handleWalletClick">
      <img src="../../../src/assets/img/开心果.png" alt="" />
      <span price style="margin-top: 10px">{{ isLogin ? `余额:${wallet.balance || "0"}个` : "未登录" }}</span>
    </div>
  </div>
</template>

<script setup>
import giftApi from "@/api/gift"
import walletApi from "@/api/wallet"
import { useStore } from "@/stores"
import { onMounted, ref, defineProps, computed } from "vue"
import { useRouter } from "vue-router"

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

const handleWalletClick = () => {
  if (isLogin.value) {
    router.push("/center/dollar/wallet")
  }
}

const handleItemClick = async (num, item) => {
  // TODO: 调用购买接口
  // message.success(`赠送${num}个${item.name}成功`)
  await giftApi.rewardGift({
    presentId: item.id,
    number: num,
    roomId: props.roomId,
  })
}

/**
 * 获取礼物列表
 */
const getGiftList = async () => {
  const res = await giftApi.getGiftList()
  const { data } = res
  giftList.value = data
}

/**
 * 获取用户钱包
 */
const getWallet = async () => {
  const res = await walletApi.getBalance()
  wallet.value = res.data
}
</script>

<style lang="scss" scoped>
.gift-wrapper {
  flex: 1;
  display: flex;

  .footer-item:hover {
    background-color: #f5f5f5;
    cursor: pointer;
  }
}

.wallet-wrapper {
  width: 100px;
  cursor: pointer;
}

.footer-item {
  width: 90px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  img {
    width: 40px;
    height: 40px;
    object-fit: cover;
  }

  span[name] {
    margin-top: 5px;
    font-size: 14px;
    color: $font-color;
  }

  span[price] {
    font-size: 12px;
    color: $font-color-light;
  }
}

.footer-item:hover {
  img {
    transition: transform 0.4s ease;
    animation: shake 0.4s ease-in-out;
  }
}

@keyframes shake {
  0% {
    transform: translate(0, 0);
  }

  25% {
    transform: translate(0, -3px);
  }

  50% {
    transform: translate(0, 0);
  }

  75% {
    transform: translate(0, 3px);
  }

  100% {
    transform: translate(0, 0);
  }
}
</style>
