# Voglander 视频监控平台

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/voglander)](https://mvnrepository.com/artifact/io.github.lunasaw/voglander)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/voglander/master/LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)

[🌐 项目主页](http://lunasaw.github.io) | [📖 文档中心](#) | [🚀 快速开始](#快速开始)

## 📋 项目介绍

Voglander 是一个基于 Spring Boot 3 和 Java 17 构建的企业级视频监控平台，专注于提供高性能、高可用、易扩展的视频监控解决方案。

![Stream操作演示](./code-log/stream.gif)

### ✨ 核心特性

- 🎯 **多协议支持** - 支持 GB28181、GT1078、ONVIF 等主流视频监控协议
- 🏭 **设备兼容** - 兼容海康、大华、宇视、中维等主流监控设备厂商
- 🔧 **模块化架构** - 采用多模块设计，支持集群化部署
- 📊 **实时监控** - 支持实时视频流处理和设备状态监控
- 🚀 **高性能** - 基于异步处理和缓存优化，支持大规模并发
- 🛡️ **安全可靠** - 提供完善的权限控制和数据安全保障

### 🎯 应用场景

- 🏢 **企业园区监控** - 办公楼宇、工厂园区安防监控
- 🏫 **教育机构** - 学校、培训机构视频监控管理
- 🏥 **医疗机构** - 医院、诊所安防监控系统
- 🏪 **商业场所** - 商场、超市、门店监控管理
- 🏘️ **社区物业** - 住宅小区、物业管理监控系统

## 🏗️ 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Frontend)                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   管理后台       │  │   监控大屏       │  │   移动端      │ │
│  │  (Vue.js)      │  │  (Dashboard)   │  │  (Mobile)    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   API Gateway     │
                    │  (Load Balancer)  │
                    └─────────┬─────────┘
                              │
┌─────────────────────────────┴─────────────────────────────────┐
│                        应用层 (Application)                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Web 层        │  │   Manager 层     │  │   Service 层  │ │
│  │ - REST API      │  │ - 业务管理       │  │ - 核心服务    │ │
│  │ - 参数校验      │  │ - 流程编排       │  │ - 业务逻辑    │ │
│  │ - 异常处理      │  │ - 缓存管理       │  │ - 事务控制    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────┴─────────────────────────────────┐
│                      数据访问层 (Repository)                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   ORM 映射      │  │   缓存管理       │  │   消息队列    │ │
│  │ - MyBatis Plus  │  │ - Redis Cache   │  │ - RabbitMQ   │ │
│  │ - 动态数据源    │  │ - 本地缓存      │  │ - RocketMQ   │ │
│  │ - 分库分表      │  │ - 分布式锁      │  │ - 异步处理    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────┴─────────────────────────────────┐
│                       存储层 (Storage)                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   关系数据库     │  │   缓存数据库     │  │   流媒体服务  │ │
│  │ - MySQL         │  │ - Redis         │  │ - ZLMediaKit │ │
│  │ - SQLite        │  │ - 集群部署      │  │ - 流转发      │ │
│  │ - 主从复制      │  │ - 高可用        │  │ - 录像存储    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 模块架构

```
voglander/
├── voglander-web/          # Web控制层 - REST API接口
│   ├── controller/         # 控制器
│   ├── filter/            # 过滤器
│   ├── interceptor/       # 拦截器
│   └── config/            # Web配置
│
├── voglander-manager/      # 业务管理层 - 复杂业务逻辑
│   ├── manager/           # 业务管理器
│   ├── assembler/         # 数据转换器
│   ├── service/           # 业务服务
│   └── async/             # 异步任务
│
├── voglander-service/      # 核心服务层 - 核心业务逻辑
│   ├── command/           # 设备命令服务
│   └── login/             # 登录注册服务
│
├── voglander-repository/   # 数据访问层 - 数据持久化
│   ├── entity/            # 数据实体
│   ├── mapper/            # 数据映射
│   ├── cache/             # 缓存管理
│   └── config/            # 数据源配置
│
├── voglander-integration/  # 集成层 - 第三方系统集成
│   ├── wrapper/           # 外部系统包装器
│   ├── domain/            # 集成领域模型
│   └── aop/               # 切面处理
│
├── voglander-client/       # 客户端层 - 外部调用客户端
│   ├── service/           # 客户端服务
│   └── domain/            # 客户端模型
│
├── voglander-common/       # 公共模块 - 通用组件
│   ├── constant/          # 常量定义
│   ├── enums/             # 枚举类型
│   ├── exception/         # 异常处理
│   └── util/              # 工具类
│
└── voglander-test/         # 测试模块 - 单元测试和集成测试
    └── resources/          # 测试配置
```

## 🔧 技术栈

### 后端技术栈

| 技术                 | 版本    | 说明      |
|--------------------|-------|---------|
| **核心框架**           |       |         |
| Java               | 17    | 开发语言    |
| Spring Boot        | 3.5.3 | 微服务框架   |
| **数据访问**           |       |         |
| MyBatis Plus       | 3.5.5 | ORM框架   |
| Dynamic DataSource | 4.3.1 | 动态数据源   |
| MySQL              | 8.2.0 | 关系数据库   |
| SQLite             | -     | 嵌入式数据库  |
| HikariCP           | -     | 数据库连接池  |
| **缓存中间件**          |       |         |
| Redis              | -     | 分布式缓存   |
| Spring Cache       | 3.3.1 | 缓存抽象层   |
| **消息队列**           |       |         |
| RabbitMQ           | -     | 消息中间件   |
| RocketMQ           | 2.3.0 | 分布式消息系统 |
| **视频协议**           |       |         |
| GB28181-Proxy      | 1.2.4 | 国标协议支持  |
| ZLMediaKit-Starter | 1.0.6 | 流媒体服务器  |
| **工具库**            |       |         |
| Luna Common        | 2.6.5 | 通用工具库   |
| EasyExcel          | 4.0.1 | Excel处理 |
| FastJSON2          | -     | JSON处理  |
| **监控与追踪**          |       |         |
| SkyWalking         | 9.1.0 | 链路追踪    |
| Swagger            | 3.0.0 | API文档   |
| **其他**             |       |         |
| Lombok             | -     | 代码生成    |
| Apache Commons     | -     | 通用工具    |

### 前端技术栈

| 技术           | 版本  | 说明      |
|--------------|-----|---------|
| Vue.js       | 3.x | 前端框架    |
| Vue Router   | 4.x | 路由管理    |
| Pinia        | 2.x | 状态管理    |
| Element Plus | -   | UI组件库   |
| Axios        | -   | HTTP客户端 |

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+ (可选，默认使用SQLite)
- **Redis**: 6.0+ (可选)
- **Node.js**: 16+ (前端开发)

### 📥 克隆项目

```bash
git clone https://github.com/lunasaw/voglander.git
cd voglander
```

### 🔧 后端启动

1. **编译项目**

```bash
mvn clean compile
```

2. **初始化数据库**

```bash
# 使用SQLite (默认)
# 数据库文件会自动创建为 app.db

# 或使用MySQL
# 1. 创建数据库: CREATE DATABASE voglander;
# 2. 执行SQL脚本: sql/voglander.sql
# 3. 修改配置文件中的数据库连接信息
```

3. **启动应用**

```bash
mvn spring-boot:run -pl voglander-web
```

4. **访问应用**

```
应用地址: http://localhost:8081
API文档: http://localhost:8081/swagger-ui.html
```

### 🎨 前端启动

```bash
cd voglander-frontend
npm install
npm run serve
```

前端访问: http://localhost:8080

### 🐳 Docker 部署 (规划中)

```bash
# 构建镜像
docker build -t voglander:latest .

# 运行容器
docker-compose up -d
```

## 📖 配置说明

### 核心配置文件

- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置
- `application-repo.yml` - 数据库配置
- `application-inte.yml` - 集成配置

### 关键配置项

```yaml
# 数据库配置
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/voglander
          username: root
          password: your_password

# Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password

# SIP协议配置
sip:
  enable: true
  port: 5060
  ip: 0.0.0.0
```

## 📚 API 文档

### 设备管理 API

```http
# 获取设备列表
GET /api/device/list

# 获取设备详情
GET /api/device/{id}

# 添加设备
POST /api/device/add

# 更新设备
PUT /api/device/update

# 删除设备
DELETE /api/device/{id}
```

### 通道管理 API

```http
# 获取设备通道
GET /api/channel/list/{deviceId}

# 通道控制命令
POST /api/channel/control
```

完整 API 文档请访问: [Swagger UI](http://localhost:8081/swagger-ui.html)
访问这个链接: [Swagger API](http://localhost:8081/v3/api-docs)
## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify -P integration-test
```

### 测试覆盖率

```bash
mvn clean test jacoco:report
```

## 📈 性能特性

- **高并发**: 支持万级并发连接
- **低延迟**: 毫秒级响应时间
- **高可用**: 99.9% 服务可用性
- **可扩展**: 水平扩展支持

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源协议发布。

## 👥 开发团队

- **Luna** - *项目维护者* - [GitHub](https://github.com/lunasaw)
- **Email**: iszychen@gmail.com

## 🔗 相关链接

- [项目主页](http://lunasaw.github.io)
- [问题反馈](https://github.com/lunasaw/voglander/issues)
- [更新日志](CHANGELOG.md)
- [开发文档](docs/)

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lunasaw/voglander&type=Date)](https://star-history.com/#lunasaw/voglander&Date)

---

<p align="center">
  <b>如果这个项目对您有帮助，请给我们一个 ⭐️ Star!</b>
</p>