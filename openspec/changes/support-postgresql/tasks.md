## 1. 依赖管理

- [x] 1.1 在根 `pom.xml` 的 `<properties>` 中添加 `<postgresql.version>42.7.3</postgresql.version>`
- [x] 1.2 在根 `pom.xml` 的 `<dependencyManagement>` 中添加 PostgreSQL 驱动依赖管理
- [x] 1.3 在 `voglander-repository/pom.xml` 中添加 PostgreSQL 驱动依赖（scope=runtime）
- [x] 1.4 验证依赖：运行 `mvn dependency:tree -pl voglander-repository | grep postgresql`

## 2. SQL 脚本转换

- [x] 2.1 复制 `sql/voglander.sql` 为 `sql/voglander-postgresql.sql`
- [x] 2.2 转换自增主键：`AUTO_INCREMENT` → `BIGSERIAL` 或 `SERIAL`
- [x] 2.3 移除 MySQL 特定语法：`ENGINE = InnoDB`、`CHARACTER SET utf8mb4 COLLATE utf8mb4_bin`
- [x] 2.4 调整索引语法：`USING BTREE` → `USING btree` 或移除
- [x] 2.5 转换反引号：MySQL `` `table` `` → PostgreSQL `"table"` 或不加引号（推荐）
- [x] 2.6 验证 `DEFAULT CURRENT_TIMESTAMP` 语法兼容性（PostgreSQL 支持但需确认）
- [x] 2.7 移除 `ON UPDATE CURRENT_TIMESTAMP`（PostgreSQL 不支持，依赖 MyBatis-Plus 自动填充）
- [x] 2.8 检查所有表的主键、唯一键、外键约束是否正确转换
- [x] 2.9 检查所有索引（包括 `idx_status_keepalive`、`idx_server_ip` 等）是否正确转换
- [x] 2.10 添加脚本头注释说明 PostgreSQL 版本要求（12+）和字符编码（UTF-8）

## 3. 配置文件更新

- [x] 3.1 在 `voglander-repository/src/main/resources/application-repo.yml` 的 `datasource.master` 注释块中添加 PostgreSQL 配置示例
- [x] 3.2 PostgreSQL 配置示例包含：`driver-class-name`、`url`（含时区参数）、`username`、`password`
- [x] 3.3 在 `voglander-test/src/main/resources/application-test.yml` 中添加 PostgreSQL 测试数据源配置示例（注释）
- [x] 3.4 验证配置格式：使用 YAML linter 检查语法正确性

## 4. 文档更新

- [x] 4.1 更新 `CLAUDE.md` 的"数据库"章节，添加 PostgreSQL 部署步骤（与 MySQL 格式一致）
- [x] 4.2 在 `doc/1.0.9/` 目录下创建 `POSTGRESQL-SUPPORT.md` 技术方案文档
- [x] 4.3 技术方案文档包含：方案概述、SQL 方言差异对照表、部署步骤、验证清单、已知限制
- [x] 4.4 在技术方案文档中记录 `ON UPDATE CURRENT_TIMESTAMP` 缺失的应对方案（MyBatis-Plus 自动填充）
- [x] 4.5 在技术方案文档中添加字符串比较大小写敏感性说明（PostgreSQL 默认敏感，需注意 `ILIKE` vs `LIKE`）

## 5. 集成测试实现

- [x] 5.1 在 `voglander-web/src/test/java/io/github/lunasaw/voglander/integration/` 创建 `PostgreSQLIntegrationTest.java`
- [x] 5.2 添加 `@SpringBootTest` 和 `@Transactional` 注解
- [x] 5.3 实现 `@BeforeEach` 方法：运行时探测 PostgreSQL 可用性（使用 `Assumptions.assumeTrue`）
- [x] 5.4 测试用例 1：验证 PostgreSQL 数据源连接成功
- [x] 5.5 测试用例 2：CRUD 操作 - 使用 `DeviceDO` 实体测试 `insert`、`selectById`、`updateById`、`deleteById`
- [x] 5.6 测试用例 3：自增主键验证 - 插入记录后检查返回的自增 ID 是否正确
- [x] 5.7 测试用例 4：`update_time` 自动更新 - 更新记录后验证 `update_time` 是否变化
- [x] 5.8 测试用例 5：事务回滚 - 手动抛出异常后验证数据未持久化
- [x] 5.9 测试用例 6：批量插入 - 使用 `saveBatch` 插入多条记录并验证
- [x] 5.10 添加测试数据清理逻辑（`@AfterEach` 或测试方法内）
- [x] 5.11 在测试类注释中说明：需本地安装 PostgreSQL 并创建测试库 `voglander_test`

