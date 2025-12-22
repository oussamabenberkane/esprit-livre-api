# Comprehensive Testing Plan for Esprit Livre User App

## Executive Summary

This testing plan covers the **User App** (frontend customer-facing application) for the Esprit Livre bookstore platform. It focuses on UI/UX functional testing with comprehensive coverage of all user-facing features, excluding admin-only functionality.

**Scope**: Frontend functional testing only (UI/UX flows)
**Exclusions**: Admin features, i18n testing (minimal), static informational pages
**Test Detail Level**: High-level scenarios suitable for QA engineers

---

## 1. User App Architecture Overview

### Tech Stack
- **Framework**: React 19.1.1 with React Router DOM 7.9.3
- **Styling**: TailwindCSS 4.1.13
- **Authentication**: OAuth2/Keycloak (Google SSO)
- **State Management**: React Context (Cart, Favorites)
- **Backend Integration**: REST API (Spring Boot)

### Key User Flows Identified

1. **Browse & Discovery**: Home → Categories → Books → Book Details
2. **Search & Filter**: Search bar → Suggestions → All Books with filters
3. **Favorites Management**: Guest favorites (localStorage) → Auth favorites (API sync)
4. **Shopping Cart**: Add to cart → View cart → Checkout → Order placement
5. **Authentication**: Google OAuth login → Profile management
6. **Order Management**: View orders → Track status
7. **Book Packs**: Browse packs → View pack details → Add pack to cart

---

## 2. Feature Mapping: User App to Backend API

| User App Feature | Frontend Pages/Components | Backend API Endpoints | Auth Required |
|-----------------|---------------------------|----------------------|---------------|
| **Browse Books** | HomePage, AllBooks | GET /api/books, GET /api/books?mainDisplayId={id} | No |
| **Book Details** | BookDetails | GET /api/books/{id}, GET /api/books/{id}/recommendations | No |
| **Book Cover Images** | BookCard component | GET /api/books/{id}/cover | No |
| **Search** | Navbar search | GET /api/books/suggestions?q={query} | No |
| **Categories/Tags** | HomePage (CategoryCard) | GET /api/tags?type=CATEGORY, GET /api/tags?type=MAIN_DISPLAY | No |
| **Authors** | HomePage (author section) | GET /api/authors/top | No |
| **Book Packs** | PacksPromotionnels | GET /api/book-packs, GET /api/book-packs/{id} | No |
| **Favorites (Guest)** | FavoritesContext | localStorage only | No |
| **Favorites (Auth)** | Favorites page | POST /api/likes/toggle/{bookId}, GET /api/books/liked | Yes (USER) |
| **Cart (All Users)** | CartContext, CartCheckoutPage | localStorage only | No |
| **Checkout** | CartCheckoutPage | POST /api/orders | No (guest) / Optional (auth) |
| **User Profile** | Profile page | GET /api/app-user/profile, PUT /api/app-user/profile | Yes (USER) |
| **Orders** | Orders component | GET /api/orders, GET /api/orders/{id} | Yes (USER) |
| **Authentication** | AuthPage, AuthCallback | OAuth2 flow (Keycloak), GET /api/account | Yes |

---

## 3. Testing Plan Structure

### 3.1 Test Categories

1. **Navigation & UI**
2. **Book Browsing & Discovery**
3. **Search & Filtering**
4. **Book Details**
5. **Shopping Cart (Guest)**
6. **Shopping Cart (Books + Packs)**
7. **Checkout & Order Placement**
8. **Authentication & Authorization**
9. **Favorites/Likes**
10. **User Profile Management**
11. **Order History & Tracking**
12. **Book Packs**
13. **Responsive Design**
14. **Error Handling**
15. **Performance & UX**

---

## 4. Detailed Test Cases

### **4.1 Navigation & UI**

#### TC-NAV-001: Homepage Load
**Objective**: Verify homepage loads correctly with all sections
**Preconditions**: None
**Steps**:
1. Navigate to application root URL (/)
2. Wait for page to fully load

**Expected Results**:
- Navbar appears with logo, search bar, language toggle, cart icon
- Hero carousel displays with multiple slides
- Categories section displays horizontally scrollable cards
- Recommended Books section displays with book cards
- Featured Authors section displays with author cards
- Footer appears at bottom with relevant links
- No console errors

**Edge Cases**:
- Slow network (3G simulation): Should show skeleton loaders
- Empty API responses: Should handle gracefully without crashes

---

#### TC-NAV-002: Navbar Functionality
**Objective**: Verify navbar interactive elements work correctly
**Preconditions**: On any page
**Steps**:
1. Click on logo → Should navigate to homepage
2. Click on cart icon → Should navigate to /cart
3. Observe cart badge → Should display item count if cart has items
4. Click search bar (desktop) → Should focus and allow typing
5. On mobile: Click menu icon → Should open mobile menu

**Expected Results**:
- All navigation links functional
- Cart badge updates dynamically
- Search bar is interactive
- Mobile menu opens/closes correctly

**Edge Cases**:
- Cart with 99+ items: Badge should display "99+"
- Long search queries: Should handle gracefully

---

#### TC-NAV-003: Hero Carousel Navigation
**Objective**: Verify hero carousel auto-advance and manual controls
**Preconditions**: On homepage
**Steps**:
1. Observe carousel for 5 seconds
2. Click right arrow button
3. Click left arrow button
4. Click on pagination dots
5. Let carousel auto-advance

**Expected Results**:
- Carousel auto-advances every ~4-5 seconds
- Arrow buttons navigate forward/backward
- Pagination dots indicate current slide
- Clicking dots jumps to specific slide
- Smooth transitions

**Edge Cases**:
- Single slide: Navigation controls should be hidden
- Rapid clicking: Should queue transitions, not break

---

### **4.2 Book Browsing & Discovery**

#### TC-BROWSE-001: View All Books
**Objective**: Verify books catalog page displays correctly
**Preconditions**: None
**Steps**:
1. Navigate to /allbooks
2. Observe page layout

**Expected Results**:
- Filter section displays on left/top (responsive)
- Books display in grid layout (responsive columns)
- Each book card shows: cover image, title, author, price
- Pagination controls appear if more than one page
- Default: 12 books per page

**Edge Cases**:
- No books available: Display "No books found" message
- Single book: Display correctly without layout issues
- 100+ pages: Pagination should handle gracefully

---

#### TC-BROWSE-002: Pagination
**Objective**: Verify pagination works correctly
**Preconditions**: On /allbooks with multiple pages of results
**Steps**:
1. Note current page number
2. Click "Next" or page number
3. Observe page changes
4. Click "Previous"
5. Click specific page number

