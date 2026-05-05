# clair-core

Spring Boot project (Java 25) with Swagger/OpenAPI.

## Features

- Spring Boot 3.x
- REST API with Swagger/OpenAPI
- Maven as dependency manager

## Requirements

- Java 25 (Maven must run on JDK 25)
- Maven 3.6 or higher

## Environment Variables

This project uses a `.env` file for configuration. Create a `.env` file in the root directory of the project with the following (or desired) variables:

```env
PORT=8080

# Database
DB_URL=jdbc:postgresql://localhost:5432/clair_core
DB_USERNAME=postgres
DB_PASSWORD=admin

# Redis (no password)
REDIS_HOST=localhost
REDIS_PORT=6379

# Resend Email
RESEND_API_KEY=your_resend_api_key
RESEND_FROM_EMAIL=onboarding@resend.dev
EMAIL_PROVIDER=console

# JWT
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_32_characters_long
JWT_EXPIRATION=3600000
```

## Compile the Project

```bash
mvn clean compile
```

Using Nix:
```bash
nix-shell -p maven jdk25 --run "mvn clean compile"
```

## Run the Project

```bash
mvn spring-boot:run
```

Using Nix:
```bash
nix-shell -p maven jdk25 --run "mvn spring-boot:run"
```

The server will be available at: `http://localhost:${PORT}` (Default: 8080)

## API Documentation

Access the interactive Swagger documentation at:
```
http://localhost:${PORT}/swagger-ui.html
```

Or view the OpenAPI JSON at:
```
http://localhost:${PORT}/v3/api-docs
```

## Endpoints

### Authentication

- **POST** `/api/v1/auth/sign-up`
  - Description: Initiates user registration, sends verification code via email
  - Body: `{ "email": "user@example.com", "password": "password123" }`
  - Response (201):
    ```json
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "message": "Registration initiated. Please check your email for the verification code."
    }
    ```

- **POST** `/api/v1/auth/confirm`
  - Description: Confirms registration with the 6-digit verification code
  - Body: `{ "sessionId": "550e8400-e29b-41d4-a716-446655440000", "verificationCode": "123456" }`
  - Response (201):
    ```json
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com"
    }
    ```

- **POST** `/api/v1/auth/sign-in`
  - Description: Authenticates a verified user and returns a JWT token
  - Body: `{ "email": "user@example.com", "password": "password123" }`
  - Response (200):
    ```json
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "token": "eyJhbGciOiJIUzI1NiIs..."
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
