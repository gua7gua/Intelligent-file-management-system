/**
 * 环境与全局常量。
 * 所有可被环境变量覆盖的地址集中在此，便于在不同机器/CI 上调整。
 */

/** 被测前端地址（Vite dev server，默认 3000） */
export const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:3000'

/** 后端 API 地址，用于 globalSetup 健康检查 / 直连 API 取证 */
export const API_BASE = process.env.E2E_API_BASE || 'http://localhost:8080'

/** 全部种子账号统一密码 */
export const PASSWORD = '123456'

/** 短信降级固定验证码 */
export const SMS_CODE = '123456'

/** 公众注册/找回密码用手机号 */
export const PUBLIC_PHONE = '18877600249'

/**
 * 运行戳：进程启动时生成一次，全 run 共享。
 * 用于幂等数据前缀（捐赠标题、移交清单标题等），保证可重复运行不冲突。
 */
export const RUN_STAMP = String(Date.now()).slice(-8)
