/** 清单批次状态 */
export const BatchStatus = {
  DRAFT: 'draft',
  PENDING_TRANSFER: 'pending_transfer',
  PENDING_CONTACT: 'pending_contact',
  PENDING_RECEIVE: 'pending_receive',
  RECEIVED: 'received',
  PARTIALLY_RECEIVED: 'partially_received',
  REJECTED: 'rejected',
  ARCHIVED: 'archived',
  SHELVED: 'shelved',
} as const

/** 清单条目状态 */
export const ItemStatus = {
  DRAFT: 'draft',
  PENDING_ACCEPTANCE: 'pending_acceptance',
  ACCEPTED: 'accepted',
  REJECTED: 'rejected',
  PENDING_ARCHIVE: 'pending_archive',
  ARCHIVED: 'archived',
} as const

/** 档案生命周期状态 */
export const ArchiveLifecycleStatus = {
  PENDING_SHELF: 'pending_shelf',
  NORMAL: 'normal',
  PENDING_DESTRUCTION: 'pending_destruction',
  DESTROYED: 'destroyed',
} as const

/** 借阅状态 */
export const BorrowStatus = {
  APPLIED: 'applied',
  REJECTED: 'rejected',
  APPROVED: 'approved',
  VOUCHER_ISSUED: 'voucher_issued',
  CHECKED_OUT: 'checked_out',
  RETURNED: 'returned',
  ABNORMAL_RETURN: 'abnormal_return',
} as const

/** 审批状态 */
export const ApprovalStatus = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const

/** 来源类型 */
export const SourceType = {
  TRANSFER: 'transfer',
  COLLECTION: 'collection',
  COMPILATION: 'compilation',
} as const

/** 载体状态 */
export const CarrierStatus = {
  ELECTRONIC: 'electronic',
  PAPER_ELECTRONIC: 'paper_electronic',
  PAPER: 'paper',
} as const

/** 保管期限 */
export const RetentionPeriod = {
  TEN_YEARS: '10y',
  THIRTY_YEARS: '30y',
  PERMANENT: 'permanent',
} as const

/** 密级（数值，0=非密） */
export const SecurityLevel = {
  PUBLIC: 0,
  INTERNAL: 1,
  SECRET: 2,
  CONFIDENTIAL: 3,
  TOP_SECRET: 4,
} as const

/** 开放状态 */
export const OpenStatus = {
  OPEN: 'open',
  CLOSED: 'closed',
} as const

/** 角色标识（对齐接口文档） */
export const RoleKey = {
  FRONT_ARCHIVIST: 'front_archivist',
  BACK_ARCHIVIST: 'back_archivist',
  TRANSFER_USER: 'transfer_user',
  INTERNAL_READER: 'internal_reader',
  DIRECTOR: 'director',
  SYS_ADMIN: 'sys_admin',
  PUBLIC_USER: 'public_user',
} as const

/** 清单状态中文映射（需结合 sourceType） */
export const BatchStatusLabel: Record<string, Record<string, string>> = {
  transfer: {
    draft: '草稿',
    pending_transfer: '待移交',
    pending_receive: '待接收',
    received: '已接收',
    partially_received: '部分接收',
    rejected: '已回退',
    archived: '已入库',
    shelved: '已上架',
  },
  collection: {
    draft: '草稿',
    pending_contact: '待联系',
    pending_receive: '待接收',
    received: '已接收',
    partially_received: '部分接收',
    rejected: '已拒绝',
    archived: '已入库',
    shelved: '已上架',
  },
}

/** 条目状态中文 */
export const ItemStatusLabel: Record<string, string> = {
  draft: '草稿',
  pending_acceptance: '待验收',
  accepted: '已接收',
  rejected: '已回退',
  pending_archive: '待入库',
  archived: '已入库',
}

/** 档案生命周期状态中文 */
export const ArchiveStatusLabel: Record<string, string> = {
  pending_shelf: '已入库未上架',
  normal: '正常',
  pending_destruction: '待销毁',
  destroyed: '已销毁',
}

/** 借阅状态中文 */
export const BorrowStatusLabel: Record<string, string> = {
  applied: '待审批',
  rejected: '已拒绝',
  approved: '已批准',
  voucher_issued: '凭证已生成',
  checked_out: '已借出',
  returned: '已归还',
  abnormal_return: '异常归还',
}

