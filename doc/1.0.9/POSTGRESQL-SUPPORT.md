# PostgreSQL 支持技术方案

## 方案概述

Voglander 1.0.9 版本新增 PostgreSQL 数据库支持，作为继 SQLite 和 MySQL 之后的第三种可选数据源。PostgreSQL 凭借其卓越的数据完整性、丰富的数据类型支持和强大的扩展性，成为企业级部署的理想选择。

**核心特性：**
- PostgreSQL 12+ 版本支持
- 完全向后兼容 SQLite/MySQL
- 基于 MyBatis-Plus + Dynamic DataSource 框架
- 通过配置文件即可切换数据源，无需修改代码

## SQL 方言差异对照表

| 特性 | MySQL | PostgreSQL | 转换说明 |
|------|-------|------------|----------|
| 自增主键 | `BIGINT AUTO_INCREMENT` | `BIGSERIAL` | PostgreSQL 使用序列实现自增 |
| 时间类型 | `DATETIME` | `TIMESTAMP` | 两者功能相近，精度略有差异 |
| 默认时间戳 | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 语法兼容 |
| 自动更新时间 | `ON UPDATE CURRENT_TIMESTAMP` | 不支持 | 依赖 MyBatis-Plus `@TableField(fill = FieldFill.INSERT_UPDATE)` |
| 字符集/排序规则 | `CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` | 不需要 | PostgreSQL 数据库级设置 UTF8 |
| 表引擎 | `ENGINE = InnoDB` | 不需要 | PostgreSQL 无表引擎概念 |
| 反引号 | `` `table_name` `` | 不加引号或 `"table_name"` | PostgreSQL 推荐不加引号 |
| 索引类型 | `KEY idx_name (col) USING BTREE` | `CREATE INDEX idx_name ON table(col)` | PostgreSQL 默认 btree |
| 唯一约束 | `UNIQUE KEY uk_name (col)` | `UNIQUE (col)` | 简化语法 |
| 字符串比较 | 默认不区分大小写（取决于 COLLATE） | 默认区分大小写 | 使用 `ILIKE` 实现不区分大小写 |

## 部署步骤

### 1. 安装 PostgreSQL

**推荐版本：** PostgreSQL 12+ (推荐 16)

**macOS (Homebrew):**
```bash
brew install postgresql@16
brew services start postgresql@16
```

**Docker:**
```bash
docker run --name voglander-postgres \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=voglander \
  -p 5432:5432 \
  -d postgres:16
```

### 2. 创建数据库

```sql
-- 以 postgres 用户登录
psql -U postgres

-- 创建数据库（UTF-8 编码）
CREATE DATABASE voglander WITH ENCODING 'UTF8';

-- 创建应用用户（可选）
CREATE USER voglander_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE voglander TO voglander_user;

-- 退出
\q
```

### 3. 执行初始化脚本

```bash
cd /path/to/voglander
psql -U postgres -d voglander -f sql/voglander-postgresql.sql
```

**验证表创建：**
```bash
psql -U postgres -d voglander -c "\dt"
```

### 4. 修改应用配置

编辑 `voglander-repository/src/main/resources/application-repo.yml`：

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          type: com.zaxxer.hikari.HikariDataSource
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://localhost:5432/voglander?serverTimezone=Asia/Shanghai
          username: postgres
          password: your_password
```

### 5. 启动应用

```bash
mvn spring-boot:run -pl voglander-web
```

**检查启动日志：**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

## 验证清单

### 功能验证

- [ ] 应用启动成功，无 SQL 语法错误
- [ ] 设备注册：通过 GB28181 协议注册设备
- [ ] 设备查询：分页查询设备列表
- [ ] 设备更新：更新设备信息，验证 `update_time` 自动更新
- [ ] 设备删除：删除设备记录
- [ ] 通道管理：查询、更新设备通道
- [ ] 事务回滚：异常情况下数据正确回滚

### 性能验证

- [ ] 连接池正常工作（HikariCP 日志无异常）
- [ ] 分页查询响应时间在可接受范围
- [ ] 批量插入性能符合预期

### 集成测试

```bash
# 运行 PostgreSQL 集成测试
mvn test -Dtest=PostgreSQLIntegrationTest
```

## 已知限制

### 1. ON UPDATE CURRENT_TIMESTAMP 缺失

**问题描述：**  
PostgreSQL 不支持 MySQL 的 `ON UPDATE CURRENT_TIMESTAMP` 语法，无法在数据库层面自动更新 `update_time` 字段。

**应对方案：**  
Voglander 所有 DO 实体已配置 MyBatis-Plus 自动填充：
```java
@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

