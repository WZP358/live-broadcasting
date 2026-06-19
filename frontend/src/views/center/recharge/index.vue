<template>
  <div class="recharge-page">
    <section class="recharge-hero">
      <div>
        <h2>充值中心</h2>
        <p>选择开心果档位，支付完成后可用于直播间送礼互动。</p>
      </div>
      <div class="recharge-hero__tips">
        <QuestionCircleOutlined />
        <span>支付完成后余额会自动到账</span>
      </div>
    </section>

    <section class="recharge-card">
      <div class="recharge-card__header">
        <h3>选择充值档位</h3>
        <p>点击卡片选择套餐，再前往收银台完成支付。</p>
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
          <a-checkbox :checked="true">我已阅读并同意<a>《PulseLive 开心果用户协议》</a></a-checkbox>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeMount, onBeforeUnmount, ref } from "vue"
import { useRouter } from "vue-router"
import WalletApi from "@/api/wallet"
import { QuestionCircleOutlined } from "@ant-design/icons-vue"
import $modal from "@/utils/message"

const router = useRouter()
const currentSelect = ref(1)
const paying = ref(false)
let payWatcher = null
const chargeList = ref([])

onBeforeMount(async () => {
  try {
    const res = await WalletApi.getRechargeTiers()
    chargeList.value = res?.data || []
    if (chargeList.value.length) currentSelect.value = chargeList.value[0].id
  } catch (e) {
    chargeList.value = [
      { id: 1, value: 6, fee: "6.00" },
      { id: 2, value: 10, fee: "10.00" },
      { id: 3, value: 50, fee: "50.00" },
      { id: 4, value: 100, fee: "100.00" },
      { id: 5, value: 128, fee: "128.00" },
      { id: 6, value: 256, fee: "256.00" },
      { id: 7, value: 328, fee: "328.00" },
      { id: 8, value: 648, fee: "648.00" },
    ]
  }
})

const handleItemClick = (item) => {
  currentSelect.value = item.id
}

const readBalance = async () => {
  const res = await WalletApi.getBalance()
  return Number(res.data?.balance || 0)
}

const escapeHtml = (value = "") => String(value)
  .replace(/&/g, "&amp;")
  .replace(/</g, "&lt;")
  .replace(/>/g, "&gt;")
  .replace(/"/g, "&quot;")
  .replace(/'/g, "&#39;")

const buildCashierHtml = (payHtml) => {
  const source = String(payHtml || "")
  const parser = new DOMParser()
  const doc = parser.parseFromString(source, "text/html")
  const form = doc.querySelector("form")
  if (!form) {
    return source
  }

  const action = form.getAttribute("action") || ""
  const method = form.getAttribute("method") || "post"
  const fields = Array.from(form.querySelectorAll("input"))
    .map((input) => {
      const name = input.getAttribute("name")
      if (!name) return ""
      return `<input type="hidden" name="${escapeHtml(name)}" value="${escapeHtml(input.getAttribute("value") || "")}" />`
    })
    .join("")

  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>支付宝沙箱收银台</title>
  <style>
    body{margin:0;min-height:100vh;display:grid;place-items:center;background:#f6f7fb;color:#1f2937;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
    main{width:min(420px,calc(100vw - 32px));padding:28px;border:1px solid #e5e7eb;border-radius:8px;background:#fff;box-shadow:0 18px 45px rgba(15,23,42,.12);text-align:center}
    h1{margin:0 0 10px;font-size:20px}
    p{margin:0 0 18px;color:#64748b;line-height:1.7}
    button{height:42px;padding:0 18px;border:0;border-radius:6px;background:#1677ff;color:#fff;font-weight:700;cursor:pointer}
  </style>
</head>
<body>
  <main>
    <h1>正在进入支付宝沙箱收银台</h1>
    <p>如果没有自动跳转，请点击下方按钮继续。</p>
    <form id="cashierForm" action="${escapeHtml(action)}" method="${escapeHtml(method)}">
      ${fields}
      <button type="submit">进入收银台</button>
    </form>
  </main>
  <script>
    window.setTimeout(function () {
      var form = document.getElementById("cashierForm");
      if (form) form.submit();
    }, 80);
  <\/script>
</body>
</html>`
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
  const selected = chargeList.value.find((item) => item.id === currentSelect.value) || chargeList.value[0]
  if (!selected) {
    $modal.msgWarning("请选择充值档位")
    return
  }
  const fee = selected.fee
  const cashierWindow = window.open("", "_blank")
  if (!cashierWindow) {
    $modal.msgWarning("浏览器阻止了收银台弹窗，请允许弹窗后重试")
    return
  }

  cashierWindow.document.open()
  cashierWindow.document.write("<!doctype html><title>支付宝沙箱收银台</title><p style='font-family:sans-serif;padding:24px'>正在创建支付订单...</p>")
  cashierWindow.document.close()

  paying.value = true
  try {
    const beforeBalance = await readBalance()
    const res = await WalletApi.recharge({ fee })
    if (!res?.data?.payHtml) {
      throw new Error("后端未返回支付宝收银台表单")
    }
    cashierWindow.document.open()
    cashierWindow.document.write(buildCashierHtml(res.data.payHtml))
    cashierWindow.document.close()
    $modal.msgSuccess("已打开收银台，支付完成后余额会自动入账")
    watchPaymentResult(cashierWindow, beforeBalance)
  } catch (error) {
    cashierWindow.close()
    $modal.msgError(error?.message || "支付宝沙箱收银台打开失败")
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
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
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
  color: var(--text-primary);
  font-size: 24px;
}

.recharge-hero p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.recharge-hero__tips {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 18px;
  background: var(--accent-light);
  color: var(--accent);
  font-weight: 800;
}

.recharge-card {
  padding: 24px 26px;
}

.recharge-card__header h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 20px;
}

.recharge-card__header p {
  margin: 0 0 18px;
  color: var(--text-secondary);
}

.charge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.charge-item {
  padding: 22px 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  cursor: pointer;
  transition: all 0.2s ease;
}

.charge-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.charge-item strong,
.charge-item span,
.charge-item b {
  display: block;
}

.charge-item strong {
  color: var(--text-primary);
  font-size: 28px;
}

.charge-item span {
  margin-top: 4px;
  color: var(--text-secondary);
}

.charge-item b {
  margin-top: 18px;
  color: var(--accent);
  font-size: 22px;
}

.charge-item--active {
  border-color: var(--accent);
  background: linear-gradient(135deg, var(--accent-light), var(--bg-card));
}

.recharge-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
}

.agreement {
  color: var(--text-secondary);
}

@media (max-width: 960px) {
  .recharge-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
