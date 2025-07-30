# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Voglander is an enterprise-grade video surveillance platform built with Spring Boot 3 and Java 17. It supports multiple video surveillance protocols (GB28181, GT1078, ONVIF) and provides device management, real-time monitoring, and video stream processing capabilities.

## Development Commands

### Build & Run
```bash
# Compile the project
mvn clean compile

# Run the application (main module)
mvn spring-boot:run -pl voglander-web

# Build specific module
mvn clean install -pl voglander-common

# Package the application
mvn clean package -pl voglander-web
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DeviceManagerTest

# Run integration tests with cache (requires Redis)
./start-redis-for-test.sh
mvn test -Dtest=MediaNodeCacheIntegrationTest

# Run tests with specific profile
mvn test -Dspring.profiles.active=test
```

### Database Setup
```bash
# Using SQLite (default)
# Database file created automatically as app.db

# Using MySQL (optional)
# 1. Create database: CREATE DATABASE voglander;
# 2. Execute SQL: sql/voglander.sql
# 3. Update application-dev.yml with connection details
```

## Architecture Overview

### Multi-Module Structure
```
voglander/
├── voglander-web/          # REST API Controllers, filters, interceptors
├── voglander-manager/      # Business logic orchestration, complex operations
├── voglander-service/      # Core business services, domain logic
├── voglander-repository/   # Data access, entities, mappers, caching
├── voglander-integration/  # External system integrations (GB28181, ZLM, Excel)
├── voglander-client/       # External service clients and DTOs
├── voglander-common/       # Shared utilities, constants, enums, exceptions
└── voglander-test/         # Test configurations and utilities
```

### Layered Architecture
- **Web Layer**: REST controllers, request/response handling, parameter validation
- **Manager Layer**: Complex business workflows, multi-service coordination
- **Service Layer**: Core business logic, single-responsibility operations
- **Repository Layer**: Data persistence, caching, database operations
- **Integration Layer**: External system wrappers with unified `ResultDTO` responses

### Key Design Patterns
- **Assembler Pattern**: Data transformation between layers (DTO ↔ DO ↔ VO)
- **Manager Pattern**: Complex business logic coordination
- **Wrapper Pattern**: External system integration with unified error handling
- **Template Pattern**: Unified internal methods for data operations with caching/logging

## Technology Stack

### Core Framework
- **Java 17** (uses `jakarta.*` packages, not `javax.*`)
- **Spring Boot 3.5.3** with auto-configuration
- **MyBatis Plus 3.5.5** for data access
- **Dynamic DataSource 4.3.1** for multi-database support

### Data & Caching
- **MySQL 8.2.0** (production) / **SQLite** (development/testing)
- **Redis** for distributed caching and locking
- **HikariCP** connection pooling

### Video & Integration
- **GB28181-Proxy 1.2.4** for surveillance protocol support
- **ZLMediaKit-Starter 1.0.6** for media streaming
- **EasyExcel 4.0.1** for Excel processing

### Testing & Monitoring
- **JUnit 5** with **Mockito** for testing
- **SkyWalking 9.1.0** for distributed tracing
- **SpringDoc 2.8.9** for API documentation

## Coding Standards

### Naming Conventions
- **Entities**: `*DO` (Data Object) - database entities
- **DTOs**: `*DTO` - data transfer between layers
- **VOs**: `*VO` - view objects for API responses
- **Requests**: `*Req` - API request objects
- **Responses**: `*Resp` - nested response objects with `items` array
- **Services**: `*Service` / `*ServiceImpl`
- **Managers**: `*Manager` - business logic coordination
- **Controllers**: `*Controller` - REST endpoints

### API Response Standards
- All APIs return `AjaxResult` wrapper with `code`, `msg`, `data`
- Time fields return Unix timestamps (milliseconds) in VOs
- Pagination responses wrap data in `items` field within `data`
- Use `@Operation`, `@Parameter`, `@Tag` for complete Swagger documentation

### Data Access Patterns
- Use **MyBatis Plus** `IService` methods for simple CRUD operations
- Manager layer provides unified internal methods: `xxxInternal()`, `deleteXxxInternal()`
- All data modifications go through unified entry points for cache/logging consistency
- Complex queries and multi-table operations in Manager layer only

### Caching Strategy
- **@Cached annotation**: Only for basic single-object DB queries in Repository layer
- **RedisCache**: Manual cache management for complex scenarios
- **RedisLockUtil**: Distributed locking for concurrent operations
- Cache keys use primary ID or unique fields, values stored as JSON strings

