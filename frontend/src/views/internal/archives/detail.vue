<template>
  <section>
    <div class="toolbar" style="margin: 0 0 12px">
      <div>
        <h1 class="page-title">档案详情</h1>
        <p class="page-subtitle">打开详情时后端重新鉴权；权限不足仅保留历史记录提示。</p>
      </div>
      <router-link to="/internal/search" class="button ghost">返回检索</router-link>
    </div>

    <div class="card panel detail-card">
      <ArchiveDetailPanel :archive-id="archiveId" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ArchiveDetailPanel from '@/views/internal/components/ArchiveDetailPanel.vue'

const route = useRoute()
const archiveId = computed(() => {
  const id = Number(route.params.id)
  return Number.isFinite(id) ? id : null
})
</script>

<style scoped>
.detail-card {
  max-width: 760px;
}
</style>
