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
  min-height: 68px;
  display: flex;
  flex-direction: row;
  gap: 12px;
  padding: 12px 0;
  align-items: center;

  & + .security-item {
    border-top: 1px solid var(--border);
  }

  .icon-wrapper {
    width: 40px;
    height: 40px;
    flex: 0 0 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--accent);
    background: color-mix(in srgb, var(--accent) 10%, var(--bg-card));
    border: 1px solid color-mix(in srgb, var(--accent) 18%, var(--border));
    border-radius: 8px;

    .icon {
      width: 21px;
      height: 21px;
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
        font-weight: 900;
      }

      .tag {
        flex: 0 0 auto;
        display: inline-flex;
        align-items: center;
        gap: 3px;
        height: 22px;
        padding: 0 7px;
        border-radius: 4px;
        font-size: 12px;
        line-height: 1.25;
        white-space: nowrap;
        color: var(--success);
        background: color-mix(in srgb, var(--success) 10%, var(--bg-card));

        :nth-child(1) {
          margin-right: 0;
        }
      }
    }

    .describe {
      display: block;
      margin-top: 6px;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 11px;
      line-height: 1.45;
      color: var(--text-muted);
    }
  }

  .btn-wrapper {
    width: 70px;
    flex: 0 0 70px;
    text-align: right;

    :deep(.ant-btn) {
      min-width: 64px;
      height: 32px;
      padding: 0 13px;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 800;
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
