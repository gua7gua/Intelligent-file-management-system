import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { API_BASE, PASSWORD } from './env'
import { ACCOUNTS, type AccountKey } from './accounts'

export interface LoginUser {
  id: number
  loginName: string
  realName: string
  roles: string[]
  [k: string]: unknown
}

export interface LoginResult {
  token: string
  user: LoginUser
}

/**
 * 直连后端登录 API 拿 token（绕过 UI），用于 globalSetup 预置登录态加速。
 * 登录页本身（含错密码）仍走真实表单，见 tests/00-login。
 */
export async function loginByApi(key: AccountKey): Promise<LoginResult> {
  const acc = ACCOUNTS[key]
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ loginName: acc.loginName, password: PASSWORD, portal: acc.portal }),
  })
  const json = await res.json().catch(() => null)
  if (!res.ok || !json || json.code !== 'OK') {
    throw new Error(`登录失败 ${acc.loginName} (HTTP ${res.status}): ${json?.message ?? res.statusText}`)
  }
  return json.data as LoginResult
}

/**
 * 为账号生成 storageState 文件（含 localStorage token）。
 * 前端 token 存在 localStorage key='token'（见 stores/auth.ts），注入后浏览器加载即视为已登录。
 */
export async function saveAuthFile(key: AccountKey, authPath: string, baseUrl: string): Promise<LoginResult> {
  const result = await loginByApi(key)
  mkdirSync(dirname(authPath), { recursive: true })
  const storageState = {
    origins: [
      {
        origin: baseUrl,
        localStorage: [{ name: 'token', value: result.token }],
      },
    ],
  }
  writeFileSync(authPath, JSON.stringify(storageState, null, 2), 'utf-8')
  return result
}

/** 用 storageState 已注入的 page，断言当前确实是某账号（调 /auth/me 取证） */
export async function fetchMe(token: string) {
  const res = await fetch(`${API_BASE}/api/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await res.json().catch(() => null)
  if (!res.ok || !json || json.code !== 'OK') {
    throw new Error(`/auth/me 失败 (HTTP ${res.status}): ${json?.message ?? res.statusText}`)
  }
  return json.data as LoginUser
}
