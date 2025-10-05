# Esprit Livre - Book E-Commerce Platform

## Tech Stack
- **Backend**: Spring Boot 3.4.5 + JHipster 8.11.0
- **Database**: PostgreSQL with Liquibase migrations
- **Authentication**: OAuth2/OIDC with Keycloak
- **Caching**: Hazelcast
- **Build**: Maven

## Implemented Features

### Authentication & User Management
- OAuth2 authentication with Keycloak (supports Okta, Auth0)
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
- Auto-generated unique order IDs (format: ORD-YYYYMMDD-XXXX)
- Order items with book references, quantities, and prices
- Stock validation on order creation
- Auto-decrement stock when order status changes to DELIVERED
- Order filtering by status, date range, amount range
- User isolation (users see only their own orders, admins see all)
- Order status workflow (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- User profile fallback for address/phone fields

## API Endpoints

### Books
- GET /api/books - List with filtering/search
- GET /api/books/{id} - Single book with like count
- GET /api/books/liked - Current user's liked books
- GET /api/books/suggestions - Autocomplete/search suggestions

### Likes
- POST /api/likes/toggle/{bookId} - Toggle like/unlike

### Orders
- POST /api/orders - Create order (guest or authenticated)
- GET /api/orders - List with filtering (user isolation)
- GET /api/orders/{id} - Single order
- PUT /api/orders/{id} - Update order (admin only)

### User Profile
- GET /api/account - Current user profile
- POST /api/account - Update profile
- POST /api/account/change-password - Change password
- POST /api/account/change-email - Request email change

### Admin
- Full CRUD for books, authors, tags, orders
- User management
- Order status updates with stock management

## Business Logic Highlights
- User fallback: Authenticated users can use profile data as fallback for order fields
- Stock management: Validated on creation, decremented on delivery
- Unique ID generation: Sequential daily format for easy tracking
- User isolation: Security enforced at repository/service level using Spring Security
