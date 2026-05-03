<script setup>
import { computed, reactive, ref } from "vue"
import { message } from "ant-design-vue"
import { useRouter, useRoute } from "vue-router"
import { useStore } from "@/stores"

const router = useRouter()
const route = useRoute()
const store = useStore()
const formRef = ref()

const formState = reactive({
  username: "root",
  password: "123123",
})

const rules = {
  username: [{ required: true, message: "请输入账号" }],
  password: [{ required: true, message: "请输入密码" }],
}

const redirect = computed(() => (route.query.redirect ? decodeURIComponent(route.query.redirect) : ""))

const submitForm = async () => {
  try {
    await formRef.value.validateFields()
    const result = await store.user().login(formState)
    if (!result) {
      return
    }

    const userStore = store.user()
    const target = userStore.isAdmin ? redirect.value || "/system/dashboard" : redirect.value || "/"
    message.success(userStore.isAdmin ? "管理员登录成功" : "登录成功")
    router.push(target)
  } catch (error) {
    // validation error handled by form
  }
}

const goHome = () => {
  router.push("/")
}
</script>

<template>
  <div class="login-page">
    <section class="login-shell">
      <div class="login-brand">
        <button class="brand-mark" type="button" @click="goHome">A</button>
        <div>
          <div class="brand-name">PulseLive</div>
          <div class="brand-desc">统一登录后，系统会根据账号权限自动进入用户端或管理后台。</div>
        </div>
      </div>

      <div class="login-main">
        <div class="login-copy">
          <span class="eyebrow">Live Platform</span>
          <h1>一个入口，进入对应系统</h1>
          <p>
            普通账号登录后进入直播前台与个人中心，管理员账号登录后直接进入后台控制台，不再区分两个网址和两套登录逻辑。
          </p>

          <div class="feature-list">
            <div class="feature-item">
              <strong>统一认证</strong>
              <span>用户与管理员共用一套登录入口</span>
            </div>
            <div class="feature-item">
              <strong>自动分流</strong>
              <span>根据角色自动跳转前台或后台</span>
            </div>
            <div class="feature-item">
              <strong>直播闭环</strong>
              <span>开播、观看、管理都在同一平台完成</span>
            </div>
          </div>
        </div>

        <div class="login-card">
          <div class="card-header">
            <h2>账号登录</h2>
            <p>输入账号和密码继续</p>
          </div>

          <a-form ref="formRef" :model="formState" class="login-form" :rules="rules" layout="vertical">
            <a-form-item name="username" label="账号">
              <a-input v-model:value="formState.username" size="large" placeholder="请输入账号" />
            </a-form-item>
            <a-form-item name="password" label="密码">
              <a-input-password v-model:value="formState.password" size="large" placeholder="请输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" size="large" block @click="submitForm">登录并进入系统</a-button>
            </a-form-item>
          </a-form>

          <div class="login-footer">
            <router-link to="/register">没有账号？去注册</router-link>
            <span v-if="redirect" class="redirect-tip">登录后将返回原目标页面</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  padding: 40px 24px;
  background:
    radial-gradient(circle at top left, rgba(14, 116, 144, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(37, 99, 235, 0.16), transparent 30%),
    linear-gradient(135deg, #f6f8fc 0%, #eef3fb 42%, #f8fafc 100%);
}

.login-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, #1677ff, #0f4cdd);
  color: #fff;
  font-size: 24px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 18px 34px rgba(22, 119, 255, 0.22);
}

.brand-name {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.brand-desc {
  margin-top: 4px;
  color: #64748b;
}

.login-main {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) 420px;
  gap: 28px;
  margin-top: 32px;
}

.login-copy,
.login-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.login-copy {
  padding: 40px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.08);
  color: #0f4cdd;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-copy h1 {
  margin: 18px 0 12px;
  font-size: 42px;
  line-height: 1.15;
  color: #0f172a;
}

.login-copy p {
  max-width: 620px;
  margin: 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.9;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 32px;
}

.feature-item {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  border: 1px solid #dbeafe;
}

.feature-item strong {
  display: block;
  margin-bottom: 10px;
  font-size: 16px;
  color: #0f172a;
}

.feature-item span {
  color: #64748b;
  line-height: 1.7;
}

.login-card {
  padding: 28px;
}

.card-header h2 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.card-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.login-form {
  margin-top: 24px;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 14px;
}

.redirect-tip {
  color: #94a3b8;
}

@media (max-width: 960px) {
  .login-main {
    grid-template-columns: 1fr;
  }

  .feature-list {
    grid-template-columns: 1fr;
  }
}
</style>