### Time Handling (Critical)
- **DO/DTO layers**: Use `LocalDateTime` exclusively (no Date/Timestamp)
- **VO responses**: Convert to Unix timestamps via `fieldNameToEpochMilli()` methods
- **Database**: Store as DATETIME/TIMESTAMP, convert via assemblers

### JSON Processing
- Use **FastJSON2** exclusively for all JSON serialization/deserialization
- Configure as default JSON processor in Spring Boot
- Avoid Jackson, Gson, or other JSON libraries

## Business Domain Knowledge

### Device Management
- **Device Types**: Camera devices with multiple protocols (GB28181, ONVIF, etc.)
- **Device States**: Online (1), Offline (0) with heartbeat monitoring
- **Registration**: SIP-based device registration with authentication
- **Channels**: Each device can have multiple video channels

### Protocol Support
- **GB28181**: National standard for video surveillance in China
- **ONVIF**: Open network video interface standard
- **SIP**: Session initiation protocol for device communication
- **ZLMediaKit**: Media server for stream forwarding and recording

### Key Services
- `DeviceRegisterService`: Device SIP registration and authentication
- `DeviceCommandService`: PTZ control and device commands
- `MediaNodeService`: Media server node management
- `ExportTaskService`: Bulk data export operations

## Integration Guidelines

### External System Wrappers
- All integration layer wrappers must return `ResultDTO` format
- Include comprehensive exception handling and logging
- Use `@Slf4j` for detailed operation tracking
- Methods: `ResultDTOUtils.success()` / `ResultDTOUtils.failure()`

### Cache Integration
- Use `@Cached` only for basic DB entity queries by ID/unique field
- Complex caching via `RedisCache` with manual key management
- Distributed operations use `RedisLockUtil` for consistency

### Async Processing
- Use `AsyncManager` for background tasks
- Configure thread pools via `ThreadPoolConfig`
- RabbitMQ for message queuing (optional RocketMQ support)

## Testing Strategy

### Test Organization
- **Unit Tests**: Service layer with `@MockBean` dependencies using `BaseMockTest`
- **Integration Tests**: Manager layer and full Spring context with `@SpringBootTest` using `BaseTest`
- **Base Configuration**: `TestConfig` excludes Redis/WebMvc for fast unit tests

### Manager Testing Rules
- **All Manager classes MUST use integration testing** - extend `BaseTest` for real data processing
- **Manager tests require complete Spring context** with actual database transactions
- **No mocking of Service/Repository dependencies** in Manager tests - test real data flow
- **Use `@Transactional` for automatic rollback** after each test method

### Test Database
- **SQLite** (`test-app.db`) for lightweight testing
- **Schema**: `schema-sqlite.sql` with all required tables
- **Cleanup**: `@BeforeEach`/`@AfterEach` for test isolation

### Mock Strategy
- **Service Layer**: Use `BaseMockTest` with `@MockitoBean` for dependencies
- **Manager Layer**: Use `BaseTest` with real beans and database transactions
- **Integration Tests**: Real database with transaction isolation
- Use `@MockitoBean` (not deprecated `@MockBean`) when mocking is needed

## Common Operations

### Adding New Features
1. Create entity in `repository` module (`*DO`)
2. Add service interface/impl with `IService<*DO>`
3. Create manager for business logic coordination
4. Add controller with full Swagger documentation
5. Create assemblers for data transformation
6. Write unit/integration tests following existing patterns

### Database Operations
1. Use `IService` base methods for simple CRUD
2. Custom queries only for complex multi-table operations
3. All modifications through Manager's unified internal methods
4. Cache invalidation via unified entry points

### Error Handling
- Throw `ServiceException` for business errors
- Use `ServiceExceptionEnum` for standardized error codes
- Global exception handling via `GlobalExceptionHandler`
- Log all errors with context information

## Configuration Files

### Application Configs
- `application.yml`: Main configuration
- `application-dev.yml`: Development environment
- `application-test.yml`: Test environment
- `application-repo.yml`: Database configuration
- `application-inte.yml`: Integration configuration

### Development Rules
- **Cursor Rules**: `.cursorrules` - Comprehensive coding standards
- **Project Rules**: `project-rule.md` - Frontend development guidelines
- Both files contain detailed architectural and coding patterns

This architecture emphasizes clean separation of concerns, comprehensive caching strategies, and robust integration patterns suitable for enterprise video surveillance systems.