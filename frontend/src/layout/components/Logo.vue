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
  min-height: 72px;
  padding: 0 18px;
  color: var(--header-text-active);
  border-bottom: 1px solid color-mix(in srgb, var(--header-text) 12%, transparent);
  background: color-mix(in srgb, var(--bg-header-soft) 58%, transparent);
}

.logo-badge {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--accent-gradient);
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent) 26%, transparent);
}

.logo-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.logo-copy strong {
  color: var(--header-text-active);
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
}

.logo-copy span {
  font-size: 12px;
  color: var(--header-text);
  white-space: nowrap;
}
</style>
