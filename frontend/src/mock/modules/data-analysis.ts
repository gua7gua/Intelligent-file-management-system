import type { PageData } from '@/types/api'
import type {
  AnalysisItem, AnalysisItemHandleData, AnalysisRule, AnalysisTask,
  AnalysisTaskCreateData, AnalysisTaskDetail, AnalysisTaskParams,
} from '@/types/data-analysis'

function mkItem(over: Partial<AnalysisItem> & Pick<AnalysisItem, 'id' | 'archiveNo' | 'title' | 'problemType'>): AnalysisItem {
  return {
    taskId: 0, archiveId: 0, problemDesc: '', suggestedAction: '', status: 'pending', ...over,
  }
}

function suggestion(archiveId: number, archiveNo: string) {
  return {
    ruleType: 'dataAnalysis' as const,
    archiveId,
    archiveNo,
    candidates: [
      { field: 'tags', suggestedValue: '["财政改革","预算"]', confidence: 0.82 },
      { field: 'summary', suggestedValue: '围绕财政改革主题的编研素材', confidence: 0.7 },
    ],
  }
}

const task1Items: AnalysisItem[] = [
  mkItem({ id: 101, archiveId: 201, archiveNo: 'KJ-2024-0201', title: '采油三厂设备验收报告', problemType: 'category_conflict', problemDesc: '当前分类「文书档案」与责任者/年度推断不符，疑似应为「科技档案」。', suggestedAction: '复核分类，调整为科技档案', suggestion: suggestion(201, 'KJ-2024-0201') }),
  mkItem({ id: 102, archiveId: 202, archiveNo: 'KJ-2024-0202', title: '管线改造竣工图', problemType: 'missing_field', problemDesc: '责任者字段缺失，无法追溯形成单位。', suggestedAction: '补充责任者', suggestion: suggestion(202, 'KJ-2024-0202') }),
  mkItem({ id: 103, archiveId: 203, archiveNo: 'KJ-2023-0203', title: '年度审计材料汇编', problemType: 'tag_suggestion', problemDesc: '基于题名与摘要，建议补充标签。', suggestedAction: '采纳标签建议', suggestion: suggestion(203, 'KJ-2023-0203') }),
  mkItem({ id: 104, archiveId: 204, archiveNo: 'KJ-2022-0204', title: '市政管网规划', problemType: 'date_abnormal', problemDesc: '形成日期早于责任单位成立日期，疑似录入错误。', suggestedAction: '人工核实形成日期' }),
  mkItem({ id: 105, archiveId: 205, archiveNo: 'KJ-2024-0205', title: '供电改造批复', problemType: 'duplicate', problemDesc: '与 KJ-2024-0212 题名、责任者高度相似，疑似重复建档。', suggestedAction: '人工比对后合并或作废' }),
]

const task2Items: AnalysisItem[] = [
  mkItem({ id: 201, archiveId: 301, archiveNo: 'WS-2024-0301', title: '党委会议纪要', problemType: 'missing_field', problemDesc: '密级字段缺失。', suggestedAction: '由馆领导核定密级（受保护字段，需走审批）' }),
  mkItem({ id: 202, archiveId: 302, archiveNo: 'WS-2023-0302', title: '年度工作总结', problemType: 'tag_suggestion', problemDesc: '建议补充「年度总结」标签。', suggestedAction: '采纳标签建议', suggestion: suggestion(302, 'WS-2023-0302') }),
]

function buildTask(over: Partial<AnalysisTaskDetail> & Pick<AnalysisTaskDetail, 'id' | 'taskNo' | 'taskType' | 'status' | 'scopeText' | 'items' | 'rule'>): AnalysisTaskDetail {
  const scannedCount = 120 + over.id * 8
  const adopted = over.items.filter((i) => i.status === 'adopted').length
  return {
    startedAt: '2026-06-16T09:00:00+08:00',
    completedAt: over.status === 'completed' ? '2026-06-16T10:30:00+08:00' : undefined,
    createdAt: '2026-06-16T08:30:00+08:00',
    scannedCount, abnormalCount: over.items.length, adoptedCount: adopted,
    progress: over.status === 'completed' ? 1 : over.status === 'running' ? 0.6 : 0,
    ...over,
  }
}

