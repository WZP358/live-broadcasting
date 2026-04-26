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
        <a-button @click="handleClick(item)"> 绑定</a-button>
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

const userInfo = computed(() => useStore().user().userInfo)
const emailBindModalRef = ref(null)
const phoneBindModalRef = ref(null)

const handleClick = (item) => {
  if (item.type === "email") {
    emailBindModalRef.value.show()
  } else if (item.type === "phone") {
    phoneBindModalRef.value.show()
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
  },
  {
    title: "登录密码",
    icon: "#icon-mima",
    subtitle: userInfo.value.password ? `为了您的账号安全，建议定期修改密码` : "请设置密码，可通过账号+密码登录",
    type: "password",
    jumpMode: "modal",
    status: !!userInfo.value.password,
    statusString: "已设置",
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
    background-color: #d6e7ff80;
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
        color: green;
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
      color: #666;
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
</style>
