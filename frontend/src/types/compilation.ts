import type { PageData, PageParams } from './api'
import type { CompilationStatusValue } from './enums'

/** 编研成果查询参数（§19.1） */
export interface CompilationParams extends PageParams {
  status?: CompilationStatusValue | ''
  keyword?: string
}

/** 编研素材引用项（DB compilation_materials，只引用档号） */
export interface CompilationMaterial {
  id: number
  compilationId: number
  archiveId: number
  archiveNo: string
  title: string
  referenceNote?: string
}

/** 编研正文附件（§19.5 生成） */
export interface CompilationAttachment {
  id: number
  fileName: string
  fileUrl: string
  attachmentType: 'report'
  generatedAt: string
}

/** 创建/更新编研草稿请求（§19.2/19.4） */
export interface CompilationWriteData {
  title: string
  compilationType: string
  dateRangeText?: string
  keywords?: string
  summary?: string
  contentHtml?: string
  materialArchiveIds: number[]
}

/** 编研成果（DB compilations + 展示字段） */
export interface Compilation {
  id: number
  compilationNo: string
  title: string
  compilationType: string
  dateRangeText?: string
  keywords?: string
  summary?: string
  contentHtml?: string
  status: CompilationStatusValue
  materialCount: number
  attachment?: CompilationAttachment
  archiveNo?: string
  archiveId?: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

/** 编研成果详情（§19.3） */
export interface CompilationDetail extends Compilation {
  materials: CompilationMaterial[]
}

/** 素材档案筛选参数 */
export interface MaterialSearchParams extends PageParams {
  keyword?: string
  categoryId?: number
  year?: number
  tag?: string
}

/** 素材档案候选（引用选择，只读摘要） */
export interface MaterialCandidate {
  id: number
  archiveNo: string
  title: string
  categoryName: string
  formedYear: number
  tags: string[]
}

/** 编研入库请求（§19.6） */
export interface CompilationArchiveData {
  fondsId: number
  categoryId: number
  formedDate: string
  retentionPeriod: string
  openStatus: string
  tagNames?: string[]
}

/** 编研状态指标卡（原型状态卡） */
export interface CompilationStatusMetrics {
  draftCount: number
  generatedCount: number
  archivedCount: number
  monthAdded: number
}

export type CompilationPage = PageData<Compilation>
export type MaterialCandidatePage = PageData<MaterialCandidate>