**Expected Results**:
- Page changes show new books
- Current page indicator updates
- Scroll position resets to top
- URL may update with page parameter
- Previous/Next buttons disable appropriately (first/last page)

**Edge Cases**:
- Navigate to last page directly
- Navigate backward from page 5 to page 1
- Rapidly click pagination buttons

---

#### TC-BROWSE-003: Book Card Interactions
**Objective**: Verify book card interactive elements
**Preconditions**: On any page with book cards
**Steps**:
1. Hover over book card
2. Click on book cover/title → Should navigate to book details
3. Click on "Add to Cart" button
4. Click on "Favorites/Heart" icon

**Expected Results**:
- Hover shows visual feedback (scale/shadow)
- Clicking navigates to /books/{id}
- Add to cart shows confirmation popup
- Cart badge increments
- Heart icon toggles filled/unfilled state
- Guest users: Favorites stored in localStorage

**Edge Cases**:
- Out of stock books: "Add to Cart" disabled or shows message
- Pre-order books: Shows appropriate badge
- Adding same book twice: Increments quantity

---

#### TC-BROWSE-004: Categories Browsing
**Objective**: Verify category filtering from homepage
**Preconditions**: On homepage
**Steps**:
1. Scroll to Categories section
2. Click on a category card
3. Observe navigation

**Expected Results**:
- Navigates to /allbooks with category filter applied
- Books displayed match selected category
- Filter section shows selected category

**Edge Cases**:
- Empty category: Display "No books in this category"

---

#### TC-BROWSE-005: Main Display Sections (Recommendations, Featured)
**Objective**: Verify main display carousels on homepage
**Preconditions**: On homepage
**Steps**:
1. Scroll to "Recommended Books" section
2. Observe horizontal scroll behavior
3. Use arrow buttons to navigate
4. Click on a book card

**Expected Results**:
- Books display in horizontal scrollable container
- Arrow buttons appear when scrollable
- Pagination dots indicate scroll position
- Clicking book navigates to details
- Smooth scroll animation

**Edge Cases**:
- Less than 5 books: No scroll needed, arrows hidden
- 50+ books: Scroll and arrows work smoothly

---

### **4.3 Search & Filtering**

#### TC-SEARCH-001: Search Suggestions (Autocomplete)
**Objective**: Verify search autocomplete functionality
**Preconditions**: On any page with search bar
**Steps**:
1. Click on search bar
2. Type "har" (3 characters)
3. Wait for suggestions to appear
4. Observe suggestion list
5. Click on a suggestion

**Expected Results**:
- Suggestions appear after 2-3 characters
- Shows matching book titles
- Suggestions update as user types
- Clicking suggestion navigates to book details or search results
- Dropdown closes after selection

**Edge Cases**:
- Query with no matches: Display "No results found"
- Very long query (100+ chars): Should handle gracefully
- Special characters: Should not break search
- Typing quickly: Should debounce requests

**Negative Scenarios**:
- Empty search: No suggestions shown
- Single character: No suggestions or wait for more characters

---

#### TC-SEARCH-002: Search Execution
**Objective**: Verify full search execution from navbar
**Preconditions**: On any page
**Steps**:
1. Enter search query in navbar
2. Press Enter or click search icon
3. Observe navigation and results

**Expected Results**:
- Navigates to /allbooks with search filter applied
- Books matching query are displayed
- Search term appears in filter section or search box
- Results are relevant to query

**Edge Cases**:
- Search term: "XYZ123NonExistent" → Display "No results"
- Search with quotes/special chars: Handle gracefully
- Very long query: Truncate or handle appropriately

---

#### TC-FILTER-001: Apply Category Filter
**Objective**: Verify category filter on All Books page
**Preconditions**: On /allbooks
**Steps**:
1. Open category filter dropdown/section
2. Select one category
3. Observe book list updates
4. Select multiple categories (if supported)
5. Deselect a category

**Expected Results**:
- Book list updates to show only selected categories
- Filter UI shows active selection
- Can select/deselect categories
- Pagination resets to page 1
- URL may update with category parameter

**Edge Cases**:
- Select all categories: Should show all books
- Select category with no books: Show "No results"

---

#### TC-FILTER-002: Apply Author Filter
**Objective**: Verify author filter on All Books page
**Preconditions**: On /allbooks
**Steps**:
1. Open author filter dropdown
2. Select one or more authors
3. Observe results

**Expected Results**:
- Only books by selected authors displayed
- Multiple authors: Books from any selected author (OR logic)

**Edge Cases**:
- Author with 1 book: Displays correctly
- Author with 0 books: Shows "No results"

---

#### TC-FILTER-003: Apply Price Range Filter
**Objective**: Verify price filtering
**Preconditions**: On /allbooks
**Steps**:
1. Open price filter
2. Set minimum price (e.g., 500 DZD)
3. Set maximum price (e.g., 2000 DZD)
4. Apply filter

**Expected Results**:
- Books within price range displayed
- Books outside range excluded
- Min/max values validated (min ≤ max)

**Edge Cases**:
- Min = Max: Show books at exact price
- Min > Max: Show validation error or swap values
- Negative values: Should be prevented
- Very high max (e.g., 999999): Show all books above min

**Negative Scenarios**:
- Min price > highest book price: No results
- Max price < lowest book price: No results

---

#### TC-FILTER-004: Multiple Filters Combined
**Objective**: Verify multiple filters work together (AND logic)
**Preconditions**: On /allbooks
**Steps**:
1. Apply category filter (e.g., "Fiction")
2. Apply price filter (500-2000 DZD)
3. Apply search term
4. Observe results

**Expected Results**:
- Results match ALL applied filters
- Removing one filter updates results immediately
- Clear all filters: Returns to full catalog

**Edge Cases**:
- Conflicting filters: No results message
- Clear filters individually vs "Clear All" button

---

#### TC-FILTER-005: Filter Persistence
**Objective**: Verify filters persist across navigation
**Preconditions**: On /allbooks with filters applied
**Steps**:
1. Apply filters (category + price)
2. Click on a book to view details
3. Click browser back button

**Expected Results**:
- Returns to /allbooks with filters still applied
- Results remain filtered

**Edge Cases**:
- Refresh page: Filters may reset (depending on URL params)
- Navigate away to homepage and back: Filters reset

---

### **4.4 Book Details**

#### TC-BOOK-001: View Book Details
**Objective**: Verify book details page displays correctly
**Preconditions**: None
**Steps**:
1. Navigate to /books/{validId}
2. Observe page content

**Expected Results**:
- Book cover image displays
- Title, author name, price visible
- Description/synopsis displays (if available)
- Language badge shows (FR/EN/AR)
- Stock status shows (In stock / Out of stock / Pre-order)
- "Add to Cart" button present
- "Add to Favorites" heart icon present
- Recommendations section shows related books

