import { expect, type Locator } from '@playwright/test'

/** 断言元素可见且可点击（enabled） */
export async function expectVisibleAndEnabled(loc: Locator): Promise<void> {
  await expect(loc).toBeVisible()
  await expect(loc).toBeEnabled()
}

/**
 * 体感断言：区域文本不应出现未汉化的英文枚举。
 * 用于堵第三轮方法论里"pending_archive / voucher_issued / M09"等原始值泄露给用户的问题。
 */
export async function expectNoRawEnum(loc: Locator, enums: string[]): Promise<void> {
  const text = (await loc.textContent()) ?? ''
  const leaked = enums.filter((e) => text.includes(e))
  if (leaked.length > 0) {
    throw new Error(`区域出现未汉化的英文枚举 ${JSON.stringify(leaked)}，文本片段：${text.slice(0, 200)}`)
  }
}

/**
 * 体感断言：区域文本不应出现未格式化的 ISO 时间（如 2026-06-19T14:30:00Z）。
 */
export async function expectNoIsoTime(loc: Locator): Promise<void> {
  const text = (await loc.textContent()) ?? ''
  if (/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(text)) {
    throw new Error(`区域出现未格式化的 ISO 时间，文本片段：${text.slice(0, 200)}`)
  }
}

/** 公认易漏的英文枚举黑名单（按第三轮历史问题整理） */
export const COMMON_RAW_ENUMS = [
  'pending_archive',
  'pending_transfer',
  'pending_receive',
  'voucher_issued',
  'checked_out',
  'returned',
  'pending_shelf',
  'pending_destroy',
  'pending_contact',
]
