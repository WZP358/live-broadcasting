<script setup>
import { computed, reactive, ref } from "vue"
import $modal from "@/utils/message"
import { useRouter, useRoute } from "vue-router"
import { useStore } from "@/stores"

const router = useRouter()
const route = useRoute()
const store = useStore()
const formRef = ref()
const loading = ref(false)

const formState = reactive({
  username: "",
  password: "",
})

const rules = {
  username: [{ required: true, message: "请输入账号" }],
  password: [{ required: true, message: "请输入密码" }],
}

const redirect = computed(() => (route.query.redirect ? decodeURIComponent(route.query.redirect) : ""))

const submitForm = async () => {
  try {
    await formRef.value.validateFields()
    loading.value = true
    const result = await store.user().login(formState)
    if (!result) {
      $modal.msgError("账号或密码错误")
      return
    }

    const userStore = store.user()
    const target = userStore.isAdmin ? redirect.value || "/system/dashboard" : redirect.value || "/"
    $modal.msgSuccess(userStore.isAdmin ? "管理员登录成功" : "登录成功")
    router.push(target)
  } catch (error) {
    if (error?.message && !String(error.message).includes("validate")) {
      $modal.msgError(error.message)
    }
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push("/")
}

const goSearch = () => {
  router.push("/search")
}

const goLiveCenter = () => {
  router.push("/center/live/live-settings")
}
</script>

<template>
  <div class="auth-page">
    <header class="auth-topbar">
      <button class="auth-brand" type="button" @click="goHome">
        <span class="auth-brand__mark">PL</span>
        <span class="auth-brand__copy">
          <strong>PulseLive</strong>
          <em>弹幕互动直播平台</em>
        </span>
      </button>
      <nav class="auth-nav">
        <button type="button" @click="goHome">首页</button>
        <button type="button" @click="goSearch">搜索</button>
        <button type="button" @click="goLiveCenter">开播</button>
      </nav>
    </header>

    <main class="auth-shell">
      <section class="live-showcase">
        <div class="showcase-player">
          <div class="player-toolbar">
            <span>推荐直播</span>
            <strong>直播中</strong>
          </div>
          <div class="danmaku danmaku--one">今晚这场太热闹了</div>
          <div class="danmaku danmaku--two">主播这波操作可以</div>
          <div class="danmaku danmaku--three">关注走一波</div>
          <div class="player-caption">
            <span class="room-tag">游戏赛事</span>
            <h1>登录后回到你常看的直播间</h1>
            <p>关注、弹幕、礼物和开播中心都在一个账号里。</p>
          </div>
        </div>

        <div class="showcase-grid">
          <article class="showcase-tile">
            <span>热门分区</span>
            <strong>游戏赛事</strong>
            <em>28.4万热度</em>
          </article>
          <article class="showcase-tile">
            <span>正在上升</span>
            <strong>娱乐连麦</strong>
            <em>弹幕互动中</em>
          </article>
          <article class="showcase-tile">
            <span>推荐主播</span>
            <strong>创作直播间</strong>
            <em>正在开播</em>
          </article>
        </div>

        <div class="rank-panel">
          <div class="rank-panel__head">
            <h2>站内热榜</h2>
            <span>实时</span>
          </div>
          <div class="rank-row">
            <span>1</span>
            <strong>峡谷冲分夜</strong>
            <em>42.1万</em>
          </div>
          <div class="rank-row">
            <span>2</span>
            <strong>户外城市漫游</strong>
            <em>18.8万</em>
          </div>
          <div class="rank-row">
            <span>3</span>
            <strong>新人主播首秀</strong>
            <em>9.6万</em>
          </div>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-tabs">
          <button class="active" type="button">登录</button>
          <router-link to="/register">注册</router-link>
        </div>

        <div class="card-header">
          <h2>欢迎回来</h2>
          <p>登录您的账号以继续观看</p>
        </div>

        <a-form ref="formRef" :model="formState" class="login-form" :rules="rules" layout="vertical">
          <a-form-item name="username">
            <template #label>
              <span class="form-label">账号</span>
            </template>
            <a-input v-model:value="formState.username" size="large" placeholder="请输入账号" autocomplete="off" />
          </a-form-item>
          <a-form-item name="password">
            <template #label>
              <span class="form-label">密码</span>
            </template>
            <a-input-password v-model:value="formState.password" size="large" placeholder="请输入密码" autocomplete="off" @keyup.enter="submitForm" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" size="large" block :loading="loading" class="auth-submit" @click="submitForm">
              <span v-if="!loading">登录</span>
            </a-button>
          </a-form-item>
        </a-form>

        <div class="auth-card__footer">
          <router-link to="/register">没有账号？<span>立即注册</span></router-link>
          <span v-if="redirect">登录后返回原页面</span>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  padding: 22px 24px 42px;
  background:
    linear-gradient(180deg, #151820 0, #151820 300px, var(--bg-primary) 300px),
    var(--bg-primary);
}

.auth-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  max-width: 1180px;
  height: 48px;
  margin: 0 auto 22px;
}

