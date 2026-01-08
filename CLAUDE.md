# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "Esprit Livre", a book e-commerce application built with JHipster 8.11.0. It's a monolithic Spring Boot application with OAuth2/Keycloak authentication, PostgreSQL database, and RESTful APIs for managing books, authors, orders, and customer interactions.

## Essential Commands

### Development Workflow
- **Start services (PostgreSQL + Keycloak)**: `docker compose up -d`
- **Start application**: `mvn` or `mvn spring-boot:run` (runs on port 8080)
- **Debug mode**: `mvn -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"`
- **Run unit tests**: `mvn test`
- **Run integration tests**: `mvn verify`
- **Build for production**: `mvn -Pprod clean verify`
- **Code formatting**: `mvn checkstyle:check`
- **Generate Docker image**: `npm run java:docker` or `mvn -ntp verify -DskipTests -Pprod jib:dockerBuild`

### Testing Commands
- **Run single test class**: `mvn test -Dtest=ClassName`
- **Run single test method**: `mvn test -Dtest=ClassName#methodName`
- **Run tests with coverage**: `npm run ci:backend:test`
- **Skip tests**: `mvn -DskipTests`

### Database Management
- **Start services**: `docker compose up -d`
- **Stop services**: `docker compose down`
- **View logs**: `docker compose logs -f postgres` or `docker compose logs -f keycloak`
- **Generate Liquibase diff**: Configure `liquibase-plugin.*` properties in pom.xml, then run `mvn liquibase:diff`

## Architecture

### Core Technologies
- **Backend**: Spring Boot 3.4.5 with Java 17
- **Database**: PostgreSQL (dev: el-dev-db, prod: espritLivre)
- **Authentication**: OAuth2/OIDC with Keycloak 26.2.3
- **Caching**: Hazelcast (port 5701 for clustering)
- **API Documentation**: SpringDoc OpenAPI at `/v3/api-docs`
- **Build**: Maven (use `mvn` directly)
- **Server**: Undertow (not Tomcat)
- **WebSocket**: Spring WebSocket support

### Domain Model

Core entities managed by the application:

- **Book**: Main product entity with title, price, stock, cover image, author relationship
- **Author**: Book authors with profile pictures
- **BookPack**: Collections of books sold as bundles
- **Tag**: Multi-type categorization (ETIQUETTE, CATEGORY, MAIN_DISPLAY) with i18n support
- **Order**: Customer orders with shipping details, status tracking, and Algeria-specific fields (wilaya, city)
- **OrderItem**: Line items that can reference either a Book or BookPack
- **Like**: User preferences for books
- **User**: Managed by OAuth2 system (skipUserManagement enabled)

### Key Package Structure
```
com.oussamabenberkane.espritlivre/
├── aop/logging/          # Aspect-oriented logging
├── config/               # Spring configuration (security, database, cache, etc.)
├── domain/               # JPA entities
│   └── enumeration/      # Enums (OrderStatus, ShippingProvider, TagType, Language, etc.)
├── repository/           # Spring Data JPA repositories
├── security/             # OAuth2 configuration and utilities
│   └── oauth2/           # OAuth2-specific security components
├── service/              # Business logic layer
│   ├── dto/              # Data Transfer Objects
│   ├── mapper/           # MapStruct mappers for entity-DTO conversion
│   └── specs/            # JPA Specification for dynamic queries
└── web/                  # REST API layer
    ├── filter/           # OAuth2 filters
    └── rest/             # REST controllers (AccountResource, BookResource, OrderResource, etc.)
```

### Database Configuration

**Development (docker-compose.yml)**:
- Host: localhost:5432
- Database: el-dev-db
- User: postgres
- Password: postgres

**Production (from application.yml)**:
- Database: espritLivre
- User: espritLivre
- Password: (no password in dev)

**Liquibase**: Schema migrations in [src/main/resources/config/liquibase/](src/main/resources/config/liquibase/)
- Master changelog: `master.xml`
- Entity changelogs timestamped and immutable
- Run diffs against Hibernate model for new migrations

### OAuth2/Keycloak Setup

**Development Configuration**:
- URL: http://localhost:9080
- Realm: jhipster
- Client ID: web_app
- Client Secret: ASoXbE72eEiIpZmvGBObIpN2dNhiyM26
- Admin credentials: admin/admin
- Required roles: ROLE_ADMIN, ROLE_USER

**Realm Import**: Auto-imported from [src/main/docker/realm-config/](src/main/docker/realm-config/)

**Alternative Providers**: Can configure Okta or Auth0 (see README.md for instructions)

### Spring Profiles

- **dev** (default): Development mode with devtools, auto-reload, CORS enabled
- **prod**: Production mode with optimizations, no devtools
- **test**: Test profile for unit tests
- **testdev**: Test profile variant for dev environment
- **testprod**: Test profile variant for prod environment
- **tls**: Enable TLS/SSL
- **no-liquibase**: Skip Liquibase migrations
- **api-docs**: Enable API documentation endpoints

Activate profiles via Maven: `-Pprod` or in IDE/environment: `spring.profiles.active=dev`

