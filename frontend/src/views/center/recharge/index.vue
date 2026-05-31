<template>
  <div class="recharge-page">
    <section class="recharge-hero">
      <div>
        <h2>充值中心</h2>
        <p>统一整理充值档位与收银入口，界面风格与钱包、账单页保持一致。</p>
      </div>
      <div class="recharge-hero__tips">
        <QuestionCircleOutlined />
        <span>使用支付宝沙箱收银台完成充值</span>
      </div>
    </section>

    <section class="recharge-card">
      <div class="recharge-card__header">
        <h3>选择充值档位</h3>
        <p>点击卡片即可选择套餐，后续可继续扩展优惠活动与支付方式。</p>
      </div>

      <div class="charge-grid">
        <article
          v-for="item in chargeList"
          :key="item.id"
          :class="item.id === currentSelect ? 'charge-item charge-item--active' : 'charge-item'"
          @click="handleItemClick(item)"
        >
          <div>
            <strong>{{ item.value }}</strong>
            <span>开心果</span>
          </div>
          <b>{{ "¥" + item.fee }}</b>
        </article>
      </div>

      <div class="recharge-actions">
        <a-button type="primary" size="large" :loading="paying" @click="recharge">前往收银台</a-button>
        <div class="agreement">
          <a-checkbox :checked="true">我已阅读并同意<a>《AntLive 开心果用户协议》</a></a-checkbox>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from "vue"
import { useRouter } from "vue-router"
import WalletApi from "@/api/wallet"
import { QuestionCircleOutlined } from "@ant-design/icons-vue"
import $modal from "@/utils/message"

const router = useRouter()
const currentSelect = ref(1)
const paying = ref(false)
let payWatcher = null
const chargeList = ref([
  { id: 1, value: 6, fee: "6.00" },
  { id: 2, value: 10, fee: "10.00" },
  { id: 3, value: 50, fee: "50.00" },
  { id: 4, value: 100, fee: "100.00" },
  { id: 5, value: 128, fee: "128.00" },
  { id: 6, value: 256, fee: "256.00" },
  { id: 7, value: 328, fee: "328.00" },
  { id: 8, value: 648, fee: "648.00" },
])

const handleItemClick = (item) => {
  currentSelect.value = item.id
}

const readBalance = async () => {
  const res = await WalletApi.getBalance()
  return Number(res.data?.balance || 0)
}

const stopPayWatcher = () => {
  if (payWatcher) {
    window.clearInterval(payWatcher)
    payWatcher = null
  }
}

const watchPaymentResult = (cashierWindow, beforeBalance) => {
  stopPayWatcher()
  const startedAt = Date.now()
  payWatcher = window.setInterval(async () => {
    try {
      const currentBalance = await readBalance()
      if (currentBalance > beforeBalance) {
        stopPayWatcher()
        $modal.msgSuccess("充值已到账")
        router.push("/center/dollar/wallet")
        return
      }

      if (cashierWindow.closed && Date.now() - startedAt > 15000) {
        stopPayWatcher()
        $modal.msg("收银台已关闭，若已完成支付请返回钱包页刷新查看")
      }
      if (Date.now() - startedAt > 180000) {
        stopPayWatcher()
      }
    } catch (error) {
      // keep polling; notify callbacks can arrive a few seconds after page return
    }
  }, 3000)
}

const recharge = async () => {
  const fee = chargeList.value[currentSelect.value - 1].fee
  const cashierWindow = window.open("", "_blank")
  if (!cashierWindow) {
    $modal.msgWarning("浏览器阻止了收银台弹窗，请允许弹窗后重试")
    return
  }

  paying.value = true
  try {
    const beforeBalance = await readBalance()
    const res = await WalletApi.recharge({ fee })
    cashierWindow.document.open()
    cashierWindow.document.write(res.data.payHtml)
    cashierWindow.document.close()
    $modal.msgSuccess("已打开支付宝沙箱收银台，支付完成后余额会自动入账")
    watchPaymentResult(cashierWindow, beforeBalance)
  } catch (error) {
    cashierWindow.close()
  } finally {
    paying.value = false
  }
}

onBeforeUnmount(() => {
  stopPayWatcher()
})
</script>

<style lang="scss" scoped>
.recharge-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.recharge-hero,
.recharge-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
}

.recharge-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 26px;
}

.recharge-hero h2 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 24px;
}

.recharge-hero p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.recharge-hero__tips {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
}

.recharge-card {
  padding: 24px 26px;
}

.recharge-card__header h3 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 20px;
}

.recharge-card__header p {
  margin: 0 0 18px;
  color: #64748b;
}

.charge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.charge-item {
  padding: 22px 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.charge-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.08);
}

.charge-item strong,
.charge-item span,
.charge-item b {
  display: block;
}

.charge-item strong {
  color: #0f172a;
  font-size: 28px;
}

.charge-item span {
  margin-top: 4px;
  color: #64748b;
}

.charge-item b {
  margin-top: 18px;
  color: #0f766e;
  font-size: 22px;
}

.charge-item--active {
  border-color: #2563eb;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.9), rgba(255, 255, 255, 1));
}

.recharge-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
}

.agreement {
  color: #64748b;
}

@media (max-width: 960px) {
  .recharge-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