/** 载体状态中文 */
export const CarrierStatusLabel: Record<string, string> = {
  electronic: '纯电子',
  paper_electronic: '纸质+电子',
  paper: '纯纸质',
}

/** 保管期限中文 */
export const RetentionPeriodLabel: Record<string, string> = {
  '10y': '10年',
  '30y': '30年',
  permanent: '永久',
}

/** 密级中文 */
export const SecurityLevelLabel: Record<number, string> = {
  0: '非密',
  1: '内部',
  2: '秘密',
  3: '机密',
  4: '绝密',
}

/** 开放状态中文 */
export const OpenStatusLabel: Record<string, string> = {
  open: '公开',
  closed: '不公开',
}

/** 鉴定结论 */
export const AppraisalResult = {
  EXTEND: 'extend',
  DESTROY: 'destroy',
} as const

/** 鉴定批次状态 */
export const AppraisalBatchStatus = {
  DRAFT: 'draft',
  COMPLETED: 'completed',
} as const

/** 销毁清册状态 */
export const DestructionListStatus = {
  DRAFT: 'draft',
  PENDING_APPROVAL: 'pending_approval',
  PENDING_DESTROY: 'pending_destroy',
  DESTROYED: 'destroyed',
} as const

/** 销毁方式 */
export const DestroyMethod = {
  SHREDDING: 'shredding',
  BURNING: 'burning',
  ENTRUSTED: 'entrusted',
} as const

/** 电子文件删除状态 */
export const FileDeleteStatus = {
  NOT_STARTED: 'not_started',
  DELETED: 'deleted',
  FAILED: 'failed',
} as const

/** 审批类型 */
export const ApprovalType = {
  SECURITY_ADJUST: 'security_adjust',
  OPEN_ADJUST: 'open_adjust',
  DESTRUCTION: 'destruction',
} as const

/** 审批目标类型 */
export const ApprovalTargetType = {
  ARCHIVE: 'archive',
  DESTRUCTION_LIST: 'destruction_list',
} as const

// 派生值类型
export type AppraisalResultValue = (typeof AppraisalResult)[keyof typeof AppraisalResult]
export type AppraisalBatchStatusValue = (typeof AppraisalBatchStatus)[keyof typeof AppraisalBatchStatus]
export type DestructionListStatusValue = (typeof DestructionListStatus)[keyof typeof DestructionListStatus]
export type DestroyMethodValue = (typeof DestroyMethod)[keyof typeof DestroyMethod]
export type FileDeleteStatusValue = (typeof FileDeleteStatus)[keyof typeof FileDeleteStatus]
export type ApprovalTypeValue = (typeof ApprovalType)[keyof typeof ApprovalType]
export type ApprovalTargetTypeValue = (typeof ApprovalTargetType)[keyof typeof ApprovalTargetType]
export type ApprovalStatusValue = (typeof ApprovalStatus)[keyof typeof ApprovalStatus]
export type SecurityLevelValue = number
export type ArchiveLifecycleStatusValue = (typeof ArchiveLifecycleStatus)[keyof typeof ArchiveLifecycleStatus]

// 中文映射
export const AppraisalResultLabel: Record<string, string> = {
  extend: '延长保存',
  destroy: '待销毁',
}
export const AppraisalBatchStatusLabel: Record<string, string> = {
  draft: '草稿',
  completed: '已完成',
}
export const DestructionListStatusLabel: Record<string, string> = {
  draft: '待提交',
  pending_approval: '待审批',
  pending_destroy: '待销毁',
  destroyed: '已销毁',
}
export const DestroyMethodLabel: Record<string, string> = {
  shredding: '粉碎',
  burning: '焚毁',
  entrusted: '委托销毁',
}
export const FileDeleteStatusLabel: Record<string, string> = {
  not_started: '未开始',
  deleted: '已删除',
  failed: '删除失败',
}
export const ApprovalTypeLabel: Record<string, string> = {
  security_adjust: '密级调整',
  open_adjust: '开放调整',
  destruction: '销毁审批',
}

/** 库房状态 */
export const WarehouseRoomStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 架位状态 */
export const LocationStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 档案盒状态（对齐数据库设计 7.3） */
export const BoxStatus = {
  NORMAL: 'normal',
  FULL: 'full',
  MOVED: 'moved',
  DESTROYED: 'destroyed',
} as const

