<template>
  <div>
    <div v-for="item in itemList" :key="item.title" class="security-item">
      <div class="icon-wrapper">
        <svg class="icon" aria-hidden="true">
          <use :xlink:href="item.icon"></use>
        </svg>
      </div>
      <div class="info-wrapper">
        <div class="info">
          <span class="label">{{ item.title }}</span>
          <span class="tag" v-if="item.status"><CheckCircleFilled />{{ item.statusString }}</span>
        </div>
        <span class="describe">{{ item.subtitle }}</span>
      </div>
      <div class="btn-wrapper">
        <a-button @click="handleClick(item)">绑定</a-button>
      </div>
    </div>
    <a-modal v-model:open="open" title="Basic Modal" @ok="handleOk">
      <p>Some contents...</p>
      <p>Some contents...</p>
      <p>Some contents...</p>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from "vue"
import { useStore } from "@/stores"
import { CheckCircleFilled } from "@ant-design/icons-vue"

const open = ref(false)
const userInfo = computed(() => useStore().user().userInfo)

const handleClick = (item) => {
  console.log(item)
  open.value = true
}

const handleOk = () => {}

const itemList = ref([
  {
    title: "实名认证",
    icon: "#icon-id-card-front",
    value: userInfo.value.hasAuth,
    subtitle: "实名认证成功后,可以享受开通直播间等服务",
    type: "phone",
    jumpMode: "modal",
    status: !!userInfo.value.hasAuth,
    statusString: "已认证",
  },
  {
    title: "支付宝",
    icon: "#icon-zhifubaozhifu",
    value: userInfo.value.hasAuth,
    subtitle: "绑定支付宝后,可以支持开心果提现等服务",
    type: "phone",
    jumpMode: "modal",
    status: !!userInfo.value.hasAuth,
    statusString: "已绑定",
  },
])
</script>

<style lang="scss" scoped>
.security-item {
  height: 60px;
  display: flex;
  flex-direction: row;
  padding: 5px;
  align-items: center;
  .icon-wrapper {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    margin: 0px 20px;
    .icon {
      width: 30px;
      height: 30px;
    }
  }
  .info-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .info {
      display: flex;
      flex-direction: row;
      align-items: center;
      .label {
        font-size: 16px;
      }
      .tag {
        margin-left: 20px;
        color: green;
        :nth-child(1) {
          margin-right: 2px;
        }
      }
    }
    .describe {
      display: block;
      font-size: 12px;
      color: #666;
    }
  }
  .btn-wrapper {
    width: 80px;
  }
}
</style>