### Key Dependencies

- **JHipster Framework**: 8.11.0
- **MapStruct**: 1.6.3 (DTO mapping)
- **Liquibase**: Database migrations
- **Hazelcast**: 5.5.0 (distributed caching)
- **SpringDoc OpenAPI**: 2.8.8 (API documentation)
- **Testcontainers**: PostgreSQL integration testing
- **Apache POI**: 5.2.5 (Excel export functionality)
- **ArchUnit**: 1.4.0 (architecture testing)

## Development Workflows

### Adding New Entities

1. Update [JDL/esprit-livre.jdl](JDL/esprit-livre.jdl) with entity definition
2. Run JHipster entity sub-generator: `jhipster entity <EntityName>`
3. Review generated Liquibase changelog in `src/main/resources/config/liquibase/changelog/`
4. Implement business logic in service layer if needed
5. Test with `mvn verify`

### API Development Patterns

**Controller Layer** ([web/rest/](src/main/java/com/oussamabenberkane/espritlivre/web/rest/)):
- REST conventions: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- Return `ResponseEntity<DTO>` for single resources
- Use pagination: `Pageable` parameter, return `Page<DTO>`
- Validation via JSR-303 annotations on DTOs
- Notable endpoints:
  - `AccountResource`: User account management and profile
  - `AdminResource`: Admin-specific operations and Keycloak integration
  - `DashboardResource`: Dashboard statistics and metrics
  - `ContactResource`: Contact form handling
  - `AuthInfoResource`: OAuth2 authentication information
  - `LogoutResource`: Custom logout handling

**Service Layer** ([service/](src/main/java/com/oussamabenberkane/espritlivre/service/)):
- Business logic in service classes
- DTOs for API contracts (in [service/dto/](src/main/java/com/oussamabenberkane/espritlivre/service/dto/))
- MapStruct mappers handle entity-DTO conversion (in [service/mapper/](src/main/java/com/oussamabenberkane/espritlivre/service/mapper/))
- JPA Specifications for complex queries (in [service/specs/](src/main/java/com/oussamabenberkane/espritlivre/service/specs/))
- Key services:
  - `FileStorageService`: Handles media file uploads and storage
  - `OrderService`: Order management and Excel export functionality
  - `DashboardService`: Admin dashboard statistics
  - `MailService`: Email notifications using Thymeleaf templates
  - `AppUserService`: Extended user management beyond OAuth2
  - `CleanupService`: Scheduled cleanup of unused resources
  - `UniqueIdGeneratorService`: Generates unique order IDs

**Repository Layer** ([repository/](src/main/java/com/oussamabenberkane/espritlivre/repository/)):
- Extend `JpaRepository` or `JpaSpecificationExecutor` for dynamic queries
- Custom query methods follow Spring Data JPA naming conventions
- Native queries when necessary with `@Query`

### Testing Strategy

**Unit Tests** (suffix: `Test.java`):
- Use `@SpringBootTest` for integration-style unit tests
- Mock dependencies with `@MockBean`
- Test services and repositories independently
- Run with: `mvn test`

**Integration Tests** (suffix: `IT.java` or `IntTest.java`):
- Full Spring context with `@SpringBootTest`
- Use Testcontainers for PostgreSQL
- Test full request-response cycles
- Run with: `mvn verify`

**Test Profiles**: Tests run with `test,testdev` or `test,testprod` profiles

### Static Media Files

Media files stored in [src/main/resources/media/](src/main/resources/media/):
- `authors/`: Author profile pictures
- `books/`: Book cover images
- `book-packs/`: Book pack cover images
- `categories/`: Category images
- `users/`: User profile pictures
- `default.png`: Default fallback image

These are served as static resources in production builds. Managed by `FileStorageService`.

### Internationalization (i18n)

- Supported languages: French (default), English, Arabic (enum Language)
- Translation files in [src/main/resources/i18n/](src/main/resources/i18n/)
- Entities like Tag have `nameEn` and `nameFr` fields for multilingual content

## Important Notes

- **Use `mvn` directly** for all Maven commands (Maven wrapper not used in this project)
- **Docker Compose**: Use root `docker-compose.yml` (not files in `src/main/docker/`)
  - Note: Spring Docker Compose integration is disabled by default (`spring.docker.compose.enabled: false`)
  - Services must be started manually with `docker compose up -d` before running the application
- **OAuth2 varies by environment**: Check [application.yml](src/main/resources/config/application.yml) and profile-specific configs
- **Liquibase changelogs are immutable**: Never modify existing changelogs, always create new ones
- **Ports**: 8080 (app), 5432 (postgres), 9080 (keycloak), 5701 (hazelcast)
- **JHipster version**: 8.11.0 (check before using generators)
- **Node.js required**: For npm scripts (version >= 22.15.0)
- **Test containers**: Integration tests use Testcontainers, requires Docker
- **Algeria-specific features**: Order entity includes wilaya and city fields for Algerian addresses
- **Shipping providers**: Yalidine and ZR Express are configured as shipping options