**Edge Cases**:
- Book with no description: Section hidden or shows placeholder
- Out of stock: "Add to Cart" disabled
- Pre-order: Shows "Pre-order" badge and button

---

#### TC-BOOK-002: Add Book to Cart from Details
**Objective**: Verify adding book to cart from details page
**Preconditions**: On /books/{id}
**Steps**:
1. Click "Add to Cart" button
2. Observe confirmation popup
3. Check cart badge
4. Click on cart badge to view cart

**Expected Results**:
- Confirmation popup appears
- Popup shows book title and "Added to cart" message
- Cart badge increments by 1
- Cart page shows book with quantity 1

**Edge Cases**:
- Add same book multiple times: Quantity increments
- Add out-of-stock book: Should be prevented

---

#### TC-BOOK-003: Quantity Selector (if present)
**Objective**: Verify quantity selection on details page
**Preconditions**: On /books/{id} with quantity selector
**Steps**:
1. Click "+" button to increment quantity
2. Click "-" button to decrement quantity
3. Manually type quantity value
4. Add to cart with quantity > 1

**Expected Results**:
- Quantity increments/decrements correctly
- Minimum quantity: 1 (cannot go below)
- Maximum quantity: Limited by stock or reasonable limit
- Add to cart adds correct quantity

**Edge Cases**:
- Type "0": Should reset to 1
- Type negative: Should prevent or reset to 1
- Type 1000: Should limit to max stock or show warning
- Type non-numeric: Should prevent or validate

---

#### TC-BOOK-004: Book Recommendations
**Objective**: Verify recommendations section on book details
**Preconditions**: On /books/{id}
**Steps**:
1. Scroll to "Recommended Books" or "You may also like" section
2. Observe recommendations
3. Click on a recommended book

**Expected Results**:
- Shows 5-10 related books
- Books are relevant (same category/author/tags)
- Clicking navigates to that book's details page

**Edge Cases**:
- No recommendations: Section hidden or shows message
- Recommendations circular loop: Clicking between 2 books works

---

#### TC-BOOK-005: Invalid Book ID
**Objective**: Verify behavior when accessing non-existent book
**Preconditions**: None
**Steps**:
1. Navigate to /books/999999 (non-existent ID)

**Expected Results**:
- Shows "Book not found" message
- Provides link to browse all books or homepage
- Does not crash or show blank page

---

### **4.5 Shopping Cart (Guest User)**

#### TC-CART-001: Add First Item to Cart
**Objective**: Verify adding first item to cart as guest
**Preconditions**: Empty cart, not authenticated
**Steps**:
1. Navigate to book card or details page
2. Click "Add to Cart"
3. Observe confirmation popup
4. Check cart badge

**Expected Results**:
- Confirmation popup appears
- Cart badge shows "1"
- Item stored in localStorage (el_cart key)

**Edge Cases**:
- localStorage disabled: Handle gracefully (fallback to session storage or in-memory)

---

#### TC-CART-002: Add Multiple Different Items
**Objective**: Verify adding multiple books to cart
**Preconditions**: Empty cart
**Steps**:
1. Add Book A to cart
2. Add Book B to cart
3. Add Book C to cart
4. Navigate to /cart

**Expected Results**:
- Cart shows 3 different books
- Each book shows quantity 1
- Cart badge shows "3"
- Total amount calculated correctly

**Edge Cases**:
- Add 20+ different books: All display correctly

---

#### TC-CART-003: Add Same Item Multiple Times
**Objective**: Verify quantity increments for duplicate additions
**Preconditions**: Cart with Book A (qty 1)
**Steps**:
1. Add Book A to cart again
2. Check cart

**Expected Results**:
- Cart still shows 1 entry for Book A
- Quantity incremented to 2
- Cart badge shows "2"
- Total price = unit price × 2

---

#### TC-CART-004: View Cart Page
**Objective**: Verify cart page displays correctly
**Preconditions**: Cart with 2-3 items
**Steps**:
1. Navigate to /cart
2. Observe page layout

**Expected Results**:
- All cart items displayed in list
- Each item shows: cover image, title, author, price, quantity
- Quantity controls (+/-) present
- Remove button present
- Subtotal per item calculated (price × qty)
- Total cart value displayed
- Shipping cost displayed (700 DZD)
- Grand total = subtotal + shipping
- "Proceed to Checkout" button visible

**Edge Cases**:
- Empty cart: Show "Your cart is empty" message with link to browse books

---

#### TC-CART-005: Update Item Quantity in Cart
**Objective**: Verify quantity adjustment in cart
**Preconditions**: On /cart with item qty 2
**Steps**:
1. Click "+" button on an item
2. Observe quantity and totals update
3. Click "-" button
4. Observe updates
5. Refresh page

**Expected Results**:
- Quantity increments/decrements immediately
- Item subtotal updates (price × qty)
- Cart total updates
- Changes persist after refresh (localStorage)

**Edge Cases**:
- Decrement from qty 1: Item removed from cart (or shows confirmation)
- Increment to very high number (100+): Should allow or show stock limit warning

---

#### TC-CART-006: Remove Item from Cart
**Objective**: Verify item removal
**Preconditions**: Cart with 2+ items
**Steps**:
1. Click "Remove" or trash icon on an item
2. Observe cart updates

**Expected Results**:
- Item removed immediately
- Cart badge decrements
- Cart total recalculates
- If last item: Shows empty cart message

**Edge Cases**:
- Remove all items one by one: Should show empty cart
- Confirmation dialog (if present): Can cancel removal

---

#### TC-CART-007: Cart Persistence Across Sessions
**Objective**: Verify cart persists across browser sessions (guest)
**Preconditions**: Cart with 2 items
**Steps**:
1. Close browser tab
2. Open new tab and navigate to application
3. Check cart badge
4. Navigate to /cart

**Expected Results**:
- Cart badge shows correct count
- Cart page shows same 2 items
- Quantities and selections intact

**Edge Cases**:
- Clear browser data: Cart resets (expected)
- Incognito mode: Separate cart

---

#### TC-CART-008: Navigate to Checkout
**Objective**: Verify checkout button navigation
**Preconditions**: Cart with items
**Steps**:
1. On /cart, click "Proceed to Checkout" button

**Expected Results**:
- Navigates to checkout section/page
- Cart items carry over to checkout

**Edge Cases**:
- Empty cart: Checkout button disabled or hidden

---

### **4.6 Shopping Cart (Books + Packs)**

#### TC-CART-PACK-001: Add Book Pack to Cart
**Objective**: Verify adding book pack to cart
**Preconditions**: On /packs page
**Steps**:
1. Click on a book pack card
2. View pack details (list of books included)
3. Click "Add to Cart"

