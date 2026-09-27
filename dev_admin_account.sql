-- 仅用于当前本地开发数据。执行 upgrade_merchant_role.sql 后运行。
-- 现有 admin 账号（id=2）保留原密码，仅赋予管理员角色。
UPDATE merchant
SET role = 'ADMIN'
WHERE id = 2 AND username = 'admin';
