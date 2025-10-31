# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "Esprit Livre", a book e-commerce application built with JHipster 8.11.0. It's a monolithic Spring Boot application with OAuth2 authentication, PostgreSQL database, and RESTful APIs.

## Essential Commands

### Development Commands
- **Start development server**: `./mvnw` or `npm run backend:start`
- **Run tests**: `./mvnw verify` or `npm run backend:unit:test`
- **Build for production**: `./mvnw -Pprod clean verify`
- **Code formatting check**: `./mvnw checkstyle:check`
- **Generate Docker image**: `npm run java:docker`

### Database Commands
- **Start PostgreSQL**: `docker compose up -d`
- **Start Keycloak (OAuth2)**: `docker compose up -d`

### Testing Commands
- **Run unit tests**: `./mvnw test`
- **Run integration tests**: `./mvnw verify -Pdev`
- **Run backend tests with coverage**: `npm run ci:backend:test`

## Architecture

### Core Technologies
- **Backend**: Spring Boot 3.4.5 with Java 17
- **Database**: PostgreSQL with Liquibase migrations
- **Authentication**: OAuth2/OIDC with Keycloak
- **Caching**: Hazelcast
- **Documentation**: SpringDoc OpenAPI
- **Build**: Maven with JHipster framework

### Domain Model (from JDL)
The application manages books, orders, and user interactions:

- **Book**: Core entity with title, author, price, stock, description
- **Tag**: Categorization system (ETIQUETTE, CATEGORY, MAIN_DISPLAY)
- **Order**: Customer orders with shipping info and status tracking
- **OrderItem**: Individual items within orders
- **Like**: User book preferences
- **User**: Managed by OAuth2 system

### Key Package Structure
```
com.oussamabenberkane.espritlivre/
├── config/          # Spring configuration classes
├── domain/          # JPA entities and enums
├── repository/      # Data access layer
├── service/         # Business logic and DTOs
├── security/        # OAuth2 and security configuration
└── web/rest/        # REST API controllers
```

### Database Configuration
- **Development**: PostgreSQL on localhost:5432/espritLivre
- **User**: espritLivre (no password in dev)
- **Liquibase**: Manages schema migrations in `src/main/resources/config/liquibase/`

### OAuth2 Setup
- **Provider**: Keycloak (default) or configurable (Okta, Auth0)
- **Development URL**: http://localhost:9080/realms/jhipster
- **Client**: web_app/ASoXbE72eEiIpZmvGBObIpN2dNhiyM26
- **Required roles**: ROLE_ADMIN, ROLE_USER

## Development Workflows

### Adding New Entities
1. Update `JDL/esprit-livre.jdl` with entity definition
2. Run JHipster entity sub-generator
3. Review generated migrations in liquibase changelog
4. Implement business logic in service layer

### API Development
- Controllers in `web.rest` package follow REST conventions
- Use DTOs for API contracts (in `service.dto`)
- MapStruct handles entity-DTO mapping
- Validation annotations on DTOs
- Exception handling via `ExceptionTranslator`

### Testing Strategy
- Unit tests for services and repositories
- Integration tests with `@SpringBootTest`
- Test containers for database integration
- Separate test profiles (test, testdev, testprod)

## Important Notes

- Use `./mvnw` instead of `mvn` (wrapper ensures correct Maven version)
- OAuth2 configuration varies by environment - check `application.yml`
- Liquibase changelogs are timestamped and immutable
- Static resources served from `src/main/resources/static/`
- Profiles: `dev` (default), `prod`, `test`
- Port 8080 for application, 5701 for Hazelcast clustering