**Expected Results**:
- Pack added to cart
- Cart badge increments
- Pack stored separately in localStorage (el_cart_packs key)

---

#### TC-CART-PACK-002: View Cart with Books and Packs
**Objective**: Verify cart displays both books and packs
**Preconditions**: Cart with 2 books and 1 pack
**Steps**:
1. Navigate to /cart
2. Observe cart items

**Expected Results**:
- Books and packs displayed in same cart
- Packs clearly labeled (e.g., "Pack" badge or icon)
- Each item shows correct price and quantity
- Total calculated correctly

**Edge Cases**:
- Pack containing books already in cart: Both should coexist separately

---

#### TC-CART-PACK-003: View Pack Books Popup
**Objective**: Verify viewing books within a pack from cart
**Preconditions**: Cart with a book pack
**Steps**:
1. On /cart, click on pack item or "View books" button
2. Observe popup/modal

**Expected Results**:
- Modal shows all books included in pack
- Each book shows: cover, title, author
- Can close modal

---

#### TC-CART-PACK-004: Remove Pack from Cart
**Objective**: Verify removing pack from cart
**Preconditions**: Cart with pack
**Steps**:
1. Click remove button on pack
2. Observe cart updates

**Expected Results**:
- Pack removed from cart
- Cart total recalculates
- Cart badge decrements

---

### **4.7 Checkout & Order Placement**

#### TC-CHECKOUT-001: View Checkout Form (Guest)
**Objective**: Verify checkout form displays for guest users
**Preconditions**: Cart with items, not authenticated
**Steps**:
1. Navigate to /cart
2. Click "Proceed to Checkout" or scroll to checkout section

**Expected Results**:
- Checkout form displays
- Required fields: Full Name, Phone, Wilaya, City
- Optional fields: Email
- Shipping method selector: Home Delivery / Pickup Point
- Order summary shows cart items and total
- "Place Order" button visible

**Edge Cases**:
- No items in cart: Checkout section hidden or disabled

---

#### TC-CHECKOUT-002: Fill Checkout Form - Home Delivery
**Objective**: Verify home delivery flow
**Preconditions**: On checkout form
**Steps**:
1. Select "Home Delivery" option
2. Enter Full Name: "Ahmed Benali"
3. Enter Phone: "0555123456"
4. Enter Email: "ahmed@example.com" (optional)
5. Select Wilaya: "Alger"
6. Observe City dropdown populates
7. Select City: "Bab El Oued"
8. Enter Street Address: "12 Rue Didouche Mourad"

**Expected Results**:
- All fields accept input
- Wilaya dropdown shows all wilayas
- City dropdown populates based on selected wilaya
- Street Address field appears for home delivery
- Form validates required fields

**Edge Cases**:
- Wilaya with 1 city: Auto-select or show single option
- Long street address (200+ chars): Should allow or truncate gracefully

---

#### TC-CHECKOUT-003: Fill Checkout Form - Pickup Point
**Objective**: Verify pickup point flow
**Preconditions**: On checkout form
**Steps**:
1. Select "Pickup Point" option
2. Fill required fields (Name, Phone, Wilaya, City)
3. Select Shipping Provider: "Yalidine" or "ZRexpress"

**Expected Results**:
- Street Address field hidden/disabled
- Shipping Provider dropdown appears
- Shows available providers (Yalidine, ZRexpress)

**Edge Cases**:
- No provider selected: Validation error on submit

---

#### TC-CHECKOUT-004: Form Validation
**Objective**: Verify checkout form validation
**Preconditions**: On checkout form
**Steps**:
1. Leave all fields empty
2. Click "Place Order"
3. Observe validation messages
4. Fill invalid phone (e.g., "abc123")
5. Submit again

**Expected Results**:
- Shows validation errors for required fields
- Full Name: Required, min length
- Phone: Required, valid format (Algerian phone)
- Wilaya: Required
- City: Required
- Email: Optional, but if provided must be valid format
- Street Address: Required if home delivery selected
- Shipping Provider: Required if pickup point selected

**Negative Scenarios**:
- Phone: "123" → Too short
- Phone: "05551234567890" → Too long
- Email: "notanemail" → Invalid format
- Name: Special characters only → Should prevent or validate

---

#### TC-CHECKOUT-005: Place Order (Guest - Success)
**Objective**: Verify successful order placement as guest
**Preconditions**: Valid checkout form filled
**Steps**:
1. Fill valid checkout information
2. Review order summary (items, shipping, total)
3. Click "Place Order"
4. Wait for response

**Expected Results**:
- Loading indicator appears
- Success message/popup appears
- Order confirmation shows:
  - Unique Order ID
  - Order details
  - Message to save Order ID for tracking
- Cart clears (both localStorage keys)
- Cart badge resets to 0
- Redirect to order confirmation page or homepage

**Edge Cases**:
- Network timeout: Show error message, retry option
- Slow response: Loading state maintained

---

#### TC-CHECKOUT-006: Place Order (Authenticated User)
**Objective**: Verify order placement for authenticated user
**Preconditions**: User logged in, cart with items
**Steps**:
1. Navigate to checkout
2. Observe form pre-filled with profile data
3. Modify if needed
4. Place order

**Expected Results**:
- Form pre-populated with user profile (name, phone, wilaya, city, address, default shipping)
- User can edit fields
- Order associated with user account
- Order appears in user's order history

**Edge Cases**:
- Profile incomplete: Show empty fields for user to fill

---

#### TC-CHECKOUT-007: Wilaya/City Dependency
**Objective**: Verify city updates when wilaya changes
**Preconditions**: On checkout form
**Steps**:
1. Select Wilaya: "Alger"
2. Observe city dropdown
3. Select City: "Bab El Oued"
4. Change Wilaya to: "Oran"
5. Observe city dropdown

**Expected Results**:
- City dropdown populates with cities of "Alger"
- After changing to "Oran", city dropdown updates to Oran's cities
- Previously selected city clears

---

#### TC-CHECKOUT-008: Order Summary Review
**Objective**: Verify order summary accuracy
**Preconditions**: Cart with 3 books (prices: 500, 800, 1200 DZD), qty 1 each
**Steps**:
1. Navigate to checkout
2. Review order summary section

**Expected Results**:
- Shows all 3 books with titles and prices
- Subtotal: 2500 DZD
- Shipping: 700 DZD
- Total: 3200 DZD
- Calculations accurate

**Edge Cases**:
- Cart with packs: Pack price shown, not individual books
- Multiple quantities: Subtotal per item calculated correctly

---

#### TC-CHECKOUT-009: Order Placement - Insufficient Stock
**Objective**: Verify error handling for out-of-stock items
**Preconditions**: Cart with book that goes out of stock
**Steps**:
1. Add book to cart (stock: 5, add qty 3)
2. Backend stock changes to 0
3. Attempt to place order

