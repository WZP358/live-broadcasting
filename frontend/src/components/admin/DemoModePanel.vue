<template>
  <AdminCard title="样例直播间" subtitle="一键启停多间精选录播直播间，关闭后只下线样例房间，不影响真实主播。">
    <div class="demo-mode-panel">
      <div class="demo-mode-panel__main">
        <div class="demo-mode-panel__state">
          <a-badge :status="demoStatus.enabled ? 'processing' : 'default'" />
          <div>
            <strong>{{ demoStatus.enabled ? "样例直播间运行中" : "样例直播间已关闭" }}</strong>
            <span>
              {{ demoStatus.livingCount || 0 }} / {{ demoStatus.roomCount || 0 }} 个样例直播间在线
            </span>
          </div>
        </div>
        <a-switch
          :checked="demoStatus.enabled"
          :loading="loading"
          checked-children="开启"
          un-checked-children="关闭"
          @change="handleToggle"
        />
      </div>

      <div v-if="demoStatus.rooms?.length" class="demo-room-list">
        <article v-for="room in visibleRooms" :key="room.id" class="demo-room-item">
          <img :src="safeCover(room.cover)" alt="" @error="onImgError" />
          <div>
            <strong>{{ room.title }}</strong>
            <span>{{ room.anchorName }} · {{ room.categoryName }}</span>
          </div>
          <a-tag :color="room.status === 1 ? 'success' : 'default'">
            {{ room.status === 1 ? "直播中" : "未开播" }}
          </a-tag>
        </article>
        <div v-if="hiddenRoomCount > 0" class="demo-room-more">
          还有 {{ hiddenRoomCount }} 个样例直播间，可在直播间管理中查看
        </div>
      </div>
    </div>
  </AdminCard>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { message } from "ant-design-vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import systemDemoApi from "@/api/systemDemo"
import { FALLBACK_COVER, resolveSafeImageUrl } from "@/utils/fallback"

const fallbackCover = FALLBACK_COVER
const safeCover = (url) => resolveSafeImageUrl(url, FALLBACK_COVER)

const emit = defineEmits(["changed"])

const loading = ref(false)
const demoStatus = ref({
  enabled: false,
  roomCount: 0,
  livingCount: 0,
  rooms: [],
})

const visibleRooms = computed(() => demoStatus.value.rooms?.slice(0, 6) || [])
const hiddenRoomCount = computed(() => Math.max((demoStatus.value.rooms?.length || 0) - visibleRooms.value.length, 0))

const loadStatus = async () => {
  const res = await systemDemoApi.getStatus()
  demoStatus.value = res?.data || demoStatus.value
}

const handleToggle = async (checked) => {
  loading.value = true
  try {
    const res = checked ? await systemDemoApi.enable() : await systemDemoApi.disable()
    demoStatus.value = res?.data || demoStatus.value
    message.success(checked ? "样例直播间已启动" : "样例直播间已关闭")
    emit("changed", demoStatus.value)
  } finally {
    loading.value = false
  }
}

const onImgError = (event) => {
  event.target.src = fallbackCover
}

onMounted(loadStatus)
</script>

<style scoped lang="scss">
.demo-mode-panel {
  display: grid;
  gap: 14px;
}

.demo-mode-panel__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 16px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: var(--bg-secondary);
}

.demo-mode-panel__state {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.demo-mode-panel__state strong,
.demo-room-item strong {
  display: block;
  overflow: hidden;
  color: var(--admin-text);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.demo-mode-panel__state span,
.demo-room-item span {
  display: block;
  margin-top: 4px;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.demo-room-list {
  display: grid;
  gap: 10px;
}

.demo-room-item {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--admin-border-light);
  border-radius: var(--admin-radius);
  background: var(--bg-card);
}

.demo-room-item img {
  width: 54px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
}

.demo-room-more {
  padding: 4px 2px 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}
</style>