.auth-brand,
.auth-nav button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  color: #fff;
}

.auth-brand__mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: var(--accent-gradient);
  font-size: 13px;
  font-weight: 900;
}

.auth-brand__copy {
  display: grid;
  gap: 2px;
  text-align: left;
}

.auth-brand__copy strong {
  font-size: 16px;
  font-weight: 900;
}

.auth-brand__copy em {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-style: normal;
}

.auth-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.auth-nav button {
  height: 34px;
  padding: 0 12px;
  border-radius: 17px;
  color: var(--header-text);
  font-size: 13px;
  font-weight: 800;
}

.auth-nav button:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto;
}

.live-showcase,
.auth-card {
  min-width: 0;
}

.showcase-player,
.rank-panel,
.auth-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-hover);
}

.showcase-player {
  position: relative;
  min-height: 390px;
  overflow: hidden;
  border-color: rgba(255, 255, 255, 0.14);
  background:
    linear-gradient(90deg, rgba(5, 6, 9, 0.86), rgba(5, 6, 9, 0.28)),
    linear-gradient(135deg, #242936, #533014 52%, #0b2625);
}

.player-toolbar {
  position: absolute;
  top: 14px;
  right: 14px;
  left: 14px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
  font-weight: 800;
}

.player-toolbar strong {
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  color: #fff;
  background: var(--danger);
  line-height: 22px;
}

.danmaku {
  position: absolute;
  right: 28px;
  max-width: 240px;
  padding: 7px 10px;
  border-radius: 4px;
  color: #fff;
  background: rgba(5, 6, 9, 0.44);
  font-size: 13px;
  white-space: nowrap;
}

.danmaku--one {
  top: 94px;
}

.danmaku--two {
  top: 150px;
  right: 84px;
}

.danmaku--three {
  top: 208px;
  right: 46px;
}

.player-caption {
  position: absolute;
  right: 32px;
  bottom: 32px;
  left: 32px;
  color: #fff;
}

.room-tag {
  display: inline-flex;
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  color: #fff;
  background: var(--accent);
  font-size: 12px;
  font-weight: 900;
  line-height: 24px;
}

.player-caption h1 {
  margin: 14px 0 8px;
  color: #fff;
  font-size: 32px;
  font-weight: 900;
  line-height: 1.2;
}

.player-caption p {
  margin: 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
}

.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.showcase-tile {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow);
}

.showcase-tile span,
.showcase-tile em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.showcase-tile strong {
  display: block;
  margin: 6px 0 4px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-panel {
  margin-top: 10px;
  padding: 14px;
}

.rank-panel__head,
.rank-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.rank-panel__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.rank-panel__head h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 900;
}

.rank-panel__head span {
  color: var(--accent);
  font-size: 12px;
  font-weight: 800;
}

.rank-row {
  padding: 9px 0;
  border-top: 1px solid var(--border);
}

.rank-row span {
  color: var(--accent);
  font-weight: 900;
}

.rank-row strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

.auth-card {
  align-self: start;
  padding: 22px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 4px;
  border-radius: var(--radius-md);
  background: var(--bg-primary);
}

.auth-tabs button,
.auth-tabs a {
  display: grid;
  height: 34px;
  place-items: center;
  border: 0;
  border-radius: 6px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 14px;
  font-weight: 900;
  text-decoration: none;
}

.auth-tabs .active {
  color: var(--accent);
  background: #fff;
  box-shadow: var(--shadow);
}

.card-header {
  margin: 24px 0;
}

.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.card-header p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.login-form {
  :deep(.ant-form-item) {
    margin-bottom: 18px;
  }
}

.form-label {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper) {
  border-radius: 8px;
  border-color: var(--border);
}

.auth-submit {
  height: 44px;
  border-radius: 8px;
  font-weight: 900;
}

.auth-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-muted);
  font-size: 12px;
}

.auth-card__footer a {
  color: var(--text-secondary);
  text-decoration: none;
}

.auth-card__footer span,
.auth-card__footer a:hover {
  color: var(--accent);
  font-weight: 800;
}

@media (max-width: 940px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-card {
    order: -1;
  }
}

@media (max-width: 620px) {
  .auth-page {
    padding: 14px;
  }

  .auth-topbar {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
  }

  .auth-nav {
    width: 100%;
    overflow-x: auto;
  }

  .showcase-player {
    min-height: 310px;
  }

  .player-caption h1 {
    font-size: 24px;
  }

  .danmaku {
    display: none;
  }

  .showcase-grid {
    grid-template-columns: 1fr;
  }
}
</style>
