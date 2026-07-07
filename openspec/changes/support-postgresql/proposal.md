## Why

Voglander 目前支持 SQLite（默认）和 MySQL 8.2.0，但在企业级部署场景中，PostgreSQL 因其卓越的数据完整性、丰富的数据类型支持、强大的扩展性和开源社区活跃度，成为许多客户的首选数据库。添加 PostgreSQL 支持将：
1. 满足企业客户对数据库选型的多样化需求
2. 提供更强的并发控制和 ACID 事务保证
3. 支持更复杂的地理信息查询（PostGIS 扩展）
4. 增强平台的市场竞争力

## What Changes

- 添加 PostgreSQL JDBC 驱动依赖（postgresql:42.7.x）
- 创建 PostgreSQL 初始化 SQL 脚本（从 MySQL 脚本转换）
- 扩展 Dynamic DataSource 配置支持 PostgreSQL 数据源
- 更新配置文件模板，提供 PostgreSQL 配置示例
- 适配 SQL 方言差异（自增主键、时间函数、JSON 类型等）
- 更新文档说明 PostgreSQL 部署步骤
- 添加 PostgreSQL 集成测试

**不涉及破坏性变更**：现有 SQLite/MySQL 功能保持不变，PostgreSQL 作为第三种可选数据源。

## Capabilities

### New Capabilities
- `postgresql-datasource`: 支持 PostgreSQL 作为 Voglander 的持久化数据源，包括连接配置、SQL 方言适配、初始化脚本

### Modified Capabilities
<!-- 无需求级别变更，仅新增数据源类型 -->

## Impact

**受影响模块**:
- `voglander-repository`: 添加 PostgreSQL 驱动依赖、SQL 脚本、可能的 Mapper SQL 方言适配
- `voglander-test`: 添加 PostgreSQL 集成测试配置和测试用例
- `voglander-web`: 更新 application-repo.yml 配置示例

**受影响文件**:
- `pom.xml`: 添加 `postgresql` 依赖版本属性和依赖管理
- `sql/`: 新增 `voglander-postgresql.sql` 初始化脚本
- `voglander-repository/src/main/resources/application-repo.yml`: 添加 PostgreSQL 配置注释示例
- `CLAUDE.md`: 更新数据库配置说明
- 可能涉及的 Mapper XML（如有 MySQL 特定语法需适配）

**技术依赖**:
- PostgreSQL JDBC Driver 42.7.x（兼容 Java 17）
- Dynamic DataSource 4.3.1（已支持多数据源，无需升级）
- HikariCP（已支持 PostgreSQL）

**兼容性**:
- 向后兼容：现有 SQLite/MySQL 配置和功能不受影响
- 数据迁移：需提供 MySQL/SQLite → PostgreSQL 迁移指南（可选，非本次范围）
