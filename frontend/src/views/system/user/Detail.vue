<template>
  <a-modal title="用户详情" :open="visible" :footer="null" width="820px" @cancel="handleCancel">
    <a-descriptions :column="2" bordered size="small">
      <a-descriptions-item label="用户ID">{{ detailData.id || "-" }}</a-descriptions-item>
      <a-descriptions-item label="账号">{{ detailData.username || "-" }}</a-descriptions-item>
      <a-descriptions-item label="昵称">{{ detailData.nickname || "-" }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="detailData.disabled === 0 ? 'success' : 'error'">
          {{ detailData.disabled === 0 ? "正常" : "已封禁" }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="性别">{{ sexText }}</a-descriptions-item>
      <a-descriptions-item label="手机">{{ detailData.mobile || "-" }}</a-descriptions-item>
      <a-descriptions-item label="邮箱" :span="2">{{ detailData.email || "-" }}</a-descriptions-item>
      <a-descriptions-item label="个性签名" :span="2">{{ detailData.signature || "-" }}</a-descriptions-item>
      <a-descriptions-item label="头像地址" :span="2">{{ detailData.avatar || "-" }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ detailData.createTime || "-" }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ detailData.updateTime || "-" }}</a-descriptions-item>
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

const sexText = computed(() => {
  if (props.detailData.sex === 1) return "男"
  if (props.detailData.sex === 2) return "女"
  return "未知"
})

const handleCancel = () => {
  emit("update:visible", false)
}
</script>
