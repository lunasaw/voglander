## Context

Voglander 目前支持两种数据库：SQLite（默认开发环境）和 MySQL 8.2.0（生产环境）。系统使用 MyBatis-Plus 3.5.5 + Dynamic DataSource 4.3.1 实现多数据源支持。所有实体层（DO）使用 `LocalDateTime` 时间类型，通过 HikariCP 管理连接池。

当前架构约束：
- 严格分层架构：repository 层负责数据持久化，不允许业务逻辑侵入
- 统一时间处理：DO/DTO 使用 `LocalDateTime`，数据库自动管理 `create_time`/`update_time`
- 无自定义 SQL：优先使用 MyBatis-Plus 基础方法，仅复杂查询才编写 Mapper XML
- FastJSON2 序列化：所有 JSON 字段统一使用 FastJSON2 处理

企业客户对 PostgreSQL 的需求来自：
1. 已有 PostgreSQL 技术栈，希望统一运维
2. 需要更强的 ACID 保证和并发控制
3. 对地理信息扩展（PostGIS）有长期规划
4. 开源社区活跃度和生态系统成熟度

技术限制：
- 现有 50+ 张表，必须保证 SQL 脚本转换的完整性
- 已有 Manager/Service 层不应因数据库切换而修改业务逻辑
- 测试策略：集成测试在 `voglander-web/src/test` 统一管理

## Goals / Non-Goals

**Goals:**
1. 添加 PostgreSQL 作为第三种可选数据源，与 SQLite/MySQL 平级
2. 提供完整的 PostgreSQL 初始化 SQL 脚本，从 MySQL 脚本转换而来
3. 适配 PostgreSQL SQL 方言差异，确保 MyBatis-Plus 实体层无缝工作
4. 添加 PostgreSQL 集成测试，验证连接、CRUD、事务等核心功能
5. 更新文档，提供清晰的 PostgreSQL 部署指南

**Non-Goals:**
1. 数据迁移工具（MySQL/SQLite → PostgreSQL）不在本次范围
2. 不修改现有业务逻辑或 Manager/Service 层代码
3. 不涉及 PostgreSQL 特有功能（如 PostGIS、全文搜索），仅保证基础功能兼容
4. 不要求同时支持多个 PostgreSQL 实例（Dynamic DataSource 支持但不强制）

## Decisions

### Decision 1: 使用 PostgreSQL JDBC 42.7.x
**选择**: 添加 `org.postgresql:postgresql:42.7.3` 作为 Maven 依赖

**理由**:
- 42.7.x 是当前稳定版本，支持 PostgreSQL 12-16
- 与 Java 17 完全兼容
- 支持 JDBC 4.3 规范，与 HikariCP 无缝集成

**替代方案考虑**:
- 42.6.x（上一代稳定版）：功能满足但缺少最新安全补丁
- 43.x（未来版本）：尚未发布

### Decision 2: SQL 脚本转换策略
**选择**: 从 `voglander.sql`（MySQL）手动转换为 `voglander-postgresql.sql`

**转换重点**:
1. **自增主键**: `AUTO_INCREMENT` → `SERIAL` 或 `BIGSERIAL`
2. **时间默认值**: `DEFAULT CURRENT_TIMESTAMP` → PostgreSQL 兼容（相同语法可用）
3. **字符集**: 移除 `CHARACTER SET utf8mb4 COLLATE utf8mb4_bin`，PostgreSQL 使用数据库级 UTF-8
4. **引擎**: 移除 `ENGINE = InnoDB`（PostgreSQL 不需要）
5. **索引语法**: `USING BTREE` → 移除或改为 `USING btree`（小写）
6. **反引号**: MySQL 的 `` `table` `` → PostgreSQL 的 `"table"` 或不加引号
7. **ON UPDATE**: PostgreSQL 不支持 `ON UPDATE CURRENT_TIMESTAMP`，需用触发器或应用层处理（但 Voglander 已在 MyBatis-Plus 自动填充处理）

**理由**:
- 自动化工具（如 pgLoader）可能误转换业务逻辑，手动控制更可靠
- 50+ 张表规模适合手动审查，确保索引、约束正确性

