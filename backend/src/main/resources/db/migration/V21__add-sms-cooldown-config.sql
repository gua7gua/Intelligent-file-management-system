-- 新增短信验证码冷却配置项（同一手机号两次发码的最小间隔，单位：秒）。
-- SmsRateLimiter 读取本项实现冷却时长可配置；默认 60s。ON CONFLICT 保证重建库重复执行幂等。
INSERT INTO system_configs (config_key, config_value, value_type, description, editable) VALUES
  ('sms.code_cooldown_seconds', '60', 'number', '同一手机号两次发送短信验证码的最小间隔（单位：秒）。', true)
ON CONFLICT (config_key) DO NOTHING;
