<template>
  <AdminPageLayout title="系统配置" description="将平台基础配置、安全策略、登录能力和直播参数拆分为清晰的配置分组，界面交互统一到后台框架。">
    <div class="config-shell">
      <AdminCard title="配置分组" subtitle="按模块维护，减少一页堆满所有配置项的阅读负担。">
        <a-menu mode="inline" :selected-keys="[activeKey]" :items="menuItems" @click="handleMenuClick" />
      </AdminCard>

      <div class="config-main">
        <AdminCard :title="activeSection.title" :subtitle="activeSection.description">
          <template #extra>
            <a-space>
              <a-button @click="handleReset">恢复默认</a-button>
              <a-button type="primary" @click="handleSave">保存草稿</a-button>
            </a-space>
          </template>

          <a-form layout="vertical" class="config-form">
            <a-row :gutter="16">
              <a-col v-for="field in activeSection.fields" :key="field.key" :span="field.type === 'textarea' ? 24 : 12">
                <a-form-item :label="field.label">
                  <a-input v-if="field.type === 'input'" v-model:value="formState[field.key]" :placeholder="field.placeholder" />
                  <a-textarea v-else-if="field.type === 'textarea'" v-model:value="formState[field.key]" :placeholder="field.placeholder" :rows="4" />
                  <a-input-number
                    v-else-if="field.type === 'number'"
                    v-model:value="formState[field.key]"
                    :min="field.min"
                    :max="field.max"
                    style="width: 100%"
                  />
                  <a-switch v-else-if="field.type === 'switch'" v-model:checked="formState[field.key]" />
                  <a-select
                    v-else-if="field.type === 'select'"
                    v-model:value="formState[field.key]"
                    :options="field.options.map((item) => ({ label: item, value: item }))"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </AdminCard>

        <div class="config-tips">
          <AdminCard title="使用说明" subtitle="当前先以本地草稿的形式落地，后续再平滑切到后端配置接口。">
            <div class="admin-summary-list">
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">草稿存储</span>
                <span class="admin-summary-item__value">浏览器本地 localStorage</span>
              </div>
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">适用阶段</span>
                <span class="admin-summary-item__value">前后端联调与后台结构重构阶段</span>
              </div>
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">后续方向</span>
                <span class="admin-summary-item__value">逐步替换为持久化系统配置接口</span>
              </div>
            </div>
          </AdminCard>
        </div>
      </div>
    </div>
  </AdminPageLayout>
</template>

<script setup>
import { computed, reactive, ref } from "vue"
import { message } from "ant-design-vue"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import { configSections, defaultConfigState } from "./sections"

const STORAGE_KEY = "ant-live-system-config-draft"
const savedDraft = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null")

const formState = reactive({
  ...defaultConfigState,
  ...(savedDraft || {}),
})

const activeKey = ref(configSections[0].key)

const menuItems = configSections.map((section) => ({
  key: section.key,
  label: section.title,
  title: section.title,
}))

const activeSection = computed(() => configSections.find((section) => section.key === activeKey.value) || configSections[0])

const handleMenuClick = ({ key }) => {
  activeKey.value = key
}

const handleSave = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(formState))
  message.success("系统配置草稿已保存")
}

const handleReset = () => {
  Object.assign(formState, defaultConfigState)
  localStorage.removeItem(STORAGE_KEY)
  message.success("已恢复默认配置")
}
</script>

<style scoped lang="scss">
.config-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
}

.config-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-form :deep(.ant-form-item) {
  margin-bottom: 18px;
}

@media (max-width: 1100px) {
  .config-shell {
    grid-template-columns: 1fr;
  }
}
</style>
