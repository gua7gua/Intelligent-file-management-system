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

/** 游标分页响应（§23 审计 / 访问日志） */
export interface CursorData<T = any> {
  records: T[]
  nextCursor: string | null
  hasNext: boolean
}

/** 游标分页请求基类 */
export interface CursorParams {
  cursor?: string
  limit?: number
}
