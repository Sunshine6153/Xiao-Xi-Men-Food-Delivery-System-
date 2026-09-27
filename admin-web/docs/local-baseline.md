# 本地联调基线

这份说明对应管理端实施方案的第 0 步。目标是确认后端、数据库、Redis、测试数据和前端代理都可用，再开始页面联调。

## 环境约定

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- MySQL：`localhost:3306`，数据库 `xiaoximen`
- Redis：`localhost:6379`
- 后端本地配置位置：`/Users/a123/Work/Spring/xiaoximen/sky-server/src/main/resources/application-dev.yml`
- 测试数据脚本：`/Users/a123/Work/Spring/xiaoximen/test-data.sql`

后端配置当前使用 MySQL `root` 账号和本机密码，实际密码以本机 `application-dev.yml` 为准。项目测试数据脚本会幂等插入商户、分类和菜品，其中部分种子账号仍是明文密码，不能用于当前按 MD5 校验的登录。当前可用于基线检查的账号是：

```text
testmerchant / 123456
admin        / 123456
```

当前 `testmerchant` 为 `MERCHANT`，`admin` 为 `ADMIN`。脚本会通过 `/admin/auth/me` 读取服务端身份，并按身份验证不同的权限边界。

## 准备步骤

1. 启动 MySQL 和 Redis，并确认 `3306`、`6379` 端口可以连接。
2. 创建或确认 `xiaoximen` 数据库及表结构，执行 `create_tables.sql`。
3. 执行 `test-data.sql`，确认测试账号、分类和菜品数据存在。
4. 在后端项目中启动 `sky-server`，确认 8080 端口可用。
5. 在前端项目执行：

```bash
npm install
BASELINE_ACCOUNT_1_USERNAME=testmerchant \
BASELINE_ACCOUNT_1_PASSWORD=123456 \
BASELINE_ACCOUNT_2_USERNAME=admin \
BASELINE_ACCOUNT_2_PASSWORD=123456 \
npm run check:baseline
```

检查脚本会验证：

- MySQL、Redis 端口；
- 后端 OpenAPI 地址；
- 两个测试账号的真实登录响应是否为 `code: 200` 且带 token；
- `/admin/auth/me` 返回的身份是否与登录响应一致；
- 两种身份是否都能读取分类；
- 商户菜品是否都属于当前商户；管理员能否访问商户分页并被本店菜品接口拒绝；
- 前端项目的环境代理是否仍指向 `http://localhost:8080`（由已有 `.env.development` 和 `vite.config.js` 保证）。

脚本不会创建、修改或删除业务数据；账号请求只读取身份、分类、商户分页和菜品分页。

## 第 0 步通过标准

`npm run check:baseline` 输出全部 `PASS`，同时 `npm run build` 通过。至少保留一套商户账号和一套管理员账号的登录、身份、分类及各自授权接口结果。

如果 MySQL、Redis 或后端未启动，脚本必须明确输出 `FAIL`，此时第 0 步只能标记为“环境阻断”，不能进入真实接口联调。

## 当前执行结果

2026-09-23 已按角色完成真实基线验证：

- MySQL `3306`：通过；
- Redis `6379`：通过；
- Spring Boot `8080`：通过，OpenAPI 返回 HTTP 200；
- `testmerchant / 123456`：登录、身份、分类、本店菜品分页和归属隔离通过；
- `admin / 123456`：登录、身份、分类、商户分页和本店菜品权限隔离通过；
- 前端 `npm run build`：通过。

基线脚本只执行登录与只读请求，不修改数据库数据。后端通过完整构建后的 `sky-server` 启动，账号身份以服务端 `/admin/auth/me` 为准。
