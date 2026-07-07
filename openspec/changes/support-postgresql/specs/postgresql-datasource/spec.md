## ADDED Requirements

### Requirement: PostgreSQL JDBC driver integration
The system SHALL include PostgreSQL JDBC driver (version 42.7.x or higher) as a dependency to enable PostgreSQL connectivity.

#### Scenario: Driver available at runtime
- **WHEN** the application starts with PostgreSQL datasource configured
- **THEN** the PostgreSQL JDBC driver MUST be available on the classpath

### Requirement: PostgreSQL datasource configuration
The system SHALL support PostgreSQL datasource configuration through application-repo.yml with standard JDBC connection parameters.

#### Scenario: Valid PostgreSQL configuration
- **WHEN** user provides valid PostgreSQL JDBC URL, username, and password in application-repo.yml
- **THEN** the application MUST successfully establish connection to the PostgreSQL database

#### Scenario: Connection pool with HikariCP
- **WHEN** PostgreSQL datasource is configured
- **THEN** the system MUST use HikariCP connection pool with configurable pool size, timeout, and connection test query

#### Scenario: Dynamic datasource selection
- **WHEN** PostgreSQL is configured as the primary datasource
- **THEN** the Dynamic DataSource framework MUST route all database operations to PostgreSQL

### Requirement: Database schema initialization
The system SHALL provide a PostgreSQL-specific SQL initialization script that creates all required tables, indexes, and constraints.

#### Scenario: Fresh database initialization
- **WHEN** the application connects to an empty PostgreSQL database
- **THEN** the system MUST execute voglander-postgresql.sql to initialize the schema

#### Scenario: Schema compatibility with existing entities
- **WHEN** the PostgreSQL schema is initialized
- **THEN** all MyBatis-Plus entity mappings (DO classes) MUST work without modification

### Requirement: SQL dialect compatibility
The system SHALL handle PostgreSQL-specific SQL syntax differences from MySQL and SQLite.

#### Scenario: Auto-increment primary keys
- **WHEN** inserting new records with auto-generated IDs
- **THEN** the system MUST use PostgreSQL SERIAL or IDENTITY columns correctly

#### Scenario: Date and time functions
- **WHEN** executing queries with date/time operations
- **THEN** the system MUST use PostgreSQL date/time functions (e.g., NOW(), CURRENT_TIMESTAMP)

#### Scenario: JSON data type handling
- **WHEN** storing or querying JSON fields
- **THEN** the system MUST use PostgreSQL JSONB data type and operators

#### Scenario: String comparison case sensitivity
- **WHEN** executing LIKE or equality queries on string fields
- **THEN** the system MUST handle PostgreSQL case-sensitive collation correctly

### Requirement: Backward compatibility
The system SHALL maintain full backward compatibility with existing SQLite and MySQL datasource configurations.

#### Scenario: Existing SQLite configuration unchanged
- **WHEN** application-repo.yml uses SQLite driver and URL
- **THEN** the application MUST function identically to pre-PostgreSQL versions

#### Scenario: Existing MySQL configuration unchanged
- **WHEN** application-repo.yml uses MySQL driver and URL
- **THEN** the application MUST function identically to pre-PostgreSQL versions

### Requirement: PostgreSQL integration testing
The system SHALL include automated integration tests that verify PostgreSQL datasource functionality.

#### Scenario: Connection and CRUD operations test
- **WHEN** PostgreSQL integration tests execute
- **THEN** the tests MUST successfully connect to PostgreSQL and perform create, read, update, delete operations on all entity types

#### Scenario: Transaction rollback test
- **WHEN** a transaction fails during PostgreSQL test
- **THEN** the system MUST correctly roll back changes

#### Scenario: Test environment isolation
- **WHEN** PostgreSQL integration tests run
- **THEN** the tests MUST use a separate test database instance and NOT affect production data

### Requirement: Documentation and deployment guide
The system SHALL provide clear documentation for deploying Voglander with PostgreSQL.

#### Scenario: Configuration example provided
- **WHEN** developer reads CLAUDE.md or application-repo.yml comments
- **THEN** complete PostgreSQL configuration examples MUST be documented

#### Scenario: Schema initialization instructions
- **WHEN** administrator deploys Voglander with PostgreSQL
- **THEN** step-by-step instructions for database creation and schema initialization MUST be provided
