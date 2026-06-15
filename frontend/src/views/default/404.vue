<template>
  <main class="error-page">
    <section class="error-panel">
      <div class="error-stage">
        <span class="live-badge">404</span>
        <h1>这个直播间暂时找不到</h1>
        <p>页面地址可能已经变化，可以回到首页继续查看正在热播的内容。</p>
        <div class="error-actions">
          <a-button type="primary" size="large" @click="handleGoBack">返回首页</a-button>
          <a-button size="large" @click="handleGoSearch">去搜索</a-button>
        </div>
      </div>
      <aside class="error-side">
        <div class="mini-player">
          <span>精彩直播</span>
          <strong>直播中</strong>
        </div>
        <div class="mini-rank">
          <div><span>1</span><strong>峡谷冲分夜</strong><em>42.1万</em></div>
          <div><span>2</span><strong>户外城市漫游</strong><em>18.8万</em></div>
          <div><span>3</span><strong>娱乐连麦</strong><em>9.6万</em></div>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from "vue-router"

const router = useRouter()

const handleGoBack = () => {
  router.push({ path: "/" })
}

const handleGoSearch = () => {
  router.push({ path: "/search" })
}
</script>

<style scoped lang="scss">
.error-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(180deg, var(--bg-header) 0, var(--bg-header) 280px, var(--bg-primary) 280px),
    var(--bg-primary);
}

.error-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  width: min(960px, 100%);
}

.error-stage,
.error-side {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--shadow-hover);
}

.error-stage {
  padding: 42px;
}

.live-badge {
  display: inline-flex;
  height: 26px;
  padding: 0 10px;
  border-radius: 4px;
  color: var(--accent-text);
  background: var(--accent);
  font-size: 12px;
  font-weight: 900;
  line-height: 26px;
}

.error-stage h1 {
  margin: 18px 0 10px;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 900;
  line-height: 1.2;
}

.error-stage p {
  max-width: 520px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.error-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.error-side {
  overflow: hidden;
}

.mini-player {
  height: 170px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 14px;
  color: #fff;
  background:
    linear-gradient(0deg, rgba(5, 6, 9, 0.76), rgba(5, 6, 9, 0.08)),
    linear-gradient(135deg, #242936, #533014 52%, #0b2625);
}

.mini-player span,
.mini-player strong {
  font-size: 12px;
  font-weight: 900;
}

.mini-player strong {
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--danger);
}

.mini-rank {
  padding: 12px 14px;
}

.mini-rank div {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px solid var(--border);
}

.mini-rank div:last-child {
  border-bottom: 0;
}

.mini-rank span {
  color: var(--accent);
  font-weight: 900;
}

.mini-rank strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-rank em {
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
}

@media (max-width: 780px) {
  .error-panel {
    grid-template-columns: 1fr;
  }

  .error-stage {
    padding: 30px 24px;
  }
}
</style>
