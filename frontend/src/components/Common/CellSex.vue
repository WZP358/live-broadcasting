<template>
    <a-tag v-if="val === '男'" color="orange" :bordered="false">
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
        background-color: var(--accent-light);
        color: var(--accent);
    }

    &.female {
        background-color: color-mix(in srgb, var(--danger) 12%, var(--bg-card));
        color: var(--danger);
    }
}
</style>
