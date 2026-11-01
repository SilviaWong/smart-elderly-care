# 智慧养老管理系统 — 阶段实施计划与任务分解

- **文档版本**: V1.0
- **基线日期**: 2026-11-01
- **开发策略**: 模块化单体、先 P0 核心闭环再扩展 P1/P2、严格执行阶段门槛与集成测试

---

## 1. 实施阶段全景图

| 阶段编号 | 阶段名称 | 范围类型 | 核心交付物 | 关键验收门槛 |
|---|---|---|---|---|
| **阶段 0** | 仓库盘点与技术方案确认 | 调研与方案 | `architecture.md`, `ADR 0001`, `implementation-plan.md` | 不改代码/不执SQL/技术栈与约束确认完毕 |
| **阶段 1** | 工程骨架与本地开发环境 | 基础工程 | 后端 Spring Boot 3.2.8 + 前端 Vue 3 + Docker Compose | 前后端可启动、健康检查通过、OpenAPI 联通 |
| **阶段 2** | 数据库初始化与系统管理 | P0 基础 | 47 表 DDL 评审、RBAC 认证授权、JWT、机构隔离 | 401/403 正常拦截、跨机构数据隔离测试通过 |
| **阶段 3** | 老人档案管理 | P0 核心 | 老人主档案 CRUD、联系人、评估、脱敏 | 老人编号机构内唯一、敏感信息脱敏、档案历史保留 |
| **阶段 4** | 空间资源、床位与入住闭环 | P0 核心（重点） | 楼栋/房间/床位、入住、调床、退住、变动日志 | **同一床位并发请求只有一个成功**、退住释放床位 |
| **阶段 5** | 服务项目与工单全流程 | P0 核心 | 服务目录、工单 9 种状态机、派单接单、流转轨迹 | 非法状态转换被拒绝、每次状态变更写日志 |
| **阶段 6** | 健康记录与健康预警 | P0/P1 | 体征指标配置、体征录入、规则匹配、预警处置 | 命中规则生成预警、**去重时间窗口内不重复告警** |
| **阶段 7** | 护理计划与任务执行 | P0 核心（重点） | 护理等级、护理计划、定时任务生成、执行闭环 | **定时任务重复触发幂等不重生成**、执行留痕 |
| **阶段 8** | 安全风险事件管理 | P0 核心 | 风险上报、分派、处置、复核、关闭 | 高级风险主管复核、关闭必填结论、轨迹可溯 |
| **阶段 9** | 餐饮管理 | P1 扩展 | 菜品、每日菜单、订餐订单、明细快照 | 订单菜品价格快照防篡改、过敏禁忌提示、截止取消 |
| **阶段 10** | 财务管理 | P1 核心（重点） | 费用项、月度账单、收款登记、退款审批、对账 | **重复收款不重复记账**、退款超额拦截、金额汇总一致 |
| **阶段 11** | 设备与 IoT 模拟接入 | P1 扩展 | 设备台账、绑定解绑、模拟数据上报、设备告警 | 解绑后数据不串联、模拟上报幂等、协议边界隔离 |
| **阶段 12** | 运营看板与异步导出 | P1 扩展 | 经营看板统计、业务报表、异步文件导出任务 | 大数据量异步流式导出无 OOM、跨机构报表隔离 |
| **阶段 13** | 监管报送 | P2 扩展 | 报送模板配置、数据映射校验、批次生成导出 | 映射校验错误精准定位、导出文件合规、不虚构政府 API |
| **阶段 14** | 安全加固与最终交付 | 全局收尾 | 端到端联调、渗透自查、演示脚本、交付文档包 | 新环境一键启动、全流程端到端演示跑通、测试全绿 |

---

## 2. 阶段 0 执行详情（当前阶段）

### 2.1 任务目标与执行原则
- 目标：全面盘点项目环境、权威文档一致性、数据库物理设计、模块分层依赖，产出实施路线图。
- 原则：不修改任何业务代码，不执行数据库 DDL/DML，不下载外部依赖或软件，不擅自启动后续阶段。

### 2.2 盘点成果概览
1. **仓库状态**: 目录已有 `docs/` 和 `database/`，`backend/` 与 `frontend/` 为空，当前非 Git 仓库。
2. **环境状态**: Java 21.0.9 LTS, Maven 3.9.16, Node.js v24.11.0, npm 11.6.1, Docker 29.7.2, MySQL client 5.7.20。
3. **一致性审查**: PRD、ER、SQL 47 张表高度自洽，状态机枚举对齐，明确了并发锁、机构隔离和幂等键方案。

---

## 3. 下一阶段（阶段 1：工程骨架）详细执行计划

### 3.1 目标与范围
建立前后端可独立运行的工程骨架，打通本地联调链路，不涉及具体业务功能。

### 3.2 拟执行步骤与新增文件清单
1. **Git 初始化**:
   - 在根目录执行 `git init`，并创建标准的 `.gitignore`（包含 Java、Maven、Node、IDE 排除项）。
2. **后端工程初始化 (`backend/`)**:
   - 创建 `backend/pom.xml`，引入 Spring Boot 3.2.8 父工程，依赖：
     - `spring-boot-starter-web`
     - `spring-boot-starter-validation`
     - `spring-boot-starter-security`
     - `mybatis-plus-spring-boot3-starter:3.5.7`
     - `mysql-connector-j`
     - `springdoc-openapi-starter-webmvc-ui:2.5.0`
     - `jjwt-api / jjwt-impl / jjwt-jackson:0.12.5`
     - `lombok`
     - `spring-boot-starter-test`
   - 创建后端启动类 `com.example.elderlycare.ElderlyCareApplication`。
   - 创建通用层包结构：
     - `common/api/ApiResponse.java`
     - `common/api/ErrorCode.java`
     - `common/exception/BusinessException.java`
     - `common/exception/GlobalExceptionHandler.java`
     - `common/entity/BaseEntity.java`
     - `common/context/OrgContextHolder.java`
   - 创建健康检查与联调测试接口：
     - `controller/HealthCheckController.java` (`GET /api/v1/health`)
   - 配置文件：
     - `application.yml`
     - `application-dev.yml` (配置数据库驱动、MyBatis-Plus 日志等，密码支持从环境变量注入)
3. **前端工程初始化 (`frontend/`)**:
   - 使用 Vite 初始化 Vue 3 + TypeScript 基础结构。
   - 配置 `package.json`（Element Plus、Pinia、Vue Router、Axios、Sass 等）。
   - 创建 Vite 代理配置 `vite.config.ts`（将 `/api` 代理到后端 `http://localhost:8080`）。
   - 创建基础布局（Layout）、空白仪表盘占位页、登录页骨架及 404 页。
   - 封装 Axios 请求工具 `src/utils/request.ts`。
4. **运行与容器编排支持**:
   - 提供 `docker-compose.yml`（配置 MySQL 8.0 容器与数据卷），提供按需启动选择。

### 3.3 阶段 1 验收门槛
- [ ] 后端通过 `mvn clean test` 编译无误。
- [ ] 后端启动后访问 `GET /api/v1/health` 正常返回 200 及应用状态。
- [ ] 后端访问 Swagger/OpenAPI 界面 (`/swagger-ui/index.html`) 正常。
- [ ] 前端构建无 TypeScript 错误，开发服务器正常启动。
- [ ] 前端页面发起 `/api/v1/health` 请求能成功获取后端数据并展示。
