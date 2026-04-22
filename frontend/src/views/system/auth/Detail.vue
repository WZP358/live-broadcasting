<template>
  <a-modal :open="visible" title="认证详情" :footer="null" width="920px" @cancel="handleCancel">
    <a-descriptions :column="2" bordered size="small">
      <a-descriptions-item label="记录ID">{{ detailData.id || "-" }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusMeta.color">{{ statusMeta.text }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="用户ID">{{ detailData.userId || "-" }}</a-descriptions-item>
      <a-descriptions-item label="真实姓名">{{ detailData.realName || "-" }}</a-descriptions-item>
      <a-descriptions-item label="身份证号" :span="2">{{ detailData.cardNo || "-" }}</a-descriptions-item>
      <a-descriptions-item label="驳回原因" :span="2">{{ detailData.rejectReason || "-" }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ detailData.createTime || "-" }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ detailData.updateTime || "-" }}</a-descriptions-item>
      <a-descriptions-item label="身份证正面" :span="2">
        <div class="image-grid">
          <a-image v-if="detailData.positiveUrl" :width="220" :src="detailData.positiveUrl" />
          <span v-else>-</span>
        </div>
      </a-descriptions-item>
      <a-descriptions-item label="身份证反面" :span="2">
        <div class="image-grid">
          <a-image v-if="detailData.reverseUrl" :width="220" :src="detailData.reverseUrl" />
          <span v-else>-</span>
        </div>
      </a-descriptions-item>
      <a-descriptions-item label="手持证件照" :span="2">
        <div class="image-grid">
          <a-image v-if="detailData.handUrl" :width="220" :src="detailData.handUrl" />
          <span v-else>-</span>
        </div>
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
import { computed } from "vue"

const props = defineProps({
  visible: { type: Boolean, default: false },
  detailData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible"])

const statusMeta = computed(() => {
  const status = props.detailData.status
  if (status === 1) return { text: "已通过", color: "success" }
  if (status === 2) return { text: "自动通过", color: "processing" }
  if (status === 3) return { text: "已驳回", color: "error" }
  return { text: "待审核", color: "warning" }
})

const handleCancel = () => {
  emit("update:visible", false)
}
</script>

<style scoped lang="scss">
.image-grid {
  display: flex;
  min-height: 48px;
  align-items: center;
}
</style>
