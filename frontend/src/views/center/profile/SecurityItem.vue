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
        <span class="describe">{{ item.subtitle || "" }}</span>
      </div>
      <div class="btn-wrapper">
        <a-button :disabled="item.disabled" @click="handleClick(item)">{{ item.actionText }}</a-button>
      </div>
    </div>
    <EmailBindModal ref="emailBindModalRef" :bind="false" />
    <PhoneBindModal ref="phoneBindModalRef" :bind="false" />
  </div>
</template>

<script setup>
import { ref, computed } from "vue"
import { useStore } from "@/stores"
import { CheckCircleFilled } from "@ant-design/icons-vue"
import EmailBindModal from "./EmailBindModal.vue"
import PhoneBindModal from "./PhoneBindModal.vue"
import $modal from "@/utils/message"

const userInfo = computed(() => useStore().user().userInfo)
const emailBindModalRef = ref(null)
const phoneBindModalRef = ref(null)

const handleClick = (item) => {
  if (item.type === "email") {
    emailBindModalRef.value.show()
  } else if (item.type === "phone") {
    phoneBindModalRef.value.show()
  } else if (item.type === "password") {
    $modal.msgWarning("密码修改需要验证原密码，当前课程演示版本暂未开放")
  }
}

const itemList = computed(() => [
  {
    title: "安全手机",
    icon: "#icon-shouji",
    value: userInfo.value.mobile,
    subtitle: `${userInfo.value.mobile ? userInfo.value.mobile : "手机号"}可用于登录、身份验证、密码找回、通知接收`,
    type: "phone",
    jumpMode: "modal",
    status: !!userInfo.value.mobile,
    statusString: "已绑定",
    actionText: userInfo.value.mobile ? "换绑" : "绑定",
  },
  {
    title: "安全邮箱",
    icon: "#icon-youxiang",
    value: userInfo.value.email,
    subtitle: `${userInfo.value.email ? userInfo.value.email : "邮箱"}可用于登录、身份验证、密码找回、通知接收`,
    type: "email",
    jumpMode: "modal",
    status: !!userInfo.value.email,
    statusString: "已绑定",
    actionText: userInfo.value.email ? "换绑" : "绑定",
  },
  {
    title: "登录密码",
    icon: "#icon-mima",
    subtitle: userInfo.value.password ? `为了您的账号安全，建议定期修改密码` : "请设置密码，可通过账号+密码登录",
    type: "password",
    jumpMode: "modal",
    status: !!userInfo.value.password,
    statusString: "已设置",
    actionText: userInfo.value.password ? "修改" : "设置",
    disabled: true,
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
    background-color: color-mix(in srgb, var(--accent) 10%, var(--bg-card));
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
</style>
