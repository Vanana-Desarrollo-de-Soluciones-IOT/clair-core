# Spring Template

A Spring Boot project with Swagger/OpenAPI and a simple REST endpoint.

## Features

- Spring Boot 3.2.0
- REST API with Swagger/OpenAPI
- Documented `hello-world` endpoint
- Maven as dependency manager

## Project Structure

```
src/
├── main/
│   ├── java/com/example/springtemplate/
│   │   ├── SpringTemplateApplication.java
│   │   └── controller/
│   │       └── HelloController.java
│   └── resources/
│       └── application.yml
└── test/java/com/example/springtemplate/
```

## Requirements

- Java 25 or higher
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

## Run the Project

```bash
mvn spring-boot:run
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

The JAR file will be located at `target/spring-template-1.0.0.jar`

## Run the JAR

```bash
java -jar target/spring-template-1.0.0.jar
```
