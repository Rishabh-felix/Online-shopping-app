# Online Shopping App

A Spring Boot **microservices** e-commerce backend demonstrating the core patterns of a
distributed system: service discovery, an API gateway, inter-service communication,
event-driven messaging, resilience, distributed tracing, and centralized monitoring.

The domain itself is intentionally small (products, orders, inventory) so the focus stays
on the microservices architecture around it.

## Architecture

```
                                   ┌─────────────┐
                                   │  Keycloak   │  issues JWTs
                                   │  (:8080)    │  realm: spring-boot-microservices-realm
                                   └──────┬──────┘
                                          │ validates JWT
                                          ▼
 Client ────────────────────────▶ ┌─────────────┐        ┌───────────────────┐
                                   │ API Gateway │───────▶│ Discovery Server  │
                                   │  (:8181)    │        │  Eureka (:8761)   │
                                   └──────┬──────┘        └───────────────────┘
                       lb://product-service  lb://order-service
                                   │                  │
                     ┌─────────────┘                  └───────────────┐
                     ▼                                                ▼
             ┌───────────────┐                              ┌────────────────┐
             │ Product Service│                              │  Order Service │
             │  (MongoDB)     │                              │  (Postgres)    │
             └───────────────┘                              └───────┬────────┘
                                                                      │ WebClient (lb://)
                                                                      ▼
                                                              ┌────────────────┐
                                                              │Inventory Service│
                                                              │  (Postgres)     │
                                                              └────────────────┘
                                                                      │
                                                       Order Service publishes
                                                       "OrderPlacedEvent" to Kafka
                                                       topic "notificationTopic"
                                                                      ▼
                                                              ┌────────────────────┐
                                                              │ Notification Service│
                                                              │ (Kafka consumer)    │
                                                              └────────────────────┘

 Cross-cutting: every service registers with Eureka, sends traces to Zipkin (:9411),
 and exposes /actuator/prometheus, scraped by Prometheus (:9090) and visualized in
 Grafana (:3000).
```

### Request flow: placing an order

1. Client calls `POST /api/order` through the **API Gateway**, with a valid Keycloak-issued JWT.
2. The gateway validates the JWT (OAuth2 resource server) and routes the call to **Order Service** via Eureka (`lb://order-service`).
3. Order Service calls **Inventory Service** (`GET /api/inventory?skuCode=...`) over a load-balanced `WebClient` to check stock, guarded by a Resilience4j circuit breaker, retry, and time limiter (all named `inventory`).
4. If every line item is in stock, the order is persisted (PostgreSQL) and an `OrderPlacedEvent` is published to the Kafka topic `notificationTopic`. If not, the request fails with an error.
5. If the inventory call fails or times out, the circuit breaker's fallback returns a friendly "try again later" message instead of an exception.
6. **Notification Service** consumes `notificationTopic` and logs the event (this is a demo project — no real email is sent).

## Services

| Service | Role | Port (local / docker) | Datastore |
|---|---|---|---|
| [discovery-server](discovery-server) | Eureka service registry | `8761` / `8761` | — |
| [api-gateway](api-gateway) | Spring Cloud Gateway — single entry point, JWT auth, request routing | `8181` / `8080` (mapped to host `8181`) | — |
| [product-service](product-service) | Product catalog CRUD | random / `8080` | MongoDB |
| [order-service](order-service) | Order placement, orchestrates inventory check + event publishing | `8081` / `8080` | PostgreSQL |
| [inventory-service](inventory-service) | Stock lookup by SKU | random / `8080` | PostgreSQL |
| [notification-service](notification-service) | Consumes order events, sends notifications | random / (random) | — |

Product, inventory, and notification services run on Eureka-assigned random ports locally
since multiple instances of the same service can be scaled and load-balanced.

## Tech stack

- **Java 17**, **Spring Boot 3.0.5**, **Spring Cloud 2022.0.1**
- **Spring Cloud Gateway** (reactive routing) + **Spring Cloud Netflix Eureka** (service discovery / client-side load balancing)
- **Spring WebFlux `WebClient`** for the order → inventory call
- **Resilience4j** (circuit breaker, retry, time limiter) around the inventory call
- **Spring Kafka** for asynchronous order → notification events
- **Spring Data MongoDB** (product-service) and **Spring Data JPA / PostgreSQL** (order-service, inventory-service)
- **Keycloak** as the OAuth2/OIDC provider; the gateway validates JWTs as a resource server
- **Micrometer Tracing + Zipkin** for distributed tracing
- **Micrometer Prometheus registry + Grafana** for metrics/dashboards
- **Maven** multi-module build (root `pom.xml` aggregates all six modules) with `jib-maven-plugin` for building container images

