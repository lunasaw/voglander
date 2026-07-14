# Voglander 测试文档索引

本目录包含 Voglander 项目的完整测试规范和指南文档。

## 📂 文档结构

```
doc/
├── 1.0.9/                          # 旧版测试文档（向后兼容）
│   ├── TEST_EXECUTION_GUIDE.md     # 测试执行命令详解
│   ├── MIGRATION_GUIDE.md          # BaseTest → BaseAsyncTest 迁移
│   ├── SERVICE_SETUP.md            # 外部服务本地搭建
│   └── TROUBLESHOOTING.md          # 常见问题排查
│
└── 1.0.10/                                # 最新测试规范（本目录）
    ├── TESTING_SPECIFICATION.md           # 完整测试规范（主文档）
    ├── TESTING_QUICK_REFERENCE.md         # 快速参考卡片
    ├── EXTERNAL_DEPENDENCY_TESTING.md     # 外部依赖测试过滤机制
    └── README.md                          # 本索引文档
```

## 📖 文档说明

### 🎯 主文档（必读）

#### [TESTING_SPECIFICATION.md](./TESTING_SPECIFICATION.md)
**完整的分层测试规范** - 48KB，约 12,500 字

涵盖内容：
1. **测试分层架构**：SIP协议层、业务层、集成测试层
2. **测试基类选择**：BaseTest、BaseAsyncTest、BaseE2eTest 使用指南
3. **环境隔离配置**：TestRedisConfig、CacheTestConfig、SSE 本地模式
4. **测试数据管理**：UniqueKeyFactory 唯一标识生成
5. **测试执行策略**：Maven 命令、并发执行、CI/CD 集成
6. **外部依赖测试过滤**：Assumptions + Maven Profile 实现自动跳过
7. **最佳实践总结**：必须遵守的规则、推荐实践、反模式

**适用场景**：
- ✅ 新成员入职培训
- ✅ 编写新测试前的规范查阅
- ✅ 测试代码 Review 基准
- ✅ 制定项目测试策略

---

### 🚀 快速参考（推荐）

#### [TESTING_QUICK_REFERENCE.md](./TESTING_QUICK_REFERENCE.md)
**测试规范快速参考卡片** - 8KB，约 2,000 字

涵盖内容：
- 🎯 测试层级决策树（1分钟快速决策）
- 📋 测试基类选择表
- 📝 测试模板速查（复制即用）
- 🚀 常用测试命令
- ✅ 必须遵守的规则（强制）
- ❌ 禁止的反模式
- 🐛 故障排查速查表

**适用场景**：
- ✅ 日常开发快速查阅（打印贴在显示器旁）
- ✅ 不确定用哪个测试基类时
- ✅ 需要测试模板时
- ✅ 遇到测试报错快速定位

---

### 🔌 外部依赖测试过滤（重要）

#### [EXTERNAL_DEPENDENCY_TESTING.md](./EXTERNAL_DEPENDENCY_TESTING.md)
**外部依赖测试过滤机制** - 详解如何让需要 Redis/PostgreSQL/ZLM 的测试在默认 `mvn test` 下自动跳过

涵盖内容：
- 🎯 核心原则：默认测试自动跳过，`-Pintegration-tests` 强制执行
- 🔧 JUnit 5 Assumptions 标准实现（Redis/PostgreSQL/ZLM 示例）
- 📦 Maven Profile 配置（`integration-tests`）
- 🏷️ JUnit 5 Tags 补充方案（`@Tag("requires-redis")` 等）
- 📊 测试报告差异对比（skipped vs failed）
- 🔄 CI/CD 多阶段测试集成示例（GitHub Actions / GitLab CI）
- 🐛 故障排查（Assumptions 未生效、集成模式仍跳过等）

**适用场景**：
- ✅ 编写需要 Redis/PostgreSQL/ZLM 等外部服务的测试
- ✅ 确保 `mvn test` 默认不依赖任何外部服务
- ✅ 配置 CI 分阶段测试（快速反馈 + 完整验证）

---

### 📚 补充文档（参考）

以下文档位于 `doc/1.0.9/`，仍然有效：

