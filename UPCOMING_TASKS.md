# Esprit Livre - Upcoming Tasks

## 🎯 Today's Remaining Task

### **2. User Profile Management**
- [ ] Create endpoint to view user profile (`GET /api/account`)
- [ ] Create endpoint to update user profile (`PUT /api/account`)
- [ ] Add custom user attributes if needed (preferences, address, etc.)
- [ ] Test profile management with Postman

---

## 📋 Critical Backend Features (Missing from Analysis)

Based on our earlier analysis, these are the **high-priority** features that need implementation:

### **Priority 1: User Experience Features**

#### **1. Like System - User Isolation** 🔴 **CRITICAL**
**Current Issue**: Any authenticated user can see ALL likes
**What's Needed**:
- [ ] Filter likes by current user in `LikeService`
- [ ] Add `GET /api/account/likes` endpoint (user's own likes only)
- [ ] Add duplicate like prevention (can't like same book twice)
- [ ] Add user-friendly endpoint: `POST /api/books/{bookId}/like`
- [ ] Add user-friendly endpoint: `DELETE /api/books/{bookId}/like`
- [ ] Test like isolation between users

**Files to Modify**:
- `LikeService.java` - Add filtering logic
- `LikeResource.java` - Add user-specific endpoints
- Create: `BookLikeResource.java` (optional, for cleaner API)

---

#### **2. Order Management - Business Logic** 🔴 **CRITICAL**
**Current Issue**: Orders are just CRUD, no business logic
**What's Needed**:
- [ ] **User Isolation**: Filter orders by current user
- [ ] **Stock Validation**: Check if books have sufficient stock before order
- [ ] **Stock Deduction**: Reduce stock quantity when order is placed
- [ ] **Order Workflow**: Implement status transitions (PENDING → CONFIRMED → SHIPPED → DELIVERED)
- [ ] **Order Items**: Link books to orders with quantities
- [ ] **Price Calculation**: Auto-calculate total amount from order items
- [ ] **Guest Orders**: Support orders without authentication (optional)
- [ ] **Order Tracking**: Add endpoint `GET /api/orders/track?uniqueId=XXX&phone=XXX` for guests

**Files to Modify**:
- `OrderService.java` - Add business logic
- `OrderResource.java` - Add workflow endpoints
- Create: `OrderWorkflowService.java` (handles status transitions)

---

#### **3. Email Notifications** 🟡 **HIGH PRIORITY**
**Current Issue**: No email service exists
**What's Needed**:
- [ ] Configure mail service (SMTP settings)
- [ ] Create `MailService.java`
- [ ] Send order confirmation email (on order creation)
- [ ] Send shipping update email (when order status changes to SHIPPED)
- [ ] Email templates (HTML templates for professional emails)
- [ ] Test email delivery

**Files to Create**:
- `service/MailService.java`
- `resources/templates/mail/order-confirmation.html`
- `resources/templates/mail/order-shipped.html`

**Configuration**:
- Update `application.yml` with SMTP settings

---

### **Priority 2: API Enhancements**

#### **4. Book Highlights & Featured Endpoints**
**Current**: Can filter by `mainDisplayId`, but no dedicated endpoints
**What's Needed**:
- [ ] `GET /api/books/featured` - Books tagged as featured/highlights
- [ ] `GET /api/books/bestsellers` - Most sold books
- [ ] `GET /api/books/new-arrivals` - Recently added books
- [ ] Add sorting options (by price, date, popularity)

**Files to Modify**:
- `BookResource.java` - Add new endpoints
- `BookService.java` - Add query methods

---

#### **5. User Dashboard/Statistics**
**What's Needed**:
- [ ] `GET /api/account/statistics` - User's order count, total spent, etc.
- [ ] `GET /api/account/recent-orders` - Last 5 orders
- [ ] `GET /api/account/liked-books` - User's liked books with details

**Files to Create**:
- `web/rest/UserDashboardResource.java`
- `service/UserDashboardService.java`

---

### **Priority 3: Data Improvements**

#### **6. Author Management Enhancements**
**Current**: Basic CRUD only
**What's Needed**:
- [ ] Add author bio, photo, social links
- [ ] Add author popularity/ranking
- [ ] Filter books by author on author detail page

**Files to Modify**:
- `domain/Author.java` - Add new fields
- Create Liquibase migration for schema changes

---

#### **7. Book Reviews/Ratings** (Future Feature)
**Not in current scope but useful**:
- [ ] Add `Review` entity
- [ ] Allow users to rate books (1-5 stars)
- [ ] Display average rating on book details

---

### **Priority 4: Security & Performance**

#### **8. Security Enhancements**
- [ ] Add method-level security for user isolation
  - Users can only update/delete their own likes
  - Users can only view their own orders
- [ ] Add `@PreAuthorize` with ownership checks
- [ ] Implement `SecurityService` to check resource ownership

**Example**:
```java
@PreAuthorize("hasAuthority('ROLE_USER') and @securityService.isLikeOwner(#id)")
public void deleteLike(Long id) { ... }
```

---

#### **9. Performance Optimizations**
- [ ] Add caching for frequently accessed data (tags, categories)
- [ ] Optimize queries (N+1 problem in likes/orders with eager loading)
- [ ] Add pagination defaults and limits
- [ ] Add database indexes for search fields

---

#### **10. API Documentation**
- [ ] Configure SpringDoc/Swagger properly
- [ ] Add API descriptions to all endpoints
- [ ] Add request/response examples
- [ ] Generate API documentation

---

### **Priority 5: Admin Panel Features**

#### **11. Admin Dashboard**
- [ ] `GET /api/admin/dashboard/stats` - Total sales, orders, users
- [ ] `GET /api/admin/orders` - All orders (not just current user)
- [ ] `GET /api/admin/users` - User management
- [ ] Order status management (approve, ship, cancel)

---

#### **12. Inventory Management**
- [ ] Bulk update book stock
- [ ] Low stock alerts
- [ ] Stock history tracking

---

### **Priority 6: Data Validation**

#### **13. Input Validation Improvements**
- [ ] Add stronger validation rules
- [ ] Custom validators for business rules
- [ ] Better error messages

**Examples**:
- Order: Phone number format validation
- Book: Price must be positive
- Order: Stock availability validation before creation

---

## 🗂️ Task Categorization

### **Backend Only** (Current Focus)
1. ✅ Authentication & OAuth2 setup
2. 🟡 User Profile Management (In Progress)
3. 🔴 Like System - User Isolation
4. 🔴 Order Management - Business Logic
5. 🟡 Email Notifications
6. 🟢 Book Highlights Endpoints
7. 🟢 User Dashboard

### **Backend + Database Changes**
8. Author Management Enhancements
9. Book Reviews/Ratings

### **Backend + Configuration**
10. Security Enhancements (ownership checks)
11. Performance Optimizations (caching, indexing)
12. Email Service Setup

### **Backend + Documentation**
13. API Documentation (Swagger/SpringDoc)
14. Admin Dashboard
15. Inventory Management

---

## 📊 Recommended Implementation Order

### **Phase 1: Core User Features** (This Week)
1. ✅ Authentication Setup
2. 🟡 User Profile Management ← **START HERE**
3. 🔴 Like System - User Isolation
4. 🔴 Order Management - Business Logic
5. 🟡 Email Notifications

### **Phase 2: API Enhancements** (Next Week)
6. Book Highlights & Featured Endpoints
7. User Dashboard/Statistics
8. Security Enhancements (ownership checks)

### **Phase 3: Admin Features** (Week After)
9. Admin Dashboard
10. Inventory Management
11. Order Workflow (admin actions)

### **Phase 4: Polish & Optimization** (Later)
12. Performance Optimizations
13. API Documentation
14. Data Validation Improvements

---

## 🎯 Immediate Next Steps (Today/Tomorrow)

1. **User Profile Management** ⏱️ ~1-2 hours
   - Add custom profile fields if needed
   - Implement update profile endpoint
   - Test with Postman

2. **Like System - User Isolation** ⏱️ ~2-3 hours
   - Filter likes by current user
   - Add user-friendly endpoints
   - Prevent duplicate likes
   - Test isolation between users

3. **Order Management - Core Logic** ⏱️ ~4-6 hours
   - User isolation for orders
   - Stock validation
   - Stock deduction
   - Basic order workflow
   - Test order placement

4. **Email Notifications** ⏱️ ~2-3 hours
   - Configure SMTP
   - Send order confirmation
   - Send shipping notification
   - Test email delivery

**Total Estimated Time for Core Features**: ~10-15 hours

---

## 💡 Notes

- Focus on **User-facing features first** (likes, orders, profile)
- **Admin panel can wait** until user features are solid
- **Email notifications are important** for e-commerce UX
- **Testing is critical** - use Postman collection to verify each feature

---

## 📝 Future Considerations (Not Urgent)

- Payment gateway integration
- Multi-language support (i18n)
- Advanced search (Elasticsearch)
- Image upload for books
- Shopping cart persistence
- Wishlist functionality
- Social login (Google, Facebook)
- Mobile app API support