/** 盘点任务状态（对齐数据库设计 10.1） */
export const InventoryTaskStatus = {
  DRAFT: 'draft',
  RUNNING: 'running',
  COMPLETED: 'completed',
} as const

/** 盘点结果（对齐数据库设计 10.2 check_result） */
export const InventoryCheckResult = {
  NORMAL: 'normal',
  MISSING: 'missing',
  MISPLACED: 'misplaced',
  DAMAGED: 'damaged',
  ON_LOAN: 'on_loan',
} as const

/** 归还检查结果（对齐数据库设计 9.1 return_check_result） */
export const ReturnCheckResult = {
  NORMAL: 'normal',
  DAMAGED: 'damaged',
  MISSING_PAGE: 'missing_page',
  OTHER: 'other',
} as const

// 派生值类型
export type WarehouseRoomStatusValue = (typeof WarehouseRoomStatus)[keyof typeof WarehouseRoomStatus]
export type LocationStatusValue = (typeof LocationStatus)[keyof typeof LocationStatus]
export type BoxStatusValue = (typeof BoxStatus)[keyof typeof BoxStatus]
export type InventoryTaskStatusValue = (typeof InventoryTaskStatus)[keyof typeof InventoryTaskStatus]
export type InventoryCheckResultValue = (typeof InventoryCheckResult)[keyof typeof InventoryCheckResult]
export type ReturnCheckResultValue = (typeof ReturnCheckResult)[keyof typeof ReturnCheckResult]

// 中文映射
export const WarehouseRoomStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '停用',
}
export const LocationStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '停用',
}
export const BoxStatusLabel: Record<string, string> = {
  normal: '正常',
  full: '满盒',
  moved: '已移动',
  destroyed: '已销毁',
}
export const InventoryTaskStatusLabel: Record<string, string> = {
  draft: '草稿',
  running: '进行中',
  completed: '已完成',
}
export const InventoryCheckResultLabel: Record<string, string> = {
  normal: '正常',
  missing: '缺失',
  misplaced: '错位',
  damaged: '损坏',
  on_loan: '借出中',
}
export const ReturnCheckResultLabel: Record<string, string> = {
  normal: '正常',
  damaged: '破损',
  missing_page: '缺页',
  other: '其他',
}

/** 备份范围（对齐 DB 11.1 backup_scope） */
export const BackupScope = {
  DATABASE: 'database',
  FILES: 'files',
  BOTH: 'both',
} as const

/** 备份任务状态（对齐 DB 11.1 status） */
export const BackupTaskStatus = {
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
} as const

/** 四性检测类型（对齐接口文档 §21.4 checkTypes） */
export const CheckType = {
  INTEGRITY: 'integrity',
  USABILITY: 'usability',
  AUTHENTICITY: 'authenticity',
  SECURITY: 'security',
} as const

/** 四性检测结果 */
export const CheckResult = {
  PASSED: 'passed',
  FAILED: 'failed',
  NOT_CONFIGURED: 'not_configured',
} as const

/** 用户类型（对齐 DB 4.3） */
export const UserType = {
  INTERNAL: 'internal',
  PUBLIC: 'public',
} as const

/** 数据范围（对齐 DB 4.3 data_scope） */
export const DataScope = {
  OWN_ORG: 'own_org',
  OWN_FONDS: 'own_fonds',
  ALL: 'all',
} as const

/** 用户状态（对齐 DB 4.3 status） */
export const UserStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 配置项值类型（对齐接口文档 §22.9 valueType） */
export const ConfigValueType = {
  STRING: 'string',
  NUMBER: 'number',
  BOOLEAN: 'boolean',
  JSON: 'json',
} as const

// 派生值类型（保存/用户/配置）
export type BackupScopeValue = (typeof BackupScope)[keyof typeof BackupScope]
export type BackupTaskStatusValue = (typeof BackupTaskStatus)[keyof typeof BackupTaskStatus]
export type CheckTypeValue = (typeof CheckType)[keyof typeof CheckType]
export type CheckResultValue = (typeof CheckResult)[keyof typeof CheckResult]
export type UserTypeValue = (typeof UserType)[keyof typeof UserType]
export type DataScopeValue = (typeof DataScope)[keyof typeof DataScope]
export type UserStatusValue = (typeof UserStatus)[keyof typeof UserStatus]
export type ConfigValueTypeValue = (typeof ConfigValueType)[keyof typeof ConfigValueType]

