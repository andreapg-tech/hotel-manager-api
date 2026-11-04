# Hotel Manager API 🏨

<p align="left">
  <a href="README.md">Leer en Español 🇪🇸</a> | <b>English</b>
</p>

REST API for managing a hotel: users, rooms and bookings. Built with **Java 21** and **Spring Boot**, with a strong focus on **security**: JWT authentication, role-based access control and brute-force protection on login.

> 🚧 The frontend (HTML, CSS, JavaScript and Bootstrap) is under development and will be published later.

## ✨ Features

- 👤 **User sign-up and login** with passwords hashed using **BCrypt**.
- 🕵️ **User enumeration protection**: login responds the same way, and in the same time, whether or not the email exists.
- 🔐 **Stateless JWT authentication**, validated on every request by a custom filter.
- 🍪 **Session-based employee portal** protected against *session fixation* (the session ID is regenerated on login and never exposed in responses).
- 🛡️ **Role-based access control** (`CLIENTE` and `ADMIN`) with Spring Security.
- 🚦 **Rate limiting on every login endpoint** (customers and employee portal): at most 5 attempts per minute per IP to stop brute-force attacks.
- 🛏️ **Available room search** by dates, type and maximum price.
- 📅 **Booking management**: create, view and cancel your own bookings.
- 🌐 **Restricted CORS** with a configurable list of trusted origins.
- 📊 **Admin panel**: statistics, booking and room management, and CSV report export.
- ⚠️ **Centralized error handling** with consistent JSON responses.
- ✅ **Integration tests** for authentication, tokens and permissions.

## 🛠️ Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot, Spring Web MVC |
| Security | Spring Security, JWT (jjwt), BCrypt |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Testing | JUnit 5, MockMvc, Mockito |
| Build | Maven |

## 🧱 Architecture

Layered architecture with a clear separation of responsibilities:

```
src/main/java/com/hotel/
├── config/         # Spring Security and CORS configuration
├── controllers/    # REST endpoints and global exception handling
├── dto/            # API request and response objects
├── filters/        # JWT filter and rate limiting
├── listeners/      # Startup and shutdown events
├── models/         # JPA entities (Usuario, Habitacion, Reserva)
├── repositories/   # Data access with Spring Data JPA
├── services/       # Business logic
└── utils/          # JWT generation and validation
```

## 🔌 Main Endpoints

Base URL: `http://localhost:8080/hotel-manager`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/registro` | Public | Register a new customer |
| `POST` | `/api/login` | Public (rate limited) | Log in; returns a JWT |
| `GET` | `/api/habitaciones/disponibles` | Public | Available rooms, with filters |
| `GET` `POST` `DELETE` | `/api/reservas` | Authenticated user | View, create and cancel your own bookings |
| `PUT` | `/api/usuarios/password` | Authenticated user | Change password |
| `GET` | `/api/admin/estadisticas` | `ADMIN` | Hotel statistics |
| `GET` `POST` `DELETE` | `/api/admin/reservas` | `ADMIN` | Manage all bookings |
| `PUT` | `/api/admin/habitaciones` | `ADMIN` | Update rooms |
| `GET` | `/api/admin/reportes/reservas` | `ADMIN` | Export bookings as CSV |
| `POST` `GET` `DELETE` | `/portal/empleado` | `ADMIN` employees (session) | Employee portal login, session check and logout |

Protected routes require the header:

```
Authorization: Bearer <token>
```

## 🚀 Getting Started

### Requirements

- Java 21
- MySQL 8 running

### 1. Clone the repository

```bash
git clone https://github.com/andreapg-tech/hotel-manager-api.git
cd hotel-manager-api
```

### 2. Create the database

```sql
CREATE DATABASE hotelweb;
```

Tables are created automatically when the application starts.

### 3. Set the environment variables

The JWT signing key and the database credentials are **never stored in the repository**. Copy the example file and edit it:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `JWT_SECRET` | Key used to sign tokens (at least 32 characters) | *Required* |
| `DB_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/hotelweb?serverTimezone=UTC` |
| `DB_USERNAME` | MySQL user | `root` |
| `DB_PASSWORD` | MySQL password | *(empty)* |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins, comma-separated | `http://localhost:5500,http://127.0.0.1:5500` |

The `.env` file is listed in `.gitignore`, so it is never committed.

### 4. Run the API

```bash
./mvnw spring-boot:run
```

On Windows: `mvnw.cmd spring-boot:run`

## ✅ Testing

```bash
./mvnw test
```

The tests start the full Spring context, so MySQL must be running. They use their own test JWT key (`src/test/resources/config/application.properties`), so no `.env` file is needed.

The integration tests check, among other cases:

- Successful login and token generation.
- Rejection of requests with no token or an invalid token.
- Access denied when a `CLIENTE` tries to use `ADMIN` routes.
- The same error for an unknown email and for a wrong password, so registered accounts are not revealed.
- Correct responses for unknown routes and unsupported methods.

## 👤 Author

**Andrea Pérez González** · [LinkedIn](https://www.linkedin.com/in/andreap%C3%A9rezgonz%C3%A1lez)