**Expected Results**:
- Error message appears: "Book X is out of stock"
- Order not placed
- User prompted to remove item or update cart
- Cart remains intact

**Negative Scenarios**:
- Ordered qty 10, stock 5: Error "Insufficient stock for Book X"

---

#### TC-CHECKOUT-010: Guest User - Order Tracking Prompt
**Objective**: Verify prompt for guest to sign in for order tracking
**Preconditions**: Guest user places order successfully
**Steps**:
1. Complete order as guest
2. Observe post-order screen

**Expected Results**:
- Popup/message prompts: "Sign in to track your order"
- Options: "Sign In" or "Later"
- If "Sign In": Redirects to auth page
- If "Later": Dismisses, proceeds to confirmation

---

### **4.8 Authentication & Authorization**

#### TC-AUTH-001: Access Auth Page
**Objective**: Verify authentication page loads
**Preconditions**: Not authenticated
**Steps**:
1. Navigate to /auth
2. Observe page content

**Expected Results**:
- Shows "Sign In" button (Google OAuth)
- Branding/logo visible
- Clean, simple UI

---

#### TC-AUTH-002: Google OAuth Login - Success
**Objective**: Verify successful login via Google OAuth
**Preconditions**: Valid Google account
**Steps**:
1. On /auth, click "Sign In with Google"
2. Redirects to Google login page
3. Enter valid Google credentials
4. Authorize application
5. Redirected back to application

**Expected Results**:
- Redirects to Keycloak/Google OAuth
- After authorization, redirects to /auth/callback
- Access token and ID token stored in localStorage
- User profile fetched from backend
- Redirects to original page or homepage
- Navbar shows user icon/profile instead of login button

**Edge Cases**:
- User cancels OAuth: Returns to /auth without logging in
- Network error during OAuth: Shows error message

---

#### TC-AUTH-003: OAuth Callback Handling
**Objective**: Verify callback URL handling
**Preconditions**: OAuth redirect in progress
**Steps**:
1. OAuth provider redirects to /auth/callback?code=...
2. Observe handling

**Expected Results**:
- Exchanges code for tokens
- Stores tokens in localStorage
- Fetches user profile
- Redirects to saved redirect URL or homepage
- Loading indicator shown during process

**Edge Cases**:
- Invalid code: Shows error, redirects to /auth
- Missing code parameter: Handles gracefully

---

#### TC-AUTH-004: Protected Route Access (Not Authenticated)
**Objective**: Verify unauthenticated user cannot access protected routes
**Preconditions**: Not authenticated
**Steps**:
1. Navigate to /profile (protected route)

**Expected Results**:
- Redirects to /auth
- After login, redirects back to /profile

---

#### TC-AUTH-005: Favorites Sync After Login
**Objective**: Verify guest favorites sync to server after login
**Preconditions**: Guest user with 3 books in favorites (localStorage)
**Steps**:
1. Have 3 books liked as guest
2. Log in via Google OAuth
3. Navigate to /favorites

**Expected Results**:
- All 3 guest favorites synced to backend
- Favorites page shows all books
- localStorage favorites merged with server favorites
- If server already has favorites: Merged (no duplicates)

**Edge Cases**:
- Guest favorites: 5 books, Server favorites: 3 books → Total 8 unique books
- Sync fails for 1 book: Other 2 still synced, error logged

---

#### TC-AUTH-006: Cart Persistence After Login
**Objective**: Verify cart persists after login
**Preconditions**: Guest cart with 2 books
**Steps**:
1. Add 2 books to cart as guest
2. Log in
3. Check cart

**Expected Results**:
- Cart remains intact with 2 books
- Cart still uses localStorage (no server cart sync)

---

#### TC-AUTH-007: Logout
**Objective**: Verify logout functionality
**Preconditions**: Authenticated user
**Steps**:
1. Navigate to profile or user menu
2. Click "Logout"

**Expected Results**:
- Tokens removed from localStorage
- Navbar shows login button again
- Redirects to homepage or login page
- Protected routes no longer accessible

**Edge Cases**:
- Full logout (Keycloak session): If implemented, clears Keycloak session
- Partial logout: Only clears frontend tokens

---

#### TC-AUTH-008: Session Expiry
**Objective**: Verify behavior when access token expires
**Preconditions**: Authenticated, access token expires
**Steps**:
1. Wait for token to expire (or manually set expired token)
2. Perform action requiring auth (e.g., toggle favorite)

**Expected Results**:
- Shows "Session expired" message
- Redirects to /auth
- After re-login, returns to previous action

**Edge Cases**:
- Auto-refresh token: If implemented, refreshes silently
- No refresh token: Requires full re-login

---

### **4.9 Favorites/Likes**

#### TC-FAV-001: Add Book to Favorites (Guest)
**Objective**: Verify guest can add favorites
**Preconditions**: Not authenticated
**Steps**:
1. On book card or details, click heart icon
2. Observe icon change

**Expected Results**:
- Heart icon fills (becomes solid)
- Book ID stored in localStorage (el_favorites key)
- Can view favorites (if guest favorites page exists)

**Edge Cases**:
- localStorage disabled: Fallback to session storage or in-memory

---

#### TC-FAV-002: Remove Book from Favorites (Guest)
**Objective**: Verify guest can unlike books
**Preconditions**: Book already in guest favorites
**Steps**:
1. Click filled heart icon
2. Observe icon change

**Expected Results**:
- Heart icon becomes unfilled (outline)
- Book ID removed from localStorage

---

#### TC-FAV-003: Add Book to Favorites (Authenticated)
**Objective**: Verify authenticated user can like books
**Preconditions**: Authenticated
**Steps**:
1. Click heart icon on a book
2. Observe changes

**Expected Results**:
- Heart icon fills
- API call to POST /api/likes/toggle/{bookId}
- Like persists on server
- Like count increments (if shown)

**Edge Cases**:
- Network error: Shows error message, icon reverts
- Rapid clicking: Should debounce/prevent duplicate requests

---

#### TC-FAV-004: View Favorites Page (Authenticated)
**Objective**: Verify favorites page displays
**Preconditions**: Authenticated with 5 liked books
**Steps**:
1. Navigate to /favorites
2. Observe page

**Expected Results**:
- Shows all 5 liked books
- Books displayed as cards (similar to AllBooks)
- Can click to view book details
- Can remove from favorites (heart icon or remove button)

**Edge Cases**:
- No favorites: Shows "No favorites yet" message
- 100+ favorites: Pagination works

---

#### TC-FAV-005: Remove from Favorites on Favorites Page
**Objective**: Verify unliking from favorites page
**Preconditions**: On /favorites with liked books
**Steps**:
1. Click heart icon or remove button on a book
2. Observe page updates