// 中文映射（保存/用户/配置）
export const BackupScopeLabel: Record<string, string> = {
  database: '数据库',
  files: '电子文件',
  both: '同时',
}
export const BackupTaskStatusLabel: Record<string, string> = {
  running: '执行中',
  success: '成功',
  failed: '失败',
}
export const CheckTypeLabel: Record<string, string> = {
  integrity: '完整性',
  usability: '可用性',
  authenticity: '真实性',
  security: '安全性',
}
// 四性检测结果保留原值展示（原型以 passed/failed/not_configured 原文呈现）
export const CheckResultLabel: Record<string, string> = {
  passed: '通过',
  failed: '未通过',
  not_configured: '未配置',
}
export const UserTypeLabel: Record<string, string> = {
  internal: '内部',
  public: '公众',
}
export const DataScopeLabel: Record<string, string> = {
  own_org: '本单位',
  own_fonds: '本全宗',
  all: '全部',
}
export const UserStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '禁用',
}
export const ConfigValueTypeLabel: Record<string, string> = {
  string: 'string',
  number: 'number',
  boolean: 'boolean',
  json: 'json',
}

/** 编研成果状态（对齐 DB compilations.status） */
export const CompilationStatus = {
  DRAFT: 'draft',
  GENERATED: 'generated',
  ARCHIVED: 'archived',
} as const

/** 研判任务状态（对齐 DB analysis_tasks.status） */
export const AnalysisTaskStatus = {
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
} as const

/** 研判任务类型（§20.5 taskType） */
export const AnalysisTaskType = {
  RULE: 'rule',
  AI: 'ai',
  MIXED: 'mixed',
} as const

/** 研判建议处理状态（对齐 DB analysis_items.status） */
export const AnalysisItemStatus = {
  PENDING: 'pending',
  ADOPTED: 'adopted',
  REJECTED: 'rejected',
} as const

/** 研判问题类型 */
export const AnalysisProblemType = {
  MISSING_FIELD: 'missing_field',
  CATEGORY_CONFLICT: 'category_conflict',
  TAG_SUGGESTION: 'tag_suggestion',
  DATE_ABNORMAL: 'date_abnormal',
  DUPLICATE: 'duplicate',
} as const

/** 研判建议处理动作（§20.7 action） */
export const AnalysisHandleAction = {
  ADOPTED: 'adopted',
  REJECTED: 'rejected',
} as const

// 派生值类型（编研/研判）
export type CompilationStatusValue = (typeof CompilationStatus)[keyof typeof CompilationStatus]
export type AnalysisTaskStatusValue = (typeof AnalysisTaskStatus)[keyof typeof AnalysisTaskStatus]
export type AnalysisTaskTypeValue = (typeof AnalysisTaskType)[keyof typeof AnalysisTaskType]
export type AnalysisItemStatusValue = (typeof AnalysisItemStatus)[keyof typeof AnalysisItemStatus]
export type AnalysisProblemTypeValue = (typeof AnalysisProblemType)[keyof typeof AnalysisProblemType]
export type AnalysisHandleActionValue = (typeof AnalysisHandleAction)[keyof typeof AnalysisHandleAction]

// 中文映射（编研/研判）
export const CompilationStatusLabel: Record<string, string> = {
  draft: '草稿',
  generated: '已生成',
  archived: '已入库',
}
export const AnalysisTaskStatusLabel: Record<string, string> = {
  running: '执行中',
  completed: '已完成',
  failed: '失败',
}
export const AnalysisTaskTypeLabel: Record<string, string> = {
  rule: '规则扫描',
  ai: 'AI研判',
  mixed: '综合',
}
export const AnalysisItemStatusLabel: Record<string, string> = {
  pending: '待处理',
  adopted: '已采纳',
  rejected: '已不采纳',
}
export const AnalysisProblemTypeLabel: Record<string, string> = {
  missing_field: '缺失字段',
  category_conflict: '分类冲突',
  tag_suggestion: '标签建议',
  date_abnormal: '日期异常',
  duplicate: '疑似重复',
}
