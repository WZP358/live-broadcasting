<template>
  <div class="auth-page">
    <header class="auth-topbar">
      <button class="auth-brand" type="button" @click="route.push('/')">
        <span class="auth-brand__mark">PL</span>
        <span class="auth-brand__copy">
          <strong>PulseLive</strong>
          <em>弹幕互动直播平台</em>
        </span>
      </button>
      <nav class="auth-nav">
        <button type="button" @click="route.push('/')">首页</button>
        <button type="button" @click="route.push('/search')">搜索</button>
        <button type="button" @click="route.push('/login')">登录</button>
      </nav>
    </header>

    <main v-if="!success" class="auth-shell">
      <section class="live-showcase">
        <div class="showcase-player">
          <div class="player-toolbar">
            <span>新人推荐</span>
            <strong>直播中</strong>
          </div>
          <div class="danmaku danmaku--one">新号也能直接看直播</div>
          <div class="danmaku danmaku--two">关注主播后不迷路</div>
          <div class="danmaku danmaku--three">开播中心入口已准备</div>
          <div class="player-caption">
            <span class="room-tag">加入平台</span>
            <h1>注册后开启完整互动体验</h1>
            <p>弹幕、关注、观看历史、钱包和开播中心都会跟随账号保存。</p>
          </div>
        </div>

        <div class="showcase-grid">
          <article class="showcase-tile">
            <span>账号权益</span>
            <strong>发送弹幕</strong>
            <em>参与直播互动</em>
          </article>
          <article class="showcase-tile">
            <span>内容沉淀</span>
            <strong>关注与历史</strong>
            <em>快速回到常看房间</em>
          </article>
          <article class="showcase-tile">
            <span>主播工具</span>
            <strong>网页开播</strong>
            <em>一键进入直播</em>
          </article>
        </div>

        <div class="rank-panel">
          <div class="rank-panel__head">
            <h2>热门分区</h2>
            <span>推荐</span>
          </div>
          <div class="rank-row">
            <span>1</span>
            <strong>游戏赛事</strong>
            <em>弹幕密集</em>
          </div>
          <div class="rank-row">
            <span>2</span>
            <strong>娱乐连麦</strong>
            <em>互动上升</em>
          </div>
          <div class="rank-row">
            <span>3</span>
            <strong>知识分享</strong>
            <em>正在热播</em>
          </div>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-tabs">
          <router-link to="/login">登录</router-link>
          <button class="active" type="button">注册</button>
        </div>

        <div class="card-header">
          <h2>创建账号</h2>
          <p>填写账号信息以加入平台</p>
        </div>

        <a-form ref="formRef" :model="formState" class="register-form" :rules="rules" layout="vertical">
          <a-form-item name="nickname" label="用户名">
            <a-input v-model:value="formState.nickname" size="large" type="text" placeholder="请输入对外展示的用户名" />
          </a-form-item>
          <a-form-item name="username" label="用户账号">
            <a-input v-model:value="formState.username" size="large" type="text" placeholder="请输入登录账号" />
          </a-form-item>
          <a-form-item name="password" label="登录密码">
            <a-input-password v-model:value="formState.password" size="large" placeholder="请输入登录密码" />
          </a-form-item>
          <a-form-item name="passwordConfirm" label="确认密码">
            <a-input-password v-model:value="formState.passwordConfirm" size="large" placeholder="请输入确认密码" @keyup.enter="submitForm" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" size="large" block class="auth-submit" @click="submitForm">注册</a-button>
          </a-form-item>
        </a-form>

        <div class="auth-card__footer">
          <router-link to="/login">已有账号？<span>去登录</span></router-link>
        </div>
      </section>
    </main>

    <main v-else class="result-shell">
      <section class="result-card">
        <span class="result-badge">注册完成</span>
        <h1>注册成功</h1>
        <p>欢迎加入 PulseLive，现在可以登录并进入直播平台。</p>
        <a-button type="primary" size="large" @click="route.push('/login')">去登录</a-button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import $modal from "@/utils/message"
import { useRouter } from "vue-router"
import userApi from "@/api/user"

const route = useRouter()
const formRef = ref({})
const formState = reactive({
  nickname: "",
  username: "",
  password: "",
  passwordConfirm: "",
})
const success = ref(false)
const rules = {
  nickname: [{ required: true, message: "请输入用户名!" }],
  username: [{ required: true, message: "请输入用户账号!" }],
  password: [{ required: true, message: "请输入登录密码!" }],
  passwordConfirm: [
    {
      required: true,
      message: "请输入确认密码!",
    },
    {
      validator: (rule, value, callback) => {
        if (value && value !== formState.password) {
          return Promise.reject("两次输入的密码不一致")
        }
        return Promise.resolve()
      },
    },
  ],
}

const submitForm = () => {
  formRef.value
    .validateFields()
    .then(() => {
      userApi
        .register(formState)
        .then((res) => {
          if (res.code === 0) {
            $modal.msgSuccess("注册成功")
            success.value = true
          } else {
            $modal.msgError(res.msg || "注册失败")
          }
        })
        .catch((error) => {
          $modal.msgError(error?.message || "注册失败，请稍后重试")
        })
    })
    .catch((error) => {
      if (error?.message && !String(error.message).includes("validate")) {
        $modal.msgError(error.message)
      }
    })
}
</script>

<style lang="scss" scoped>
.auth-page {
  min-height: 100vh;
  padding: 22px 24px 42px;
  background:
    linear-gradient(180deg, var(--bg-header) 0, var(--bg-header) 300px, var(--bg-primary) 300px),
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
  color: var(--header-text-active);
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
  color: var(--header-text);
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
  color: var(--header-text-active);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
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
.auth-card,
.result-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--shadow-hover);
}

.showcase-player {
  position: relative;
  min-height: 390px;
  overflow: hidden;
  border-color: color-mix(in srgb, var(--accent) 26%, var(--border));
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--player-bg) 88%, transparent), color-mix(in srgb, var(--player-bg) 34%, transparent)),
    linear-gradient(
      135deg,
      var(--player-bg),
      color-mix(in srgb, var(--accent) 26%, var(--player-bg)) 52%,
      color-mix(in srgb, var(--success) 20%, var(--player-bg))
    );
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
  max-width: 260px;
  padding: 7px 10px;
  border-radius: 4px;
  color: #fff;
  background: color-mix(in srgb, var(--player-bg) 66%, transparent);
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
  color: var(--accent-text);
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
  background: var(--bg-card);
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
  background: var(--bg-card);
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

.register-form {
  :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  :deep(.ant-form-item-label > label) {
    color: var(--text-primary);
    font-weight: 800;
  }
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
  color: var(--text-muted);
  font-size: 12px;
  text-align: right;
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

.result-shell {
  max-width: 520px;
  margin: 76px auto 0;
}

.result-card {
  padding: 38px;
  text-align: center;
}

.result-badge {
  display: inline-flex;
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  color: var(--accent);
  background: var(--accent-light);
  font-size: 12px;
  font-weight: 900;
  line-height: 24px;
}

.result-card h1 {
  margin: 16px 0 8px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 900;
}

.result-card p {
  margin: 0 0 22px;
  color: var(--text-secondary);
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
