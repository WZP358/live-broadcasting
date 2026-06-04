<template>
  <div class="comment-page">
    <section class="comment-hero">
      <div>
        <span>互动记录</span>
        <h2>评论与弹幕</h2>
        <p>这里会整理你在直播间产生的互动内容，方便回看近期发言。</p>
      </div>
      <a-button type="primary" @click="$router.push('/home')">去看直播</a-button>
    </section>

    <section class="comment-panel">
      <a-empty description="暂无评论记录" />
    </section>
  </div>
</template>

<script setup></script>

<style lang="scss" scoped>
.comment-page {
  display: grid;
  gap: 16px;
}

.comment-hero,
.comment-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.comment-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 13%, transparent), transparent),
    var(--bg-card);
}

.comment-hero span {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 900;
}

.comment-hero h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.comment-hero p {
  margin: 0;
  color: var(--text-secondary);
}

.comment-panel {
  min-height: 360px;
  display: grid;
  place-items: center;
  padding: 32px;
}

@media (max-width: 720px) {
  .comment-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
