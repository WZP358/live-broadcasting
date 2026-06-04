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
  color: var(--header-text-active);
  border-bottom: 1px solid color-mix(in srgb, var(--header-text) 14%, transparent);
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
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent) 26%, transparent);
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
  color: var(--header-text);
}
</style>
