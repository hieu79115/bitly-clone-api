# Bitly Clone API

A high-performance, production-ready URL shortener and click analytics RESTful service built with Spring Boot, PostgreSQL, and Redis.

---

## Tech Stack

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white)

---

## Key Features

- **High-Throughput URL Shortening**: Generates unique short codes using Base62 encoding with custom alias support and conflict detection.
- **Fast 302 Redirection & Caching**: Multi-tier Redis caching for ultra-low latency redirection (`GET /{shortCode}`) with automatic cache eviction on update/delete.
- **Asynchronous Click Analytics**: Non-blocking analytics logging capturing IP, device type, browser, operating system (via `uap-java`), and referrers.
- **Sliding-Window Rate Limiting (Redis Lua)**: Atomic, tiered rate limiting protecting authentication, link creation, and public redirection against spam and brute-force attacks.
- **Stateless JWT Security**: Access & Refresh token rotation with Redis-backed token blacklisting on logout.
- **Server-Side Search & Filtering**: Advanced URL querying using Spring Data JPA Specifications (filter by active/expired status, search by title/original URL/short code, and sort by date or clicks).
- **Interactive OpenAPI / Swagger**: Complete documentation with direct authorization testing support.

---

## Project Structure

```text
src/main/java/com/thinh/shortener/
├── config/              # Redis, OpenAPI, Security, Rate Limit & Async configurations
├── controller/          # REST API controllers
├── domain/
│   ├── dto/             # Request & Response Data Transfer Objects
│   ├── entity/          # JPA database entities (User, Url, Tag, ClickAnalytics, UserProfile)
│   └── mapper/          # MapStruct DTO mappers
├── exception/           # Global exception handler & custom business exceptions
├── repository/          # Spring Data JPA repositories & Specifications
├── security/            # Spring Security filter chain, JWT provider, and rate limiting filters
├── service/             # Business logic interfaces & implementations
└── util/                # Base62 encoder, IP utility, User-Agent parser & constants
```

---

## Prerequisites

Ensure you have the following installed on your system:

- **JDK 21** or higher
- **PostgreSQL 15+**
- **Redis 7+**
- *(Optional)* **Docker & Docker Compose**

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
   # Expected response: PONG
   ```

3. Configure environment variables or customize `src/main/resources/application.yml`:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `PORT` | Application server port | `8080` |
| `SPRING_DATASOURCE_URL` | JDBC database connection URL | `jdbc:postgresql://localhost:5432/bitly_clone` |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `123456` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | 256-bit secret key for signing JWTs | *(Preconfigured default secret)* |
| `JWT_EXPIRATION` | Access token lifetime (ms) | `3600000` (1 hour) |
| `JWT_REFRESH_EXPIRATION` | Refresh token lifetime (ms) | `604800000` (7 days) |
| `APP_DOMAIN` | Base domain prepended to short links | `http://localhost:8080/` |
| `FRONTEND_URL` | Frontend client URL (for redirection fallbacks) | `http://localhost:5173` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins | `http://localhost:5173,http://localhost:3000,http://localhost:4173` |
| `RATE_LIMIT_ENABLED` | Global toggle for rate limiting filter | `true` |
| `RATE_LIMIT_AUTH_CAPACITY` | Max requests per minute for auth endpoints (`/auth/*`) | `10` |
| `RATE_LIMIT_REDIRECT_CAPACITY` | Max requests per minute for public redirect (`/{code}`) | `100` |
| `RATE_LIMIT_CREATE_URL_CAPACITY`| Max requests per minute for link creation (`POST /urls`) | `20` |
| `RATE_LIMIT_GENERAL_CAPACITY` | Max requests per minute for general endpoints | `120` |

---

## Running the Application

### Option 1: Using Docker Compose (Recommended)

1. Create your `.env` file from the example:
   ```bash
   cp .env.example .env
   ```
2. Build and start all services (PostgreSQL, Redis, and Spring Boot API):
   ```bash
   docker compose up --build -d
   ```
3. Check container status:
   ```bash
   docker compose ps
   ```
4. View live logs:
   ```bash
   docker compose logs -f app
   ```
5. Stop services:
   ```bash
   docker compose down
   ```

### Option 2: Using Maven Wrapper (Local Standalone)

- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

The API will be accessible at `http://localhost:8080`.

---

## Running Automated Tests

Run the complete test suite (33 automated unit and integration tests covering business logic, rate limiting, and security filters):

- **Windows**:
  ```powershell
  .\mvnw.cmd test
  ```

- **Linux / macOS**:
  ```bash
  ./mvnw test
  ```

---

## API Documentation (Swagger / OpenAPI)

All API endpoints, schemas, parameters, and authentication methods are interactively documented via SpringDoc OpenAPI.

Once the application is running, navigate to:

- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3.0 Specification (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Testing authenticated endpoints in Swagger:**
> 1. Call `POST /api/v1/auth/login` to retrieve an `accessToken`.
> 2. Click the green **Authorize** button at the top of the Swagger page.
> 3. Enter your token in the format: `Bearer <your-token>`.
