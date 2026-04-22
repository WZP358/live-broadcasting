<template>
    <div v-if="val == -1">
        <MinusCircleTwoTone two-tone-color="#f56c6c" /><span style="margin-left: 5px;">禁用</span>
    </div>
    <div v-else>
        <CheckCircleTwoTone two-tone-color="#52c41a" /><span style="margin-left: 5px;">启用</span>
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