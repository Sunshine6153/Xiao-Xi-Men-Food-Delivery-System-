# 小西门校园美食配送系统

校园多商户点餐与帮带配送系统，包含 Spring Boot 后端、Vue 管理端与微信小程序。

## 项目目录

| 目录 | 内容 |
| --- | --- |
| `sky-common`、`sky-pojo`、`sky-server` | 后端公共模块、数据对象和业务服务 |
| `admin-web` | Vue 3 / Vite 管理员及商户管理端，Element Plus 和 ECharts |
| `miniprogram` | 微信小程序客户端 |
| 根目录 `*.sql` | 建表、升级及开发种子数据 |
| `admin-web/docs` | 联调、功能接入及演示数据说明 |

## 本地启动

后端使用 JDK 17、Maven、MySQL 和 Redis。先执行 `create_tables.sql`，已有旧库按需执行对应 `upgrade_*.sql`，不要重复覆盖业务数据。

复制 `sky-server/src/main/resources/application-dev.example.yml` 为同目录 `application-dev.yml`，填好数据库、Redis、微信、JWT 等本机配置。真实密码和 AppSecret 不上传。

```sh
mvn clean install
mvn -pl sky-server spring-boot:run
```

管理端默认请求通过 Vite 的 `/api` 代理连接本地 8080 后端：

```sh
cd admin-web
npm ci
npm run dev
```

微信开发者工具导入 `miniprogram`，按自己的小程序账号核对 `project.config.json` 中的 AppID，并检查 `utils` 下的服务地址。AppID 是公开标识，AppSecret 只在后端本机配置。

各端详细说明见各目录文档。开发 SQL 中的演示账号仅用于本机测试，不能用于公开部署。

## 数据与安全

- 当前版本包含源码、测试、SQL 和 Excel 报表模板，不包含数据库实际数据备份、构建产物、依赖目录或 IDE 私有配置。
- 拟真演示数据生成脚本在 `admin-web/scripts/seed-realistic-data.mjs`，目前按原开发机器的路径配置；在其他机器运行前需调整路径并核对目标数据库。脚本会备份，已有本批数据时拒绝重复插入。
- 旧提交中可能存在历史开发数据库密码；当前配置已改为环境变量，但历史不会被本次提交删除。公开部署前应更换历史凭据并设置自己的账号和密钥。
