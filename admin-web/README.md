# 小西门管理端前端

Vue 3 + Vite + Vue Router + Axios。现已实现商家端菜品管理，以及管理员商户、分类、跨商户菜品和用户管理，并接入服务端身份和权限校验。

## 后端接口约定

后端: `/Users/a123/Work/Spring/xiaoximen`,端口 `8080`

- `POST /admin/merchant/login`
- 请求体: `{ "username": "...", "password": "..." }` (明文,后端做 MD5)
- 成功响应: `{ "code": 200, "message": "操作成功", "data": { "id": 1, "name": "xxx", "token": "jwt", "role": "MERCHANT" } }`；管理员的 `role` 为 `ADMIN`。
- `GET /admin/auth/me` 使用 `token` 请求头校验当前账号 ID 与角色。
- 失败: 业务 `code` 可能是 `401/403/500`；后端已有全局异常处理器，前端同时处理业务错误与 HTTP 异常。

## 本地联调

1. 启动后端(Spring Boot,`sky-server`,端口 8080,保证数据库 `xiaoximen` 可连)。
2. 启动前端:

```bash
npm install
npm run dev
```

3. 浏览器打开 http://localhost:5173 ,输入 `merchant` 表中的账号密码登录。

## 跨域说明

后端目前没有 CORS 配置,浏览器不能从 `:5173` 直连 `:8080`。
开发环境已用 Vite 代理解决:前端请求 `/api/admin/merchant/login`,Vite 转发到
`http://localhost:8080/admin/merchant/login`(`vite.config.js` + `.env.development`)。

生产若前后端分离部署,请把 `.env.production` 中的 `VITE_API_BASE_URL`
改成后端公网地址,并在后端加 CORS 放行前端域名;
若同源部署(Nginx 将 `/api` 反代到 8080),保持 `/api` 即可。

## 登录成功后

- `token / merchant_id / merchant_name / role` 存入 `localStorage`；路由每次从 `/admin/auth/me` 核验角色，不以本地值授权。
- 后续请求由 `src/utils/request.js` 自动带 `token` 请求头
- 商家进入 `/session`；管理员进入 `/admin`。管理员菜单已开放，尚未实现的业务页面会明确显示“待接入”。旧 `/admin-pending` 地址会跳转到 `/admin`。
- 管理员“菜品管理 → 分类管理”已接入真实分类列表、详情、新增、编辑、启停与删除接口；有关联菜品的分类不能删除。
- 管理员“菜品管理 → 菜品列表”已接入全平台分页、商户/分类/状态筛选、详情与上下架接口；不显示后端未授权的新增、编辑和删除操作。
- 管理员“商户管理”已接入真实分页、搜索、详情、新增、编辑与启停接口；管理员账号不会混入商户列表。
- 管理员“用户管理”已接入真实分页、搜索、详情与启禁用接口，仅展示管理所需的非敏感字段。
- 商家“菜品管理”已接入真实查询、详情、新增、编辑、图片上传、口味、上下架与删除接口。
