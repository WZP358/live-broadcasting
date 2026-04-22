<template>
  <div class="center-panel">
    <section class="center-panel__header">
      <div>
        <h2>我的关注</h2>
        <p>统一展示已关注的主播与直播间，方便快速回到常看的内容。</p>
      </div>
      <span class="center-panel__count">共 {{ total }} 个关注</span>
    </section>

    <section class="center-panel__body">
      <a-spin :spinning="spinning">
        <div v-if="list.length" class="center-card-grid">
          <Item v-for="item in list" :key="item.id" v-bind="item" />
        </div>
        <a-empty v-else description="暂无关注内容" class="center-panel__empty" />
      </a-spin>
    </section>

    <section class="center-panel__footer">
      <a-pagination :current="current" :total="total" :page-size="pageSize" show-less-items @change="onChange" />
    </section>
  </div>
</template>

<script setup>
import Item from "../view-history/item.vue"
import liveApi from "@/api/live"
import { onMounted, reactive, ref } from "vue"

const pageSize = 12
const current = ref(1)
const total = ref(0)
const list = reactive([])
const spinning = ref(false)

onMounted(() => {
  listData()
})

const listData = async () => {
  spinning.value = true
  try {
    const res = await liveApi.listHistory({ type: 1, page: current.value, limit: pageSize })
    const { list: data, total: totalNum } = res.data
    list.splice(0, list.length)
    if (data) {
      list.push(...data)
    }
    total.value = totalNum
  } finally {
    spinning.value = false
  }
}

const onChange = (currentPageNo) => {
  current.value = currentPageNo
  listData()
}
</script>

<style scoped lang="scss">
.center-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.center-panel__header,
.center-panel__body,
.center-panel__footer {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
}

.center-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 26px;
}

.center-panel__header h2 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 24px;
}

.center-panel__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.center-panel__count {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.center-panel__body {
  min-height: 380px;
  padding: 22px;
}

.center-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(248px, 1fr));
  gap: 18px;
}

.center-panel__empty {
  padding: 48px 0;
}

.center-panel__footer {
  display: flex;
  justify-content: center;
  padding: 18px 20px;
}

@media (max-width: 960px) {
  .center-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