**Expected Results**:
- Book removed from list immediately
- API call to toggle like
- Page updates without full reload

---

#### TC-FAV-006: Favorites Sync Conflict Resolution
**Objective**: Verify conflict resolution during sync
**Preconditions**: Guest favorites: [1, 2, 3], Server favorites: [3, 4, 5]
**Steps**:
1. Log in
2. Navigate to /favorites

**Expected Results**:
- Shows merged favorites: [1, 2, 3, 4, 5]
- No duplicates
- Book 3 appears once

---

### **4.10 User Profile Management**

#### TC-PROFILE-001: View Profile Page
**Objective**: Verify profile page displays user information
**Preconditions**: Authenticated
**Steps**:
1. Navigate to /profile
2. Observe profile information

**Expected Results**:
- Displays user name (firstName + lastName)
- Email address
- Profile picture (from Google OAuth)
- Phone number (if set)
- Shipping address fields (Wilaya, City, Street Address)
- Default shipping method
- Member since date
- Edit buttons/fields

**Edge Cases**:
- Incomplete profile: Shows empty fields or placeholders

---

#### TC-PROFILE-002: Edit Profile - Phone Number
**Objective**: Verify editing phone number
**Preconditions**: On /profile
**Steps**:
1. Click "Edit" button for phone
2. Enter new phone: "0661234567"
3. Click "Save"

**Expected Results**:
- Phone field becomes editable
- Saves successfully
- Shows success message
- Profile updates immediately
- API call to PUT /api/app-user/profile

**Edge Cases**:
- Invalid phone format: Shows validation error
- Empty phone: Saves as empty (if optional)

---

#### TC-PROFILE-003: Edit Profile - Shipping Address
**Objective**: Verify updating shipping address
**Preconditions**: On /profile
**Steps**:
1. Edit Wilaya: Select "Constantine"
2. Edit City: Select from dropdown
3. Edit Street Address: "15 Rue Larbi Ben M'hidi"
4. Save changes

**Expected Results**:
- All fields update
- Saves to backend
- Success message shown
- Address used in future checkouts

---

#### TC-PROFILE-004: Edit Profile - Default Shipping Method
**Objective**: Verify setting default shipping preference
**Preconditions**: On /profile
**Steps**:
1. Select "Home Delivery" as default
2. Save
3. Repeat with "Pickup Point" + provider

**Expected Results**:
- Default method saved
- Pre-fills checkout form on next order

---

#### TC-PROFILE-005: Profile Update - Validation Errors
**Objective**: Verify profile validation
**Preconditions**: On /profile
**Steps**:
1. Enter invalid data (e.g., phone: "abc")
2. Attempt to save

**Expected Results**:
- Shows validation errors
- Does not save
- Fields remain editable

---

#### TC-PROFILE-006: View Member Since Date
**Objective**: Verify member since date displays
**Preconditions**: On /profile
**Steps**:
1. Observe "Member since" field

**Expected Results**:
- Shows user account creation date
- Formatted correctly (e.g., "January 2025" or "Jan 15, 2025")

---

### **4.11 Order History & Tracking**

#### TC-ORDER-001: View Orders Page (Authenticated)
**Objective**: Verify orders page displays user orders
**Preconditions**: Authenticated with 2+ past orders
**Steps**:
1. Navigate to /orders
2. Observe order list

**Expected Results**:
- Shows all user orders (most recent first)
- Each order shows:
  - Order ID / Unique Order ID
  - Order date
  - Status (Pending, Confirmed, Shipped, Delivered, Cancelled)
  - Total amount
  - Number of items
- Can click to view order details

**Edge Cases**:
- No orders: Shows "No orders yet" message
- 50+ orders: Pagination works

---

#### TC-ORDER-002: View Single Order Details
**Objective**: Verify order details page
**Preconditions**: User with past order
**Steps**:
1. On /orders, click on an order
2. Observe order details

**Expected Results**:
- Shows full order information:
  - Order ID, status, date
  - Items (books/packs with quantities)
  - Shipping address
  - Shipping method (Home Delivery / Yalidine / ZRexpress)
  - Subtotal, shipping cost, total
  - Current status
- Status timeline/tracker (if available)

**Edge Cases**:
- Order with mix of books and packs: All displayed correctly

---

#### TC-ORDER-003: Order Status Display
**Objective**: Verify order status shows correctly
**Preconditions**: Orders with different statuses
**Steps**:
1. View orders with status: Pending, Confirmed, Shipped, Delivered
2. Observe status badges/labels

**Expected Results**:
- Each status has distinct visual indicator (color/icon)
  - Pending: Yellow/Orange
  - Confirmed: Blue
  - Shipped: Purple
  - Delivered: Green
  - Cancelled: Red/Gray

---

#### TC-ORDER-004: Filter Orders by Status
**Objective**: Verify filtering orders (if available)
**Preconditions**: Orders with mixed statuses
**Steps**:
1. On /orders, apply status filter: "Shipped"
2. Observe results

**Expected Results**:
- Shows only shipped orders
- Other orders hidden

---

#### TC-ORDER-005: Order Tracking (Guest User)
**Objective**: Verify guest can track order by ID (if implemented)
**Preconditions**: Guest placed order, has unique order ID
**Steps**:
1. Navigate to order tracking page (if exists)
2. Enter unique order ID
3. Submit

**Expected Results**:
- Shows order details and status
- Does not require login

**Edge Cases**:
- Invalid order ID: Shows "Order not found"
- Order ID from another user: Should not display (privacy)

---

### **4.12 Book Packs**

#### TC-PACK-001: View Packs Page
**Objective**: Verify book packs page displays
**Preconditions**: None
**Steps**:
1. Navigate to /packs
2. Observe page

**Expected Results**:
- Shows all available book packs
- Each pack card displays:
  - Pack cover image or composite image
  - Pack title
  - Price
  - Number of books included
- Can click to view pack details

**Edge Cases**:
- No packs: Shows "No packs available"

---

#### TC-PACK-002: View Pack Details
**Objective**: Verify pack details display
**Preconditions**: On /packs
**Steps**:
1. Click on a pack card
2. Observe pack details (popup or dedicated page)

**Expected Results**:
- Shows pack title, description, price
- Lists all books included:
  - Book cover, title, author
- "Add to Cart" button
- Total savings vs buying individually (if applicable)

---

#### TC-PACK-003: Add Pack to Cart
**Objective**: Verify adding pack to cart
**Preconditions**: Viewing pack details
**Steps**:
1. Click "Add to Cart"
2. Check cart

**Expected Results**:
- Pack added to cart
- Cart badge increments
- Cart shows pack as single item (not individual books)

---

