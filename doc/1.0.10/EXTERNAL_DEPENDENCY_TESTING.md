# 外部依赖测试过滤机制

## 概述

Voglander 测试体系遵循"**默认测试内部自洽**"原则，即 `mvn test` 应在无外部服务的情况下完整执行。对于需要外部依赖（Redis、PostgreSQL、ZLM 等）的测试，提供**自动过滤**和**显式执行**两种模式。

---

## 1. 过滤机制设计

### 1.1 核心原则

```
┌─────────────────────────────────────────────────────────┐
│ mvn test（默认）                                         │
│ ├─ 单元测试：全部执行                                    │
│ ├─ 集成测试：使用 Mock 依赖（TestRedisConfig）         │
│ └─ 外部依赖测试：自动跳过（Assumptions）                │
│                                                          │
│ mvn test -Pintegration-tests（显式）                     │
│ ├─ 单元测试：全部执行                                    │
│ ├─ 集成测试：使用 Mock 依赖                             │
│ └─ 外部依赖测试：必须执行，失败则报错                    │
└─────────────────────────────────────────────────────────┘
```

### 1.2 实现方式对比

| 方式 | 优点 | 缺点 | 推荐度 |
|-----|------|------|--------|
| **JUnit 5 Assumptions** | 运行时探测，优雅跳过，显示原因 | 需每个测试类显式检测 | ⭐⭐⭐⭐⭐ 强烈推荐 |
| JUnit 5 @EnabledIf | 声明式，支持条件表达式 | 条件在类加载时评估，无法动态探测 | ⭐⭐⭐ 适用简单场景 |
| Maven Profile + @Tag | 编译时过滤，清晰分离 | 无法区分"跳过"和"未执行" | ⭐⭐ 不推荐 |

**推荐使用 JUnit 5 Assumptions**，原因：
1. 运行时真实探测服务可用性
2. 跳过时显示明确原因（日志可见）
3. 与 `-Pintegration-tests` Profile 完美配合
4. 测试报告中清晰区分 `skipped` vs `failed`

---

## 2. Assumptions 实现标准

### 2.1 Redis 依赖测试

#### 场景：验证 RedisBackedSseEventBus 行为

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "sse.type=redis")  // 覆盖默认 local
public class RedisBackedSseEventBusIntegrationTest {
    
    @Autowired(required = false)
    private RedisBackedSseEventBus sseEventBus;
    
    /**
     * 在每个测试方法执行前检测 Redis 可用性
     * 不可用时跳过整个测试类
     */
    @BeforeEach
    void checkRedisAvailable() {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.ping();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "Redis 不可用，跳过测试: " + e.getMessage());
        }
    }
    
    @Test
    public void testRedisPubSub() {
        // 测试逻辑：仅在 Redis 可用时执行
        assertNotNull(sseEventBus);
        // ...
    }
}
```

#### 关键点：
1. **不导入 TestRedisConfig**（需要真实 Redis 连接）
2. **@TestPropertySource** 覆盖 `sse.type=redis`
3. **@BeforeEach** 在每个测试前检测（防止中途服务停止）
4. **assumeTrue(false, message)** 跳过时显示原因

### 2.2 PostgreSQL 依赖测试

#### 场景：验证 PostgreSQL 特定功能（JSONB、数组类型）

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/voglander_test",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
public class PostgreSQLIntegrationTest extends BaseTest {
    
    @Autowired
    private DataSource dataSource;
    
    @BeforeEach
    void checkPostgreSQLAvailable() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String dbName = meta.getDatabaseProductName();
            Assumptions.assumeTrue("PostgreSQL".equalsIgnoreCase(dbName),
                "当前数据库不是 PostgreSQL，跳过测试");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "PostgreSQL 不可用，跳过测试: " + e.getMessage());
        }
    }
    
    @Test
    public void testPostgreSQLJsonbType() {
        // 测试 PostgreSQL JSONB 字段
    }
}
```

### 2.3 ZLMediaKit 依赖测试

#### 场景：验证 ZLM 媒体流处理

```java
@SpringBootTest
@ActiveProfiles("test")
public class ZlmMediaIntegrationTest extends BaseTest {
    
    @Autowired
    private ZlmIntegrationConfig zlmConfig;
    
    @BeforeEach
    void checkZlmAvailable() {
        try {
            ZlmServerConfig server = zlmConfig.getDefaultServer();
            String url = server.getHttpUrl() + "/index/api/getServerConfig";
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            Assumptions.assumeTrue(response.getStatusCode().is2xxSuccessful(),
                "ZLM 服务不可用");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, 
                "ZLM 不可用，跳过测试: " + e.getMessage());
        }
    }
    
    @Test
    public void testZlmStreamProxy() {
        // 测试 ZLM 流代理功能
    }
}
```

---

## 3. Maven Profile 配置

### 3.1 pom.xml 配置

