import type { PageData, PageParams } from './api'
import type {
  AnalysisHandleActionValue, AnalysisItemStatusValue,
  AnalysisProblemTypeValue, AnalysisTaskStatusValue, AnalysisTaskTypeValue,
} from './enums'

/** 研判扫描规则（§20.5 rule） */
export interface AnalysisRule {
  categoryIds?: number[]
  formedYearStart?: number
  formedYearEnd?: number
  includeAiSuggestion?: boolean
}

/** 创建研判任务请求（§20.5） */
export interface AnalysisTaskCreateData {
  taskType: AnalysisTaskTypeValue
  rule: AnalysisRule
}

/** 研判任务查询参数（§20.4） */
export interface AnalysisTaskParams extends PageParams {
  status?: AnalysisTaskStatusValue | ''
  taskType?: AnalysisTaskTypeValue | ''
}

/** AI 候选建议 JSON（ruleType=dataAnalysis，原型 AI JSON 区） */
export interface AnalysisSuggestionCandidate {
  field: string
  currentValue?: string
  suggestedValue: string
  confidence: number
}

export interface AnalysisSuggestionJson {
  ruleType: 'dataAnalysis'
  archiveId: number
  archiveNo: string
  candidates: AnalysisSuggestionCandidate[]
}

/** 研判建议项（DB analysis_items + 展示字段） */
export interface AnalysisItem {
  id: number
  taskId: number
  archiveId: number
  archiveNo: string
  title: string
  problemType: AnalysisProblemTypeValue
  problemDesc: string
  suggestedAction: string
  status: AnalysisItemStatusValue
  suggestion?: AnalysisSuggestionJson
  handleNote?: string
  handledAt?: string
  handledBy?: string
}

/** 研判任务摘要（列表行） */
export interface AnalysisTask {
  id: number
  taskNo: string
  taskType: AnalysisTaskTypeValue
  status: AnalysisTaskStatusValue
  scopeText: string
  scannedCount: number
  abnormalCount: number
  adoptedCount: number
  progress: number
  startedAt?: string
  completedAt?: string
  createdAt: string
}

/** 研判任务详情（§20.6） */
export interface AnalysisTaskDetail extends AnalysisTask {
  rule: AnalysisRule
  items: AnalysisItem[]
}

/** 处理研判项请求（§20.7） */
export interface AnalysisItemHandleData {
  action: AnalysisHandleActionValue
  note?: string
}

export type AnalysisTaskPage = PageData<AnalysisTask>