## 6. SQL 方言兼容性验证

- [x] 6.1 审查所有 Mapper XML 文件（如有），检查是否有 MySQL 特定语法（如 `LIMIT offset, count`）
- [x] 6.2 验证 MyBatis-Plus 的 `@TableId(type = IdType.AUTO)` 在 PostgreSQL SERIAL 主键下的行为
- [x] 6.3 验证分页查询：使用 `Page<DeviceDO>` 查询，确认 PostgreSQL `LIMIT/OFFSET` 语法正确
- [x] 6.4 验证模糊查询：使用 `LambdaQueryWrapper.like()` 查询，确认不受大小写敏感性影响
- [x] 6.5 验证时间函数：检查是否有使用 `NOW()`、`CURRENT_TIMESTAMP` 的地方，确认 PostgreSQL 兼容
- [x] 6.6 如发现不兼容的 SQL，记录到 `doc/1.0.9/POSTGRESQL-SUPPORT.md` 的"已知限制"章节

## 7. 手动验证测试

- [ ] 7.1 本地安装 PostgreSQL（版本 12+ 推荐 16）
- [ ] 7.2 创建测试数据库：`CREATE DATABASE voglander WITH ENCODING 'UTF8';`
- [ ] 7.3 执行初始化脚本：`psql -U postgres -d voglander -f sql/voglander-postgresql.sql`
- [ ] 7.4 修改 `application-dev.yml` 切换到 PostgreSQL 数据源
- [ ] 7.5 启动应用：`mvn spring-boot:run -pl voglander-web`
- [ ] 7.6 验证设备注册：通过协议验证台或 API 注册一个 GB28181 设备
- [ ] 7.7 验证设备查询：调用 `/device/getPage` 接口查询设备列表
- [ ] 7.8 验证设备更新：调用 `/device/update` 接口更新设备信息，检查 `update_time` 是否自动更新
- [ ] 7.9 验证设备删除：调用 `/device/deleteOne` 接口删除设备
- [ ] 7.10 检查应用日志，确认无 SQL 语法错误或警告

## 8. 构建与测试验证

- [x] 8.1 运行完整构建：`mvn clean install`（确保所有模块编译通过）
- [ ] 8.2 运行全部测试：`mvn test`（确保现有测试不受影响）
- [ ] 8.3 运行 PostgreSQL 集成测试：`mvn test -Dtest=PostgreSQLIntegrationTest`（如 PostgreSQL 可用）
- [ ] 8.4 运行覆盖率检查：`./generate-coverage-report.sh`（确认新增代码覆盖率）
- [x] 8.5 检查 Maven 依赖冲突：`mvn dependency:tree | grep -i conflict`

## 9. 回滚验证

- [ ] 9.1 恢复 `application-dev.yml` 到 SQLite 配置
- [ ] 9.2 重启应用，验证 SQLite 功能正常
- [ ] 9.3 切换到 MySQL 配置，验证 MySQL 功能正常
- [ ] 9.4 确认切换数据源无需重新构建，仅需修改配置并重启

## 10. 最终检查与交付

- [ ] 10.1 检查所有新增文件已提交到 Git
- [ ] 10.2 确认 `doc/1.0.9/POSTGRESQL-SUPPORT.md` 包含完整的部署指南和已知限制
- [ ] 10.3 确认 `CLAUDE.md` 更新已提交
- [ ] 10.4 确认 `sql/voglander-postgresql.sql` 脚本完整且可执行
- [ ] 10.5 编写 Git commit message，说明添加 PostgreSQL 支持的范围和验证情况
- [ ] 10.6 可选：创建 Pull Request，附上测试截图和部署验证结果