```xml
<profiles>
    <!-- 默认 profile：快速测试，外部依赖测试自动跳过 -->
    <profile>
        <id>default</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <!-- 不设置系统属性，依赖 Assumptions 运行时探测 -->
        </properties>
    </profile>
    
    <!-- 集成测试 profile：强制执行外部依赖测试 -->
    <profile>
        <id>integration-tests</id>
        <properties>
            <!-- 标记集成测试模式 -->
            <integration.tests.enabled>true</integration.tests.enabled>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <!-- 传递系统属性到测试 JVM -->
                            <integration.tests.enabled>true</integration.tests.enabled>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 3.2 增强的 Assumptions 检测（可选）

如果需要在 `-Pintegration-tests` 模式下强制外部服务可用：

```java
@BeforeEach
void checkRedisAvailable() {
    boolean integrationMode = Boolean.getBoolean("integration.tests.enabled");
    
    try (Jedis jedis = new Jedis("localhost", 6379)) {
        jedis.ping();
    } catch (Exception e) {
        if (integrationMode) {
            // 集成测试模式：外部服务不可用应失败
            fail("集成测试模式要求 Redis 可用，但连接失败: " + e.getMessage());
        } else {
            // 默认模式：优雅跳过
            Assumptions.assumeTrue(false, 
                "Redis 不可用，跳过测试: " + e.getMessage());
        }
    }
}
```

---

## 4. JUnit 5 Tags 过滤（补充方案）

### 4.1 为外部依赖测试添加标签

```java
@Tag("requires-redis")
@SpringBootTest
public class RedisBackedSseEventBusIntegrationTest {
    // ...
}

@Tag("requires-postgresql")
@SpringBootTest
public class PostgreSQLIntegrationTest extends BaseTest {
    // ...
}

@Tag("requires-zlm")
@SpringBootTest
public class ZlmMediaIntegrationTest extends BaseTest {
    // ...
}
```

### 4.2 Maven 配置排除标签

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <groups>!requires-redis &amp;&amp; !requires-postgresql &amp;&amp; !requires-zlm</groups>
    </configuration>
</plugin>
```

### 4.3 命令行使用

```bash
# 排除所有外部依赖测试
mvn test -DexcludedGroups="requires-redis,requires-postgresql,requires-zlm"

# 仅执行 Redis 依赖测试
mvn test -Dgroups="requires-redis"

# 执行所有集成测试（包括外部依赖）
mvn test -Dgroups="integration | requires-redis | requires-postgresql"
```

### 4.4 Tags vs Assumptions 对比

| 维度 | Tags | Assumptions |
|-----|------|-------------|
| **过滤时机** | 编译时/启动前 | 运行时 |
| **服务探测** | 无法探测 | 真实探测 |
| **跳过原因** | 不显示 | 显示详细原因 |
| **报告区分** | 未执行（不计入统计）| 跳过（计入统计）|
| **灵活性** | 静态分组 | 动态决策 |
| **推荐场景** | CI 分阶段执行 | 本地开发 |

**推荐组合使用**：
- **Tags**：用于 CI/CD 分阶段执行（阶段1排除外部依赖，阶段2仅执行外部依赖）
- **Assumptions**：用于运行时服务可用性探测（无论 Tags 如何过滤）

---

## 5. 测试报告差异

### 5.1 默认模式（mvn test）

```
[INFO] Tests run: 150, Failures: 0, Errors: 0, Skipped: 5
[INFO] 
[INFO] Skipped:
[INFO]   RedisBackedSseEventBusIntegrationTest.testRedisPubSub
[INFO]     Reason: Redis 不可用，跳过测试: Connection refused
[INFO]   PostgreSQLIntegrationTest.testPostgreSQLJsonbType
[INFO]     Reason: PostgreSQL 不可用，跳过测试: Unknown database
```

### 5.2 集成测试模式（mvn test -Pintegration-tests）

**服务可用时**：
```
[INFO] Tests run: 155, Failures: 0, Errors: 0, Skipped: 0
[INFO] All tests passed
```

**服务不可用时**：
```
[ERROR] Tests run: 155, Failures: 5, Errors: 0, Skipped: 0
[ERROR] 
[ERROR] Failures:
[ERROR]   RedisBackedSseEventBusIntegrationTest.testRedisPubSub
[ERROR]     Cause: 集成测试模式要求 Redis 可用，但连接失败: Connection refused
```

---

## 6. CI/CD 集成示例

### 6.1 GitHub Actions 多阶段测试

```yaml
name: Test

on: [push, pull_request]

jobs:
  # 阶段1：快速反馈（2-3分钟）
  unit-and-default-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run Default Tests (无外部依赖)
        run: mvn test
        # Assumptions 会自动跳过外部依赖测试
  
  # 阶段2：完整验证（5-10分钟）
  integration-tests-with-services:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: voglander_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Wait for Services
        run: |
          timeout 30 bash -c 'until redis-cli -h localhost ping; do sleep 1; done'
          timeout 30 bash -c 'until pg_isready -h localhost; do sleep 1; done'
      
      - name: Run Integration Tests (含外部依赖)
        run: mvn test -Pintegration-tests
        # 外部服务不可用时测试失败，阻止合并
```

### 6.2 GitLab CI 配置

