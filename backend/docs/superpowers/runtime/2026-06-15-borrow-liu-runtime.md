# feat/borrow-liu Runtime 全链路验证记录

日期：2026-06-15
负责人：刘星
分支：`feat/borrow-liu`
环境：Postgres 17（archive-postgres 容器）+ MinIO + ClamAV + 后端 `spring-boot:run -Dspring-boot.run.profiles=dev`（端口 8080）

种子用户（demo 密码均 `123456`）：
- li.reader（internal_reader，portal=internal）
- liu.back（back_archivist，portal=admin）
- chen.front（front_archivist，portal=admin）

Sa-Token 鉴权：`Authorization: Bearer <token>`。

测试档案：种子库无可借档案，临时插入 archive id=8（ARC-TEST-BORROW，paper/normal/available/normal）。验证后已软删 + 复位序列。

---

## 验证结果

| # | 场景 | 结果 | 证据 |
|---|------|------|------|
| 1 | 登录 li.reader / liu.back 取 token | ✅ | code=OK，roles=[internal_reader] / back_archivist |
| 2 | 申请借阅 archive 8（11.7） | ✅ | id=2，requestNo=BRW-000002，status=applied |
| 3 | **审批通过**（12.3） | ✅（修复后） | 见下方「发现并修复的 bug」 |
| 4 | 导出凭证首次（11.10） | ✅ | Content-Type=application/pdf，PDF 头 `%PDF`，2338B；DB status=voucher_issued，voucherNo=VCH-000002 |
| 5 | 出库（12.4） | ✅ | status=checked_out，archive8 loan_status=on_loan |
| 6 | 正常归还（12.5 normal） | ✅ | status=returned，archive8 loan_status=available |
| 7 | 异常归还（damaged） | ✅ | status=abnormal_return，archive8 condition_status=damaged、loan_status=available |
| 8 | 异常归还后再申请被实体状态拦截 | ✅ | BUSINESS_CONFLICT「档案实体状态异常，暂停借阅」（条件 5） |
| 9 | 重复申请拦截（同一档案有未结束申请） | ✅ | BUSINESS_CONFLICT「该档案存在未结束的借阅申请」（条件 7，apply 1-arg） |
| 10 | 凭证号不匹配 checkout | ✅ | BUSINESS_CONFLICT「凭证号不匹配」 |
| 11 | 越权：internal_reader 调管理端列表 | ✅ | HTTP 403（角色强制生效） |
| 12 | 编号序列 | ✅ | BRW-000002/3/4、VCH-000002/3/4 连续自增 |

### 发现并修复的 bug（runtime 暴露，单元测试未覆盖）

**现象**：申请成功（applied）后，审批通过返回 `BUSINESS_CONFLICT`，状态停在 applied，无法进入 approved。

**根因**：`BorrowService.approve` / `checkout` 在状态变更前调 `eligibilityChecker.checkBorrowable(archiveId)` 复校；该方法的条件 7「无未结束借阅申请」把**当前正在审批/出库的这条申请自身**计入未结束申请 → 复校恒失败。单元测试未发现，因 approve/checkout 测试中 `eligibilityChecker` 是 mock（no-op）。

**修复**（commit `339a750` `fix(borrow): 审批出库复校排除当前申请避免误拦`）：`BorrowEligibilityChecker` 新增 `checkBorrowable(archiveId, excludeRequestId)` 重载，1-arg 委托；条件 7 在 `excludeRequestId != null` 时加 `.ne("id", excludeRequestId)`。`apply` 仍用 1-arg（拦截重复申请），`approve`/`checkout` 改用 2-arg 传入当前 requestId。新增测试 `排除当前申请时条件7不误拦`。修复后重启后端，审批→出库全链路通过。

### 测试脚本自身的非问题

凭证号不匹配 checkout 首次返回 `INTERNAL_ERROR`：经日志定位是脚本 artifact——前一步申请被拒无 id，`jq -r .data.id` 返回字面 `null`，checkout 路径变成 `/borrow-requests/null/checkout`，`@PathVariable Long` 解析 "null" 抛 NumberFormatException。改用真实 request id 后复测，返回正确的 `BUSINESS_CONFLICT「凭证号不匹配」`。非产品 bug。

---

## 清理

- 软删测试 archive（id=8）；删除测试 borrow_requests（id=2/3/4）。
- 复位 `seq_borrow_request_no` / `seq_borrow_voucher_no` 至 1。
- 停止后端进程（8080 释放）。
- 验证后 DB 恢复种子态：borrow_requests=1（种子 id=1 checked_out），archives=5。

## 结论

借阅模块 9 接口 + 工作台整合 + 状态机 + 档案联动 + 角色强制 + 凭证 PDF 经真实登录态 + 真实 Postgres 全链路验证通过；并发现修复了 1 个 mock 未覆盖的复校逻辑 bug。模块可交付方江苏 review。
