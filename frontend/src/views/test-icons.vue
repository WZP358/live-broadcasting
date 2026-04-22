<template>
  <div class="test-icons">
    <h2>图标测试页面</h2>
    <div class="icon-grid">
      <div class="icon-item">
        <SvgIcon icon="jiaoseguanli" size="24px" />
        <span>jiaoseguanli</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="icon-jiaoseguanli" size="24px" />
        <span>icon-jiaoseguanli</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="#icon-jiaoseguanli" size="24px" />
        <span>#icon-jiaoseguanli</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="guanliyuan_jiaoseguanli" size="24px" />
        <span>guanliyuan_jiaoseguanli</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="jiankong" size="24px" />
        <span>jiankong</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="jietu" size="24px" />
        <span>jietu</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="zhiboshuju" size="24px" />
        <span>zhiboshuju</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="jiaoseguanli1" size="24px" />
        <span>jiaoseguanli1</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="jiankong1" size="24px" />
        <span>jiankong1</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="zhiboshuju1" size="24px" />
        <span>zhiboshuju1</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="zhiboshuju2" size="24px" />
        <span>zhiboshuju2</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="zhiboshuju3" size="24px" />
        <span>zhiboshuju3</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="jiaosefenpei" size="24px" />
        <span>jiaosefenpei</span>
      </div>
      <div class="icon-item">
        <SvgIcon icon="caidanguanli" size="24px" />
        <span>caidanguanli</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import SvgIcon from '@/components/SvgIcon/index.vue'
</script>

<style scoped>
.test-icons {
  padding: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #f9f9f9;
}

.icon-item span {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  text-align: center;
}
</style>