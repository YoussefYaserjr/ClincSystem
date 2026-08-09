# ClinicSystem

A RESTful clinic management system built with **Spring Boot 4.1**, **Java 21**, **Spring Security (JWT)** and **MySQL**. Patients book appointments with doctors, doctors manage their schedules and medical records, and an administrator approves doctors and oversees the system.

Containerized with Docker and published to Docker Hub as [`youssefyaser/clinicsystem`](https://hub.docker.com/r/youssefyaser/clinicsystem).

## Features

- **Role-based access control** — `PATIENT`, `DOCTOR` and `ADMIN` roles with endpoint-level security (JWT, stateless).
- **Doctor discovery** — public, paginated search by specialty and/or location.
- **Admin approval gate** — doctors only appear in public discovery after an admin approves them.
- **Appointment lifecycle** — book, cancel, confirm, reject and complete appointments with status tracking (`PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED`).
- **Slot concurrency safety** — optimistic `SELECT ... FOR UPDATE` locking prevents double-booking the same slot; a unique constraint on `schedule_id` is the DB-level backstop.
- **Schedules** — doctors create availability slots; patients view a doctor's free slots for a date.
- **Medical records** — doctors attach records to patient visits; patients and doctors can read them with strict ownership checks.
- **Admin dashboard data** — user/doctor/appointment/schedule/medical-record statistics.
- **Pagination** — every list endpoint returns a consistent `PageResponse<T>` shape.
- **Rate limiting** — IP-based fixed-window limits on `/auth/**` and appointment booking. Single-instance (in-memory) by default, or **distributed via Redis** so the budget is shared across every replica behind a load balancer (disabled by default).
- **Email notifications** — hooks on appointment events; logs instead of sending when disabled (default), or send real email via `spring.mail.*`.
- **Swagger UI** — interactive API documentation at `/swagger-ui.html`.
- **Health & metrics** — Spring Boot Actuator at `/actuator/health` (with Kubernetes `liveness`/`readiness` probes), `/actuator/metrics` and `/actuator/prometheus` for orchestrators to ping.
- **Test suite** — pure-Mockito service unit tests plus Testcontainers-backed integration tests against a real MySQL 8.4.

## Tech Stack

| Layer      | Technology                                                              |
|------------|-------------------------------------------------------------------------|
| Runtime    | Java 21, Spring Boot 4.1.0                                              |
| Web/Security | Spring WebMVC, Spring Security, JWT (JJWT 0.12.6), BCrypt               |
| Persistence| Spring Data JPA, Hibernate, MySQL 8.x (`mysql-connector-j`)              |
| Docs       | springdoc-openapi 3.1.0 (Swagger UI)                                    |
| Build      | Maven                                                                   |
| Testing    | JUnit 5, Mockito, AssertJ, Testcontainers 1.21.4                        |
| DevOps     | Docker (multi-stage), Docker Compose, Docker Hub                        |

## Project Structure

```
clinicsystem/
├── Dockerfile                     # Multi-stage build (Maven -> Temurin 21 JRE)
├── docker-compose.yml             # MySQL 8.4 + app, healthcheck, volume
├── .env.example                   # Copy to .env for compose deployment
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/clinicsystem/
    │   │   ├── controller/        # REST endpoints (auth, doctors, schedules,
    │   │   │                      #  appointments, medical-records, admin)
    │   │   ├── service/           # Business logic interfaces + impl/
    │   │   ├── repository/        # Spring Data repositories
    │   │   ├── entity/            # User, Doctor, Patient, Schedule,
    │   │   │                      #  Appointment, MedicalRecord + enums
    │   │   ├── dto/               # request/ + response/ (incl. PageResponse)
    │   │   ├── security/          # JwtService, JwtAuthFilter, rate limiting,
    │   │   │                      #  authenticated-user resolution
    │   │   ├── config/            # SecurityConfig, AdminBootstrap, OpenApiConfig
    │   │   └── exception/         # Custom exceptions + GlobalExceptionHandler
    │   └── resources/application.properties
    └── test/
        ├── java/com/clinicsystem/        # Controller integration tests + base class
        │   └── service/                  # Mockito unit tests
        └── resources/application.properties   # Test config (shadows main on classpath)
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8.x (for local runs) — or Docker for the containerized path
- Docker Desktop (only for integration tests / compose)

### 1. Run locally

Create a MySQL database (the default JDBC URL uses `createDatabaseIfNotExist=true`), then:

```bash
export DB_PASSWORD=your_password     # Windows (PowerShell): $env:DB_PASSWORD="..."
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Swagger UI: <http://localhost:8080/swagger-ui.html>.

### 2. Run with Docker Compose

```bash
cp .env.example .env    # adjust secrets (DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD)
docker compose up -d
```

This starts `clinic-mysql` (MySQL 8.4), `clinic-redis` (Redis 7) and `clinic-app` (built image or `youssefyaser/clinicsystem:latest`) with a healthcheck-gated startup order. `clinic-app` itself has a container healthcheck that curls `/actuator/health`, so `docker compose ps` shows `healthy` once the app is ready.

### 3. Build & run the Docker image yourself

```bash
docker build -t youssefyaser/clinicsystem:latest .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/clinic?useSSL=false&allowPublicKeyRetrieval=true" \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET="a_long_random_string_at_least_32_chars" \
  youssefyaser/clinicsystem:latest
```

## Configuration

All settings can be overridden with environment variables.

| Variable                     | Default                                                                 | Description                              |
|------------------------------|-------------------------------------------------------------------------|------------------------------------------|
| `DB_URL`                     | `jdbc:mysql://localhost:3306/clinic?...`                                | JDBC URL (uses `allowPublicKeyRetrieval`)|
| `DB_USERNAME`                | `root`                                                                  | Database user                            |
| `DB_PASSWORD`                | `123123`                                                                | Database password                        |
| `JWT_SECRET`                 | dev placeholder                                                         | JWT signing key (≥ 32 chars for HS256)   |
| `JWT_EXPIRATION_MS`          | `86400000` (24h)                                                        | Token lifetime                           |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | `admin@clinic.com` / `Admin@12345` / `Clinic Admin` | First admin account, created on startup only if no admin exists |
| `NOTIFICATIONS_ENABLED`      | `false`                                                                 | When `true` + `spring.mail.*` set, sends real emails |
| `RATE_LIMIT_ENABLED`         | `false`                                                                 | When `true`, enforces the limits below   |
| `RATE_LIMIT_STORE`           | `memory`                                                                | Rate-limit backend: `memory` (single instance) or `redis` (distributed across replicas) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / *(empty)*                    | Redis connection (used when `RATE_LIMIT_STORE=redis`) |
| `REDIS_HEALTH_ENABLED`       | `false`                                                                 | When `true`, `/actuator/health` turns `DOWN` if Redis is unreachable (default `false`: Redis is optional, the rate limiter fails open) |
| `RATE_LIMIT_AUTH_MAX` / `RATE_LIMIT_AUTH_WINDOW` | `20` / `60`                                            | Max `/auth/**` requests per window (seconds) |
| `RATE_LIMIT_BOOKING_MAX` / `RATE_LIMIT_BOOKING_WINDOW` | `10` / `60`                                        | Max `POST /appointments` per window      |

> **Important:** never use the default `JWT_SECRET` or admin credentials in production. Admin accounts cannot be self-registered — they are only created via `ADMIN_EMAIL`/`ADMIN_PASSWORD` at startup.

## Health & metrics (Actuator)

Actuator endpoints are public (no auth) so orchestrators can ping them:

| Endpoint                          | Purpose                                                        |
|-----------------------------------|----------------------------------------------------------------|
| `/actuator/health`                | Overall status (`UP`/`DOWN`), includes DB health detail        |
| `/actuator/health/liveness`       | Kubernetes liveness probe — `UP` while the JVM is alive       |
| `/actuator/health/readiness`      | Kubernetes readiness probe — `UP` when the app can serve      |
| `/actuator/metrics`               | JVM / HTTP / DB metrics                                        |
| `/actuator/prometheus`            | Prometheus scrape endpoint (Micrometer)                        |
| `/actuator/info`                  | Build info (version, time) from `build-info.properties`        |

Use in Kubernetes:

```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
```

Notes:

- `management.health.redis.enabled` defaults to `false` (override with `REDIS_HEALTH_ENABLED=true`). Redis is optional — the rate limiter fails open — so by default an unreachable Redis does **not** mark the pod `DOWN`. Enable it when your deployment treats Redis as a hard dependency.
- The `spring-boot-maven-plugin` generates `build-info.properties`, which feeds `/actuator/info`.

## Distributed rate limiting (how we solved the multi-instance problem)

**The problem.** The original limiter used a `ConcurrentHashMap` inside each running app (`InMemoryRateLimiter`). That is fine while there is exactly one instance, but as soon as you scale out behind a load balancer each replica keeps its **own** counter, so the limit is effectively multiplied by the number of replicas — and a bursty client can simply be routed to a different instance to dodge the limit.

**The solution.** Move the counter out of the app process and into Redis, so every replica reads and writes the **same** counter. Scaling from one to N replicas does not change the effective budget at all.

Three pieces were added:

1. **`RateLimiter` interface** (`security/RateLimiter.java`) — `tryAcquire(key, maxRequests, windowSeconds)` abstracts the backend.
2. **`InMemoryRateLimiter`** — the old logic, kept as the default (`app.rate-limit.store=memory`), so existing single-instance deployments and tests are unchanged.
3. **`RedisRateLimiter`** — a fixed-window counter implemented as an atomic Lua script:

   ```lua
   local current = redis.call('INCR', KEYS[1])
   if current == 1 then
       redis.call('EXPIRE', KEYS[1], ARGV[2])
   end
   return current > tonumber(ARGV[1]) and 0 or 1
   ```

   `INCR` + `EXPIRE` run as one Redis script, so the increment and the window expiry are atomic — no two requests can read the same counter value. The key is `rate-limit:<clientIp>|<rule>`, where `clientIp` honors `X-Forwarded-For` (first IP) so requests behind a reverse proxy are keyed by the real client.

`RateLimiterConfig` picks the backend from `app.rate-limit.store`: `redis` requires a `StringRedisTemplate` (via `spring-boot-starter-data-redis`, mapped from `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`). If Redis is unreachable the limiter **fails open** — the request is allowed and a throttled WARN is logged — so the app stays available rather than rejecting everyone during a Redis outage.

Two bugs found and fixed while building this:

- `RateLimitFilter` was registered as a `@Bean` **and** added to the security chain, so it ran **twice per request** (a limit of 20 hit at 10). Fix: construct it inline in `SecurityConfig.filterChain()` so Spring Boot does not auto-register it as a servlet filter.
- The Lua arguments must be passed as individual varargs, not `List.of(...)` (a `List` arg caused a `ClassCastException` that the fail-open path swallowed). Fix: `String.valueOf(maxRequests), String.valueOf(windowSeconds)`.

Verified end-to-end: two app containers sharing one Redis, budget of 20 — instance A used 12, instance B got the remaining 8 then `429` for the rest, and the Redis counter showed exactly one increment per request across both instances. With the old in-memory limiter, B would have allowed all 15.

## Authentication

Register or log in to receive a JWT:

```bash
# Register (PATIENT or DOCTOR)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Dr. Smith","email":"smith@clinic.com","password":"password123",
       "phone":"01000000000","role":"DOCTOR","specialty":"Cardiology","location":"Cairo"}'

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smith@clinic.com","password":"password123"}'
```

Response:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "userId": 2, "role": "DOCTOR" }
```

Send the token on protected endpoints:

```bash
curl http://localhost:8080/appointments/me -H "Authorization: Bearer <token>"
```

## API Overview

Interactive docs: <http://localhost:8080/swagger-ui.html>

### Public

| Method | Path                       | Description                                     |
|--------|----------------------------|-------------------------------------------------|
| POST   | `/auth/register`           | Register a `PATIENT` or `DOCTOR` (not `ADMIN`)  |
| POST   | `/auth/login`              | Login, returns a JWT                            |
| GET    | `/doctors`                 | Approved doctors, filter by `specialty`/`location`, `page`/`size` |
| GET    | `/doctors/{id}`            | Approved doctor by id                           |
| GET    | `/search/doctors`          | Alias for `/doctors` filtering                  |
| GET    | `/schedules/doctor/{doctorId}?date=YYYY-MM-DD` | A doctor's available slots for a date |

### DOCTOR

| Method | Path                 | Description                                        |
|--------|----------------------|----------------------------------------------------|
| POST   | `/schedules`         | Create an availability slot (`availableDate`, `startTime`, `endTime`) |
| DELETE | `/schedules/{id}`    | Delete one of your slots                           |
| POST   | `/appointments/{id}/confirm` | Accept a `PENDING` appointment             |
| POST   | `/appointments/{id}/reject`  | Reject a `PENDING` appointment and free the slot |
| POST   | `/appointments/{id}/complete`| Complete a `CONFIRMED` appointment        |
| POST   | `/medical-records`   | Attach a record to a visit (`patientId`, `appointmentId`, `diagnosis`) |
| GET    | `/medical-records/patient/{patientId}` | Read a patient's records (patient or their doctor) |
| GET    | `/medical-records/mine` | Records you authored                            |
| DELETE | `/medical-records/{id}` | Delete a record you authored                    |

### PATIENT

| Method | Path                 | Description                                        |
|--------|----------------------|----------------------------------------------------|
| POST   | `/appointments`      | Book a slot (`scheduleId`, optional `notes`)       |
| DELETE | `/appointments/{id}` | Cancel your appointment (frees the slot)           |
| GET    | `/appointments/me`   | Your appointments, optional `status` filter        |
| GET    | `/medical-records/patient/{patientId}` | Read your own records                |

### ADMIN

| Method | Path                        | Description                                     |
|--------|-----------------------------|-------------------------------------------------|
| GET    | `/admin/users`              | Paginated list of all users                     |
| GET    | `/admin/doctors?approved=false` | Doctors by approval state (`true` = approved) |
| POST   | `/admin/doctors/{id}/approve`  | Approve a doctor (makes them discoverable)    |
| POST   | `/admin/doctors/{id}/reject`   | Withdraw approval                             |
| DELETE | `/admin/doctors/{id}`       | Delete a doctor with no appointments/schedules/records |
| GET    | `/admin/stats`              | Oversight statistics                           |

### Pagination

List endpoints return a uniform shape:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "last": false
}
```

## Testing

The integration tests boot a real MySQL 8.4 via **Testcontainers**, so Docker must be running.

```bash
mvn test
```

- `src/test/java/com/clinicsystem/` — controller integration tests (MockMvc + Testcontainers), incl. `ActuatorTest` (health/probes/metrics reachability).
- `src/test/java/com/clinicsystem/service/` — pure Mockito unit tests.
- `src/test/java/com/clinicsystem/security/` — rate limiter unit tests (`InMemoryRateLimiterTest`, `RedisRateLimiterTest`).
- `src/test/resources/application.properties` shadows the main one on the test classpath; keep the `app.*`, `jwt.*` keys in sync when adding config.

## Contributing

1. Fork the repo.
2. Create a feature branch (`git checkout -b feature/xyz`).
3. Commit your changes (`git commit -m 'Add xyz'`).
4. Push to the branch and open a pull request.
