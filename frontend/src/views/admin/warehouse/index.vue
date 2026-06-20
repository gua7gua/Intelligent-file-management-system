<template>
  <div class="warehouse">
    <section class="hero-line">
      <div>
        <h1 class="page-title">库房管理</h1>
        <p class="page-subtitle">维护库房房间、机架、层、盒位和档案盒占用状态，管理员可见完整架位编码。</p>
      </div>
      <button class="button" @click="roomDialogVisible = true">+ 添加库房</button>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.activeRooms }}</div><div class="metric-label">启用库房</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.totalCapacity }}</div><div class="metric-label">总盒位</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.totalOccupied }}</div><div class="metric-label">已占用盒位</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.warningRooms }}</div><div class="metric-label">容量告警</div></div>
    </section>

    <section class="workspace">
      <RoomList :rooms="rooms" v-model="selectedRoomId" @delete="onDeleteRoom" />
      <section class="rack-area">
        <div class="filters">
          <div class="field"><label>机架</label>
            <select v-model.number="rackFilter">
              <option :value="0">全部机架</option>
              <option v-for="r in rackOptions" :key="r" :value="r">机架 {{ pad(r) }}</option>
            </select>
          </div>
          <div class="field"><label>架位状态</label>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="free">空闲</option>
              <option value="occupied">已占用</option>
              <option value="disabled">停用</option>
            </select>
          </div>
        </div>

        <div v-if="locLoading" class="empty">架位加载中...</div>
        <div v-else-if="locError" class="empty">架位加载失败：<button class="link" @click="loadLocations">重试</button></div>
        <div v-else class="location-layout">
          <RackBoard :locations="filteredLocations" v-model="selectedLocationId" />
          <BoxDetailDrawer :location="selectedLocation" :free-locations="freeLocations" @refresh="onRefresh" />
        </div>
      </section>
    </section>

    <div style="margin-top: 16px">
      <RecentBoxesTable :boxes="recentBoxes" />
    </div>

    <CreateRoomDialog v-model="roomDialogVisible" @create="onCreateRoom" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ArchiveBox, StorageLocation, WarehouseRoom, WarehouseRoomCreateData } from '@/types/warehouse'
import { createWarehouseRoom, deleteWarehouseRoom, getArchiveBoxes, getStorageLocations, getWarehouseRooms } from '@/api/warehouse'
import RoomList from './components/RoomList.vue'
import RackBoard from './components/RackBoard.vue'
import BoxDetailDrawer from './components/BoxDetailDrawer.vue'
import CreateRoomDialog from './components/CreateRoomDialog.vue'
import RecentBoxesTable from './components/RecentBoxesTable.vue'

const rooms = ref<WarehouseRoom[]>([])
const selectedRoomId = ref<number | null>(null)
const locations = ref<StorageLocation[]>([])
const locLoading = ref(false)
const locError = ref(false)
const selectedLocationId = ref<number | null>(null)
const recentBoxes = ref<ArchiveBox[]>([])
const roomDialogVisible = ref(false)
const rackFilter = ref(0)
const statusFilter = ref<'' | 'free' | 'occupied' | 'disabled'>('')

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

const metrics = computed(() => {
  const active = rooms.value.filter((r) => r.status === 'active')
  return {
    activeRooms: active.length,
    totalCapacity: active.reduce((s, r) => s + r.capacity, 0),
    totalOccupied: active.reduce((s, r) => s + r.occupiedSlots, 0),
    warningRooms: active.filter((r) => r.warning).length,
  }
})

const rackOptions = computed(() => [...new Set(locations.value.map((l) => l.rackNo))].sort((a, b) => a - b))

const filteredLocations = computed(() => {
  let list = locations.value
  if (rackFilter.value) list = list.filter((l) => l.rackNo === rackFilter.value)
  if (statusFilter.value === 'free') list = list.filter((l) => !l.occupied && l.status === 'active')
  else if (statusFilter.value === 'occupied') list = list.filter((l) => l.occupied)
  else if (statusFilter.value === 'disabled') list = list.filter((l) => l.status === 'disabled')
  return list
})

const selectedLocation = computed(() => locations.value.find((l) => l.id === selectedLocationId.value) ?? null)
const freeLocations = computed(() => filteredLocations.value.filter((l) => !l.occupied && l.status === 'active'))

async function loadRooms() {
  try {
    rooms.value = await getWarehouseRooms()
    if (!selectedRoomId.value && rooms.value.length) selectedRoomId.value = rooms.value[0].id
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '库房加载失败')
  }
}

async function loadLocations() {
  if (!selectedRoomId.value) return
  locLoading.value = true
  locError.value = false
  try {
    const page = await getStorageLocations({ roomId: selectedRoomId.value, pageSize: 500 })
    locations.value = page.records
    selectedLocationId.value = null
  } catch {
    locError.value = true
  } finally {
    locLoading.value = false
  }
}

async function loadRecentBoxes() {
  try {
    const page = await getArchiveBoxes({ pageSize: 10 })
    recentBoxes.value = page.records
  } catch {
    recentBoxes.value = []
  }
}

async function onCreateRoom(data: WarehouseRoomCreateData) {
  try {
    const room = await createWarehouseRoom(data)
    roomDialogVisible.value = false
    ElMessage.success(`库房 ${room.roomNo} 已创建，生成 ${room.capacity} 个架位。`)
    await loadRooms()
    selectedRoomId.value = room.id
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function onDeleteRoom(roomId: number) {
  try {
    await ElMessageBox.confirm(
      '删除库房不可恢复。仅当库房内无活动档案盒时方可删除；历史档案盒记录会保留但解除架位归属。确认删除？',
      '删除库房',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteWarehouseRoom(roomId)
    ElMessage.success('库房已删除')
    if (selectedRoomId.value === roomId) selectedRoomId.value = null
    await loadRooms()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onRefresh() {
  await loadLocations()
  await loadRooms()
  await loadRecentBoxes()
}

watch(selectedRoomId, () => {
  loadLocations()
})

onMounted(async () => {
  await loadRooms()
  await loadRecentBoxes()
})
</script>

<style scoped>
.warehouse { padding: 0; }
.hero-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.workspace { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.rack-area { display: grid; gap: 12px; }
.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; min-width: 140px; }
.location-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; align-items: start; }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
@media (max-width: 1180px) { .metric-row, .workspace, .location-layout { grid-template-columns: 1fr; } }
</style>
