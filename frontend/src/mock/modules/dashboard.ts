import type { DashboardSummary, OverviewTodo } from '@/types/dashboard'

const summary: DashboardSummary = {
  todos: {
    pendingTransferReception: 12,
    pendingArchive: 37,
    borrowApproval: 6,
    approvalPending: 9,
    pendingDestruction: 2,
    pendingShelf: 11,
    appraisalDue: 64,
    newBorrowRequests: 6,
  },
  archiveSummary: {
    totalArchives: 248920,
    monthAdded: 1286,
    storageUsage: 0.86,
    storageWarningThreshold: 0.85,
  },
  warehouseWarnings: [
    { roomNo: '402', roomName: '会计档案库房', occupancyRate: 0.88, warningThreshold: 0.85 },
  ],
  recentAuditLogs: [
    { id: 1, operator: '小陈', module: '移交验收', action: '导出 YJ-2026-0008 接收回执', operatedAt: '09:42' },
    { id: 2, operator: '小刘', module: '待入库', action: '确认 KJ-2025-0102 入库', operatedAt: '10:18' },
    { id: 3, operator: '王主任', module: '审批工作台', action: '通过 BR-202606-017 借阅审批', operatedAt: '10:55' },
    { id: 4, operator: '小李', module: '档案鉴定', action: '创建 PD-202606 鉴定批次', operatedAt: '11:20' },
    { id: 5, operator: '系统', module: '档案保存', action: '完成全量备份任务', operatedAt: '12:00' },
  ],
  todoEntries: [
    { key: 'pending_transfer', title: '待验收移交清单', description: '前台核对实物、上传 U 盘文件、导出回执', count: 12, targetRoute: '/admin/transfer-reception', targetQuery: { status: 'pending_transfer' }, severity: 'warning' },
    { key: 'pending_archive', title: '待 AI 补全与入库', description: '已接收条目等待后台确认字段', count: 37, targetRoute: '/admin/pending-archive', targetQuery: { status: 'accepted' }, severity: 'info' },
    { key: 'pending_shelf', title: '待上架纸质档案', description: '已入库未上架，不向查阅者开放', count: 11, targetRoute: '/admin/pending-archive', targetQuery: { status: 'pending_shelf' }, severity: 'warning' },
    { key: 'storage_warning', title: '存储空间告警', description: 'MinIO 使用率超过系统配置阈值', count: 86, targetRoute: '/admin/preservation', severity: 'danger' },
  ] as OverviewTodo[],
  summarizedAt: '2026-06-17T08:30:00+08:00',
}

export function mockDashboardSummary(): DashboardSummary {
  return JSON.parse(JSON.stringify(summary))
}
