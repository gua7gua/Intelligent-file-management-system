/** 统一响应结构 */
export interface ApiResponse<T = any> {
  code: string
  message: string
  data: T
  traceId: string
}

/** 分页响应结构 */
export interface PageData<T = any> {
  records: T[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
}

/** 分页请求参数 */
export interface PageParams {
  pageNo?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}
