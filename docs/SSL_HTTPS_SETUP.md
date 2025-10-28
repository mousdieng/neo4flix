# SSL/HTTPS Setup Guide for Neo4flix

This guide explains how to secure Neo4flix with HTTPS using SSL/TLS certificates.

## Table of Contents
1. [Development Setup (Self-Signed Certificates)](#development-setup)
2. [Production Setup (Let's Encrypt)](#production-setup)
3. [Using Nginx as Reverse Proxy](#nginx-reverse-proxy)
4. [Direct Spring Boot SSL](#spring-boot-ssl)
5. [Testing HTTPS Configuration](#testing)

---

## Development Setup (Self-Signed Certificates)

### 1. Generate Self-Signed Certificates

Self-signed certificates have already been generated in `ssl-certs/` directory:

```bash
cd ssl-certs/
openssl req -x509 -newkey rsa:4096 \
  -keyout neo4flix-key.pem \
  -out neo4flix-cert.pem \
  -days 365 -nodes \
  -subj "/C=US/ST=State/L=City/O=Neo4flix/OU=Development/CN=localhost"
```

### 2. Convert to PKCS12 for Spring Boot

```bash
openssl pkcs12 -export \
  -in neo4flix-cert.pem \
  -inkey neo4flix-key.pem \
  -out neo4flix-keystore.p12 \
  -name neo4flix \
  -passout pass:neo4flix123
```

### 3. Trust the Certificate (Optional for browsers)

**Chrome/Edge:**
1. Navigate to `chrome://settings/certificates`
2. Go to "Authorities" tab
3. Import `neo4flix-cert.pem`
4. Trust for identifying websites

**Firefox:**
1. Navigate to `about:preferences#privacy`
2. Click "View Certificates"
3. Import `neo4flix-cert.pem`
4. Trust for identifying websites

---

## Production Setup (Let's Encrypt)

### Prerequisites
- Domain name pointing to your server
- Ports 80 and 443 open in firewall
- Nginx installed
- Certbot installed

### 1. Install Certbot

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install certbot python3-certbot-nginx
```

**CentOS/RHEL:**
```bash
sudo yum install certbot python3-certbot-nginx
```

**Using Snap:**
```bash
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

### 2. Obtain SSL Certificate

Replace `your-domain.com` with your actual domain:

```bash
sudo certbot --nginx -d neo4flix.com -d www.neo4flix.com
```

Follow the prompts:
1. Enter email address
2. Agree to terms
3. Choose redirect options (recommended: redirect HTTP to HTTPS)

### 3. Auto-Renewal Setup

Certbot creates a systemd timer for auto-renewal. Verify it:

```bash
sudo systemctl status certbot.timer
sudo certbot renew --dry-run
```

### 4. Update Nginx Configuration

Certbot automatically updates your Nginx configuration, but verify:

```nginx
server {
    listen 443 ssl http2;
    server_name neo4flix.com www.neo4flix.com;

    ssl_certificate /etc/letsencrypt/live/neo4flix.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/neo4flix.com/privkey.pem;
    ssl_trusted_certificate /etc/letsencrypt/live/neo4flix.com/chain.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # ... rest of configuration
}
```

---

## Quick Start Commands

### Development (Self-Signed)

```bash
# Certificates already generated in ssl-certs/

# Start Gateway with SSL
cd microservices/gateway-service
mvn spring-boot:run

# Access at: https://localhost:9443
```

### Production (Let's Encrypt)

```bash
# Obtain certificate
sudo certbot --nginx -d your-domain.com

# Verify auto-renewal
sudo certbot renew --dry-run
```

---

## Additional Resources

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Certbot Documentation](https://certbot.eff.org/docs/)
