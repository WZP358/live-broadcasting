<template>
    <a-tag v-if="val === '男'" color="blue" :bordered="false">
        <template #icon>
            <ManOutlined />
        </template>
        男
    </a-tag>
    <a-tag v-else-if="val === '女'" color="magenta" :bordered="false">
        <template #icon>
            <WomanOutlined />
        </template>
        女
    </a-tag>
    <a-tag v-else :bordered="false">
        未知
    </a-tag>
</template>

<script setup>
import { ManOutlined, WomanOutlined } from '@ant-design/icons-vue'
defineProps({
    val: {
        type: String,
        default: '',
    }
})
</script>

<style lang="scss" scoped>
.sex-tag {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;

    &.male {
        background-color: #e6f7ff;
        color: #1890ff;
    }

    &.female {
        background-color: #fff0f6;
        color: #eb2f96;
    }
}
</style>