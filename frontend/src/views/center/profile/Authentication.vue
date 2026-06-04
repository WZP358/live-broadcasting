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
        <a-button @click="handleClick(item)">{{ item.actionText }}</a-button>
      </div>
    </div>
    <a-modal v-model:open="open" :title="activeItem?.title || '账号安全'" @ok="handleOk">
      <p class="modal-title">{{ activeItem?.modalTitle }}</p>
      <p class="modal-desc">{{ activeItem?.modalDesc }}</p>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from "vue"
import { useStore } from "@/stores"
import { CheckCircleFilled } from "@ant-design/icons-vue"

const open = ref(false)
const activeItem = ref(null)
const userInfo = computed(() => useStore().user().userInfo)

const handleClick = (item) => {
  activeItem.value = item
  open.value = true
}

const handleOk = () => {}

const itemList = computed(() => [
  {
    title: "实名认证",
    icon: "#icon-id-card-front",
    value: userInfo.value.hasAuth,
    subtitle: "实名认证成功后,可以享受开通直播间等服务",
    actionText: userInfo.value.hasAuth ? "查看" : "认证",
    modalTitle: userInfo.value.hasAuth ? "实名认证已完成" : "完成实名认证后即可申请开播",
    modalDesc: "请根据页面提示提交真实身份信息，认证通过后可使用更多主播服务。",
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
    actionText: userInfo.value.hasAuth ? "查看" : "绑定",
    modalTitle: userInfo.value.hasAuth ? "支付宝已绑定" : "绑定支付宝用于收益提现",
    modalDesc: "请确认账号信息准确无误，绑定成功后可在钱包中管理收益。",
    type: "phone",
    jumpMode: "modal",
    status: !!userInfo.value.hasAuth,
    statusString: "已绑定",
  },
])
</script>

<style lang="scss" scoped>
.security-item {
  min-height: 56px;
  display: flex;
  flex-direction: row;
  gap: 10px;
  padding: 6px 2px;
  align-items: center;
  .icon-wrapper {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    margin: 0 8px 0 6px;
    .icon {
      width: 24px;
      height: 24px;
    }
  }
  .info-wrapper {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .info {
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 10px;
      min-width: 0;
      .label {
        flex: 0 0 auto;
        font-size: 14px;
        line-height: 1.25;
        white-space: nowrap;
      }
      .tag {
        flex: 0 0 auto;
        margin-left: 0;
        font-size: 12px;
        line-height: 1.25;
        white-space: nowrap;
        color: var(--success);
        :nth-child(1) {
          margin-right: 2px;
        }
      }
    }
    .describe {
      display: block;
      margin-top: 4px;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 11px;
      line-height: 1.35;
      color: var(--text-muted);
    }
  }
  .btn-wrapper {
    width: 66px;
    flex: 0 0 66px;
    :deep(.ant-btn) {
      min-width: 60px;
      height: 32px;
      padding: 0 12px;
      font-size: 13px;
    }
  }
}

.modal-title {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-weight: 800;
}

.modal-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
