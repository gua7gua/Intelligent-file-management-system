# 接口冻结 — 观察项处理结论（周扬运行时体检产出）

- 编写人：周扬（后端）
- 日期：2026-06-16
- 关联：`2026-06-16-iface-freeze-zhou-runtime.md`（运行时验证报告）
- 状态：**全部经组长拍板，已收敛，无遗留阻塞。**

## 结论汇总

| # | 观察项 | 组长决定 | 处理 |
|---|--------|----------|------|
| 1 | sys_admin 是否统一为业务超级角色 | **无需**（设计即 3 个系统页面） | 撤回该观察项，不改代码 |
| 2 | 枚举非法值错误提示是否精确化 | **保持通用 400** | 维持本次 §3 修复（通用「请求参数不合法」） |
| 3 | AI 通路 dev 验证 | **环境有 key** | 已实测通过（见运行时报告 §5），非「不可验证」 |
| 4 | 上传端点非 multipart→500 是否处理 | **不必要** | 不改（低优先边缘） |

## 1. sys_admin 权限（撤回）

组长确认：sys_admin 设计上**仅 3 个系统页面**（用户/角色/系统配置域），不需要业务模块权限；**无任何文档要求其额外权限**。
故初版报告所述「sys_admin 在各模块权限不一致」**不构成问题**——业务模块对 sys_admin 返回 403 属预期设计，撤回该观察项，不改动任何 `hasRole` 检查。

## 2. 枚举错误提示（保持通用）

非法枚举值经本次修复统一返回 **400「请求参数不合法」**（`GlobalExceptionHandler` 新增 IAE 处理器）。组长决定保持通用提示，不做逐字段精确化。当前实现已满足。

## 3. AI 通路（已实测，纠正初版误判）

**初版报告误称「dev 无 DEEPSEEK_API_KEY、AI 不可验证」——已纠正。** 实际情况：
- `DEEPSEEK_API_KEY` **在环境中存在**；
- AI 不可用是因为功能开关 `ai.enabled` 在 `application.yml` 硬编码 `false`、dev profile 未覆盖、且无 `${AI_ENABLED}` 环境变量开关。
- 以 `--ai.enabled=true` 启动后，**真正调 AI 的 3 个端点 happy-path 全部实测通过**：
  - 公众/内部 `archives/ai-query` → 真实 DeepSeek 结构化结果（200，~2s）；
  - `pending-archive/.../ai-completion` → 对 seed batch 2 触发，ai_task 异步执行 completed，`raw_response` 捕获真实 AI 返回、item 的 ai_suggestion 落库（验证后已精确还原 seed）。
- **澄清**：`compilations/{id}/generate` **不是 AI 端点**（初版误标），它是把已有 contentHtml 渲染成 PDF 的文档生成功能，不调 AI。

**遗留提醒**：当前代码默认 `ai.enabled=false`，冻结前需确认预发/生产环境显式开启该配置。

## 4. MultipartException（不处理）

上传端点收到非 multipart 请求会 500（属兜底）。组长判定**无必要**单独加 `MultipartException→400` 处理器，前端按文档用 multipart 即可。不改。

---

## 已自行修复（仅汇报）

`GlobalExceptionHandler` 新增 `IllegalArgumentException → 400` 处理器（+13 行），解决全仓 ~10 处 `Enum.valueOf(用户输入)` 导致的 500，并使 `OrganizationService` 既有注释「IAE→全局 400」兑现。详见运行时报告 §3，已 dev 重启验证 + 221 单测零回归。

> 以上 4 项均已闭环，不影响「接口可冻结」的结论。