```yaml
stages:
  - test-fast
  - test-full

# 快速测试：无外部依赖
test:default:
  stage: test-fast
  script:
    - mvn test
  tags:
    - docker

# 完整测试：含外部依赖
test:integration:
  stage: test-full
  services:
    - redis:7
    - postgres:15
  variables:
    POSTGRES_DB: voglander_test
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
  script:
    - mvn test -Pintegration-tests
  tags:
    - docker
  only:
    - master
    - merge_requests
```

---

## 7. 本地开发最佳实践

### 7.1 推荐工作流

```bash
# 步骤1：快速验证（每次提交前）
mvn test
# → 2-3 分钟，跳过外部依赖测试

# 步骤2：完整验证（重要功能或发布前）
# 启动外部服务
docker-compose up -d redis postgres zlm

# 执行集成测试
mvn test -Pintegration-tests
# → 5-10 分钟，验证所有外部依赖

# 步骤3：清理
docker-compose down
```

### 7.2 IDEA 配置

**配置1：默认运行配置（无外部依赖）**
```
Name: Test (Default)
Working directory: $MODULE_DIR$
Command line: test
```

**配置2：集成测试运行配置（含外部依赖）**
```
Name: Test (Integration)
Working directory: $MODULE_DIR$
Command line: test -Pintegration-tests
```

**快捷键切换**：
- `Ctrl+Shift+F10`：运行当前测试（使用默认配置）
- `Alt+Shift+F10` → 选择 "Test (Integration)"：运行集成测试

---

## 8. 故障排查

### 8.1 Assumptions 未生效

**现象**：外部依赖测试未被跳过，而是失败

**原因**：
1. 未在 `@BeforeEach` 中调用 Assumptions
2. Assumptions 判断逻辑错误

**解决方案**：
```java
@BeforeEach
void checkRedisAvailable() {
    try (Jedis jedis = new Jedis("localhost", 6379)) {
        jedis.ping();
        // 成功：继续执行测试
    } catch (Exception e) {
        // 失败：跳过测试
        Assumptions.assumeTrue(false, "Redis 不可用: " + e.getMessage());
    }
}
```

### 8.2 集成测试模式下仍被跳过

**现象**：`mvn test -Pintegration-tests` 仍跳过外部依赖测试

**原因**：
1. Profile 配置未生效
2. 未实现"集成模式下强制失败"逻辑

**解决方案**：
```java
@BeforeEach
void checkRedisAvailable() {
    boolean integrationMode = Boolean.getBoolean("integration.tests.enabled");
    
    try (Jedis jedis = new Jedis("localhost", 6379)) {
        jedis.ping();
    } catch (Exception e) {
        if (integrationMode) {
            fail("集成测试模式要求 Redis 可用: " + e.getMessage());
        } else {
            Assumptions.assumeTrue(false, "Redis 不可用: " + e.getMessage());
        }
    }
}
```

### 8.3 测试报告中看不到跳过原因

**现象**：测试被跳过但不知道原因

**解决方案**：
1. 启用详细日志
```bash
mvn test -X
```

2. 确保 Assumptions 包含消息
```java
Assumptions.assumeTrue(false, "Redis 不可用: " + e.getMessage());
//                             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 必须提供消息
```

---

## 9. 检查清单

### 编写外部依赖测试时

- [ ] 添加 `@Tag("requires-xxx")` 标记测试类
- [ ] 在 `@BeforeEach` 实现服务可用性检测
- [ ] 使用 `Assumptions.assumeTrue(false, message)` 跳过
- [ ] 不导入 `TestRedisConfig`（需要真实连接时）
- [ ] 使用 `@TestPropertySource` 覆盖测试配置

### 提交前验证

- [ ] `mvn test` 能正常执行（外部依赖测试被跳过）
- [ ] 启动外部服务后 `mvn test -Pintegration-tests` 能通过
- [ ] 测试报告中显示跳过原因

### CI 配置验证

- [ ] 阶段1（快速反馈）不启动外部服务
- [ ] 阶段2（完整验证）启动所有外部服务
- [ ] 外部服务健康检查配置正确

---

## 10. 总结

### 推荐方案

**JUnit 5 Assumptions + Maven Profile**

```java
@Tag("requires-redis")  // 可选：用于 CI 分阶段
@SpringBootTest
public class RedisIntegrationTest {
    
    @BeforeEach
    void checkRedisAvailable() {
        boolean integrationMode = Boolean.getBoolean("integration.tests.enabled");
        
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.ping();
        } catch (Exception e) {
            if (integrationMode) {
                fail("集成测试模式要求 Redis 可用: " + e.getMessage());
            } else {
                Assumptions.assumeTrue(false, "Redis 不可用，跳过测试");
            }
        }
    }
}
```

### 核心优势

1. ✅ **默认快速**：`mvn test` 自动跳过外部依赖，2-3分钟反馈
2. ✅ **按需验证**：`mvn test -Pintegration-tests` 完整验证
3. ✅ **运行时探测**：真实检测服务可用性，不依赖静态配置
4. ✅ **清晰反馈**：跳过时显示原因，失败时明确错误
5. ✅ **CI友好**：支持多阶段测试，灵活控制成本

---

**文档版本**：1.0.10  
**最后更新**：2026-07-14  
**维护者**：Voglander 开发团队
