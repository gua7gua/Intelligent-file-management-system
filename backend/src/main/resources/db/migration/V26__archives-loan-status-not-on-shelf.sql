-- P2-3：archives.loan_status 新增 not_on_shelf（纸质档案入库后待上架，未上架期间不可借阅）
-- 配合 LoanStatus 枚举新增项；原 check 约束仅允许 available/on_loan，会拒绝 not_on_shelf 导致入库 500。
ALTER TABLE archives DROP CONSTRAINT IF EXISTS archives_loan_status_check;
ALTER TABLE archives ADD CONSTRAINT archives_loan_status_check
    CHECK (loan_status IN ('available', 'on_loan', 'not_on_shelf'));
