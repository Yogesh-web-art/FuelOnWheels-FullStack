# ⛽ FuelOnWheels - Full Stack On-Demand Fuel Delivery Platform

![Java](https://img.shields.io/badge/Java-21%20%7C%2025%20LTS-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JJWT-6DB33F?logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data-JPA%20%2F%20Hibernate-59666C?logo=hibernate&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-9.7-4479A1?logo=mysql&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2034%20(Android%2014)-3DDC84?logo=android&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI-Swagger%203-85EA2D?logo=swagger&logoColor=black)

**FuelOnWheels** is an enterprise-grade, full-stack on-demand fuel delivery platform. The system connects vehicle owners needing emergency or doorstep fuel delivery with nearby registered fuel stations and dispatch drivers in real time.

---

## 🏛️ System Architecture

```
                    ┌─────────────────────────────────────────┐
                    │              Client Layer               │
                    │   Android 14 App (Retrofit2 / OSMDroid)  │
                    │        Interactive Swagger UI           │
                    └────────────────────┬────────────────────┘
                                         │ HTTP REST / JSON + JWT
                    ┌────────────────────▼────────────────────┐
                    │    Spring Boot 3 REST Microservice      │
                    │              (Port 8080)                │
                    ├─────────────────────────────────────────┤
                    │ • Security: JJWT + BCrypt Encryption    │
                    │ • Global Exception Handling             │
                    │ • Controllers: Auth, Station, Order, GPS│
                    │ • Service Layer (@Transactional ACID)   │
                    │ • Algorithmic: Haversine Proximity Search│
                    │ • Spring Data JPA / Hibernate 6         │
                    └────────────────────┬────────────────────┘
                                         │ JDBC / Connection Pool
                    ┌────────────────────▼────────────────────┐
                    │             Database Layer              │
                    │  MySQL 9.7 (fuelonwheels_db) / H2 Mem   │
                    └─────────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

### Backend Microservice (`fuel-backend`)
* **Core:** Java 21 / 25 LTS, Spring Boot 3.3.4
* **Web & REST:** Spring Web MVC, Spring Validation (Jakarta)
* **Persistence & ORM:** Spring Data JPA, Hibernate ORM 6.5
* **Security & Auth:** Spring Security 6, JJWT (HMAC-SHA512 token parsing & verification), BCrypt password hashing
* **Database:** MySQL 9.7 Connector/J (Production), H2 In-Memory (Dev/Testing profile)
* **API Documentation:** SpringDoc OpenAPI 3 / Swagger UI
* **Build System:** Apache Maven 3.10

### Mobile Frontend (`FuelOnWheels`)
* **Platform:** Native Android (Target SDK 34 / Android 14)
* **Networking:** Retrofit 2.9, OkHttp 4.12 Logging Interceptor, Gson
* **Maps & Geospatial:** OSMDroid 6.1 (OpenStreetMap Tile Provider), OSMBonusPack, OSRM Road Routing
* **Architecture:** MVVM (Model-View-ViewModel), LiveData, ViewBinding
* **Authentication:** Dual-mode (Spring Boot REST JWT + Firebase Auth fallback)

---

## 🚀 Key Features

1. **Real-Time GPS Tracking & Fleet Simulation:**
   * Live driver vehicle marker progression on OpenStreetMap along calculated road waypoints.
   * Real-time ETA deduction and dynamic delivery status transitions (`PLACED` ➔ `CONFIRMED` ➔ `DISPATCHED` ➔ `OUT_FOR_DELIVERY` ➔ `DELIVERED`).
2. **Proximity Search via Haversine Algorithm:**
   * High-performance mathematical Haversine formula implemented in Java to query, filter, and sort fuel stations by distance from the user's GPS coordinates in sub-millisecond execution time.
3. **ACID Transaction Management:**
   * `@Transactional` boundaries on order placement guaranteeing atomic balance calculation, delivery fee computation, dynamic OTP generation, and driver assignment.
4. **Stateless JWT Security:**
   * Fine-grained role-based access control (`ROLE_CUSTOMER`, `ROLE_DRIVER`, `ROLE_ADMIN`).
5. **Interactive Swagger API Sandbox:**
   * Visual documentation and one-click API testing playground available at `/swagger-ui.html`.
6. **Automated Seed Data:**
   * Auto-populates realistic top Indian fuel stations (IndianOil, BPCL, HP, Shell) and demo accounts on initial boot.

---

## 📋 REST API Reference

| HTTP Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---:|
| `POST` | `/api/v1/auth/register` | Register new customer account | No |
| `POST` | `/api/v1/auth/login` | Authenticate user & return JWT token | No |
| `GET` | `/api/v1/stations` | Retrieve all active fuel stations | No |
| `GET` | `/api/v1/stations/nearby` | Proximity search via Haversine distance (`lat`, `lng`, `radiusKm`) | No |
| `GET` | `/api/v1/stations/{id}` | Get fuel station details by ID | No |
| `POST` | `/api/v1/orders` | Place new fuel delivery order | Yes (Bearer JWT) |
| `GET` | `/api/v1/orders/{orderCode}` | Get order details & live tracking info | Yes (Bearer JWT) |
| `GET` | `/api/v1/orders/user/{userId}` | Order history for a user | Yes (Bearer JWT) |
| `GET` | `/api/v1/tracking/{orderCode}` | Get driver GPS coordinates & ETA | No |
| `POST` | `/api/v1/tracking/{orderCode}/simulate-step` | Advance driver 35% closer to customer | No |

---

## ⚙️ How to Run

### 1. Spring Boot Backend
Navigate to the backend directory and run:

```bash
# Option A: Run with instant in-memory database (zero setup required)
java -jar "target/fuel-backend-1.0.0.jar" --spring.profiles.active=h2

# Option B: Run with MySQL
java -jar "target/fuel-backend-1.0.0.jar" --spring.profiles.active=mysql --spring.datasource.password=YOUR_PASSWORD
```

Access Swagger UI at: **`http://localhost:8080/swagger-ui/index.html`**

**Default Demo Credentials:**
* **Email:** `user@fuelonwheels.com`
* **Password:** `password123`

### 2. Android App
1. Open Android Studio and select the `FuelOnWheels` project directory.
2. Allow Gradle sync to complete.
3. Run on an Android Emulator or physical device (Target SDK 34).

---

## 💼 Full Stack Java Developer Portfolio Highlights
* Designed end-to-end distributed client-server architecture with separation of concerns across presentation, business logic, persistence, and database layers.
* Implemented custom Spring Security filter chains with stateless JWT validation and BCrypt password encryption.
* Applied domain modeling in Spring Data JPA with relational mappings (`@ManyToOne`, `@OneToOne`), cascading, and lazy fetching.
* Integrated Retrofit 2 with Android MVVM architecture to consume backend REST endpoints and handle real-time UI state streams.
