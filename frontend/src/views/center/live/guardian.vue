<template>
  <div class="guardian-page">
    <section class="guardian-header">
      <div>
        <h2>守护管理</h2>
        <p>查看你的守护关系、粉丝团状态和续费情况。</p>
      </div>
      <a-button @click="refresh">刷新</a-button>
    </section>

    <section class="guardian-card">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="myGuardians" tab="我的守护">
          <a-table :columns="guardianColumns" :data-source="guardians" :pagination="false" row-key="id" size="middle" :loading="guardianLoading">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="levelColor(record.level)">{{ levelName(record.level) }}</a-tag>
              </template>
              <template v-if="column.key === 'expireTime'">
                {{ formatDate(record.expireTime) }}
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" v-if="record.autoRenew" @click="cancelRenew(record)">取消续费</a-button>
                <a-button type="link" size="small" @click="renewGuardian(record)">续费</a-button>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!guardianLoading && guardians.length === 0" description="还没有守护任何主播" />
        </a-tab-pane>
        <a-tab-pane key="myFans" tab="我的粉丝团">
          <a-table :columns="fanColumns" :data-source="fans" :pagination="false" row-key="id" size="middle" :loading="fanLoading">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="levelColor(record.level)">{{ levelName(record.level) }}</a-tag>
              </template>
              <template v-if="column.key === 'expireTime'">
                {{ formatDate(record.expireTime) }}
              </template>
            </template>
          </a-table>
          <a-empty v-if="!fanLoading && fans.length === 0" description="还没有粉丝开通守护" />
        </a-tab-pane>
      </a-tabs>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import guardianApi from '@/api/guardian'
import $modal from '@/utils/message'

const activeTab = ref('myGuardians')
const guardians = ref([])
const fans = ref([])
const guardianLoading = ref(false)
const fanLoading = ref(false)

const guardianColumns = [
  { title: '主播', dataIndex: 'targetUserId', key: 'targetUserId' },
  { title: '守护等级', key: 'level' },
  { title: '到期时间', key: 'expireTime' },
  { title: '操作', key: 'action' }
]
const fanColumns = [
  { title: '粉丝', dataIndex: 'userId', key: 'userId' },
  { title: '守护等级', key: 'level' },
  { title: '到期时间', key: 'expireTime' }
]

const levelName = (lv) => ['', '青铜守护', '白银守护', '黄金守护'][lv] || '青铜守护'
const levelColor = (lv) => ['', 'brown', 'grey', 'gold'][lv] || 'brown'

const formatDate = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}

const loadGuardians = async () => {
  guardianLoading.value = true
  try {
    const res = await guardianApi.myGuardians({ page: 1, limit: 100 })
    guardians.value = (res.data && res.data.records) ? res.data.records : []
  } catch (e) {
    $modal.msgError('加载失败')
  } finally {
    guardianLoading.value = false
  }
}

const loadFans = async () => {
  fanLoading.value = true
  try {
    const res = await guardianApi.myFans({ page: 1, limit: 100 })
    fans.value = (res.data && res.data.records) ? res.data.records : []
  } catch (e) {
    $modal.msgError('加载失败')
  } finally {
    fanLoading.value = false
  }
}

const cancelRenew = async (record) => {
  try {
    await guardianApi.cancelAutoRenew({ targetUserId: record.targetUserId })
    $modal.msgSuccess('已取消自动续费')
    loadGuardians()
  } catch (e) {
    $modal.msgError('操作失败')
  }
}

const renewGuardian = (record) => {
  $modal.msg('续费功能请前往直播间操作')
}

const refresh = () => {
  loadGuardians()
  loadFans()
}

onMounted(() => { refresh() })
</script>

<style scoped lang="scss">
.guardian-page {
  display: grid;
  gap: 16px;
}

.guardian-header,
.guardian-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow);
}

.guardian-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.guardian-header h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.guardian-header p {
  margin: 0;
  color: var(--text-secondary);
}

.guardian-card {
  padding: 18px;
}

@media (max-width: 720px) {
  .guardian-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