**替代方案考虑**:
- 使用 pgLoader 自动迁移：适合大规模迁移但需额外验证
- 使用 Liquibase/Flyway：引入额外依赖，当前 Voglander 未使用迁移工具

### Decision 3: MyBatis-Plus 方言适配
**选择**: 依赖 MyBatis-Plus 内置的 PostgreSQL 方言支持，无需自定义

**理由**:
- MyBatis-Plus 3.5.5 已内置 `PostgreSQLInjector`，自动处理分页、主键生成
- Dynamic DataSource 4.3.1 通过 JDBC URL 自动识别数据库类型
- 现有代码使用 `LambdaQueryWrapper`，不依赖数据库特定语法

**需验证点**:
- `@TableId(type = IdType.AUTO)` 在 PostgreSQL SERIAL 主键下的行为
- `BaseMapper.insert()` 返回自增 ID 是否正确

**替代方案考虑**:
- 自定义 SQL 方言处理器：过度设计，增加维护成本

### Decision 4: 配置文件结构
**选择**: 在 `application-repo.yml` 中添加注释示例，不创建独立的 `application-postgresql.yml`

**理由**:
- 保持与 SQLite/MySQL 配置的一致性
- 用户通过修改 `driver-class-name` 和 `url` 即可切换，降低学习成本
- 注释示例足够清晰：
  ```yaml
  # PostgreSQL 示例（取消注释并修改连接信息）
  # driver-class-name: org.postgresql.Driver
  # url: jdbc:postgresql://localhost:5432/voglander?serverTimezone=Asia/Shanghai
  # username: postgres
  # password: your_password
  ```

**替代方案考虑**:
- 创建 `application-postgresql.yml`：增加文件数量，profile 切换不够灵活

### Decision 5: 集成测试策略
**选择**: 添加 `PostgreSQLIntegrationTest`，仿照 `MediaNodeCacheIntegrationTest` 的运行时探测模式

**测试范围**:
1. 数据源连接与配置验证
2. CRUD 操作（使用现有 DO 实体，如 `DeviceDO`）
3. 事务提交与回滚
4. 自增主键生成验证

**运行时探测逻辑**:
```java
@BeforeEach
void checkPostgreSQLAvailable() {
    try (Connection conn = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/voglander_test", "postgres", "test")) {
        // PostgreSQL 可用，继续测试
    } catch (SQLException e) {
        Assumptions.assumeTrue(false, "PostgreSQL not available, skipping tests");
    }
}
```

**理由**:
- CI/CD 环境可能没有 PostgreSQL，自动跳过避免失败
- 开发者可选择性运行，不强制本地安装

**替代方案考虑**:
- 使用 Testcontainers：引入 Docker 依赖，增加测试启动时间
- 仅手动测试：缺少自动化保障

### Decision 6: 文档更新范围
**选择**: 更新 `CLAUDE.md` 数据库章节，添加 PostgreSQL 部署步骤

**新增内容**:
```markdown
### 数据库
- 默认 **SQLite**（`app.db` 自动创建），测试用 `test-app.db` + `schema-sqlite.sql`
- 可选 **MySQL**：建库 `voglander` → 执行 `sql/voglander.sql` → 改 `application-dev.yml`
- 可选 **PostgreSQL**：建库 `voglander` → 执行 `sql/voglander-postgresql.sql` → 改 `application-dev.yml`
```

**理由**:
- 用户优先查看 CLAUDE.md，集中文档降低查找成本
- 与 MySQL 配置说明保持一致的格式

## Risks / Trade-offs

### Risk 1: SQL 方言差异导致运行时错误
**风险**: 手动转换的 SQL 脚本可能遗漏细节（如特殊索引、约束）

**缓解措施**:
1. 对比 `voglander.sql` 和 `voglander-postgresql.sql`，逐表审查
2. 运行 PostgreSQL 集成测试，覆盖所有 DO 实体的 CRUD 操作
3. 在 `doc/1.0.9/` 中记录已知差异和验证清单