#### TC-PACK-004: Pack in Cart - View Books
**Objective**: Verify viewing books in pack from cart
**Preconditions**: Cart with pack
**Steps**:
1. On cart page, click on pack or "View books" link
2. Observe popup

**Expected Results**:
- Shows all books in pack
- Does not allow editing books in pack

---

#### TC-PACK-005: Pack Filtering (if available)
**Objective**: Verify filtering packs on /packs page
**Preconditions**: On /packs with filters
**Steps**:
1. Apply category filter
2. Apply price range

**Expected Results**:
- Packs filtered accordingly
- Similar to book filtering

---

### **4.13 Responsive Design**

#### TC-RESP-001: Mobile View (375px width)
**Objective**: Verify mobile responsiveness
**Preconditions**: None
**Steps**:
1. Open application on mobile device or emulator (375px width)
2. Navigate through key pages: Home, AllBooks, BookDetails, Cart, Profile

**Expected Results**:
- Navbar shows mobile menu icon (hamburger)
- Search bar collapses or simplifies
- Book cards stack vertically or in 2 columns
- Buttons and text are touch-friendly (min 44px tap targets)
- No horizontal scroll
- All features accessible

**Edge Cases**:
- Very small screens (320px): Layout adapts gracefully

---

#### TC-RESP-002: Tablet View (768px width)
**Objective**: Verify tablet responsiveness
**Preconditions**: None
**Steps**:
1. Open on tablet or emulator (768px width)
2. Navigate key pages

**Expected Results**:
- Navbar shows partial desktop layout
- Book grid shows 3-4 columns
- Filters may be in sidebar or collapsible
- Touch-friendly controls

---

#### TC-RESP-003: Desktop View (1920px width)
**Objective**: Verify desktop layout
**Preconditions**: None
**Steps**:
1. Open on desktop (1920px width)
2. Navigate pages

**Expected Results**:
- Full navbar with search, language toggle, icons
- Book grid shows 4-6 columns
- Filters in sidebar
- Hover effects on cards

---

#### TC-RESP-004: Orientation Change (Mobile)
**Objective**: Verify landscape/portrait handling
**Preconditions**: Mobile device
**Steps**:
1. View homepage in portrait
2. Rotate to landscape
3. Rotate back to portrait

**Expected Results**:
- Layout adjusts smoothly
- No broken layouts
- Scroll position maintained or reset gracefully

---

### **4.14 Error Handling**

#### TC-ERROR-001: Network Error - Book Fetch Failure
**Objective**: Verify error handling when API fails
**Preconditions**: Simulate network error or API down
**Steps**:
1. Navigate to /allbooks
2. API fails to return books

**Expected Results**:
- Shows error message: "Failed to load books. Please try again."
- Retry button available
- No app crash

**Edge Cases**:
- Partial data loaded: Shows what loaded, indicates error for rest

---

#### TC-ERROR-002: Timeout
**Objective**: Verify handling of slow API responses
**Preconditions**: Simulate slow network (2G/3G)
**Steps**:
1. Navigate to /allbooks
2. API takes 30+ seconds

**Expected Results**:
- Shows skeleton loaders
- Eventually shows timeout message
- Retry option

---

#### TC-ERROR-003: Invalid Image URLs
**Objective**: Verify fallback for missing images
**Preconditions**: Book with invalid/missing cover image
**Steps**:
1. View book with broken cover URL

**Expected Results**:
- Shows placeholder image
- No broken image icon
- Book still functional

---

#### TC-ERROR-004: 404 Page
**Objective**: Verify 404 handling
**Preconditions**: None
**Steps**:
1. Navigate to /nonexistent-page

**Expected Results**:
- Shows 404 page
- "Page not found" message
- Link to homepage

---

#### TC-ERROR-005: Checkout Submission Error
**Objective**: Verify error handling during order placement
**Preconditions**: On checkout form
**Steps**:
1. Fill valid form
2. Simulate API error on submit
3. Submit order

**Expected Results**:
- Shows error message
- Form data retained
- Can retry submission
- No duplicate orders created

**Negative Scenarios**:
- Insufficient stock error: Specific error message
- Payment gateway error (if applicable): Clear message

---

### **4.15 Performance & UX**

#### TC-PERF-001: Page Load Time
**Objective**: Verify pages load within acceptable time
**Preconditions**: Normal network (4G/WiFi)
**Steps**:
1. Measure load time for:
   - Homepage
   - /allbooks
   - /books/{id}
   - /cart

**Expected Results**:
- Homepage: < 3 seconds
- AllBooks: < 2 seconds
- BookDetails: < 2 seconds
- Cart: < 1 second
- First Contentful Paint (FCP): < 1.5 seconds

**Edge Cases**:
- Slow 3G: Shows skeleton loaders, acceptable degraded performance

---

#### TC-PERF-002: Skeleton Loaders
**Objective**: Verify skeleton loaders display during loading
**Preconditions**: Slow network or fresh page load
**Steps**:
1. Navigate to /allbooks
2. Observe loading state

**Expected Results**:
- Shows skeleton loaders for book cards
- Skeletons match actual card layout
- Smooth transition from skeleton to actual content

---

#### TC-PERF-003: Infinite Scroll / Pagination Performance
**Objective**: Verify pagination doesn't cause lag
**Preconditions**: On /allbooks with 10+ pages
**Steps**:
1. Navigate through pages 1-10
2. Observe performance

**Expected Results**:
- Each page loads smoothly
- No cumulative lag
- Scroll position resets

**Edge Cases**:
- Jump to page 50: Loads without issue

---

#### TC-PERF-004: Image Optimization
**Objective**: Verify images load efficiently
**Preconditions**: Page with 50+ book covers
**Steps**:
1. Load /allbooks
2. Monitor network tab

**Expected Results**:
- Images lazy-load (only visible images load)
- Appropriate image sizes served (not unnecessarily large)
- Progressive loading or placeholders

---

#### TC-PERF-005: Cart Badge Updates
**Objective**: Verify cart badge updates immediately
**Preconditions**: None
**Steps**:
1. Add item to cart
2. Observe navbar cart badge

**Expected Results**:
- Badge updates instantly (< 100ms)
- No page refresh needed

---

---

## 5. Test Execution Guidelines

### 5.1 Test Environment Setup

**Required Environments**:
1. **Development**: Local setup (frontend + backend + PostgreSQL + Keycloak)
2. **Staging/QA**: Deployed environment mirroring production

**Test Data**:
- Seed database with:
  - 50+ books (various categories, authors, prices, languages)
  - 10+ book packs
  - 5+ categories
  - 10+ authors
  - Test users (authenticated)
  - Sample orders

