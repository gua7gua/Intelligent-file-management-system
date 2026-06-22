import { API_BASE, BASE_URL } from './support/env'
import { ACCOUNTS, authFile, type AccountKey } from './support/accounts'
import { saveAuthFile } from './support/auth'

/** 探活：GET 一次，接受 200/401/404 都算"进程在响应" */
async function checkHealth(url: string, name: string): Promise<void> {
  try {
    const res = await fetch(url, { method: 'GET' })
    if (![200, 401, 403, 404].includes(res.status)) {
      throw new Error(`HTTP ${res.status}`)
    }
  } catch (e) {
    throw new Error(
      `${name} 不可达（${url}）：${(e as Error).message}\n` +
        `请先启动前后端：\n` +
        `  前端 ${BASE_URL}（cd frontend && npm run dev）\n` +
        `  后端 ${API_BASE}（cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev）`,
    )
  }
}

/**
 * globalSetup：① 探活前后端 ② 为每个角色 API 登录生成 storageState。
 * 失败立即中止整个 run，给出清晰原因，避免后续测试集体"无头苍蝇"式失败。
 */
export default async function globalSetup(): Promise<void> {
  console.log('\n[globalSetup] 探活前后端…')
  await checkHealth(BASE_URL, '前端')
  await checkHealth(`${API_BASE}/actuator/health`, '后端')

  console.log('[globalSetup] 为各角色 API 登录并生成 storageState…')
  const keys = Object.keys(ACCOUNTS) as AccountKey[]
  for (const key of keys) {
    const { user } = await saveAuthFile(key, authFile(key), BASE_URL)
    console.log(`  ✓ ${ACCOUNTS[key].loginName.padEnd(16)} roles=[${user.roles.join(', ')}]`)
  }
  console.log('[globalSetup] 完成。\n')
}