#### [TEST_EXECUTION_GUIDE.md](../1.0.9/TEST_EXECUTION_GUIDE.md)
测试执行命令详解，包括：
- Maven 测试命令完整说明
- 测试 Profile 配置
- JUnit 5 Tags 使用
- 并发测试执行参数

#### [MIGRATION_GUIDE.md](../1.0.9/MIGRATION_GUIDE.md)
从 BaseTest 迁移到 BaseAsyncTest 的完整指南：
- 何时需要迁移
- 迁移步骤清单
- 常见迁移问题
- Before/After 对比示例

#### [SERVICE_SETUP.md](../1.0.9/SERVICE_SETUP.md)
Redis、PostgreSQL 本地环境搭建：
- Docker 启动命令
- 本地安装步骤
- 连接配置示例
- 健康检查脚本

#### [TROUBLESHOOTING.md](../1.0.9/TROUBLESHOOTING.md)
常见测试问题排查手册：
- Bean 冲突问题
- Redis 连接问题
- SQLite 锁冲突
- 事务回滚问题
- 端口冲突问题

---

## 🎓 学习路径

### 新手入门（2小时）

1. **阅读快速参考**（15分钟）
   - [TESTING_QUICK_REFERENCE.md](./TESTING_QUICK_REFERENCE.md)
   - 了解测试层级划分和基类选择

2. **理解核心概念**（30分钟）
   - [TESTING_SPECIFICATION.md](./TESTING_SPECIFICATION.md) 第1-2章
   - 测试分层架构和测试金字塔

3. **实践测试编写**（1小时）
   - 参考 [TESTING_QUICK_REFERENCE.md](./TESTING_QUICK_REFERENCE.md) 的模板
   - 编写一个 Manager 集成测试
   - 编写一个 Controller 单元测试

4. **验证环境配置**（15分钟）
   - 运行 `mvn test -Dtest=ConfigBootSmokeTest`
   - 确认 TestRedisConfig 和 CacheTestConfig 生效

### 进阶提升（4小时）

1. **深入分层测试**（1.5小时）
   - [TESTING_SPECIFICATION.md](./TESTING_SPECIFICATION.md) 第3-5章
   - 理解 SIP协议层、业务层、集成测试层的差异
   - 学习不同层级的依赖隔离策略

2. **掌握环境隔离**（1小时）
   - [TESTING_SPECIFICATION.md](./TESTING_SPECIFICATION.md) 第6章
   - 理解 TestRedisConfig、CacheTestConfig 的设计
   - 学习 SSE 事件总线的切换机制

3. **实践异步测试**（1小时）
   - [MIGRATION_GUIDE.md](../1.0.9/MIGRATION_GUIDE.md)
   - 编写一个 BaseAsyncTest 测试
   - 理解事务回滚与异步操作的冲突

4. **CI/CD 集成**（30分钟）
   - [TESTING_SPECIFICATION.md](./TESTING_SPECIFICATION.md) 第8.4节
   - 了解测试阶段划分
   - 学习 GitHub Actions 配置示例

### 专家级（持续）

1. **测试架构设计**
   - 为新模块设计测试策略
   - 评估测试覆盖率并制定改进计划
   - Review 团队成员的测试代码

2. **测试工具优化**
   - 优化 UniqueKeyFactory 生成策略
   - 改进 TestRedisConfig Mock 行为
   - 扩展测试基类功能

3. **测试性能优化**
   - 分析测试执行时间瓶颈
   - 优化并发测试配置
   - 减少不必要的 Spring 容器启动

---

## 🔍 快速查找

### 按问题查找

| 问题 | 查找文档 | 章节 |
|-----|---------|------|
| 不知道用哪个测试基类 | TESTING_QUICK_REFERENCE.md | 测试基类选择表 |
| 测试报错 `NoUniqueBeanDefinitionException` | TESTING_QUICK_REFERENCE.md | 故障排查速查 |
| 需要测试模板 | TESTING_QUICK_REFERENCE.md | 测试模板速查 |
| 理解测试分层架构 | TESTING_SPECIFICATION.md | 第2章 |
| 配置测试环境隔离 | TESTING_SPECIFICATION.md | 第6章 |
| Redis 连接问题 | TROUBLESHOOTING.md | Redis 问题排查 |
| 从 BaseTest 迁移到 BaseAsyncTest | MIGRATION_GUIDE.md | 完整文档 |
| 本地搭建 Redis/PostgreSQL | SERVICE_SETUP.md | 完整文档 |

