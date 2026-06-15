# 模块三运行时验证报告：日志审计查询/导出（feat/audit-log-zhou）

- 验证人：周扬
- 验证日期：2026-06-15
- 分支：`feat/audit-log-zhou`
- 环境：PostgreSQL 17（archive-postgres:5432）+ 后端 dev profile（8080）

## 验证范围

§23.1 / §23.2 + 导出：审计日志、档案访问日志的游标分页查询 + xlsx 导出。只读、不可删改。

## 启动与单测

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 启动成功。
- 新增单测：`CursorCodecTest`(2)、`AuditLogQueryServiceTest`(4)、`ArchiveAccessLogQueryServiceTest`(2) 共 8 项全绿。
- 验证前 `audit_logs` 已有 69 条（各业务模块写入）；`archive_access_logs` 有真实访问记录。

## 验证结果（真实 HTTP）

登录 admin(sys_admin)。

| # | 场景 | 期望 | 实际 | 结论 |
|---|------|------|------|------|
| 1 | 审计日志首页 limit=5 | 5 条 + hasNext + nextCursor | n=5、hasNext=True、nextCursor 非空、首条 id=69（最新） | ✅ |
| 2 | 带 cursor 翻第二页 | 续接、id 递减 | 第二页 ids=[64,63,62,61,60] | ✅ |
| 3 | 翻页无重复 | 前两页 10 条无重复 | P1=69..65、P2=64..60，去重后仍 10 | ✅ |
| 4 | 筛选 operationType=reset_password | 仅命中该类型 | 命中 1 条，operationType 全为 reset_password | ✅ |
| 5 | 导出审计日志 xlsx | 200 + Excel 文件 | HTTP=200、8656 字节、`file` 识别 Microsoft Excel 2007+ | ✅ |
| 6 | 访问日志查询 limit=5 | 200 + 分页 | OK、5 条、hasNext=True（有真实数据） | ✅ |
| 6b | 导出访问日志 xlsx | 200 + Excel 文件 | HTTP=200、3787 字节、Excel 2007+ | ✅ |
| 7 | back_archivist 查审计日志 | 403 仅系统管理员 | FORBIDDEN 仅系统管理员可查询审计日志 | ✅ |
| 7b | back_archivist 查访问日志 | 200 允许 | HTTP=200 | ✅ |

## 只读约束

`AuditLogController` / `ArchiveAccessLogController` 仅定义 `@GetMapping`（查询 + 导出），无 POST/PUT/DELETE，Swagger 不出现写端点。

## 结论

模块三运行时验证全部通过，游标分页（按 §23 契约 records/nextCursor/hasNext）、筛选、xlsx 导出、权限、只读约束均符合设计与 §23 预期。基本完成。
