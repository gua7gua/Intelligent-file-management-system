/**
 * 闭环状态登记表。
 * 同一 spec 文件内（workers:1 + serial）的 test 共享本模块实例，
 * 用于跨步骤/跨角色传递产出编号（BAT/ARC/APP/DES/BRW/CMP/ANA），下游步骤不臆造。
 *
 * 各闭环 spec 自带一个 key（如 'W-A'），存放该闭环产出的编号。
 */
export interface WorkflowRecord {
  batchNo?: string
  archiveNo?: string
  approvalNo?: string
  destroyNo?: string
  borrowNo?: string
  compilationNo?: string
  analysisNo?: string
  [k: string]: unknown
}

const store: Record<string, WorkflowRecord> = {}

/** 取/初始化某闭环的登记记录 */
export function wf(key: string): WorkflowRecord {
  if (!store[key]) store[key] = {}
  return store[key]
}
