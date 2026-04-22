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
        <a-button size="large">提现功能规划中</a-button>
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
            <p>让用户理解余额来源、用途和后续资金动作。</p>
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
            <span>当前项目先保留入口样式，后续可接入真实提现审核流程。</span>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue"
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

onMounted(() => {
  getBalance()
  listLogs()
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
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
}

.wallet-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 24px 26px;
  background:
    radial-gradient(circle at right top, rgba(59, 130, 246, 0.14), transparent 22%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
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
  border-radius: 18px;
  background: linear-gradient(135deg, #0f766e, #38bdf8);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.wallet-balance-card p,
.wallet-balance-card span {
  margin: 0;
  color: #64748b;
}

.wallet-balance-card h2 {
  margin: 6px 0;
  color: #0f172a;
  font-size: 34px;
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
  color: #0f172a;
  font-size: 20px;
}

.wallet-card__header p {
  margin: 0 0 18px;
  color: #64748b;
}

.wallet-notes {
  display: grid;
  gap: 12px;
}

.wallet-note {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.wallet-note strong,
.wallet-note span {
  display: block;
}

.wallet-note strong {
  color: #0f172a;
  margin-bottom: 6px;
}

.wallet-note span {
  color: #64748b;
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
