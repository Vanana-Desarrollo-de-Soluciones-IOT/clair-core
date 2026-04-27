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

### Hello World

- **GET** `/api/hello-world`
- Description: Returns a greeting message
- Response:
  ```json
  {
    "message": "Hello World"
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
