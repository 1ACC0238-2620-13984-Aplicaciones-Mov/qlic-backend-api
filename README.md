# qlic-backend-api

RESTful API of **Qlic** (startup WASD) — UPC 1ACC0238 Mobile Applications.

## Stack

| Item | Version |
|---|---|
| Language | Kotlin 2.3 on the JVM (toolchain 24) |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security OAuth2 Resource Server) |
| Documentation | springdoc-openapi 3.1 (OpenAPI 3 + Swagger UI) |
| Database | In-memory H2 (`dev` profile) · PostgreSQL (`prod` profile) |
| Build | Gradle Wrapper 9.7 (Kotlin DSL) |

## Implemented bounded contexts

| Bounded context | Stories | Endpoints |
|---|---|---|
| IAM | US05, US06, TS01 | `POST /api/v1/authentication/sign-up` · `POST /api/v1/authentication/sign-in` |
| Device Monitoring | US10, US11, US12 | `POST /api/v1/devices` · `PATCH /api/v1/devices/{id}/water-point` · `GET /api/v1/devices` |
| Alerting | US15, US16 | `POST /api/v1/alerts/evaluations` · `GET /api/v1/alerts` |

The `authentication` endpoints are public; every other endpoint requires the header `Authorization: Bearer <token>` (`401` without token). IAM messages are in English by default; send `Accept-Language: es-419` to receive them in Latin American Spanish.

## Run locally

```bash
./gradlew bootRun     # dev profile (H2)
./gradlew test        # unit and integration tests
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:qlicdb`, user `sa`)

## Deployment (Render, Docker)

The `Dockerfile` builds the JAR with JDK 24 and runs it on JRE 24 using the `PORT` assigned by Render. Environment variables of the web service:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/<database>` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | PostgreSQL credentials |
| `JWT_SECRET` | HS256 signing key, at least 32 characters |
| `JWT_EXPIRATION_MINUTES` | Token lifetime (default 1440) |

## Conventions

GitFlow (`main`, `develop`, `feature/*`, `release/*`, `hotfix/*`), Conventional Commits, Semantic Versioning and the Kotlin Coding Conventions.
