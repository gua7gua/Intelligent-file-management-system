import type { PageData } from '@/types/api'
import type {
  InventoryCompleteData, InventoryItem, InventoryItemUpdateData, InventoryTask,
  InventoryTaskCreateData, InventoryTaskDetail, InventoryTaskParams, InventoryTaskStats,
} from '@/types/inventory'

const roomMap: Record<number, string> = { 1: '401', 2: '402', 3: '403' }
const categoryMap: Record<number, string> = { 1: '科技档案', 2: '文书档案', 3: '会计档案', 4: '音像档案', 5: '人事档案' }

function mkItem(over: Partial<InventoryItem>): InventoryItem {
  return { id: 0, taskId: 0, archiveId: 0, archiveNo: '', title: '', expectedLocationCode: '', checkResult: '', loanStatus: 'available', ...over }
}

function statsFromItems(items: InventoryItem[]): InventoryTaskStats {
  return {
    total: items.length,
    checked: items.filter((i) => i.checkResult !== '').length,
    normalCount: items.filter((i) => i.checkResult === 'normal').length,
    missingCount: items.filter((i) => i.checkResult === 'missing').length,
    misplacedCount: items.filter((i) => i.checkResult === 'misplaced').length,
    damagedCount: items.filter((i) => i.checkResult === 'damaged').length,
    onLoanCount: items.filter((i) => i.checkResult === 'on_loan').length,
  }
}

function buildSummary(s: InventoryTaskStats): string {
  return `应盘 ${s.total} 件，正常 ${s.normalCount} 件，缺失 ${s.missingCount} 件，错位 ${s.misplacedCount} 件，损坏 ${s.damagedCount} 件，借出 ${s.onLoanCount} 件`
}

const task1Items: InventoryItem[] = [
  mkItem({ id: 11, archiveId: 101, archiveNo: 'KJ-2025-00018', title: '采油一厂设备验收报告', expectedLocationCode: '401-03-02-05', actualLocationCode: '401-03-02-05', checkResult: 'normal', note: '实物一致' }),
  mkItem({ id: 12, archiveId: 102, archiveNo: 'KJ-2025-00019', title: '管线改造竣工图', expectedLocationCode: '401-03-03-01', actualLocationCode: '401-03-03-02', checkResult: 'misplaced', note: '同机架相邻盒位' }),
  mkItem({ id: 13, archiveId: 103, archiveNo: 'KJ-2025-00020', title: '年度审计材料', expectedLocationCode: '401-02-04-03', checkResult: 'missing', note: '架位未找到' }),
  mkItem({ id: 14, archiveId: 104, archiveNo: 'KJ-2024-00031', title: '油田道路施工图', expectedLocationCode: '401-01-01-02', checkResult: 'on_loan', note: 'BR-202606-017 已出库', loanStatus: 'on_loan' }),
  mkItem({ id: 15, archiveId: 105, archiveNo: 'KJ-2023-00008', title: '设备维修记录汇编', expectedLocationCode: '401-02-04-04', actualLocationCode: '401-02-04-04', checkResult: 'damaged', note: '盒角受潮' }),
  mkItem({ id: 16, archiveId: 106, archiveNo: 'KJ-2025-00021', title: '市政管网规划', expectedLocationCode: '401-02-05-01' }),
  mkItem({ id: 17, archiveId: 107, archiveNo: 'KJ-2025-00022', title: '环保验收报告', expectedLocationCode: '401-02-05-02' }),
  mkItem({ id: 18, archiveId: 108, archiveNo: 'KJ-2025-00023', title: '供电改造批复', expectedLocationCode: '401-02-05-03' }),
]

const task2Items: InventoryItem[] = [
  mkItem({ id: 21, archiveId: 201, archiveNo: 'KJ-2024-00040', title: '2024 会计凭证 A', expectedLocationCode: '402-01-01-01', actualLocationCode: '402-01-01-01', checkResult: 'normal' }),
  mkItem({ id: 22, archiveId: 202, archiveNo: 'KJ-2024-00041', title: '2024 会计凭证 B', expectedLocationCode: '402-01-01-02' }),
  mkItem({ id: 23, archiveId: 203, archiveNo: 'KJ-2024-00042', title: '2024 会计凭证 C', expectedLocationCode: '402-01-01-03' }),
]

const task3Items: InventoryItem[] = [
  mkItem({ id: 31, archiveId: 301, archiveNo: 'WS-2024-001', title: '党委会议纪要', expectedLocationCode: '401-04-01-01', actualLocationCode: '401-04-01-01', checkResult: 'normal' }),
  mkItem({ id: 32, archiveId: 302, archiveNo: 'WS-2024-002', title: '年度工作总结', expectedLocationCode: '401-04-01-02', actualLocationCode: '401-04-01-02', checkResult: 'normal' }),
]