const tasks: AnalysisTaskDetail[] = [
  buildTask({ id: 1, taskNo: 'FX-202606-001', taskType: 'mixed', status: 'completed', scopeText: '科技档案 / 2020-2026 / 含 AI 建议', items: task1Items, rule: { categoryIds: [1], formedYearStart: 2020, formedYearEnd: 2026, includeAiSuggestion: true } }),
  buildTask({ id: 2, taskNo: 'FX-202606-002', taskType: 'mixed', status: 'running', scopeText: '文书档案 / 2018-2026 / 仅规则扫描', items: task2Items, rule: { categoryIds: [2], formedYearStart: 2018, formedYearEnd: 2026, includeAiSuggestion: false } }),
  buildTask({ id: 3, taskNo: 'FX-202605-003', taskType: 'rule', status: 'failed', scopeText: '会计档案 / 2015-2026', items: [], rule: { categoryIds: [3], formedYearStart: 2015, formedYearEnd: 2026 } }),
]

export function mockAnalysisTasks(params?: AnalysisTaskParams): PageData<AnalysisTask> {
  let list = tasks.slice()
  if (params?.status) list = list.filter((t) => t.status === params.status)
  if (params?.taskType) list = list.filter((t) => t.taskType === params.taskType)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  const records = list.slice(start, start + pageSize).map(({ items, rule, ...rest }) => rest)
  return { records, pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockAnalysisTaskDetail(id: number): AnalysisTaskDetail {
  const found = tasks.find((t) => t.id === id)
  if (!found) throw new Error('研判任务不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextTaskId = 4
export function mockCreateAnalysisTask(data: AnalysisTaskCreateData): AnalysisTaskDetail {
  const id = nextTaskId++
  const rule: AnalysisRule = data.rule
  const includeAi = data.taskType !== 'rule' && rule.includeAiSuggestion !== false
  const sampleNos = ['KJ-2024-0901', 'KJ-2024-0902', 'KJ-2024-0903']
  const items: AnalysisItem[] = sampleNos.map((no, i) => mkItem({
    id: id * 100 + i, taskId: id, archiveId: 900 + i, archiveNo: no, title: `范围内档案 ${i + 1}`,
    problemType: i === 0 ? 'missing_field' : i === 1 ? 'tag_suggestion' : 'category_conflict',
    problemDesc: i === 0 ? '责任者字段缺失。' : i === 1 ? '建议补充标签。' : '分类疑似冲突。',
    suggestedAction: i === 0 ? '补充责任者' : i === 1 ? '采纳标签建议' : '复核分类',
    suggestion: includeAi ? suggestion(900 + i, no) : undefined,
  }))
  const detail = buildTask({
    id, taskNo: `FX-202606-${String(id).padStart(3, '0')}`, taskType: data.taskType, status: 'running',
    scopeText: `分类 ${rule.categoryIds?.join('/') ?? '全部'} / ${rule.formedYearStart ?? '?'}-${rule.formedYearEnd ?? '?'} / ${includeAi ? '含 AI 建议' : '仅规则扫描'}`,
    items, rule, createdAt: '2026-06-17T09:00:00+08:00', startedAt: '2026-06-17T09:00:00+08:00',
  })
  tasks.unshift(detail)
  return JSON.parse(JSON.stringify(detail))
}

export function mockHandleAnalysisItem(itemId: number, data: AnalysisItemHandleData): AnalysisItem {
  for (const t of tasks) {
    const item = t.items.find((i) => i.id === itemId)
    if (item) {
      if (item.status !== 'pending') throw new Error('当前建议已处理，不可重复处理')
      item.status = data.action
      item.handleNote = data.note
      item.handledAt = '2026-06-17T10:00:00+08:00'
      item.handledBy = '郭一坤'
      t.adoptedCount = t.items.filter((i) => i.status === 'adopted').length
      return JSON.parse(JSON.stringify(item))
    }
  }
  throw new Error('研判项不存在')
}
