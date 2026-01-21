# Esprit Livre Production Deployment Guide

## Prerequisites

- VPS with Ubuntu 22.04+ (Contabo Cloud VPS 10 with 8GB RAM)
- Domain: `espritlivre.com` with DNS configured
- SSL certificates (fullchain.pem and privkey.pem)
- Docker and Docker Compose installed

## DNS Configuration

Point these records to your VPS IP address:

| Type | Name | Value |
|------|------|-------|
| A | @ | YOUR_VPS_IP |
| A | www | YOUR_VPS_IP |
| A | api | YOUR_VPS_IP |
| A | admin | YOUR_VPS_IP |
| A | auth | YOUR_VPS_IP |

## VPS Initial Setup

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose plugin
sudo apt install docker-compose-plugin -y

# Logout and login again to apply docker group
exit
```

## Project Deployment

### 1. Clone the Repository

```bash
# Create project directory
mkdir -p ~/espritlivre
cd ~/espritlivre

# Clone all three repositories (api, user, admin)
git clone https://github.com/YOUR_USERNAME/esprit-livre-api.git api
git clone https://github.com/YOUR_USERNAME/esprit-livre-user.git user
git clone https://github.com/YOUR_USERNAME/esprit-livre-admin.git admin
```

### 2. Configure Environment

```bash
cd ~/espritlivre/api

# Copy and edit environment file
cp .env.example .env
nano .env
```

Fill in all the required values in `.env`:
- Database passwords (generate strong passwords)
- Keycloak admin password
- OAuth client secret (from Keycloak)
- Shipping API keys
- Email credentials

### 3. Setup SSL Certificates

```bash
# Create SSL directory
mkdir -p ~/espritlivre/api/nginx/ssl

# Copy your certificates
cp /path/to/your/fullchain.pem ~/espritlivre/api/nginx/ssl/
cp /path/to/your/privkey.pem ~/espritlivre/api/nginx/ssl/

# Set proper permissions
chmod 644 ~/espritlivre/api/nginx/ssl/fullchain.pem
chmod 600 ~/espritlivre/api/nginx/ssl/privkey.pem
```

### 4. Initialize Keycloak Database

Before first deployment, create the Keycloak database:

```bash
# Start only PostgreSQL first
docker compose -f docker-compose.prod.yml up -d postgres

# Wait for it to be ready
sleep 10

# Create Keycloak database
docker exec -it espritlivre-postgres psql -U espritlivre -c "CREATE DATABASE keycloak;"
```

### 5. Deploy the Application

```bash
cd ~/espritlivre/api

# Build and start all services
docker compose -f docker-compose.prod.yml up -d --build

# Watch the logs
docker compose -f docker-compose.prod.yml logs -f
```

### 6. Verify Deployment

Check each service:

```bash
# Check all containers are running
docker compose -f docker-compose.prod.yml ps

# Test endpoints
curl -k https://espritlivre.com
curl -k https://admin.espritlivre.com
curl -k https://api.espritlivre.com/management/health
curl -k https://auth.espritlivre.com
```

## Post-Deployment Configuration

### Configure Keycloak

1. Access Keycloak admin: `https://auth.espritlivre.com`
2. Login with admin credentials from `.env`
3. Go to **Realm Settings** → **jhipster** realm
4. Update **Frontend URL** to `https://auth.espritlivre.com`
5. Go to **Clients** → **web_app**
6. Update redirect URIs:
   - `https://espritlivre.com/*`
   - `https://admin.espritlivre.com/*`
   - `https://api.espritlivre.com/*`
7. Update Web Origins:
   - `https://espritlivre.com`
   - `https://admin.espritlivre.com`

### Migrate Existing Data (Optional)

If migrating from development:

```bash
# Export from dev
pg_dump -U postgres el-dev-db > backup.sql

# Copy to VPS and import
docker cp backup.sql espritlivre-postgres:/tmp/
docker exec -it espritlivre-postgres psql -U espritlivre -d espritlivre -f /tmp/backup.sql
```

## Useful Commands

```bash
# View logs
docker compose -f docker-compose.prod.yml logs -f api
docker compose -f docker-compose.prod.yml logs -f keycloak
docker compose -f docker-compose.prod.yml logs -f nginx

# Restart a service
docker compose -f docker-compose.prod.yml restart api

# Rebuild and restart a service
docker compose -f docker-compose.prod.yml up -d --build api

# Stop all services
docker compose -f docker-compose.prod.yml down

# Stop and remove volumes (CAUTION: destroys data)
docker compose -f docker-compose.prod.yml down -v

# Check resource usage
docker stats

# Access API container shell
docker exec -it espritlivre-api sh

# Access database
docker exec -it espritlivre-postgres psql -U espritlivre -d espritlivre
```

## Updating the Application

```bash
cd ~/espritlivre

# Pull latest changes
cd api && git pull && cd ..
cd user && git pull && cd ..
cd admin && git pull && cd ..

# Rebuild and restart
cd api
docker compose -f docker-compose.prod.yml up -d --build
```

## Backup Strategy

### Database Backup

```bash
# Create backup script
cat > ~/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=~/backups
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup PostgreSQL
docker exec espritlivre-postgres pg_dump -U espritlivre espritlivre > $BACKUP_DIR/db_$DATE.sql
docker exec espritlivre-postgres pg_dump -U espritlivre keycloak > $BACKUP_DIR/keycloak_$DATE.sql

# Backup media files
docker cp espritlivre-api:/app/media $BACKUP_DIR/media_$DATE

# Keep only last 7 days
find $BACKUP_DIR -mtime +7 -delete

echo "Backup completed: $DATE"
EOF

chmod +x ~/backup.sh

# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * ~/backup.sh >> ~/backup.log 2>&1") | crontab -
```

## Troubleshooting

### Container won't start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs api

# Check if port is in use
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :443
```

### Database connection issues

```bash
# Check if postgres is healthy
docker compose -f docker-compose.prod.yml ps postgres

# Test connection from API container
docker exec -it espritlivre-api sh -c "curl -v postgres:5432"
```

### Keycloak not accessible

```bash
# Check Keycloak logs
docker compose -f docker-compose.prod.yml logs keycloak

# Verify Keycloak is healthy
docker exec -it espritlivre-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password YOUR_PASSWORD
```

### SSL certificate issues

```bash
# Verify certificates
openssl x509 -in nginx/ssl/fullchain.pem -text -noout

# Check nginx can read them
docker exec -it espritlivre-nginx nginx -t
```

## Security Checklist

- [ ] Strong passwords in `.env` (use `openssl rand -base64 32`)
- [ ] SSL certificates properly configured
- [ ] Firewall configured (only 80, 443 open)
- [ ] Regular backups configured
- [ ] `.env` file not in version control
- [ ] Keycloak admin password changed from default
- [ ] API rate limiting enabled in nginx
