import type { PageParams } from './api'

/** 全宗查询参数（§17.1，只读引用） */
export interface FondsParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 全宗引用（对齐 DB 4.2 fonds 子集） */
export interface FondsReference {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  status: 'active' | 'disabled'
}
