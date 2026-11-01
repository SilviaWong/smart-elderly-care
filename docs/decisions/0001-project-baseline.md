# ADR 0001: 技术基线与核心架构决策

- **状态**: Accepted
- **日期**: 2026-11-01
- **决策人**: 项目技术组

## 1. 背景与上下文
智慧养老管理系统旨在建设集老人档案、空间床位、服务工单、健康监测、护理计划、餐饮财务、IoT 与安全风险于一体的机构运营平台。
根据项目级 Codex 总 Prompt、PRD、ER 设计和 schema.sql，系统定义为面向 ToB 养老机构的业务平台。

## 2. 核心决策与技术选型

### 2.1 架构形态：模块化单体（Modular Monolith）
- **决策**: 采用单体应用结构，按业务领域分包，禁止初期微服务化。
- **理由**:
  1. 当前业务处于核心功能落地与闭环阶段，模块化单体能最大限度保证事务一致性（如入住事务、调床事务、退款事务）。
  2. 运维与部署成本低，避免分布式事务、跨服务网络调用及复杂中间件开销。
  3. 模块间仅通过明确的 Service 接口协作，禁止跨模块直接访问 Mapper，未来若需解耦微服务具有天然切分边界。

### 2.2 技术栈与运行环境兼容
- **后端技术栈**:
  - JDK: 开发环境检测到 JDK 21.0.9 LTS。基线目标保持为 Java 17 字节码标准（通过 Maven `<java.version>17</java.version>`），完全兼容 Java 17 与 Java 21 运行环境。
  - 核心框架: Spring Boot 3.2.8
  - 持久层: MyBatis-Plus 3.5.x + MySQL 8.0
  - 安全与认证: Spring Security 6.x + 独立无状态 JWT 认证
  - 接口规范: OpenAPI 3 / SpringDoc 2.x
- **前端技术栈**:
  - Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + Vue Router + Axios
- **环境基准**:
  - 本地环境已具备：Java 21.0.9, Maven 3.9.16, Node.js v24.11.0, npm 11.6.1, Docker 29.7.2。

### 2.3 多机构数据隔离策略（Multi-Tenant by OrgId）
- **决策**: 逻辑多租户隔离，所有业务表包含 `org_id` 字段。
- **实现规则**:
  1. `org_id` 必须在用户完成 JWT 认证后，由服务端从认证上下文提取并绑定到当前请求线程（如 `OrgContextHolder`），绝不信任前端请求体或 URL 参数传入的 `org_id`。
  2. 业务查询与更新均强制追加 `org_id = currentOrgId` 条件，服务层操作通过 ID 查询记录后必须双重校验所属机构。
  3. 平台级超管账号（`org_id IS NULL`）与机构级用户权限严格隔离。

### 2.4 床位并发控制与入住一致性
- **决策**: 采用数据库行级悲观锁（`SELECT ... FOR UPDATE`）结合乐观锁字段 `bed.version`。
- **实现规则**:
  1. 入住、调床操作必须在单个 `@Transactional` 事务内完成。
  2. 先锁定 `bed` 记录，校验状态必须为 `AVAILABLE`，并校验该老人当前无 `ACTIVE` 入住记录。
  3. 在同一事务内原子更新：创建 `admission`、更新 `bed.status = 'OCCUPIED'`、更新 `elder.status = 'IN_HOUSE'`、写入 `bed_change_log`。

### 2.5 逻辑删除与流水防篡改
- **决策**: 主数据适度逻辑删除，核心业务流水禁止物理删除与逻辑删除。
- **实现规则**:
  1. 仅在 `elder` 主数据表应用 `deleted` 字段逻辑删除，并在业务层面保证“退住不删除老人主档案”。
  2. 空间（楼栋、房间、床位）、字典、服务项等使用 `status = 'DISABLED'` 状态停用。
  3. `admission`、`service_order`、`health_record`、`care_task`、`billing`、`payment_record`、`risk_event` 等流水记录一律禁止物理删除和逻辑删除，更正采用红冲、退款或日志流转记录。

### 2.6 幂等性控制
- **决策**: 核心定时任务与支付流水由数据库唯一索引进行最终兜底。
- **实现规则**:
  1. 护理任务生成：依托 `care_task` 的唯一索引 `uk_care_task_idem (org_id, idempotency_key)`，幂等键规则：`{plan_item_id}_{task_date}_{scheduled_time}`。
  2. 收款与退款登记：依托 `payment_record` 的唯一索引 `uk_payment_idem (org_id, idempotency_key)`。

## 3. 待确认事项与风险应对
1. **真实数据库连接**: 本机已有 MySQL 客户端与 Docker，需确认后续阶段是连接本地已有的 MySQL 8.0 实例还是通过 Docker Compose 启动容器。
2. **敏感数据加密**: 身份证 `id_no_ciphertext` 在阶段 3 实现时采用 AES/SM4 对称加密，哈希列 `id_no_hash` 采用 SHA-256 用于精确匹配检索。
