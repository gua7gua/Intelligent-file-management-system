/**
 * 异步轮询工具。用于 AI 任务（补全/研判/生成正文，30-60s）、列表刷新、状态机推进等
 * 「触发后需等待」的场景。优先用 Playwright 的 expect.poll；此处在不便用 expect 时提供通用轮询。
 */
export async function pollUntil<T>(
  fn: () => Promise<T>,
  opts: { timeout?: number; interval?: number; message?: string } = {},
): Promise<T> {
  const { timeout = 120_000, interval = 2_000, message = '轮询超时' } = opts
  const deadline = Date.now() + timeout
  let lastErr: unknown
  while (Date.now() < deadline) {
    try {
      return await fn()
    } catch (e) {
      lastErr = e
      await new Promise((r) => setTimeout(r, interval))
    }
  }
  throw new Error(`${message}（${timeout}ms）：${(lastErr as Error)?.message ?? lastErr}`)
}