## Prerequisites

- JDK 17
- Maven (or use the bundled `./mvnw`)
- Docker & Docker Compose

## Running everything with Docker Compose

The project's [docker-compose.yml](docker-compose.yml) brings up the full stack:
Postgres (x2), MongoDB, Keycloak + its MySQL DB, Zookeeper + Kafka, Zipkin, Eureka,
the API gateway, all four business services, Prometheus, and Grafana.

```bash
docker-compose up -d
```

> **Note:** `docker-compose.yml` mounts a `./realms/` directory into Keycloak for
> `--import-realm`, but no realm export is committed to this repo. Before the gateway's
> JWT validation will work end to end, create a realm named
> `spring-boot-microservices-realm` in Keycloak (http://localhost:8080, `admin`/`admin`)
> yourself, or export one into `./realms/`.

Once running:

| UI / endpoint | URL |
|---|---|
| API Gateway | http://localhost:8181 |
| Eureka dashboard | http://localhost:8181/eureka/web (proxied) or http://localhost:8761 directly |
| Keycloak admin console | http://localhost:8080 (`admin` / `admin`) |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (`admin` / `password`) |

The compose file references prebuilt images (`kunaldo07/*:latest`). To run your own
local changes instead, build and push/tag images with the module's `jib-maven-plugin`
(`./mvnw compile jib:dockerBuild` from a module directory), or build jars and swap in
your own `Dockerfile`s.

## Running locally without Docker (per service)

Each service also has an `application.properties` for local development, pointed at
`localhost` infra (MySQL for order/inventory, MongoDB on `localhost:27017`, Kafka on
`localhost:9092`, Eureka on `localhost:8761`, Zipkin on `localhost:9411`). Start the
infra you need (e.g. via `docker-compose up mongo broker zookeeper zipkin`), then run
services individually, **starting with discovery-server**:

```bash
./mvnw -pl discovery-server spring-boot:run
./mvnw -pl api-gateway spring-boot:run
./mvnw -pl product-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
```

## API reference

All endpoints below are also reachable through the gateway at `http://localhost:8181`
(with a valid Bearer token), or directly against each service's own port.

### Product Service — `/api/product`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/product` | Create a product. Body: `{ "name", "description", "price" }` |
| `GET` | `/api/product` | List all products |

### Order Service — `/api/order`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/order` | Place an order. Body: `{ "orderLineItemsDtoList": [{ "skuCode", "price", "quantity" }] }`. Checks inventory before saving; publishes an event on success. |

### Inventory Service — `/api/inventory`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/inventory?skuCode=A&skuCode=B` | Returns `[{ "skuCode", "isInStock" }, ...]` for the given SKUs |

Inventory Service seeds two rows on startup (see `InventoryServiceApplication`'s
`CommandLineRunner`) so `POST /api/order` has something to check against out of the box.

## Observability

- **Tracing**: every service reports 100%-sampled traces to Zipkin; a single order
  placement produces a trace spanning the gateway, order-service, and its call into
  inventory-service.
- **Metrics**: each service exposes Spring Boot Actuator's `/actuator/prometheus`
  endpoint; `prometheus/prometheus.yml` scrapes product-service, order-service,
  inventory-service, and notification-service every 10s.
- **Dashboards**: Grafana is preloaded with the `./grafana` data volume (existing
  dashboards/alerts) and points at the bundled Prometheus data source.

## Repository layout

```
.
├── api-gateway/            Spring Cloud Gateway + Keycloak JWT resource server
├── discovery-server/       Eureka service registry
├── product-service/        Product catalog (MongoDB)
├── order-service/          Order placement, inventory orchestration, Kafka producer
├── inventory-service/      Stock lookup (PostgreSQL)
├── notification-service/   Kafka consumer for order events
├── docker-compose.yml      Full local stack: infra + all services + observability
├── prometheus/             Prometheus scrape config
├── grafana/                Grafana data volume (dashboards, alerting)
└── pom.xml                 Root Maven aggregator (packaging: pom)
```

## Known limitations

This is a learning/demo project; a few rough edges are worth knowing about before
relying on it:

- No Keycloak realm export is committed — auth must be configured manually (see above).
- Notification Service only logs received events; it does not send real emails.
- The order → inventory `WebClient` call is blocking (`.block()`) despite using a
  reactive client.
- Local profiles for order-service and inventory-service default to MySQL, while the
  docker-compose stack runs PostgreSQL for both — the `docker` Spring profile switches
  the datasource accordingly.
