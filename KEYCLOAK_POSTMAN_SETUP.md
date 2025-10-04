# Keycloak & Postman OAuth2 Setup Guide

## 🎯 Quick Start

### Step 1: Configure Keycloak

1. **Access Keycloak Admin Console**
   - URL: http://localhost:9080
   - Click **Administration Console**
   - Login: `admin` / `admin`

2. **Create `jhipster` Realm**
   - Click dropdown (top-left, says "master")
   - Click **Create Realm**
   - Realm name: `jhipster`
   - Click **Create**

3. **Create `web_app` Client**
   - Go to **Clients** → **Create client**
   - **Client ID**: `web_app`
   - Click **Next**
   - **Client authentication**: ON ✅
   - **Authentication flow**:
     - ✅ Standard flow
     - ✅ Direct access grants ⚠️ **REQUIRED for Postman**
   - Click **Next**
   - **Valid redirect URIs**:
     - `http://localhost:8080/*`
     - `https://oauth.pstmn.io/v1/callback`
   - **Web origins**: `http://localhost:8080`
   - Click **Save**

4. **Get/Set Client Secret**
   - Go to **Clients** → `web_app` → **Credentials** tab
   - Copy the secret (or regenerate if different from config)
   - Current secret in config: `ZR0ibYlwv5tExzTX4QGsFqieCQ8AKgc3`

5. **Create Realm Roles**
   - Go to **Realm roles** → **Create role**
   - Create: `ROLE_USER`
   - Create: `ROLE_ADMIN`

6. **Create Test Users**

   **Admin User:**
   - **Users** → **Add user**
   - Username: `admin`
   - Email: `admin@localhost`
   - First name: `Admin`
   - Last name: `User`
   - Email verified: ON
   - Click **Create**
   - **Credentials** tab:
     - Set password: `admin`
     - Temporary: OFF
     - Click **Save**
   - **Role mappings** tab:
     - Click **Assign role**
     - Select: `ROLE_USER`, `ROLE_ADMIN`
     - Click **Assign**

   **Regular User:**
   - Repeat with:
     - Username: `user`
     - Password: `user`
     - Email: `user@localhost`
     - Roles: `ROLE_USER` only

---

### Step 2: Import Postman Collection

1. Open Postman
2. Click **Import**
3. Select: `esprit livre.postman_collection.json`
4. Collection imported with all variables configured ✅

---

### Step 3: Test Authentication

#### **Test 1: Get Admin Token**

1. Open **Authentication** folder
2. Run: **Get Token (Password Grant) - Admin**
3. Check response:
   ```json
   {
     "access_token": "eyJhbGc...",
     "token_type": "Bearer",
     "expires_in": 300
   }
   ```
4. ✅ Token automatically saved to `{{access_token}}` variable

#### **Test 2: Verify Authenticated Access**

1. Run: **Get Current User Account**
2. Should return:
   ```json
   {
     "login": "admin",
     "authorities": ["ROLE_USER", "ROLE_ADMIN"],
     "email": "admin@localhost"
   }
   ```

#### **Test 3: Try with Regular User**

1. Run: **Get Token (Password Grant) - User**
2. Run: **Get Current User Account**
3. Should return user with only `ROLE_USER`

---

## 🔐 How Authentication Works

### Collection-Level Auth
- All requests inherit Bearer token authentication
- Token stored in: `{{access_token}}` collection variable
- Auto-injected in `Authorization: Bearer {{access_token}}` header

### Override Auth for Specific Requests
- Authentication requests have `"auth": { "type": "noauth" }`
- This prevents circular auth (can't use token to get token)

---

## 📋 Available Endpoints

### Public (No Auth Required)
- `GET /api/books` - List books (currently public)
- `GET /api/books/suggestions` - Search suggestions
- `GET /api/tags` - Get tags by type

### Authenticated (USER role)
- `GET /api/account` - Get current user
- `GET /api/likes` - Get user's liked books
- `POST /api/likes` - Like a book
- `DELETE /api/likes/{id}` - Unlike a book
- `GET /api/orders` - Get user's orders
- `POST /api/orders` - Create order

### Admin Only
- `POST /api/books` - Create book
- `PUT /api/books/{id}` - Update book
- `DELETE /api/books/{id}` - Delete book
- All `/api/admin/**` endpoints

---

## 🧪 Testing Different Roles

### Test as Admin:
1. Run: **Get Token (Password Grant) - Admin**
2. Test any endpoint (all should work)

### Test as User:
1. Run: **Get Token (Password Grant) - User**
2. Test user endpoints (should work)
3. Test admin endpoints (should get 403 Forbidden)

### Test Unauthenticated:
1. Clear `{{access_token}}` variable
2. Test protected endpoints (should get 401 Unauthorized)

---

## 🛠 Troubleshooting

### "401 Unauthorized" on all requests
- Check if token is saved: `{{access_token}}` variable
- Token might be expired (5 min default) → Get new token

### "User not found" when getting token
- Verify users exist in Keycloak
- Check username/password match

### "invalid_client" error
- Verify client_id: `web_app`
- Verify client_secret matches Keycloak
- Check "Direct Access Grants" is enabled

### "403 Forbidden" on admin endpoints
- Verify user has `ROLE_ADMIN` role in Keycloak
- Check token roles: Decode JWT at https://jwt.io

---

## 🔑 Token Details

**Token Type**: JWT (JSON Web Token)
**Default Expiry**: 5 minutes
**Refresh Token**: Available (use for production apps)

**To decode token** (for debugging):
1. Copy `access_token` from Postman variable
2. Go to https://jwt.io
3. Paste token
4. Check `realm_access.roles` contains your roles

---

## ✅ Next Steps

After authentication is working:

1. **Remove public access** from `/api/books` and `/api/tags`
2. **Add role-based security** to endpoints
3. **Implement user profile management**
4. **Add custom user attributes** in Keycloak
