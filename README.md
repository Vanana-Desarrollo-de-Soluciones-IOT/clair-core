# clair-core

Spring Boot project (Java 25) with Swagger/OpenAPI.

## Features

- Spring Boot 3.x
- REST API with Swagger/OpenAPI
- Maven as dependency manager
- JWT Authentication with Refresh Tokens
- Email Verification Flow
- Redis Caching

## Requirements

- Java 25 (Maven must run on JDK 25)
- Maven 3.6 or higher
- PostgreSQL 15+
- Redis 7+

## Environment Variables

This project uses a `.env` file for configuration. Create a `.env` file in the root directory:

```env
PORT=49220

# Database
DB_URL=jdbc:postgresql://localhost:5432/clair_core
DB_USERNAME=postgres
DB_PASSWORD=admin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# SMTP Email (Resend)
SMTP_HOST=smtp.resend.com
SMTP_PORT=465
SMTP_USERNAME=resend
SMTP_PASSWORD=your_resend_api_key
SMTP_FROM=noreply@yourdomain.com

# JWT
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_32_characters_long
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Core -> Edge webhook (hint only; edge reconciles over HTTP)
EDGE_WEBHOOK_URL=http://127.0.0.1:5000
# Core -> Edge token (must match EDGE_TOKEN in edge)
EDGE_TOKEN=change-me-long-random-secret
# Edge -> Core token (must match EDGE_TO_CORE_TOKEN in edge)
EDGE_TO_CORE_TOKEN=change-me-long-random-secret

# CORS — tu web app Angular
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

> ⚠️ **Importante:** Si borraste la base de datos, la primera vez corre con `ddl-auto: update` en `application.yml`. Cuando arranque bien, cámbialo a `validate`.

## Compile the Project

```bash
mvn clean compile
```

Using Nix:
```bash
nix develop --command mvn clean compile
```

## Run the Project

```bash
mvn spring-boot:run
```

Using Nix:
```bash
nix develop --command mvn spring-boot:run
```

## Stripe CLI (Nix)

If you want Stripe CLI available via Nix:

```bash
nix develop --command stripe version
```

Example webhook forward:

```bash
nix develop --command stripe listen --forward-to localhost:49220/api/v1/billing/webhook
```

The server will be available at: `http://localhost:${PORT}` (Default: 49220)

## API Documentation

Access the interactive Swagger documentation at:
```
http://localhost:${PORT}/swagger-ui.html
```

Or view the OpenAPI JSON at:
```
http://localhost:${PORT}/v3/api-docs
```

## Authentication Endpoints

### 1. Sign Up
- **POST** `/api/v1/auth/sign-up`
- Body: `{ "email": "user@example.com", "password": "SecurePass123!" }`
- Response (201):
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "Registration initiated. Please check your email for the verification code."
  }
  ```

### 2. Confirm Registration
- **POST** `/api/v1/auth/confirm`
- Body: `{ "sessionId": "550e8400-e29b-41d4-a716-446655440000", "verificationCode": "6G13-789D" }`
- Response (201):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  }
  ```

### 3. Sign In
- **POST** `/api/v1/auth/sign-in`
- Body: `{ "email": "user@example.com", "password": "SecurePass123!" }`
- Response (200):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```

### 4. Refresh Token
- **POST** `/api/v1/auth/refresh`
- Body: `{ "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }`
- Response (200):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```

### 5. Verify Token
- **GET** `/api/v1/auth/verify`
- Header: `Authorization: Bearer <token>`
- Response (200):
  ```json
  {
    "valid": true,
    "email": "user@example.com",
    "expiresAt": "2026-05-05T17:43:27.000Z"
  }
  ```

## Angular Integration

Tu web app en `http://localhost:4200` ya está permitida por CORS.

### Ejemplo de servicio en Angular:

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:49220/api/v1/auth';

  constructor(private http: HttpClient) {}

  signUp(email: string, password: string) {
    return this.http.post(`${this.apiUrl}/sign-up`, { email, password });
  }

  confirm(sessionId: string, verificationCode: string) {
    return this.http.post(`${this.apiUrl}/confirm`, { sessionId, verificationCode });
  }

  signIn(email: string, password: string) {
    return this.http.post<{token: string, refreshToken: string}>(`${this.apiUrl}/sign-in`, { email, password });
  }

  refreshToken(refreshToken: string) {
    return this.http.post<{token: string, refreshToken: string}>(`${this.apiUrl}/refresh`, { refreshToken });
  }

  verifyToken(token: string) {
    return this.http.get(`${this.apiUrl}/verify`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  }
}
```

### Guardar tokens después del login:

```typescript
this.authService.signIn(email, password).subscribe(response => {
  localStorage.setItem('token', response.token);
  localStorage.setItem('refreshToken', response.refreshToken);
});
```

### Enviar token en cada request protegido:

```typescript
// Interceptor
const token = localStorage.getItem('token');
if (token) {
  req = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}
```

## Production Build

```bash
mvn clean package
```

The JAR file will be located under `target/`.

## Run the JAR

```bash
java -jar target/clair-core-1.0.0.jar
```

## Security Checklist for Production

- [ ] Change `ddl-auto` from `update` to `validate` in `application.yml`
- [ ] Rotate the JWT secret (minimum 32 characters)
- [ ] Rotate the Resend API key
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to your real domain(s)
- [ ] Enable HTTPS (HSTS is already configured)
