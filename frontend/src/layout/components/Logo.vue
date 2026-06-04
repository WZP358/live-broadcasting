<template>
  <section class="logo-wrapper">
    <div class="logo-badge">PL</div>
    <div class="logo-copy">
      <strong>PulseLive</strong>
      <span>后台管理系统</span>
    </div>
  </section>
</template>

<script setup></script>

<style scoped lang="scss">
.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-badge {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--accent-gradient);
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(255, 153, 0, 0.24);
}

.logo-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo-copy strong {
  font-size: 14px;
  letter-spacing: 0.02em;
}

.logo-copy span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}
</style>
