<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col v-for="item in metrics" :key="item.label" :xs="24" :sm="12" :lg="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
          <div class="metric-card__hint">{{ item.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <div slot="header"><span>运营概览</span></div>
          <el-table :data="overview" border>
            <el-table-column prop="name" label="指标" />
            <el-table-column prop="value" label="当前值" />
            <el-table-column prop="desc" label="说明" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <div slot="header"><span>快捷入口</span></div>
          <el-button type="primary" plain icon="el-icon-video-camera" @click="$router.push('/live/room')">直播间管理</el-button>
          <el-button type="success" plain icon="el-icon-user" @click="$router.push('/user-center/user')">用户管理</el-button>
          <el-button type="warning" plain icon="el-icon-present" @click="$router.push('/live/gift')">礼物管理</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { dashboardSummary } from '@/api/pulselive'

export default {
  name: 'PulseLiveDashboard',
  data() {
    return {
      metrics: [
        { label: '用户数', value: 0, hint: '平台注册用户' },
        { label: '直播间', value: 0, hint: '已创建直播间' },
        { label: '开播中', value: 0, hint: '当前在线直播' },
        { label: '消息数', value: 0, hint: '累计互动消息' }
      ]
    }
  },
  computed: {
    overview() {
      return this.metrics.map(item => ({ name: item.label, value: item.value, desc: item.hint }))
    }
  },
  created() {
    dashboardSummary().then(res => {
      const data = res.data || {}
      const source = Array.isArray(data.metrics) ? data.metrics : []
      if (source.length) {
        this.metrics = source.map(item => ({
          label: item.label || item.name,
          value: item.value || 0,
          hint: item.hint || item.description || '-'
        }))
      }
    }).catch(() => {})
  }
}
</script>

<style scoped>
.metric-card {
  margin-bottom: 20px;
}
.metric-card__label {
  color: #909399;
  font-size: 13px;
}
.metric-card__value {
  margin: 12px 0;
  color: #303133;
  font-size: 28px;
  font-weight: 600;
}
.metric-card__hint {
  color: #c0c4cc;
  font-size: 12px;
}
.mt20 {
  margin-top: 20px;
}
.el-button {
  margin: 0 10px 10px 0;
}
</style>
