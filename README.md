# Bitly Clone API

A high-performance URL shortening and click analytics RESTful service built with Spring Boot, PostgreSQL, and Redis.

---

## Tech Stack

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white)

---

## Project Structure 

```text
src/main/java/com/thinh/shortener/
├── config/              # Redis, OpenAPI, Async & Web MVC configurations
├── controller/          # REST API controllers & endpoint definitions
├── domain/
│   ├── dto/             # Request & Response Data Transfer Objects
│   ├── entity/          # JPA database entities (User, Url, Tag, ClickAnalytics)
│   └── mapper/          # MapStruct/DTO mapping components
├── exception/           # Global exception handler & custom business exceptions
├── repository/          # Spring Data JPA repositories
├── security/            # Spring Security configuration, JWT provider & filters
├── service/             # Business logic interfaces & implementation classes
└── util/                # Base62 encoder, User-Agent parser & constants
```

---

## Prerequisites

Ensure you have the following installed and running on your system:

- **JDK 21** or higher
- **PostgreSQL 15+**
- **Redis 7+**

---

## Setup and Installation

### 1. Clone the repository

```bash
git clone https://github.com/hieu79115/bitly-clone-api.git
cd bitly-clone-api
```

### 2. Configure Database and Redis

1. Create a PostgreSQL database named `bitly_clone`:
   ```sql
   CREATE DATABASE bitly_clone;
   ```

2. Ensure your Redis server is running:
   ```bash
   redis-cli ping
   # Should return PONG
   ```

3. (Optional) Adjust environment variables or modify `src/main/resources/application.yml` if your database credentials differ from defaults:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC database URL | `jdbc:postgresql://localhost:5432/bitly_clone` |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `123456` |
| `REDIS_HOST` | Redis server host | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `JWT_SECRET` | 256-bit Hex/Base64 secret | *(Preconfigured default secret)* |
| `APP_DOMAIN` | Base domain for short links | `http://localhost:8080/` |

---

## Running the Application

### Using Maven Wrapper

- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

The application will start on port `8080` by default.

---

## Running Automated Tests

Run the complete test suite (26 unit tests):

- **Windows**:
  ```powershell
  .\mvnw.cmd test
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw test
  ```

---

## API Documentation

Once the server is running, you can access the interactive Swagger UI at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification (JSON format):

```text
http://localhost:8080/v3/api-docs
```

---

## API Endpoints Overview

### Public & Redirection
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/{shortCode}` | Redirects (HTTP 302) to the original URL and records click analytics asynchronously |

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new user account |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain Access + Refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Rotate and issue a new Access Token + Refresh Token |
| `POST` | `/api/v1/auth/logout` | Invalidate tokens and blacklist the current Access Token |

### URLs (`/api/v1/urls`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/urls` | Create a shortened URL (auto Base62 or custom alias) |
| `GET` | `/api/v1/urls` | Get paginated list of user's URLs (supports filtering by `tagId`) |
| `PUT` | `/api/v1/urls/{id}` | Update URL expiration date and tags |
| `DELETE` | `/api/v1/urls/{id}` | Soft delete URL and evict Redis cache |

### Analytics (`/api/v1/analytics`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/{shortCode}` | Get total clicks and breakdown by Browser, OS, and Device |

### Tags (`/api/v1/tags`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/tags` | Create a new tag |
| `GET` | `/api/v1/tags` | Get all tags created by current user |
| `PUT` | `/api/v1/tags/{id}` | Update tag name |
| `DELETE` | `/api/v1/tags/{id}` | Delete a tag |

### Profile (`/api/v1/profile`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/profile` | Get current user's profile and overall statistics |
| `PUT` | `/api/v1/profile` | Update profile information |
| `PUT` | `/api/v1/profile/password` | Change user password |
