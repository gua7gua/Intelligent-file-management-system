-- 用户硬删除（D1）前置：审计日志与档案访问日志是「痕迹」类记录，应随用户清理而保留、仅置空外键，
-- 否则 audit_logs.actor_user_id / archive_access_logs.user_id 的 NO ACTION 外键会阻止任何有过操作/访问记录的用户被删除。
-- 改为 ON DELETE SET NULL：硬删用户时历史日志保留（actor/user 置空，前端兜底显示「已删除用户」），满足「留痕」要求。
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fk_audit_logs_actor_user_id;
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_actor_user_id
  FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE archive_access_logs DROP CONSTRAINT IF EXISTS fk_archive_access_logs_user_id;
ALTER TABLE archive_access_logs ADD CONSTRAINT fk_archive_access_logs_user_id
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
