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
- **Rate limiting** — in-memory, IP-based, applied to `/auth/**` and appointment booking (disabled by default).
- **Email notifications** — hooks on appointment events; logs instead of sending when disabled (default), or send real email via `spring.mail.*`.
- **Swagger UI** — interactive API documentation at `/swagger-ui.html`.
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

This starts `clinic-mysql` (MySQL 8.4) and `clinic-app` (built image or `youssefyaser/clinicsystem:latest`) with a healthcheck-gated startup order.

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
| `RATE_LIMIT_AUTH_MAX` / `RATE_LIMIT_AUTH_WINDOW` | `20` / `60`                                            | Max `/auth/**` requests per window (seconds) |
| `RATE_LIMIT_BOOKING_MAX` / `RATE_LIMIT_BOOKING_WINDOW` | `10` / `60`                                        | Max `POST /appointments` per window      |

> **Important:** never use the default `JWT_SECRET` or admin credentials in production. Admin accounts cannot be self-registered — they are only created via `ADMIN_EMAIL`/`ADMIN_PASSWORD` at startup.

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

- `src/test/java/com/clinicsystem/` — controller integration tests (MockMvc + Testcontainers).
- `src/test/java/com/clinicsystem/service/` — pure Mockito unit tests.
- `src/test/resources/application.properties` shadows the main one on the test classpath; keep the `app.*`, `jwt.*` keys in sync when adding config.

## Contributing

1. Fork the repo.
2. Create a feature branch (`git checkout -b feature/xyz`).
3. Commit your changes (`git commit -m 'Add xyz'`).
4. Push to the branch and open a pull request.
