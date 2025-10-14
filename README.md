# 二手房屋销售全栈示例

该项目演示了一个使用 Spring Boot (Java 21) 与 Vue 3 (Node.js 22) 实现的二手房屋销售管理系统，包含房源的增、删、改、查功能。后端提供 RESTful API，对接 MySQL 数据库，前端通过 Axios 与后端交互。

## 目录结构

```
.
├── demo/                             # Spring Boot 后端
├── front/second-hand-house-frontend/ # Vue 3 前端 (Vite)
└── database/                         # MySQL 初始化脚本
```

## 后端运行 (Spring Boot)

1. 进入 `demo` 目录并根据需要调整 `src/main/resources/application.yml` 中的数据库配置：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/secondhand_house?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
       username: your_mysql_username
       password: your_mysql_password
   ```

2. 确认数据库中已执行 `database/secondhand_house_schema.sql` 创建数据库及表结构。
3. 启动后端服务：

   ```bash
   ./gradlew bootRun
   ```

   服务默认在 `http://localhost:8080` 启动，API 基础路径为 `/api/houses`。

### 主要 API

| 方法 | 路径               | 说明             |
| ---- | ------------------ | ---------------- |
| GET  | `/api/houses`      | 查询全部房源     |
| GET  | `/api/houses/{id}` | 查询指定房源     |
| POST | `/api/houses`      | 新增房源         |
| PUT  | `/api/houses/{id}` | 更新指定房源     |
| DELETE | `/api/houses/{id}` | 删除指定房源   |

> POST/PUT 请求示例
>
> ```json
> {
>   "title": "三室两厅学区房",
>   "address": "杭州市西湖区教工路88号",
>   "price": 280,
>   "area": 98.5,
>   "description": "满五唯一，南北通透，拎包入住",
>   "sellerName": "王先生",
>   "contactNumber": "13800001111",
>   "listingDate": "2024-10-01"
> }
> ```

## 前端运行 (Vue 3 + Vite)

1. 进入 `front/second-hand-house-frontend` 目录安装依赖：

   ```bash
   npm install
   ```

2. 运行开发服务器：

   ```bash
   npm run dev
   ```

   默认监听 `http://localhost:5173`，已配置代理将 `/api` 请求转发到 `http://localhost:8080`。

3. 如果后端部署在其他地址，可通过创建 `.env` 文件覆盖 `VITE_API_BASE_URL`：

   ```bash
   echo "VITE_API_BASE_URL=https://your-server/api" > .env.local
   ```

## 数据库脚本

`database/secondhand_house_schema.sql` 包含数据库及房源表的建表语句，可直接在 MySQL 中执行。

## 测试

- 后端：`./gradlew test`
- 前端构建：`npm run build`（需先 `npm install`）

## 技术栈

- 后端：Spring Boot 3.5、Spring Data JPA、Validation、MySQL
- 前端：Vue 3、Vite、Axios
- 语言：Java 21、Node.js 22

## 截图

前端组件支持在表格内编辑/删除房源，以及在表单中新增房源。运行 `npm run dev` 后即可访问管理界面。
