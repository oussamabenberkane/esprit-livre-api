# Frontend Authentication Implementation - Spring OAuth2 Login Flow

I need to implement OAuth2 authentication for my React frontend to integrate with my Spring Boot + Keycloak backend using the **Authorization Code Flow**.

## BACKEND CONFIGURATION

- **Backend URL:** http://localhost:8080
- **Keycloak URL:** http://localhost:9080/realms/jhipster
- **Client ID:** web_app
- **OAuth2 Flow:** Authorization Code with PKCE (handled by Spring Security)
- **Session Management:** Backend creates httpOnly session cookie after successful OAuth2 login
- **Token Refresh:** Backend automatically refreshes tokens via OAuth2RefreshTokensWebFilter (frontend doesn't need to handle this)
- **CSRF Protection:** Required for POST/PUT/DELETE/PATCH requests (except `/api/orders` and `/api/contact`)
- **CORS:** Enabled for http://localhost:5173 in development

## AUTHENTICATION FLOW

1. User clicks "Login" button → Frontend redirects to `http://localhost:8080/oauth2/authorization/oidc`
2. Backend redirects browser to Keycloak login page at `http://localhost:9080`
3. User enters credentials at Keycloak hosted login page
4. Keycloak validates credentials and redirects back to backend with authorization code
5. Backend exchanges authorization code for access/refresh tokens (transparent to frontend)
6. Backend creates HTTP session and sets httpOnly session cookie
7. Backend redirects browser to frontend success URL (e.g., `http://localhost:5173/books`)
8. Frontend makes API calls - session cookie is sent automatically with `credentials: 'include'`
9. Backend validates session on each request and auto-refreshes expired tokens

## BACKEND API ENDPOINTS

### Check Authentication Status
```
GET /api/authenticate
Response: 204 No Content (authenticated) OR 401 Unauthorized (not authenticated)
```

### Get Current User Profile
```
GET /api/account
Response: {
  "login": "admin",
  "firstName": "Administrator",
  "lastName": "User",
  "email": "admin@localhost",
  "imageUrl": null,
  "activated": true,
  "langKey": "en",
  "authorities": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### Logout
```
POST /api/logout
Response: {
  "logoutUrl": "http://localhost:9080/realms/jhipster/protocol/openid-connect/logout?id_token_hint=..."
}
```
Frontend must redirect to this URL to complete Keycloak logout.

### Get Keycloak Configuration (optional, for debugging)
```
GET /api/auth-info
Response: {
  "issuer-uri": "http://localhost:9080/realms/jhipster",
  "client-id": "web_app"
}
```

## CSRF TOKEN HANDLING

1. Backend sends CSRF token in cookie named `XSRF-TOKEN` (not httpOnly - readable by JavaScript)
2. Frontend must read this cookie and include its value in `X-XSRF-TOKEN` header for state-changing requests
3. **Exemptions:** POST `/api/orders` and POST `/api/contact` do NOT require CSRF tokens
4. CSRF token must be included in: POST, PUT, DELETE, PATCH requests (except exempted endpoints)

## CURRENT FRONTEND SETUP

- **Framework:** React + Vite
- **Dev Server:** http://localhost:5173
- **API Service:** src/services/booksApi.js (axios instance with Bearer token interceptor - needs update)
- **Login Page:** src/pages/Login.jsx (has UI with email/password fields - needs complete replacement)
- **Books Page:** src/pages/Books.jsx (already working, should not be modified)
- **Books Table:** src/components/books/BooksTable.jsx (already working, should not be modified)

## IMPLEMENTATION REQUIREMENTS

### 1. Create Authentication API Service

**File:** src/services/authApi.js

Implement functions:
- `checkAuth()` - Calls GET /api/authenticate to check if user is logged in
- `getCurrentUser()` - Calls GET /api/account to get user profile
- `logout()` - Calls POST /api/logout and redirects to Keycloak logout URL
- Use axios with proper configuration (baseURL: http://localhost:8080, withCredentials: true)

### 2. Update Books API Service

**File:** src/services/booksApi.js (UPDATE EXISTING FILE)

Current state:
- Already has axios instance configured
- Already has Bearer token interceptor (can be removed since we're using session cookies)

Required changes:
- Add `withCredentials: true` to axios instance config (enables session cookie transmission)
- Add CSRF token request interceptor:
  - Read `XSRF-TOKEN` cookie value
  - Add `X-XSRF-TOKEN` header for POST/PUT/DELETE/PATCH requests
  - Skip CSRF for `/api/orders` and `/api/contact` endpoints
- Add 401/403 response interceptor:
  - Clear auth state from AuthContext
  - Redirect to `/login` page
- Remove Bearer token interceptor (no longer needed with session-based auth)

### 3. Create Authentication Context

**File:** src/contexts/AuthContext.jsx

Implement AuthProvider component with state:
- `user` - Current user object from /api/account (null if not authenticated)
- `isAuthenticated` - Boolean flag
- `isLoading` - Boolean flag for initial auth check
- `login()` - Redirects to `http://localhost:8080/oauth2/authorization/oidc`
- `logout()` - Calls authApi.logout() which redirects to Keycloak logout
- `checkAuthStatus()` - Calls authApi.checkAuth() and authApi.getCurrentUser()

Context initialization:
- On mount, call checkAuthStatus() to restore auth state
- Handle the case where user returns from Keycloak redirect (successful login)
- Set isLoading to false after initial check completes

Export useAuth() hook for consuming components.

### 4. Replace Login Page Logic

**File:** src/pages/Login.jsx (REPLACE EXISTING LOGIC)

Current state:
- Has email and password input fields
- Has mock authentication on lines 24-28
- Form submission handler needs complete replacement

Required changes:
- **REMOVE** all mock authentication logic
- **REMOVE** email and password state (not used in OAuth2 flow)
- **REPLACE** form with simple "Login with Keycloak" button or auto-redirect
- **Option 1 (Recommended):** Single button that calls `login()` from AuthContext
- **Option 2:** Auto-redirect to OAuth2 endpoint on component mount (no form needed)
- Show loading state while redirecting
- Remove email/password fields entirely (OAuth2 login happens at Keycloak, not in your app)

### 5. Create Protected Route Component

**File:** src/components/ProtectedRoute.jsx

Implement route guard:
- Check `isAuthenticated` from AuthContext
- If not authenticated, redirect to `/login`
- If authenticated, render children
- Show loading state while auth is being checked

### 6. Update App Router

**File:** src/App.jsx (UPDATE EXISTING FILE)

Required changes:
- Wrap entire app with `<AuthProvider>`
- Wrap routes that require authentication with `<ProtectedRoute>`
- Keep `/login` route public (not wrapped in ProtectedRoute)
- Example protected routes: `/books`, `/admin/*`, `/account`

### 7. Add CSRF Cookie Utility

**File:** src/utils/cookies.js

Implement utility function:
- `getCookie(name)` - Reads cookie value by name from document.cookie
- Used by axios interceptor to read `XSRF-TOKEN`

## CSRF INTERCEPTOR IMPLEMENTATION EXAMPLE

```javascript
// In src/services/booksApi.js
import { getCookie } from '../utils/cookies';

// Request interceptor for CSRF token
api.interceptors.request.use(config => {
  // Add CSRF token for state-changing requests
  if (['post', 'put', 'delete', 'patch'].includes(config.method?.toLowerCase())) {
    // Check if endpoint is exempt from CSRF
    const isExempt = config.url === '/api/orders' || config.url === '/api/contact';

    if (!isExempt) {
      const csrfToken = getCookie('XSRF-TOKEN');
      if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
      }
    }
  }
  return config;
});

// Response interceptor for 401/403
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Redirect to login page
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

## AXIOS CONFIGURATION REQUIREMENTS

All axios instances must include:
```javascript
{
  baseURL: 'http://localhost:8080',
  withCredentials: true, // CRITICAL: Enables session cookie transmission
  headers: {
    'Content-Type': 'application/json'
  }
}
```

## DELIVERABLES CHECKLIST

- [ ] Create src/services/authApi.js - Authentication API calls
- [ ] Create src/utils/cookies.js - Cookie reading utility
- [ ] Update src/services/booksApi.js - Add CSRF interceptor, remove Bearer token interceptor, add withCredentials
- [ ] Create src/contexts/AuthContext.jsx - Auth state management
- [ ] Update src/pages/Login.jsx - Replace with OAuth2 redirect (remove email/password form)
- [ ] Create src/components/ProtectedRoute.jsx - Route guard
- [ ] Update src/App.jsx - Add AuthProvider and ProtectedRoute wrappers

## CONSTRAINTS AND REQUIREMENTS

- **DO NOT** implement username/password form submission - OAuth2 login happens at Keycloak hosted page
- **DO NOT** implement manual token refresh - backend handles this automatically
- **DO NOT** store tokens in localStorage - use httpOnly session cookies (automatic with withCredentials: true)
- **DO NOT** modify src/pages/Books.jsx or src/components/books/BooksTable.jsx
- **DO** use simple, maintainable code
- **DO** add proper error handling and user feedback (loading states, error messages)
- **DO** ensure session cookie is sent with every request (withCredentials: true)
- **DO** read and include CSRF token for state-changing requests

## USER EXPERIENCE FLOW

1. User visits app → AuthContext checks authentication status
2. If not authenticated, user is shown login page
3. User clicks "Login" → Redirected to Keycloak login page (leaves your app temporarily)
4. User enters credentials at Keycloak
5. Keycloak redirects back to your app (e.g., http://localhost:5173/books)
6. AuthContext detects authentication and fetches user profile
7. User can now access protected pages
8. All API calls automatically include session cookie
9. User clicks "Logout" → Redirected to Keycloak logout → Session cleared → Redirected back to login page

## IMPORTANT NOTES

- The login form with email/password fields should be **completely removed** - it's not compatible with OAuth2 Authorization Code Flow
- Replace it with a simple "Login with Keycloak" button or auto-redirect
- Users will enter credentials at Keycloak's hosted login page, not in your React app
- Session cookies are httpOnly and secure - JavaScript cannot read them (this is correct and secure)
- Backend automatically refreshes tokens - frontend has zero token management responsibility
- CSRF tokens are in a separate cookie (XSRF-TOKEN) that JavaScript CAN read (by design for SPA compatibility)

## TESTING CHECKLIST

After implementation, verify:
- [ ] Clicking login redirects to Keycloak login page
- [ ] After Keycloak login, user is redirected back to app
- [ ] User profile is displayed correctly
- [ ] Protected routes are accessible after login
- [ ] API calls to /api/books work (GET requests)
- [ ] Admin operations work with CSRF token (POST/PUT/DELETE to /api/books)
- [ ] 401/403 errors redirect to login page
- [ ] Logout clears session and redirects to Keycloak logout
- [ ] Refreshing page maintains authentication state
- [ ] Session cookie is sent with every request (check DevTools Network tab)
- [ ] CSRF token is sent with POST/PUT/DELETE requests (check DevTools Network tab)

Please implement this complete OAuth2 authentication solution following Spring Security's Authorization Code Flow pattern.
