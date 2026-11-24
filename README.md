# Rydr

Rydr is a microservices-based ride-hailing platform built with **Java 21**, **Spring Boot**, **Spring Cloud**, and an **Expo / React Native** mobile client.

## Architecture

The backend is organized into independent Spring Boot services coordinated through **Netflix Eureka** service discovery, **Spring Cloud Gateway**, and **Apache Kafka** event streaming, backed by **PostgreSQL (PostGIS)**, **Redis**, and **OSRM** for routing.

### Services

| Service | Port | Responsibility | Data / Integrations |
|---|---|---|---|
| [`discovery-service`](./discovery-service) | `8761` | Eureka service registry | — |
| [`gateway-service`](./gateway-service) | `8080` | API gateway, CORS, service-discovery routing, WebSocket proxy (`/ws/**`) | Eureka |
| [`user-service`](./user-service) | `8000` | Authentication (RSA JWT + refresh tokens), users, driver onboarding, vehicles, admin verification | PostgreSQL (`users`), Kafka |
| [`rating-service`](./rating-service) | `8001` | Trip and driver ratings | PostgreSQL (`ratings`) |
| [`trip-service`](./trip-service) | `8002` | Trip lifecycle (`create`, `arrived`, `start`, `cancel`, `complete`), fare calculation, transactional outbox | PostgreSQL (`trips` + PostGIS), Kafka, OSRM |
| [`location-service`](./location-service) | `8003` | Real-time driver location pings (REST & STOMP/SockJS `/ws`) and nearby driver search | Redis, Kafka |
| [`matching-service`](./matching-service) | `8004` | Event-driven driver matching for created trips using nearby driver lookup and OSRM ETA/routing | Kafka, OSRM, Location Service |
| [`payment-service`](./payment-service) | `8005` | Payment processing integration (in progress) | PostgreSQL (`payments`), Kafka, Iyzico |
| [`mobile`](./mobile) | — | Cross-platform mobile app (Expo Router, React Native, TypeScript) | Gateway API |

### Infrastructure

Defined in [`compose.yaml`](./compose.yaml):

- **PostgreSQL + PostGIS** (`postgis/postgis:16-3.4`, port `5432`) — initializes `users`, `trips`, `ratings`, and `payments` databases via [`docker/postgres/init-databases.sql`](./docker/postgres/init-databases.sql)
- **Redis** (`redis:7-alpine`, port `6379`) — geospatial storage for live driver locations
- **Apache Kafka** (`apache/kafka:3.8.0` in KRaft mode, ports `9092` internal / `29092` external) — asynchronous domain events
- **Kafka UI** (`provectuslabs/kafka-ui`, port `8085`) — web UI for inspecting topics and messages
- **OSRM** (`osrm/osrm-backend`, port `5000`) — road network routing and distance/duration estimation

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose**
- **Java 21** (for running or testing backend services locally via Gradle)
- **Node.js & npm** (for running the Expo mobile app)

### 1. Environment Configuration

Copy the sample environment file and adjust values as needed (such as `IYZICO_API_KEY` and `IYZICO_SECRET_KEY` if running `payment-service`):

```bash
cp .env.example .env
```

### 2. OSRM Map Data

The `osrm-prepare` container in [`compose.yaml`](./compose.yaml) expects `turkey-latest.osm.pbf` inside `./docker/osrm/data/` on first run to build the `.osrm` routing graph. Place the `.osm.pbf` file there before starting the stack if it is not already prepared.

### 3. Start the Backend with Docker Compose

```bash
docker compose up --build
```

> **Note:** `compose.yaml` currently builds and starts `discovery`, `gateway`, `user-service`, `location-service`, `trip-service`, and `matching-service` alongside the infrastructure containers. `rating-service` and `payment-service` are not yet included in `compose.yaml` and can be run individually with Gradle.

Once running, key endpoints are available at:

- **API Gateway:** `http://localhost:8080`
- **Eureka Dashboard:** `http://localhost:8761`
- **Kafka UI:** `http://localhost:8085`
- **OSRM:** `http://localhost:5000`

### 4. Run a Service Locally (Optional)

Each backend module includes a Gradle wrapper. From any service directory (e.g. `user-service`):

```bash
./gradlew bootRun
```

To run tests for a service:

```bash
./gradlew test
```

### 5. Run the Mobile App

```bash
cd mobile
npm install
npx expo start
```

See [`mobile/README.md`](./mobile/README.md) for Expo-specific commands (Android, iOS, Web).

## API Reference

HTTP endpoints, request/response payloads, and STOMP/WebSocket destinations are documented in [`API.md`](./API.md).
