<template>
    <div v-if="val == -1">
        <MinusCircleTwoTone two-tone-color="var(--danger)" /><span class="status-text">禁用</span>
    </div>
    <div v-else>
        <CheckCircleTwoTone two-tone-color="var(--success)" /><span class="status-text">启用</span>
    </div>
</template>

<script setup>
import { CheckCircleTwoTone, MinusCircleTwoTone } from "@ant-design/icons-vue"


defineProps({
    val: {
        type: Number,
        default: () => { return -1 },
    },
})

</script>

<style scoped>
.status-text {
    margin-left: 5px;
    color: var(--text-primary);
}
</style>
