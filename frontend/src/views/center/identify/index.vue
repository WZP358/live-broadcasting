<template>
  <div class="identify-page">
    <section class="identify-hero">
      <div>
        <span>账号认证</span>
        <h2>实名认证</h2>
        <p>完成认证后可使用主播收益结算、提现等账号能力。</p>
      </div>
      <a-tag color="gold">待认证</a-tag>
    </section>

    <section class="identify-panel">
      <div class="identify-card">
        <strong>认证资料</strong>
        <p>请确认姓名、证件信息与本人一致，提交后平台会尽快处理。</p>
        <a-button type="primary">开始认证</a-button>
      </div>
      <div class="identify-note">
        <span>认证说明</span>
        <p>认证信息仅用于账号安全校验与收益结算，不会在直播间公开展示。</p>
      </div>
    </section>
  </div>
</template>

<script setup></script>

<style scoped lang="scss">
.identify-page {
  display: grid;
  gap: 16px;
}

.identify-hero,
.identify-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow);
}

.identify-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background:
    linear-gradient(90deg, rgba(255, 153, 0, 0.12), rgba(255, 216, 77, 0.05)),
    #fff;
}

.identify-hero span {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 900;
}

.identify-hero h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.identify-hero p,
.identify-card p,
.identify-note p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.identify-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.9fr);
  gap: 16px;
  padding: 18px;
}

.identify-card,
.identify-note {
  min-height: 180px;
  display: grid;
  align-content: center;
  gap: 12px;
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fafbfc;
}

.identify-card strong,
.identify-note span {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.identify-card .ant-btn {
  width: fit-content;
}

@media (max-width: 820px) {
  .identify-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .identify-panel {
    grid-template-columns: 1fr;
  }
}
</style>
