import { PASSWORD } from './env'

export type Portal = 'admin' | 'transfer' | 'internal' | 'public'

export interface Account {
  /** 登录名 */
  loginName: string
  /** 真实姓名（断言用） */
  realName: string
  /** 登录页「登录角色」下拉值 */
  role: string
  /** 登录请求 portal 字段 */
  portal: Portal
  /** 登录成功后默认进入的门户路径 */
  defaultPath: string
}

/**
 * 8 个种子测试账号（密码统一 123456）。
 * 来源：doc/测试计划.md §2.2 与 tmp-doc/第三轮逐页测试操作手册。
 */
export const ACCOUNTS = {
  liuBack: { loginName: 'liu.back', realName: '小刘', role: 'back_archivist', portal: 'admin', defaultPath: '/admin' },
  chenFront: { loginName: 'chen.front', realName: '小陈', role: 'front_archivist', portal: 'admin', defaultPath: '/admin' },
  fangDirector: { loginName: 'fang.director', realName: '小方', role: 'director', portal: 'admin', defaultPath: '/admin' },
  admin: { loginName: 'admin', realName: '系统管理员', role: 'sys_admin', portal: 'admin', defaultPath: '/admin' },
  liReader: { loginName: 'li.reader', realName: '小李', role: 'internal_reader', portal: 'internal', defaultPath: '/internal' },
  zhangTransfer: { loginName: 'zhang.transfer', realName: '小张', role: 'transfer_user', portal: 'transfer', defaultPath: '/transfer' },
  wangTransfer: { loginName: 'wang.transfer', realName: '王敏', role: 'transfer_user', portal: 'transfer', defaultPath: '/transfer' },
  zhouPublic: { loginName: 'zhou.public', realName: '小周', role: 'public_user', portal: 'public', defaultPath: '/public' },
} satisfies Record<string, Account>

export type AccountKey = keyof typeof ACCOUNTS

export const DEFAULT_PASSWORD = PASSWORD

/** storageState 文件路径：global-setup 为每个账号生成 */
export function authFile(key: AccountKey): string {
  return `.auth/${ACCOUNTS[key].loginName}.json`
}