function buildTask(over: Partial<InventoryTaskDetail> & Pick<InventoryTaskDetail, 'id' | 'taskNo' | 'taskName' | 'roomId' | 'categoryId' | 'status' | 'items'>): InventoryTaskDetail {
  const stats = statsFromItems(over.items)
  const abnormalCount = stats.missingCount + stats.misplacedCount + stats.damagedCount
  const base: InventoryTask = {
    id: over.id, taskNo: over.taskNo, taskName: over.taskName, roomId: over.roomId, roomNo: roomMap[over.roomId] ?? '',
    categoryId: over.categoryId, categoryName: categoryMap[over.categoryId] ?? '', status: over.status,
    total: stats.total, checked: stats.checked, abnormalCount, createdAt: over.createdAt ?? '2026-06-08T09:00:00+08:00',
    startedAt: over.startedAt, completedAt: over.completedAt, summary: over.summary,
  }
  return { ...base, items: over.items, stats }
}

const tasks: InventoryTaskDetail[] = [
  buildTask({ id: 1, taskNo: 'PD-202606-001', taskName: '2026 年度 401 库房科技档案盘点', roomId: 1, categoryId: 1, status: 'running', items: task1Items, startedAt: '2026-06-08T09:30:00+08:00', createdAt: '2026-06-08T09:00:00+08:00' }),
  buildTask({ id: 2, taskNo: 'PD-202606-002', taskName: '2026 年度 402 库房会计档案盘点', roomId: 2, categoryId: 3, status: 'running', items: task2Items, startedAt: '2026-06-10T10:00:00+08:00', createdAt: '2026-06-10T09:30:00+08:00' }),
  buildTask({ id: 3, taskNo: 'PD-202605-003', taskName: '2026 春季 401 库房文书档案复盘', roomId: 1, categoryId: 2, status: 'completed', items: task3Items, startedAt: '2026-05-15T09:00:00+08:00', completedAt: '2026-05-21T16:00:00+08:00', summary: '应盘 2 件，正常 2 件', createdAt: '2026-05-15T08:30:00+08:00' }),
  buildTask({ id: 4, taskNo: 'PD-202606-004', taskName: '2026 年度 403 库房音像档案盘点', roomId: 3, categoryId: 4, status: 'draft', items: [], createdAt: '2026-06-14T14:00:00+08:00' }),
]

export function mockInventoryTasks(params?: InventoryTaskParams): PageData<InventoryTask> {
  let list = tasks.slice()
  if (params?.status) list = list.filter((t) => t.status === params.status)
  if (params?.roomId) list = list.filter((t) => t.roomId === params.roomId)
  if (params?.categoryId) list = list.filter((t) => t.categoryId === params.categoryId)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  const records = list.slice(start, start + pageSize).map(({ items, stats, ...rest }) => rest)
  return { records, pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockInventoryTaskDetail(id: number): InventoryTaskDetail {
  const found = tasks.find((t) => t.id === id)
  if (!found) throw new Error('盘点任务不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextTaskId = 5
export function mockCreateInventoryTask(data: InventoryTaskCreateData): InventoryTaskDetail {
  const id = nextTaskId++
  const items: InventoryItem[] = Array.from({ length: 4 }, (_, i) => mkItem({
    id: id * 100 + i, taskId: id, archiveId: 500 + i, archiveNo: `KJ-2026-${String(500 + i).padStart(5, '0')}`,
    title: `范围内档案 ${i + 1}`, expectedLocationCode: `${roomMap[data.roomId] ?? '000'}-01-01-${String(i + 1).padStart(2, '0')}`,
  }))
  const detail = buildTask({
    id, taskNo: `PD-202606-${String(id).padStart(3, '0')}`, taskName: data.taskName,
    roomId: data.roomId, categoryId: data.categoryId, status: 'draft', items, createdAt: '2026-06-15T10:00:00+08:00',
  })
  tasks.push(detail)
  return JSON.parse(JSON.stringify(detail))
}

export function mockStartInventoryTask(id: number): InventoryTaskDetail {
  const task = tasks.find((t) => t.id === id)
  if (!task) throw new Error('盘点任务不存在')
  if (task.items.length === 0) throw new Error('任务无盘点明细，不可开始')
  task.status = 'running'
  task.startedAt = '2026-06-15T10:30:00+08:00'
  return JSON.parse(JSON.stringify(task))
}

export function mockUpdateInventoryItem(taskId: number, itemId: number, data: InventoryItemUpdateData): InventoryItem {
  const task = tasks.find((t) => t.id === taskId)
  if (!task) throw new Error('盘点任务不存在')
  if (task.status !== 'running') throw new Error('任务非进行中，不可更新明细')
  const item = task.items.find((i) => i.id === itemId)
  if (!item) throw new Error('盘点明细不存在')
  item.actualLocationCode = data.actualLocationCode
  item.checkResult = data.checkResult
  item.note = data.note
  task.stats = statsFromItems(task.items)
  task.checked = task.stats.checked
  task.abnormalCount = task.stats.missingCount + task.stats.misplacedCount + task.stats.damagedCount
  return { ...item }
}

export function mockCompleteInventoryTask(id: number, data: InventoryCompleteData): InventoryTaskDetail {
  const task = tasks.find((t) => t.id === id)
  if (!task) throw new Error('盘点任务不存在')
  if (task.status !== 'running') throw new Error('任务非进行中，不可完成')
  task.status = 'completed'
  task.completedAt = '2026-06-15T17:00:00+08:00'
  task.summary = data.summary.trim() || buildSummary(task.stats)
  return JSON.parse(JSON.stringify(task))
}
