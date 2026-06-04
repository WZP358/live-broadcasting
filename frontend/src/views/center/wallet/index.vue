<template>
  <div class="wallet-page">
    <section class="wallet-hero">
      <div class="wallet-balance-card">
        <div class="wallet-balance-card__icon">¥</div>
        <div>
          <p>钱包余额</p>
          <h2>{{ balance }}</h2>
          <span>用于礼物打赏、平台消费与收益结算</span>
        </div>
      </div>

      <div class="wallet-actions">
        <a-button type="primary" size="large" @click="gotoCharge">立即充值</a-button>
        <a-button size="large">提现说明</a-button>
      </div>
    </section>

    <div class="wallet-grid">
      <section class="wallet-card">
        <div class="wallet-card__header">
          <div>
            <h3>最近流水</h3>
            <p>展示最近的钱包变动记录，帮助用户快速核对资产变化。</p>
          </div>
        </div>

        <a-table :data-source="dataSource" :columns="columns" size="middle" :pagination="false" row-key="id" />
      </section>

      <section class="wallet-card wallet-card--tips">
        <div class="wallet-card__header">
          <div>
            <h3>钱包说明</h3>
            <p>了解开心果余额的用途、来源与提现规则。</p>
          </div>
        </div>

        <div class="wallet-notes">
          <article class="wallet-note">
            <strong>余额用途</strong>
            <span>用于直播打赏、活动消费和平台内资产结算。</span>
          </article>
          <article class="wallet-note">
            <strong>收入来源</strong>
            <span>可来自充值、礼物收益或平台运营奖励。</span>
          </article>
          <article class="wallet-note">
            <strong>提现说明</strong>
            <span>主播收益结算后可按平台规则发起提现申请。</span>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import walletApi from "@/api/wallet"

const router = useRouter()
const balance = ref(0)
const dataSource = ref([])

const columns = [
  {
    title: "时间",
    dataIndex: "createTime",
    key: "createTime",
    width: 220,
  },
  {
    title: "变化金额",
    dataIndex: "fee",
    key: "fee",
    width: 140,
    customRender: ({ text }) => (Number(text) > 0 ? `+${text}` : `${text}`),
  },
  {
    title: "备注",
    dataIndex: "remark",
    key: "remark",
  },
]

const gotoCharge = () => {
  router.push("/center/dollar/recharge")
}

const getBalance = async () => {
  const res = await walletApi.getBalance()
  if (res.code === 0) {
    balance.value = res.data.balance
  }
}

const listLogs = async () => {
  const res = await walletApi.listRecentWalletLogs()
  if (res.code === 0) {
    dataSource.value = res.data.list || []
  }
}

const refreshWallet = () => {
  getBalance()
  listLogs()
}

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible") {
    refreshWallet()
  }
}

onMounted(() => {
  refreshWallet()
  window.addEventListener("focus", refreshWallet)
  document.addEventListener("visibilitychange", handleVisibilityChange)
})

onBeforeUnmount(() => {
  window.removeEventListener("focus", refreshWallet)
  document.removeEventListener("visibilitychange", handleVisibilityChange)
})
</script>

<style scoped lang="scss">
.wallet-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.wallet-hero,
.wallet-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.wallet-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 24px 26px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 15%, transparent), transparent),
    var(--bg-card);
}

.wallet-balance-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.wallet-balance-card__icon {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: var(--accent-gradient);
  color: var(--accent-text);
  font-size: 28px;
  font-weight: 900;
}

.wallet-balance-card p,
.wallet-balance-card span {
  margin: 0;
  color: var(--text-secondary);
}

.wallet-balance-card h2 {
  margin: 6px 0;
  color: var(--text-primary);
  font-size: 34px;
  font-weight: 900;
}

.wallet-actions {
  display: flex;
  gap: 12px;
}

.wallet-grid {
  display: grid;
  grid-template-columns: 1.35fr 0.95fr;
  gap: 18px;
}

.wallet-card {
  padding: 22px;
}

.wallet-card__header h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 900;
}

.wallet-card__header p {
  margin: 0 0 18px;
  color: var(--text-secondary);
}

.wallet-notes {
  display: grid;
  gap: 12px;
}

.wallet-note {
  padding: 16px 18px;
  border-radius: var(--radius-md);
  background: var(--accent-light);
  border: 1px solid color-mix(in srgb, var(--accent) 22%, var(--border));
}

.wallet-note strong,
.wallet-note span {
  display: block;
}

.wallet-note strong {
  color: var(--text-primary);
  margin-bottom: 6px;
}

.wallet-note span {
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .wallet-hero,
  .wallet-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .wallet-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
