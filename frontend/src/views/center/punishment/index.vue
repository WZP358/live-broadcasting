<template>
  <div class="security-status">
    <section class="security-main">
      <div class="score-card">
        <span class="score-number">100</span>
        <span class="score-text">运营评分</span>
      </div>
      <div class="security-info">
        <h2 class="security-title">直播间运营情况良好</h2>
        <p class="security-subtitle">最近无违规记录，请继续保持健康互动氛围。</p>
      </div>
    </section>

    <section class="security-panel">
      <div class="status-row">
        <span>内容状态</span>
        <strong>正常</strong>
      </div>
      <div class="status-row">
        <span>最近记录</span>
        <strong>暂无违规</strong>
      </div>
      <div class="status-row">
        <span>互动建议</span>
        <strong>保持友好弹幕和清晰直播标题</strong>
      </div>
    </section>
  </div>
</template>

<script setup></script>

<style lang="scss" scoped>
.security-status {
  display: grid;
  gap: 16px;
}

.security-main,
.security-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.security-main {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 30px;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 15%, transparent), transparent),
    var(--bg-card);
}

.score-card {
  width: 150px;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 8px;
  background: var(--accent-gradient);
  color: var(--accent-text);
  text-align: center;
}

.score-number {
  display: block;
  margin-bottom: 4px;
  font-size: 48px;
  font-weight: 900;
  line-height: 1;
}

.score-text {
  display: block;
  font-size: 14px;
  font-weight: 800;
}

.security-info {
  flex: 1;
}

.security-title {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 900;
  line-height: 1.2;
}

.security-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
}

.security-panel {
  padding: 18px 22px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.status-row:last-child {
  border-bottom: 0;
}

.status-row span {
  color: var(--text-secondary);
}

.status-row strong {
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .security-main {
    align-items: flex-start;
    flex-direction: column;
    gap: 24px;
    padding: 24px;
  }

  .score-card {
    width: 130px;
    height: 130px;
  }

  .score-number {
    font-size: 40px;
  }

  .security-title {
    font-size: 24px;
  }

  .status-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
