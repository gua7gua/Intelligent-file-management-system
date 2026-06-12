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
