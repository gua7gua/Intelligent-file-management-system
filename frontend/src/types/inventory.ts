import type { PageData, PageParams } from './api'
import type { InventoryCheckResultValue, InventoryTaskStatusValue } from './enums'

/** 盘点任务查询参数（§18.1） */
export interface InventoryTaskParams extends PageParams {
  status?: InventoryTaskStatusValue | ''
  roomId?: number
  categoryId?: number
}

/** 创建盘点任务请求（§18.2） */
export interface InventoryTaskCreateData {
  taskName: string
  roomId: number
  categoryId: number
}

/** 盘点任务统计（§18.4 返回的统计部分） */
export interface InventoryTaskStats {
  total: number
  checked: number
  normalCount: number
  missingCount: number
  misplacedCount: number
  damagedCount: number
  onLoanCount: number
}

/** 盘点任务摘要（列表行） */
export interface InventoryTask {
  id: number
  taskNo: string
  taskName: string
  roomId: number
  roomNo: string
  categoryId: number
  categoryName: string
  status: InventoryTaskStatusValue
  total: number
  checked: number
  abnormalCount: number
  startedAt?: string
  completedAt?: string
  summary?: string
  createdAt: string
}

/** 盘点明细项（§18.4 / DB 10.2 + 展示字段） */
export interface InventoryItem {
  id: number
  taskId: number
  archiveId: number
  archiveNo: string
  title: string
  /** 应在架位编码（由 expected_location_id 解析） */
  expectedLocationCode: string
  /** 实际架位编码 */
  actualLocationCode?: string
  checkResult: InventoryCheckResultValue | ''
  note?: string
  /** 关联档案当前借阅状态，用于展示「暂停借阅/借出中」 */
  loanStatus?: string
}

/** 盘点任务详情（§18.4） */
export interface InventoryTaskDetail extends InventoryTask {
  items: InventoryItem[]
  stats: InventoryTaskStats
}

/** 更新盘点明细请求（§18.5） */
export interface InventoryItemUpdateData {
  actualLocationCode?: string
  checkResult: InventoryCheckResultValue
  note?: string
}

/** 完成盘点请求（§18.6） */
export interface InventoryCompleteData {
  summary: string
}

export type InventoryTaskPage = PageData<InventoryTask>
