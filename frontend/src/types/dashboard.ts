/** 概览待办计数（§6.1 todos） */
export interface DashboardTodos {
  pendingTransferReception: number
  pendingArchive: number
  borrowApproval: number
  approvalPending: number
  pendingDestruction: number
  pendingShelf: number
  appraisalDue: number
  newBorrowRequests: number
}

/** 馆藏汇总（§6.1 archiveSummary，概览指标卡取数） */
export interface ArchiveSummary {
  totalArchives: number
  monthAdded: number
  storageUsage: number
  storageWarningThreshold: number
}

/** 库房告警项（§6.1 warehouseWarnings） */
export interface WarehouseWarning {
  roomNo: string
  roomName: string
  occupancyRate: number
  warningThreshold: number
}

/** 最近操作日志行（§6.1 recentAuditLogs，原型 log-row） */
export interface RecentAuditLog {
  id: number
  operator: string
  module: string
  moduleLabel?: string
  action: string
  actionLabel?: string
  operatedAt: string
}

/** 待办提醒项（原型 todo-list，跳转入口 + 筛选意图） */
export interface OverviewTodo {
  key: string
  title: string
  description: string
  count: number
  targetRoute: string
  targetQuery?: Record<string, string>
  severity: 'info' | 'warning' | 'danger'
}

/** 概览聚合响应（§6.1 GET /api/admin/dashboard） */
export interface DashboardSummary {
  todos: DashboardTodos
  archiveSummary: ArchiveSummary
  warehouseWarnings: WarehouseWarning[]
  recentAuditLogs: RecentAuditLog[]
  todoEntries: OverviewTodo[]
  summarizedAt: string
}
