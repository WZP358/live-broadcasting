<template>
  <div class="admin-pagination">
    <div class="admin-pagination__summary">
      共 <strong>{{ total }}</strong> 条
    </div>
    <a-pagination
      :total="total"
      :current="current"
      :page-size="pageSize"
      :show-size-changer="true"
      :show-quick-jumper="true"
      :page-size-options="['10', '20', '50', '100']"
      @change="handleChange"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  total: {
    type: Number,
    default: 0,
  },
  current: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
})

const emit = defineEmits(["change"])

const handleChange = (page, pageSize) => {
  emit("change", page, pageSize)
}
</script>
