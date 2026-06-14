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
