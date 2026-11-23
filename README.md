# 🎟️ Ticket Reservation System API

A secure, high-performance RESTful API for event ticket booking built with **Spring Boot** and **MongoDB**. Designed with **Role-Based Access Control (RBAC)** via **JWT (JSON Web Tokens)**, atomic seat reservation routines to prevent overselling, and centralized exception handling.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Architecture](#-project-architecture)
- [Prerequisites](#-prerequisites)
- [Configuration](#-configuration)
- [Default Seed Accounts](#-default-seed-accounts)
- [Running the Application](#-running-the-application)
- [API Reference](#-api-reference)
  - [Authentication](#1-authentication-apiauth)
  - [Events](#2-events-apievents)
  - [Reservations](#3-reservations-apireservations)
  - [Admin Management](#4-admin-management-apiadmin)
- [Concurrency & Atomic Operations](#-concurrency--atomic-operations)
- [Error Handling](#-error-handling)

---

## ✨ Features

- **Role-Based Access Control (RBAC):** Distinct permissions across `ADMIN`, `ORGANIZER`, and `CUSTOMER` roles.
- **JWT Authentication:** Stateless security architecture utilizing HMAC-SHA384 signed tokens.
- **Race Condition Prevention:** Atomic seat reservations via MongoDB's `findAndModify`, preventing double-booking and overselling under concurrent loads.
- **Event Lifecycle Management:** Create, update, and manage events with validation rules (e.g., cannot reduce capacity below existing bookings; cannot delete events with active reservations).
- **Booking & Cancellation:** Unique ticket reservation codes (`TKT-XXXXXXXX`), automatic seat restocking upon cancellation, and customer-specific booking views.
- **System Metrics & Analytics:** Admin dashboard statistics covering user counts, total events, and confirmed/cancelled reservation totals.
- **Unified JSON Error Responses:** Consistent error reporting for validation failures, authentication errors, and business domain violations.
- **Automatic Seed Data:** Automatically bootstraps default users on startup if they don't already exist.

---

## 🛠️ Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4.2.0
  - Spring Web MVC
  - Spring Security
  - Spring Data MongoDB
  - Spring Validation (Jakarta Validation)
- **Database:** MongoDB (Atlas or local)
- **Authentication:** JJWT (Java JWT) 0.11.5
- **Utilities:** Project Lombok, Jackson (`JavaTimeModule`)
- **Build Tool:** Apache Maven (includes `mvnw` wrapper)

---

## 📂 Project Architecture

```
ticket-reservation/
├── src/
│   ├── main/
│   │   ├── java/com/example/ticketreservation/
│   │   │   ├── config/              # Application initializers and startup runners
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controller/          # REST controllers exposing HTTP endpoints
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── EventController.java
│   │   │   │   └── ReservationController.java
│   │   │   ├── dto/                 # Immutable Request/Response records with validation
│   │   │   ├── exception/           # Custom exceptions and @RestControllerAdvice handler
│   │   │   │   ├── ApiException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/               # MongoDB entity documents (AppUser, Event, Reservation)
│   │   │   ├── repository/          # Spring Data Mongo repositories
│   │   │   ├── security/            # Security filters, JWT utilities, authentication handlers
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JsonAccessDeniedHandler.java
│   │   │   │   ├── JsonAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── service/             # Core business logic layer
│   │   │   │   ├── AdminService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── EventService.java
│   │   │   │   └── ReservationService.java
│   │   │   └── TicketReservationApplication.java
│   │   └── resources/
│   │       └── application.properties # App properties & database connections
├── pom.xml
└── README.md
```

---

## ⚙️ Prerequisites

- **Java Development Kit (JDK):** Version 25 (or compatible JDK 21+)
- **MongoDB:** A running MongoDB instance or a [MongoDB Atlas](https://www.mongodb.com/atlas) cluster
- **Maven:** Bundled via `mvnw` / `mvnw.cmd`

---

## 🔧 Configuration

Update [src/main/resources/application.properties](file:///src/main/resources/application.properties) with your MongoDB connection string and secret keys:

```properties
spring.application.name=ticket-reservation

# MongoDB Connection String
spring.mongodb.uri=mongodb+srv://<username>:<password>@cluster.mongodb.net/ticket_reservation_db?retryWrites=true&w=majority
spring.data.mongodb.auto-index-creation=true

# JWT Configuration
app.jwt.secret=YourSuperSecretKeyForHS384SignMin384BitsLongHere!!!!!!
app.jwt.expiration-ms=3600000
```

> **Note:** The HMAC-SHA384 signature algorithm requires a minimum secret key length of 48 bytes (384 bits).

---

## 👥 Default Seed Accounts

On initial startup, `DataInitializer` seeds the database with the following demo users if they do not exist:

| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | `ADMIN` | System administrator (full access) |
| `organizer` | `organizer123` | `ORGANIZER` | Event organizer (manages own events) |
| `customer` | `customer123` | `CUSTOMER` | Customer (reserves and cancels tickets) |

---

## 🚀 Running the Application

### Using the Maven Wrapper

**Windows (PowerShell or CMD):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

The application runs on port `8080` by default: `http://localhost:8080`.

---

## 📖 API Reference

All protected endpoints require the HTTP Authorization header:
```http
Authorization: Bearer <your_jwt_token>
```

### 1. Authentication (`/api/auth`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/register` | `POST` | Public | Register a new customer account |
| `/api/auth/login` | `POST` | Public | Log in to obtain a JWT token |
| `/api/auth/me` | `GET` | Authenticated | Retrieve current user profile |

#### Register (`POST /api/auth/register`)
**Request:**
```json
{
  "username": "johndoe",
  "password": "password123",
  "fullName": "John Doe"
}
```
**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "expiresInMs": 3600000,
  "username": "johndoe",
  "fullName": "John Doe",
  "role": "CUSTOMER"
}
```

#### Login (`POST /api/auth/login`)
**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```
**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "expiresInMs": 3600000,
  "username": "admin",
  "fullName": "System Administrator",
  "role": "ADMIN"
}
```

---

### 2. Events (`/api/events`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/events` | `GET` | Authenticated | List all events |
| `/api/events/{id}` | `GET` | Authenticated | Get event by ID |
| `/api/events` | `POST` | `ADMIN`, `ORGANIZER` | Create a new event |
| `/api/events/{id}` | `PUT` | `ADMIN`, `ORGANIZER` | Update an existing event |
| `/api/events/{id}` | `DELETE` | `ADMIN` | Delete event (fails if reservations exist) |

#### Create Event (`POST /api/events`)
**Request:**
```json
{
  "name": "Tech Conference 2027",
  "description": "Annual developer summit",
  "venue": "Grand Convention Center, Hall A",
  "eventDate": "2027-06-15T10:00:00",
  "totalSeats": 200,
  "ticketPrice": 49.99
}
```
**Response (201 Created):**
```json
{
  "id": "6741234567890abcdef12345",
  "name": "Tech Conference 2027",
  "description": "Annual developer summit",
  "venue": "Grand Convention Center, Hall A",
  "eventDate": "2027-06-15T10:00:00",
  "totalSeats": 200,
  "availableSeats": 200,
  "ticketPrice": 49.99,
  "organizerUsername": "organizer"
}
```

---

### 3. Reservations (`/api/reservations`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/reservations` | `POST` | `CUSTOMER` | Book seats for an event |
| `/api/reservations/my` | `GET` | `CUSTOMER` | View own booking history |
| `/api/reservations` | `GET` | `ADMIN`, `ORGANIZER` | View all reservations (organizers see only their events) |
| `/api/reservations/{id}` | `DELETE` | `ADMIN`, `CUSTOMER` | Cancel reservation and release seats |

#### Book Tickets (`POST /api/reservations`)
**Request:**
```json
{
  "eventId": "6741234567890abcdef12345",
  "seats": 2
}
```
**Response (201 Created):**
```json
{
  "id": "6741234567890abcdef99999",
  "reservationCode": "TKT-B4E81A2F",
  "customerUsername": "customer",
  "eventId": "6741234567890abcdef12345",
  "seats": 2,
  "totalAmount": 99.98,
  "status": "CONFIRMED",
  "createdAt": "2026-11-23T14:30:00"
}
```

#### Cancel Reservation (`DELETE /api/reservations/{id}`)
**Response (204 No Content)**

---

### 4. Admin Management (`/api/admin`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/admin/users` | `POST` | `ADMIN` | Create `ADMIN` or `ORGANIZER` account |
| `/api/admin/users` | `GET` | `ADMIN` | List all registered users |
| `/api/admin/stats` | `GET` | `ADMIN` | Retrieve system analytics |

#### Get System Statistics (`GET /api/admin/stats`)
**Response (200 OK):**
```json
{
  "totalUsers": 12,
  "totalEvents": 5,
  "totalReservations": 34,
  "confirmedReservations": 30,
  "cancelledReservations": 4
}
```

---

## 🔒 Concurrency & Atomic Operations

Seat reservation in high-traffic ticket systems is vulnerable to race conditions (two users simultaneously booking the last remaining seat). 

This project solves this directly in MongoDB without distributed locks:

```java
Query seatQuery = new Query(
    Criteria.where("_id").is(request.eventId())
            .and("availableSeats").gte(request.seats())
);

Update seatUpdate = new Update()
        .inc("availableSeats", -request.seats());

Event event = mongoTemplate.findAndModify(
    seatQuery,
    seatUpdate,
    FindAndModifyOptions.options().returnNew(true),
    Event.class
);
```

- If `event == null`, the seat allocation fails immediately with a `409 Conflict` (or `404 Not Found` if the event does not exist).
- When a reservation is cancelled, seats are restored atomically using `$inc: { availableSeats: +seats }`.

---

## ⚠️ Error Handling

Errors return a consistent JSON response:

```json
{
  "timestamp": "2026-11-23T13:00:00.000",
  "status": 409,
  "error": "Conflict",
  "message": "Not enough available seats",
  "path": "/api/reservations"
}
```

| HTTP Status | Trigger Example |
| :--- | :--- |
| `400 Bad Request` | DTO validation failure (e.g. negative seat count, past event date) |
| `401 Unauthorized` | Missing, expired, or invalid JWT token |
| `403 Forbidden` | Accessing an endpoint restricted to another role |
| `404 Not Found` | Event or reservation ID does not exist |
| `409 Conflict` | Insufficient seats, duplicate username, or deleting an event with active bookings |
| `500 Internal Server Error` | Unhandled runtime errors |
