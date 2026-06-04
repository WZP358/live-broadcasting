<template>
  <AdminPageLayout title="系统配置" description="集中维护站点展示、安全策略、登录能力和直播运营参数。">
    <div class="config-shell">
      <AdminCard title="配置分组" subtitle="按模块维护，减少一页堆满所有配置项的阅读负担。">
        <a-menu mode="inline" :selected-keys="[activeKey]" :items="menuItems" @click="handleMenuClick" />
      </AdminCard>

      <div class="config-main">
        <AdminCard :title="activeSection.title" :subtitle="activeSection.description">
          <template #extra>
            <a-space>
              <a-button @click="handleReset">恢复默认</a-button>
              <a-button type="primary" @click="handleSave">保存配置</a-button>
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
          <AdminCard title="配置说明" subtitle="保存后将作为当前后台的运营配置使用。">
            <div class="admin-summary-list">
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">保存方式</span>
                <span class="admin-summary-item__value">当前配置会在本机后台保留</span>
              </div>
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">适用范围</span>
                <span class="admin-summary-item__value">站点展示、登录开关、素材上传和直播管理</span>
              </div>
              <div class="admin-summary-item">
                <span class="admin-summary-item__label">操作建议</span>
                <span class="admin-summary-item__value">调整前请确认当前运营策略，保存后及时复查页面效果</span>
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
  message.success("系统配置已保存")
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
