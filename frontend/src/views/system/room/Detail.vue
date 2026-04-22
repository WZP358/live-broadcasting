<template>
  <a-modal title="直播间详情" :open="visible" :footer="null" width="900px" @cancel="handleCancel">
    <a-descriptions :column="2" bordered size="small">
      <a-descriptions-item label="房间ID">{{ detailData.id || "-" }}</a-descriptions-item>
      <a-descriptions-item label="直播状态">
        <a-tag :color="detailData.status === 1 ? 'success' : 'default'">
          {{ detailData.status === 1 ? "直播中" : "未开播" }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="标题" :span="2">{{ detailData.title || "-" }}</a-descriptions-item>
      <a-descriptions-item label="主播昵称">{{ detailData.userNickname || "-" }}</a-descriptions-item>
      <a-descriptions-item label="主播ID">{{ detailData.userId || "-" }}</a-descriptions-item>
      <a-descriptions-item label="分类">{{ detailData.categoryName || "-" }}</a-descriptions-item>
      <a-descriptions-item label="封禁状态">
        <a-tag :color="detailData.disabled === 0 ? 'success' : 'error'">
          {{ detailData.disabled === 0 ? "正常" : "已封禁" }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="简介" :span="2">{{ detailData.introduce || "-" }}</a-descriptions-item>
      <a-descriptions-item label="公告" :span="2">{{ detailData.notice || "-" }}</a-descriptions-item>
      <a-descriptions-item label="封面地址" :span="2">{{ detailData.cover || "-" }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ detailData.createTime || "-" }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ detailData.updateTime || "-" }}</a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  detailData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible"])

const handleCancel = () => {
  emit("update:visible", false)
}
</script>
