import { resolve } from 'node:path'
import { RUN_STAMP } from './env'

// doc/e2e-tests/support/fixtures.ts → 仓库根是上三级
const REPO_ROOT = resolve(__dirname, '..', '..', '..')

/** 后端测试夹具图片目录（02~08，已纳入版本库） */
export const FIXTURE_DIR = resolve(REPO_ROOT, 'backend/src/test/resources/fixtures/archives')

/** 测试用夹具文件绝对路径，覆盖各业务闭环所需载体 */
export const FIXTURES = {
  /** 会计凭证电子件（移交验收/电子预览/四性） */
  voucher: resolve(FIXTURE_DIR, '02-accounting-voucher.png'),
  /** 文书会议纪要 */
  meeting: resolve(FIXTURE_DIR, '03-meeting-minutes.png'),
  /** 文书政府批文 */
  govApproval: resolve(FIXTURE_DIR, '04-government-approval.png'),
  /** 公众捐赠老照片 */
  oldPhoto: resolve(FIXTURE_DIR, '05-old-city-photo-2003.png'),
  /** 科技工程蓝图 */
  blueprint: resolve(FIXTURE_DIR, '06-engineering-blueprint.png'),
  /** 密级/开放调整凭证档案 */
  authLetter: resolve(FIXTURE_DIR, '07-archive-disposition-authorization.png'),
  /** 销毁确认现场照片 */
  destroyScene: resolve(FIXTURE_DIR, '08-destruction-scene.png'),
  /** 临时 PDF 夹具 */
  sealPdf: resolve(REPO_ROOT, 'tmp-doc/test-archives/test-seal.pdf'),
} as const

/**
 * 生成本次运行唯一的业务标题，用于幂等创建数据。
 * 同一 run 内不同测试/不同次执行互不冲突，断言只认本前缀。
 */
export function uniqueTitle(label: string, suffix = ''): string {
  return `PW${RUN_STAMP}-${label}${suffix}`
}