在 `MetaObjectHandler` 中自动填充：
```java
@Override
public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
}
```

**影响：** 应用层更新可正常工作；直接执行 SQL UPDATE 语句不会自动更新 `update_time`。

**验证方法：**  
集成测试 `PostgreSQLIntegrationTest.testUpdateTimeAutoUpdate()` 已覆盖此场景。

### 2. 字符串比较大小写敏感性

**问题描述：**  
PostgreSQL 默认字符串比较区分大小写，而 MySQL 的 `utf8mb4_bin` 排序规则也区分大小写，但混合查询可能导致行为差异。

**应对方案：**  
- 使用 MyBatis-Plus 的 `LambdaQueryWrapper.like()` 进行模糊查询（自动处理）
- 如需不区分大小写查询，使用 PostgreSQL 的 `ILIKE` 操作符：
  ```java
  queryWrapper.apply("name ILIKE {0}", "%" + keyword + "%");
  ```

**影响：** 精确字符串匹配按预期工作；模糊查询需注意大小写。

**验证方法：**  
集成测试 `PostgreSQLIntegrationTest.testLikeQuery()` 已覆盖模糊查询场景。

### 3. 序列管理

**问题描述：**  
PostgreSQL 使用序列（SEQUENCE）管理自增主键，与 MySQL 的 AUTO_INCREMENT 机制不同。在某些并发场景下，序列值可能出现跳跃。

**应对方案：**  
- MyBatis-Plus 已内置 PostgreSQL 方言支持，自动处理序列
- `@TableId(type = IdType.AUTO)` 在 PostgreSQL 中正确工作

**影响：** 自增 ID 可能不连续（正常行为），但唯一性和递增性有保证。

**验证方法：**  
集成测试 `PostgreSQLIntegrationTest.testAutoIncrementPrimaryKey()` 已验证自增主键功能。

### 4. 索引差异

**问题描述：**  
部分 MySQL 特定的索引选项（如 `USING BTREE`）在 PostgreSQL 中被移除或简化。

**应对方案：**  
- PostgreSQL 默认使用 B-tree 索引，性能与 MySQL 相当
- 核心索引（主键、唯一键）已正确转换
- 部分辅助索引可根据实际查询性能按需创建

**影响：** 基本功能不受影响；性能敏感场景可能需要额外索引优化。

## 故障排查

### 连接失败

**错误信息：**
```
Connection refused: connect
```

**排查步骤：**
1. 检查 PostgreSQL 服务状态：`pg_isready -h localhost -p 5432`
2. 检查 `pg_hba.conf` 允许本地连接
3. 检查防火墙规则

### 权限错误

**错误信息：**
```
permission denied for table tb_device
```

**排查步骤：**
1. 授予用户权限：
   ```sql
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO voglander_user;
   GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO voglander_user;
   ```

### 字符编码问题

**错误信息：**
```
character with byte sequence 0xXX in encoding "UTF8" has no equivalent in encoding "LATIN1"
```

**排查步骤：**
1. 确认数据库编码：`\l` 查看数据库列表
2. 重建数据库并指定编码：
   ```sql
   DROP DATABASE voglander;
   CREATE DATABASE voglander WITH ENCODING 'UTF8';
   ```

## 从 MySQL/SQLite 迁移

### 数据迁移工具（未来支持）

当前版本 (1.0.9) 仅提供 PostgreSQL 初始化 SQL 脚本，不包含数据迁移工具。

**手动迁移步骤：**
1. 从 MySQL/SQLite 导出数据为 CSV
2. 使用 PostgreSQL `COPY` 命令导入
3. 验证数据完整性

**推荐工具：**
- `pgLoader`：自动化迁移工具
- `Flyway`：数据库版本管理工具（需集成到项目）

### 配置切换

切换数据源仅需修改配置文件，无需重新构建：

1. 停止应用
2. 修改 `application-repo.yml` 中的数据源配置
3. 重启应用

**注意：** 不同数据源的数据互相独立，切换后需重新初始化数据。

## 相关资源

- PostgreSQL 官方文档：https://www.postgresql.org/docs/
- MyBatis-Plus 文档：https://baomidou.com/
- HikariCP 配置参考：https://github.com/brettwooldridge/HikariCP

## 版本信息

- 引入版本：Voglander 1.0.9
- PostgreSQL 驱动版本：42.7.3
- 支持的 PostgreSQL 版本：12+（推荐 16）
- 测试验证：macOS + PostgreSQL 16
