# Esprit Livre - Book E-Commerce Platform

## Project Overview

Esprit Livre is a book e-commerce platform built using JHipster 8.11.0 with a Spring Boot 3.4.5 backend. The application was generated as a monolithic application with OAuth2 authentication, PostgreSQL database, Hazelcast caching, and REST API endpoints. The platform is designed for selling books with features for user management, book catalog, liking system, and order management.

**Project Type**: E-commerce web application (Backend API)
**Architecture**: Monolithic application built with JHipster
**Primary Language**: Java (Spring Boot)
**Client**: API-only (no frontend, skipClient=true)

## Key Technologies & Dependencies

- **Framework**: JHipster 8.11.0, Spring Boot 3.4.5
- **Database**: PostgreSQL with JPA/Hibernate and Liquibase migrations
- **Authentication**: OAuth2/OIDC with Keycloak (also supports Okta, Auth0)
- **Caching**: Hazelcast for second-level caching
- **Build Tool**: Maven
- **Web Server**: Undertow (instead of Tomcat)
- **API Documentation**: Springdoc OpenAPI
- **Testing**: JUnit, Spring Test, Testcontainers
- **Containerization**: Docker and Jib
- **Code Quality**: Checkstyle, Spotless, SonarQube

## Project Structure

```
el-api/
├── src/main/
│   ├── java/com/oussamabenberkane/espritlivre/     # Java source code
│   ├── resources/                                  # Configuration files
│   │   └── config/                                # Application configuration
│   └── docker/                                    # Docker configurations
├── src/test/                                      # Test files
├── .jhipster/                                     # JHipster entity configurations
└── ...
```

The `/src/main/resources/config/` directory contains various application.yml files for different profiles (dev, prod, test).

## Building and Running

### Prerequisites
- Java 17+
- Node.js >= 22.15.0
- Maven 3.2.5+
- Docker (for containerized services)

### Running Locally

1. **Start required services (Keycloak, PostgreSQL) with Docker**:
   ```bash
   docker compose up -d
   ```

2. **Run the application**:
   ```bash
   ./mvnw  # On Windows: mvnw.cmd
   # Or with npm scripts:
   npm run app:start
   ```
   
### Profiles
- **dev** (default): Development profile with hot reloading
- **prod**: Production profile with optimized settings
- **api-docs**: Enables API documentation endpoints

## Development Conventions

### Code Style
- Java code follows standard Spring Boot/JHipster conventions
- Code formatting is enforced with Spotless
- Checkstyle enforces coding standards
- MapStruct for DTO mapping
- Hibernate/JPA for database operations

### Testing
- Unit tests with JUnit 5 and Spring Test
- Integration tests with Testcontainers
- Code coverage with JaCoCo
- Architecture tests with ArchUnit
- API tests with Spring REST Docs (if configured)

### Security
- OAuth2/OIDC authentication with Keycloak
- Role-based authorization (ROLE_ADMIN, ROLE_USER)
- User isolation enforced at service/repository level
- Password encryption with Spring Security
- CSRF protection

## Main Features

### Authentication & User Management
- OAuth2 authentication with Keycloak
- User profiles with address fields (wilaya, city, streetAddress, postalCode, phone)
- Profile update endpoints with email change verification
- Password change functionality
- Role-based access (ROLE_ADMIN, ROLE_USER)

### Book Management
- CRUD operations for books with author, price, stock, description, images
- Author entity with relationship to books
- Tag system (ETIQUETTE, CATEGORY, MAIN_DISPLAY) for categorization
- Search and filtering (by title, author, price range, category, mainDisplay)
- Book suggestions/autocomplete endpoint
- Stock quantity tracking
- Product availability scheduler

### Like System
- Toggle like/unlike for books (single endpoint)
- Like count displayed on book details
- `isLikedByCurrentUser` flag on BookDTO
- User isolation (users can only manage their own likes)
- Unique constraint prevents duplicate likes
- GET /api/books/liked with full filtering support

### Order Management
- Order creation for authenticated users and guests
- Address fields: wilaya (required), city (required), streetAddress (optional), postalCode (optional)
- Auto-generated unique order IDs (format: ORD-ORD-YYYYMMDD-XXXX)
- Order items with book references, quantities, and prices
- Stock validation on order creation
- Auto-decrement stock when order status changes to DELIVERED
- Order filtering by status, date range, amount range
- User isolation (users see only their own orders, admins see all)
- Order status workflow (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- User profile fallback for address/phone fields

## API Endpoints

### Books
- `GET /api/books` - List with filtering/search
- `GET /api/books/{id}` - Single book with like count
- `GET /api/books/liked` - Current user's liked books
- `GET /api/books/suggestions` - Autocomplete/search suggestions

### Likes
- `POST /api/likes/toggle/{bookId}` - Toggle like/unlike

### Orders
- `POST /api/orders` - Create order (guest or authenticated)
- `GET /api/orders` - List with filtering (user isolation)
- `GET /api/orders/{id}` - Single order
- `PUT /api/orders/{id}` - Update order (admin only)

### User Profile
- `GET /api/account` - Current user profile
- `POST /api/account` - Update profile
- `POST /api/account/change-password` - Change password
- `POST /api/account/change-email` - Request email change

### Admin
- Full CRUD for books, authors, tags, orders
- User management
- Order status updates with stock management

## Configuration Files

### Main Configuration
- `src/main/resources/config/application.yml` - Base configuration
- `src/main/resources/config/application-dev.yml` - Development profile
- `src/main/resources/config/application-prod.yml` - Production profile

### Database
- PostgreSQL with Hibernate/JPA
- Liquibase for database migrations
- Connection pooling with HikariCP

### Security
- OAuth2 with Keycloak server on port 9080
- JWT tokens for authentication
- Client ID: web_app, Client Secret: GClkRcZ7OYmB8aDInuIqFsVdPm8BwugS

### Caching
- Hazelcast configured for second-level Hibernate cache
- Cache regions for entities with proper configuration

## Testing

### Running Tests
```bash
# Run all tests
./mvnw verify

# Run only unit tests
./mvnw test

# Run integration tests
./mvnw failsafe:integration-test

# Using npm scripts
npm run backend:unit:test
```

### Code Quality
- Checkstyle: `./mvnw checkstyle:check`
- Sonar analysis: `./mvnw sonar:sonar -Dsonar.login=admin -Dsonar.password=admin`
- JaCoCo coverage reports available after test execution

## Deployment

### Production Build
```bash
./mvnw -Pprod clean verify
```

### Running Production JAR
```bash
java -jar target/*.jar
```

### Docker Deployment
```bash
# Build production Docker image
npm run java:docker:prod

# Run with Docker Compose
docker compose -f src/main/docker/app.yml up --wait
```

## Development Notes

1. The application is configured to use Hazelcast for caching but has disabled Spring Boot Devtools restart to prevent issues with Hazelcast.

2. The project has entities: Book, Tag, Like, Order, OrderItem (based on .yo-rc.json)

3. The application is designed to be used together with a frontend (though currently skipClient=true)

4. Keycloak must be running for authentication to work. Default credentials are admin/admin.

5. The project includes support for multiple authentication providers (Keycloak, Okta, Auth0) through OAuth2/OIDC.
