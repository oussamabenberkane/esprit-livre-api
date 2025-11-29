# Google OAuth Configuration for Keycloak

This realm configuration file (`jhipster-realm.json`) has been pre-configured with Google OAuth integration for Esprit Livre.

## ✅ What's Already Configured

### 1. **web_app Client Settings**
- ✅ PKCE enabled (`pkce.code.challenge.method`: S256)
- ✅ Public client (no client secret required in frontend)
- ✅ Standard flow enabled
- ✅ Direct access grants disabled
- ✅ Redirect URIs include `http://localhost:5173/auth/callback`
- ✅ Post logout redirect URIs configured

### 2. **Google Identity Provider**
- ✅ Provider alias: `google`
- ✅ Display name: "Esprit Livre Google Provider"
- ✅ Trust email: ON
- ✅ Store tokens: ON
- ✅ Default scopes: `openid profile email`

### 3. **Identity Provider Mappers**
- ✅ `email` → user attribute `email`
- ✅ `given_name` → user attribute `firstName`
- ✅ `family_name` → user attribute `lastName`
- ✅ `picture` → user attribute `picture`

---

## 🔧 Required: Add Your Google OAuth Credentials

**Before starting Keycloak**, you MUST update the Google OAuth credentials in the realm config file:

### Step 1: Open the Realm Config File

```bash
cd c:\work\Esprit Livre\el-api\src\main\docker\realm-config
# Edit jhipster-realm.json
```

### Step 2: Find and Replace Google Credentials

Search for these lines (around line 1418-1419):

```json
"clientId": "YOUR_GOOGLE_CLIENT_ID_HERE",
"clientSecret": "YOUR_GOOGLE_CLIENT_SECRET_HERE",
```

Replace with your actual Google OAuth credentials from Google Cloud Console:

```json
"clientId": "48309858472-ulc9g73dbu58ka3vs7836iqdk15kdmsn.apps.googleusercontent.com",
"clientSecret": "YOUR_ACTUAL_CLIENT_SECRET",
```

### Step 3: Save the File

Save `jhipster-realm.json` after making the changes.

---

## 🚀 Starting Keycloak

After updating the credentials, start Keycloak:

```bash
cd c:\work\Esprit Livre\el-api
docker-compose up -d espritlivre-keycloak
```

Keycloak will automatically import the realm configuration on first startup.

---

## 🔍 Verifying the Configuration

1. **Access Keycloak Admin Console**
   - URL: http://localhost:9080/admin
   - Username: `admin`
   - Password: `admin`

2. **Verify Google IDP**
   - Navigate to: **Identity Providers**
   - You should see: **Esprit Livre Google Provider** (enabled)
   - Click on it to verify:
     - Alias: `google`
     - Client ID and Secret are populated
     - Store Tokens: ON
     - Trust Email: ON

3. **Verify web_app Client**
   - Navigate to: **Clients → web_app**
   - **Settings** tab:
     - Client authentication: OFF
     - Standard flow enabled: ON
     - Direct access grants enabled: OFF
   - **Advanced** section:
     - Proof Key for Code Exchange Code Challenge Method: **S256**

4. **Verify Mappers**
   - Navigate to: **Identity Providers → google → Mappers**
   - You should see 4 mappers:
     - email
     - firstName
     - lastName
     - picture

---

## 📝 Google Cloud Console Configuration

Make sure your Google OAuth 2.0 Client ID has these settings:

### Authorized JavaScript origins:
```
http://localhost:9080
```

### Authorized redirect URIs:
```
http://localhost:9080/realms/jhipster/broker/google/endpoint
```

**Note:** When deploying to production, add your production URLs to both the Google Cloud Console AND update the realm configuration.

---

## 🔄 Updating Configuration After Keycloak is Running

If Keycloak is already running and you need to update the Google credentials:

### Option 1: Via Admin Console (Recommended)
1. Go to http://localhost:9080/admin
2. Navigate to **Identity Providers → google**
3. Update Client ID and Client Secret
4. Click **Save**

### Option 2: Restart Keycloak with Updated Realm Config
1. Stop Keycloak: `docker-compose stop espritlivre-keycloak`
2. Remove the Keycloak volume (this will reset all data):
   ```bash
   docker-compose down -v
   ```
3. Update `jhipster-realm.json` with your credentials
4. Start Keycloak again: `docker-compose up -d espritlivre-keycloak`

---

## ✅ Testing the Integration

1. **Start the frontend:**
   ```bash
   cd c:\work\Esprit Livre\el-front\el-user-app
   npm run dev
   ```

2. **Navigate to:** http://localhost:5173/auth

3. **Click "Continue with Google"**

4. **Expected Flow:**
   - Redirect to Google sign-in
   - Authenticate with Google
   - Redirect back to your app
   - User logged in successfully

---

## 📄 What Happens on First Run

When you start Keycloak for the first time with this configuration:

1. Keycloak reads `jhipster-realm.json`
2. Creates the `jhipster` realm with all settings
3. Configures the `web_app` client with PKCE
4. Sets up Google Identity Provider
5. Adds attribute mappers for user data

**No manual configuration needed in Keycloak Admin Console!**

---

## 🚨 Important Notes

1. **Google Client Secret Security:**
   - The client secret in this file is for the Keycloak server (backend)
   - It's NOT exposed to the frontend
   - Keep this file secure and don't commit real secrets to public repos

2. **Production Deployment:**
   - Update redirect URIs in Google Cloud Console
   - Update `jhipster-realm.json` with production URLs
   - Use environment variables for sensitive data in production

3. **Realm Import:**
   - This configuration only imports on first startup
   - If Keycloak already has data, you'll need to clear volumes first

---

## 📚 Additional Resources

- [Keycloak Google Identity Provider Docs](https://www.keycloak.org/docs/latest/server_admin/#_google)
- [OAuth 2.0 PKCE](https://oauth.net/2/pkce/)
- [Google OAuth 2.0 Setup](https://developers.google.com/identity/protocols/oauth2)

---

## 🐛 Troubleshooting

### "Redirect URI mismatch" error
- Verify Google Cloud Console redirect URI matches exactly:
  `http://localhost:9080/realms/jhipster/broker/google/endpoint`

### "Invalid client" error
- Check that clientId and clientSecret are correctly set in `jhipster-realm.json`
- Verify Google OAuth client is enabled in Google Cloud Console

### Configuration not loading
- Ensure `jhipster-realm.json` is in the correct location
- Check Docker volume mounts in `docker-compose.yml`
- Clear Keycloak volumes and restart

### 401 error on token exchange
- This shouldn't happen with this config, but if it does:
- Verify PKCE is enabled (S256)
- Check that client authentication is OFF
- Ensure redirect URIs match exactly