### Risk 2: ON UPDATE CURRENT_TIMESTAMP 缺失
**风险**: PostgreSQL 不支持 MySQL 的 `ON UPDATE CURRENT_TIMESTAMP`，可能导致 `update_time` 未更新

**缓解措施**:
1. 确认 MyBatis-Plus 的 `@TableField(fill = FieldFill.INSERT_UPDATE)` 已在所有 DO 实体配置
2. 测试 `update()` 操作后 `update_time` 是否自动更新
3. 如需数据库层保障，编写 PostgreSQL 触发器（但优先应用层处理）

### Risk 3: 自增主键行为差异
**风险**: PostgreSQL SERIAL 与 MySQL AUTO_INCREMENT 在并发插入、序列重置时行为可能不同

**缓解措施**:
1. 测试 `BaseMapper.insert()` 返回值是否正确获取自增 ID
2. 验证批量插入场景（`saveBatch`）
3. 文档说明 PostgreSQL 序列管理注意事项

### Risk 4: 字符串比较大小写敏感性
**风险**: PostgreSQL 默认大小写敏感，MySQL utf8mb4_bin 排序规则也区分大小写，但混合查询可能出错

**缓解措施**:
1. 审查所有 Mapper XML 中的 `LIKE`、`=` 查询，确认是否需要 `ILIKE`（PostgreSQL 不区分大小写）
2. 统一使用 `LambdaQueryWrapper.like()` 避免手写 SQL
3. 集成测试覆盖模糊查询场景

### Trade-off 1: 不提供数据迁移工具
**取舍**: 用户从 MySQL/SQLite 切换到 PostgreSQL 需自行迁移数据

**理由**:
- 数据迁移工具开发成本高，且使用频率低
- 企业用户通常有专业 DBA 团队处理迁移
- 可在未来版本补充（如提供 Flyway 脚本）

### Trade-off 2: 不使用 PostgreSQL 特有功能
**取舍**: 本次仅保证基础 CRUD 兼容，不利用 JSONB、数组、全文搜索等特性

**理由**:
- 保持数据库抽象层干净，降低维护复杂度
- 未来可根据需求逐步引入（如用 JSONB 优化 `extend` 字段查询）

## Migration Plan

### 部署步骤（用户视角）
1. **安装 PostgreSQL**（版本 12+ 推荐）
2. **创建数据库**:
   ```sql
   CREATE DATABASE voglander WITH ENCODING 'UTF8';
   ```
3. **执行初始化脚本**:
   ```bash
   psql -U postgres -d voglander -f sql/voglander-postgresql.sql
   ```
4. **修改配置** `voglander-web/src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       dynamic:
         datasource:
           master:
             driver-class-name: org.postgresql.Driver
             url: jdbc:postgresql://localhost:5432/voglander?serverTimezone=Asia/Shanghai
             username: postgres
             password: your_password
   ```
5. **启动应用**:
   ```bash
   mvn spring-boot:run -pl voglander-web
   ```

### 回滚策略
如果 PostgreSQL 部署失败，用户可立即切回 SQLite/MySQL：
1. 恢复 `application-dev.yml` 中的原数据源配置
2. 重启应用（无需重新构建）

### 开发者验证清单
1. Maven 构建通过（`mvn clean install`）
2. PostgreSQL 集成测试通过（`mvn test -Dtest=PostgreSQLIntegrationTest`）
3. 手动验证：启动应用 → 设备注册 → 查询设备列表 → 更新设备 → 删除设备
4. 检查 `update_time` 自动更新
5. 检查日志无 SQL 语法错误

## Open Questions

1. **是否需要支持 PostgreSQL 特定的连接池配置**？
   - 例如 `prepareThreshold`（预编译语句缓存），当前使用 HikariCP 默认配置

2. **是否需要在 CI/CD 中添加 PostgreSQL 测试**？
   - 当前 CI 仅测试 SQLite，需评估 PostgreSQL 容器化测试的成本

3. **未来是否考虑 PostGIS 扩展**？
   - 如果需要地理信息查询，需提前在 SQL 脚本中预留扩展安装步骤

4. **是否需要性能基准测试**？
   - 对比 SQLite/MySQL/PostgreSQL 在相同负载下的响应时间和资源消耗
