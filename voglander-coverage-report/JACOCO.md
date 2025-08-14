# JaCoCo 测试覆盖率聚合报告

## 概述

voglander 项目已成功配置 JaCoCo 代码覆盖率统计和聚合报告功能。项目包含以下模块的覆盖率统计：

- **voglander-client**: 客户端代码
- **voglander-common**: 公共工具类
- **voglander-manager**: 业务逻辑管理层
- **voglander-service**: 服务层
- **voglander-repository**: 数据访问层
- **voglander-integration**: 外部系统集成层
- **voglander-web**: Web控制器层
- **voglander-coverage-report**: 聚合报告模块

## 配置说明

### 1. 根模块配置

根 `pom.xml` 中已配置：

```xml
<!-- 在根模块激活JaCoCo插件以确保所有子模块生成覆盖率数据 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

### 2. 插件管理配置

根 `pom.xml` 的 `pluginManagement` 部分配置了 JaCoCo 插件的默认行为：

- 自动在测试前准备 JaCoCo 代理
- 在测试后生成报告
- 排除不需要测试覆盖的类（Application、Config、DTO、VO等）

### 3. 各子模块配置

所有业务模块都已激活 JaCoCo 插件：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

### 4. 聚合报告模块

`voglander-coverage-report` 模块专门用于生成聚合报告：

- 依赖所有需要统计的模块
- 使用 `report-aggregate` goal 生成多模块聚合报告

## 使用方法

### 方法一：使用提供的脚本（推荐）

```bash
# 运行生成脚本
./generate-coverage-report.sh
```

该脚本将：

1. 清理项目
2. 运行有测试的模块
3. 生成聚合报告
4. 自动打开报告（macOS）

### 方法二：手动执行 Maven 命令

#### 步骤1：运行所有模块测试

```bash
# 运行所有测试
mvn clean test

# 或者运行特定模块的测试
mvn test -pl voglander-integration
mvn test -pl voglander-web
```

#### 步骤2：生成聚合报告

```bash
# 生成聚合报告
mvn verify -pl voglander-coverage-report
```

### 方法三：一键生成（包含所有模块）

```bash
# 清理、测试、生成聚合报告一条命令完成
mvn clean test verify -pl voglander-coverage-report
```

## 报告位置

生成的聚合报告位于：

```
voglander-coverage-report/target/site/jacoco-aggregate/
├── index.html          # HTML格式报告（主报告）
├── jacoco.xml          # XML格式报告
├── jacoco.csv          # CSV格式报告
└── jacoco-resources/   # 报告资源文件
```

## 查看报告

在浏览器中打开以下文件查看详细的覆盖率报告：

```
voglander-coverage-report/target/site/jacoco-aggregate/index.html
```

报告包含：

- **指令覆盖率**: 执行的字节码指令比例
- **分支覆盖率**: 条件分支的覆盖情况
- **行覆盖率**: 代码行的执行情况
- **复杂度覆盖率**: 圈复杂度的覆盖情况
- **方法覆盖率**: 方法调用的覆盖情况
- **类覆盖率**: 类的覆盖情况

## 排除规则

以下类型的代码已配置为排除统计：

- Spring Boot 启动类 (`*Application.class`)
- 配置类 (`*Config.class`, `*Configuration.class`)
- 数据模型类 (`model/**`, `dto/**`, `vo/**`, `req/**`, `resp/**`, `entity/**`)
- 常量和枚举 (`constant/**`, `enums/**`)

## 集成到CI/CD

可以将覆盖率报告集成到持续集成流程中：

```bash
# 在CI脚本中添加
mvn clean test verify -pl voglander-coverage-report
```

建议设置覆盖率阈值：

- **指令覆盖率**: ≥ 60%
- **分支覆盖率**: ≥ 50%

## 故障排查

### 问题1：报告显示无数据

**解决方案**：

1. 确保先运行了测试：`mvn test`
2. 检查是否生成了 `jacoco.exec` 文件
3. 确认各模块都正确配置了 JaCoCo 插件

### 问题2：某些模块未包含在聚合报告中

**解决方案**：

1. 检查该模块是否在 `voglander-coverage-report/pom.xml` 的依赖中
2. 确认该模块激活了 JaCoCo 插件
3. 确保该模块有测试并且测试被执行

### 问题3：覆盖率数据不准确

**解决方案**：

1. 运行 `mvn clean` 清理旧数据
2. 重新运行测试生成新的覆盖率数据
3. 检查测试是否正确执行

## 注意事项

1. **测试执行**: 必须先运行测试才能生成覆盖率数据
2. **模块依赖**: 聚合报告模块必须依赖所有要统计的模块
3. **插件版本**: 使用 JaCoCo 0.8.11 版本，与项目中配置保持一致
4. **文件路径**: 所有路径都应使用绝对路径
5. **权限要求**: 确保有足够权限创建和写入目标目录

## 版本信息

- **JaCoCo版本**: 0.8.11
- **Maven版本**: 兼容 Maven 3.6+
- **Java版本**: Java 17+
- **Spring Boot版本**: 3.5.3

## 更新历史

- **2024-08-11**: 初始配置完成，支持多模块聚合报告生成