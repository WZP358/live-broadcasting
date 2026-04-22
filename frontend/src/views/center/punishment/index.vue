<template>
  <div class="security-status">
    <div class="security-shield">
      <div class="shield-icon">
        <div class="score">
          <span class="score-number">100</span>
          <span class="score-text">直播间运营评分</span>
        </div>
      </div>
    </div>
    <div class="security-info">
      <h2 class="security-title">直播间运营情况极佳</h2>
      <p class="security-subtitle">请继续保持哦</p>
      <div class="last-login">
        <!-- <span class="login-time">最近于 2025-07-23 00:42:41 </span> -->
        <span class="login-location">最近无违规记录</span>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.security-status {
  background: linear-gradient(135deg, #e8f5e8 0%, #f0f8f0 100%);
  padding: 40px;
  display: flex;
  align-items: center;
  gap: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.security-shield {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.shield-icon {
  position: relative;
  width: 170px;
  height: 170px;
  background-image: url('@/assets/img/icon_high.png');
  background-size: 100% 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.score {
  text-align: center;
}

.score-number {
  display: block;
  font-size: 48px;
  font-weight: bold;
  color: #fff;
  line-height: 1;
  margin-bottom: 4px;
}

.score-text {
  display: block;
  font-size: 14px;
  color: #fff;
  font-weight: 500;
}

.security-info {
  flex: 1;
}

.security-title {
  font-size: 32px;
  font-weight: bold;
  color: #2E7D32;
  margin: 0 0 8px 0;
  line-height: 1.2;
}

.security-subtitle {
  font-size: 16px;
  color: #4CAF50;
  margin: 0 0 24px 0;
}

.last-login {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  padding: 12px 0;
  border-top: 1px solid rgba(76, 175, 80, 0.2);
}

.login-time {
  font-weight: 500;
}

.login-location {
  color: #888;
}

.arrow-icon {
  margin-left: auto;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.last-login:hover .arrow-icon {
  opacity: 1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .security-status {
    flex-direction: column;
    text-align: center;
    padding: 24px;
    gap: 24px;
  }

  .security-title {
    font-size: 24px;
  }

  .shield-icon {
    width: 100px;
    height: 120px;
  }

  .score-number {
    font-size: 36px;
  }
}
</style>
