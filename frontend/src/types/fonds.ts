import type { PageParams } from './api'

/** 全宗查询参数（§17.1，含原型筛选「关联状态」） */
export interface FondsParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
  organizationId?: number
  /** 关联状态：linked 有归档数据 / empty 无关联 / disabled 已停用 */
  relation?: 'linked' | 'empty' | 'disabled' | ''
}

/** 全宗引用（对齐 DB 4.2 fonds 子集，用户管理页只读引用） */
export interface FondsReference {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  status: 'active' | 'disabled'
}

/** 门类分布条目（富字段，可选） */
export interface FondsCategorySlice {
  category: string
  count: number
}

/** 最近入库记录（富字段，可选） */
export interface FondsRecentIntake {
  date: string
  title: string
}

/** 全宗完整项（页面主用，对齐 §17.1 返回 + DB §4.2） */
export interface FondsItem {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  archiveCount: number
  boxCount: number
  status: 'active' | 'disabled'
  categoryDistribution?: FondsCategorySlice[]
  recentIntake?: FondsRecentIntake[]
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
}

/** 新增全宗请求体（§17.2） */
export interface FondsCreate {
  fondsNo: string
  fondsName: string
  organizationId: number
  description?: string
}

/** 更新全宗请求体（§17.3，fondsNo 不可改 → 不含） */
export interface FondsUpdate {
  fondsName?: string
  organizationId?: number
  description?: string
  status?: 'active' | 'disabled'
}