### 按测试类型查找

| 测试类型 | 规范章节 | 模板位置 |
|---------|---------|---------|
| GB28181 协议层测试 | TESTING_SPECIFICATION.md 第3章 | TESTING_QUICK_REFERENCE.md 模板1 |
| Controller 单元测试 | TESTING_SPECIFICATION.md 第4.2节 | TESTING_QUICK_REFERENCE.md 模板2 |
| Service 单元测试 | TESTING_SPECIFICATION.md 第4.3节 | - |
| Manager 集成测试（同步）| TESTING_SPECIFICATION.md 第4.4.1节 | TESTING_QUICK_REFERENCE.md 模板3 |
| Manager 集成测试（异步）| TESTING_SPECIFICATION.md 第4.4.2节 | TESTING_QUICK_REFERENCE.md 模板4 |
| E2E 端到端测试 | TESTING_SPECIFICATION.md 第5.1节 | - |

---

## 📋 测试规范检查清单

在提交测试代码前，请确认：

### 基础检查
- [ ] 选择了正确的测试基类（参考决策树）
- [ ] 使用了 `UniqueKeyFactory` 生成唯一标识
- [ ] 协议层/Controller 测试未使用 `@SpringBootTest`
- [ ] Manager 测试使用了集成测试基类

### 环境隔离
- [ ] 集成测试导入了 `TestRedisConfig` 和 `CacheTestConfig`
- [ ] 未依赖外部 Redis/PostgreSQL（或使用 Assumptions）
- [ ] 使用了 `RANDOM_PORT`

### 数据清理
- [ ] BaseTest：确认 `@Transactional` 自动回滚生效
- [ ] BaseAsyncTest：实现了 `@AfterEach` 手动清理
- [ ] E2E 测试：清理了所有测试数据

### Mock 配置
- [ ] 外部 Wrapper 使用 `@MockitoBean`
- [ ] Assembler 使用 `@MockitoBean` + `thenAnswer` + FastJSON
- [ ] IService 使用 `@Autowired` 真实 Bean

### 测试质量
- [ ] 测试命名清晰（testXxx_Scenario_ExpectedBehavior）
- [ ] 使用了 Given-When-Then 结构
- [ ] 验证了关键断言（assertNotNull、assertEquals 等）
- [ ] 验证了 Mock 调用次数（verify(xxx, times(1))）

---

## 🛠️ 常用工具

### 测试执行

```bash
# 快速验证（跳过外部服务）
mvn test

# 单个测试类
mvn test -Dtest=DeviceManagerTest

# 覆盖率报告
./generate-coverage-report.sh

# 并行加速
mvn test -DforkCount=2
```

### 环境检查

```bash
# 验证测试配置
mvn test -Dtest=ConfigBootSmokeTest

# 检查 Redis 可用性
redis-cli ping

# 检查 PostgreSQL 可用性
psql -h localhost -U postgres -c "SELECT 1"
```

### 代码生成

```bash
# 使用 IDEA Live Template 快速生成测试模板
# 设置：Settings → Editor → Live Templates
# 添加模板：参考 TESTING_QUICK_REFERENCE.md 的模板章节
```

---

## 📞 获取帮助

### 文档问题
- 📧 邮件：voglander-dev@example.com
- 💬 钉钉群：Voglander 研发群

### 技术问题
1. 查阅 [TESTING_QUICK_REFERENCE.md](./TESTING_QUICK_REFERENCE.md) 故障排查
2. 查阅 [TROUBLESHOOTING.md](../1.0.9/TROUBLESHOOTING.md)
3. 在项目 Issue 中搜索相似问题
4. 提交新 Issue（模板：测试规范问题）

---

## 🔄 版本历史

| 版本 | 日期 | 变更内容 |
|-----|------|---------|
| 1.0.10 | 2026-07-14 | 发布完整分层测试规范 |
| 1.0.9 | 2025-06-22 | 发布测试执行指南、迁移指南 |

---

## 📄 许可证

本文档遵循 Voglander 项目许可证。

---

**维护者**：Voglander 开发团队  
**最后更新**：2026-07-14  
**文档版本**：1.0.10