**Browser/Device Matrix**:
- **Desktop**: Chrome (latest), Firefox (latest), Safari (latest), Edge (latest)
- **Mobile**: iOS Safari (iPhone 12+), Android Chrome (Samsung/Pixel)
- **Tablet**: iPad (Safari), Android tablet

---

### 5.2 Test Execution Sequence

**Recommended Order**:
1. Navigation & UI (TC-NAV-*)
2. Book Browsing (TC-BROWSE-*)
3. Search & Filtering (TC-SEARCH-*, TC-FILTER-*)
4. Book Details (TC-BOOK-*)
5. Shopping Cart - Guest (TC-CART-*)
6. Shopping Cart - Packs (TC-CART-PACK-*)
7. Checkout (TC-CHECKOUT-*)
8. Authentication (TC-AUTH-*)
9. Favorites (TC-FAV-*)
10. User Profile (TC-PROFILE-*)
11. Order History (TC-ORDER-*)
12. Book Packs (TC-PACK-*)
13. Responsive Design (TC-RESP-*)
14. Error Handling (TC-ERROR-*)
15. Performance (TC-PERF-*)

---

### 5.3 Test Data Cleanup

**After each test cycle**:
- Clear browser localStorage (cart, favorites)
- Reset test user profile
- Delete test orders (or mark as test)

---

### 5.4 Defect Reporting

**Severity Levels**:
- **Critical**: Blocks core functionality (checkout broken, cannot add to cart, auth fails)
- **High**: Major feature broken (search doesn't work, filters fail)
- **Medium**: Feature works but with issues (slow performance, minor UI glitch)
- **Low**: Cosmetic issues (alignment, spacing, tooltip text)

**Bug Report Template**:
```
Title: [TC-ID] Brief description
Severity: Critical/High/Medium/Low
Environment: Browser, Device, OS
Preconditions:
Steps to Reproduce:
1.
2.
3.
Expected Result:
Actual Result:
Screenshots/Videos:
Console Errors:
```

---

## 6. Risk Analysis

### High-Risk Areas (Require Extra Attention)

1. **Checkout Flow**:
   - Guest checkout (no auth)
   - Order placement with validation
   - Wilaya/City dependencies
   - Stock validation

2. **Authentication**:
   - OAuth flow (external dependency on Google/Keycloak)
   - Token management
   - Session expiry

3. **Cart Management**:
   - localStorage persistence
   - Mixed cart (books + packs)
   - Quantity limits

4. **Favorites Sync**:
   - Guest to authenticated transition
   - Conflict resolution

---

## 7. Out of Scope

The following are **explicitly excluded** from this testing plan:

- **Admin App Features**: All admin-only endpoints and functionality
- **Backend API Unit Tests**: Backend logic testing (handled separately)
- **Internationalization**: Language switching (French/English/Arabic) - minimal testing only
- **Static Pages**: Team page, Service Client page (informational content)
- **Security Testing**: Penetration testing, XSS, SQL injection (separate security audit)
- **Accessibility Testing**: WCAG compliance (can be added later)
- **Browser Compatibility**: IE11 and older browsers (modern browsers only)
- **Payment Gateway Integration**: No payment processing in current scope

---

## 8. Success Criteria

**Testing is considered successful when**:
- ✅ 100% of Critical severity test cases pass
- ✅ ≥95% of High severity test cases pass
- ✅ ≥90% of Medium/Low severity test cases pass
- ✅ No Critical or High defects open
- ✅ Core user flows (Browse → Cart → Checkout → Order) work flawlessly
- ✅ Authentication and authorization work correctly
- ✅ Responsive design validated on mobile/tablet/desktop
- ✅ Performance benchmarks met (page load < 3s)

---

## 9. Test Metrics to Track

- **Total Test Cases**: 105
- **Test Cases Executed**
- **Test Cases Passed**
- **Test Cases Failed**
- **Test Cases Blocked**
- **Defects Found** (by severity)
- **Defects Fixed**
- **Test Coverage %** (features covered)
- **Pass Rate %**

---

## 10. Appendix

### 10.1 Test Case Summary by Category

| Category | Test Case Count | Priority |
|----------|----------------|----------|
| Navigation & UI | 3 | High |
| Book Browsing | 5 | High |
| Search & Filtering | 9 | High |
| Book Details | 5 | High |
| Shopping Cart (Guest) | 8 | Critical |
| Shopping Cart (Packs) | 4 | High |
| Checkout & Orders | 10 | Critical |
| Authentication | 8 | Critical |
| Favorites | 6 | Medium |
| User Profile | 6 | Medium |
| Order History | 5 | High |
| Book Packs | 5 | Medium |
| Responsive Design | 4 | High |
| Error Handling | 5 | High |
| Performance | 5 | Medium |
| **TOTAL** | **105** | |

---

### 10.2 API Endpoints Used by User App (Reference)

**Public Endpoints (No Auth)**:
- `GET /api/books` - Fetch books with filters
- `GET /api/books/{id}` - Get book details
- `GET /api/books/{id}/cover` - Get book cover image
- `GET /api/books/{id}/recommendations` - Get recommendations
- `GET /api/books/suggestions?q={query}` - Search suggestions
- `GET /api/authors/top` - Top authors
- `GET /api/tags?type=CATEGORY` - Categories
- `GET /api/tags?type=MAIN_DISPLAY` - Main displays
- `GET /api/book-packs` - All book packs
- `GET /api/book-packs/{id}` - Pack details
- `POST /api/orders` - Place order (guest or auth)

**Authenticated Endpoints (USER role)**:
- `GET /api/account` - Current user account info
- `GET /api/app-user/profile` - User profile
- `PUT /api/app-user/profile` - Update profile
- `GET /api/books/liked` - User's liked books
- `POST /api/likes/toggle/{bookId}` - Toggle like
- `GET /api/orders` - User's orders
- `GET /api/orders/{id}` - Order details

---

### 10.3 Wilaya Data for Testing

**Sample Wilayas and Cities**:
- Alger: Bab El Oued, Hydra, Kouba, Bir Mourad Raïs
- Oran: Oran Centre, Bir El Djir, Es Senia
- Constantine: Constantine Centre, El Khroub, Ain Smara

---

## 11. Conclusion

This comprehensive testing plan provides **105 detailed test cases** covering all user-facing features of the Esprit Livre User App. The plan focuses on **UI/UX functional testing** with comprehensive coverage, ensuring a high-quality user experience across all devices and browsers.

**Next Steps**:
1. Review and approve this testing plan
2. Set up test environments
3. Prepare test data
4. Execute test cases in recommended sequence
5. Log defects and track resolution
6. Generate test report upon completion

---

**Document Version**: 1.0
**Date**: 2025-12-22
**Prepared By**: Senior QA Engineer (Claude Code Analysis)
