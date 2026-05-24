# Country Land Route API

A production-quality Spring Boot 3 REST API that calculates the **shortest possible land route** between two countries using Bidirectional BFS over an in-memory adjacency graph built from the [mledoze/countries](https://github.com/mledoze/countries) dataset.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Bidirectional BFS](#bidirectional-bfs)
4. [Scalability](#scalability)
5. [Caching Strategy](#caching-strategy)
6. [Build Instructions](#build-instructions)
7. [Run Instructions](#run-instructions)
8. [Docker Instructions](#docker-instructions)
9. [Swagger UI](#swagger-ui)
10. [API Reference](#api-reference)
11. [Error Reference](#error-reference)
12. [Design Decisions](#design-decisions)

---

## Project Overview

The service exposes a single endpoint:

```
GET /routing/{origin}/{destination}
```

Countries are identified by their **ISO 3166-1 alpha-3** (`cca3`) codes (e.g. `CZE`, `ITA`, `DEU`).  
Country codes are **case-insensitive** — `cze`, `CZE`, and `Cze` are all equivalent.

---

## Architecture

```
src/main/java/com/example/routing
├── RoutingApplication.java               # Spring Boot entry point
├── controller/
│   └── RoutingController.java            # HTTP layer — delegates to RoutingService
├── service/
│   ├── CountryDataLoader.java            # Startup: downloads + parses countries.json
│   ├── RoutingService.java               # Interface contract
│   └── BidirectionalBfsRoutingService.java  # Core algorithm + caching
├── model/
│   ├── Country.java                      # JSON-mapped record (cca3 + borders only)
│   ├── RouteResponse.java                # Successful response record
│   └── ErrorResponse.java               # Error response record
├── exception/
│   ├── RouteNotFoundException.java
│   ├── InvalidCountryException.java
│   └── GlobalExceptionHandler.java      # @RestControllerAdvice
├── config/
│   ├── CacheConfig.java                  # Caffeine cache manager
│   ├── OpenApiConfig.java               # Swagger / OpenAPI metadata
│   └── RestClientConfig.java            # RestClient bean
└── util/
    └── RouteBuilder.java                 # Path reconstruction from BFS parent maps
```

### Key design principles

| Principle | How it's applied |
|---|---|
| Clean architecture | Controller → Service → Utility; each layer has one responsibility |
| Constructor injection | All dependencies injected via constructor; zero field injection |
| Immutability | `Country`, `RouteResponse`, `ErrorResponse` are Java records; the adjacency graph is `Collections.unmodifiableMap` |
| Thread safety | Graph is read-only after startup; BFS state is local per call; caches are thread-safe |
| No Lombok | All code uses plain Java |
| No overengineering | One interface, one implementation; utilities are static helpers |

---

## Bidirectional BFS

**Bidirectional BFS** runs two simultaneous BFS searches:
- **Forward BFS** starts at the origin and expands outward.
- **Backward BFS** starts at the destination and expands outward.
- The algorithm stops as soon as the two frontiers **intersect**.

### Implementation details

1. **Level-by-level expansion** — the smaller frontier is always expanded first, keeping both searches balanced.
2. **Optimal meeting point selection** — when multiple nodes in the current expansion level exist in both visited maps, the node that minimises `depth(from origin) + depth(from destination)` is chosen to guarantee a shortest path.
3. **Path reconstruction** — `RouteBuilder` walks the forward parent-chain back to origin, reverses it, then walks the backward parent-chain forward to destination.

### Worked example: `CZE → ITA`

```
Forward BFS:  CZE → {AUT, SVK, POL, DEU}
Backward BFS: ITA → {FRA, CHE, AUT, SVN, HRV, SMR, VAT}

Meeting point: AUT (depth 1 from both sides)
Route: [CZE, AUT, ITA]
```
---

## Scalability

The service is designed to scale horizontally behind a load balancer:

| Property | Detail |
|---|---|
| **Stateless** | No server-side session; each instance is interchangeable |
| **Immutable graph** | Loaded once at startup; zero contention on reads |
| **In-memory cache** | Bounded Caffeine cache per instance; no shared cache server required |
| **No database** | All routing state is in-process; no DB bottleneck |
| **Low memory footprint** | ~250 nodes × ~5 edges ≈ negligible heap usage |

To scale: deploy multiple instances behind a load balancer (e.g. Kubernetes HPA, AWS ALB).  
Cache warm-up is fast (bounded BFS on a tiny graph), so cold-start latency is minimal.

---

## Caching Strategy

Spring Cache + Caffeine is used for two separate concerns:

| Cache | What's stored | Configuration |
|---|---|---|
| `routes` (`@Cacheable`) | Successful route lists, keyed by `ORIGIN-DESTINATION` | `maximumSize=10000`, `expireAfterWrite=1h` |
| `noRouteCache` (in-memory `Set`) | Country-pair keys for which BFS found no route | Unbounded set (at most `V^2` ≈ 62,500 entries) |

Cache keys are always **normalised to uppercase** via the SpEL expression:
```java
@Cacheable(value = "routes", key = "#origin.toUpperCase() + '-' + #destination.toUpperCase()")
```

---

## Build Instructions

**Prerequisites:** Java 21, Maven 3.9+

```bash
# Clone the repository
git clone <repo-url>
cd routing

# Compile and run all tests
mvn clean verify

# Package without running tests
mvn clean package -DskipTests
```

---

## Run Instructions

```bash
# Start with Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/routing-1.0.0.jar
```

The service starts on **port 8080** and downloads `countries.json` on startup.  
If the download fails, the application refuses to start with a clear error message.

---

## Docker Instructions

```bash
# Build image
docker build -t routing-service:latest .

# Run with Docker
docker run -p 8080:8080 routing-service:latest

# Run with Docker Compose
docker-compose up --build

# Stop
docker-compose down
```

The Docker image uses a **multi-stage build**: the builder stage uses a full JDK; the runtime stage uses a minimal JRE Alpine image. The container runs as a **non-root user** for security.

---

## Swagger UI

After starting the application, visit:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## API Reference

### `GET /routing/{origin}/{destination}`

| Parameter | Type | Description |
|---|---|---|
| `origin` | path | ISO cca3 country code (case-insensitive) |
| `destination` | path | ISO cca3 country code (case-insensitive) |

#### 200 OK — Route found

```json
{
  "route": ["CZE", "AUT", "ITA"]
}
```

#### 200 OK — Same country

```
GET /routing/CZE/CZE
```

```json
{
  "route": ["CZE"]
}
```

---

## Error Reference

All error responses use the same structure:

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Route Not Found",
  "message": "No land route found between USA and FRA",
  "path": "/routing/USA/FRA"
}
```

| Scenario | HTTP Status | `error` field |
|---|---|---|
| No land route (e.g. island nations) | 400 | `Route Not Found` |
| Unknown country code | 400 | `Invalid Country` |
| Unexpected server error | 500 | `Internal Server Error` |

---

## Actuator Endpoints

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Application health status |
| `GET /actuator/metrics` | JVM, cache, and HTTP metrics |
| `GET /actuator/info` | Application info |

---

## Design Decisions

**Why Bidirectional BFS?**  
For an unweighted graph, BFS guarantees the shortest path. Bidirectional BFS halves the effective search radius, giving `O(b^(d/2))` instead of `O(b^d)`. For the country graph (~250 nodes) the practical difference is small, but the selection demonstrates algorithm awareness appropriate for a senior engineer.

**Why immutable in-memory graph?**  
The dataset is static (country borders rarely change). Loading once at startup eliminates all read-write contention and allows lock-free access from any number of goroutines. There is no need for a database.

**Why Caffeine?**  
Caffeine is the highest-performance JVM-native caching library. It uses a window-TinyLFU eviction policy, providing near-optimal hit rates. It is the default cache implementation recommended by Spring Boot.

**Why records for DTOs?**  
Java records provide immutability, `equals`/`hashCode`/`toString` for free, and are concise without annotation processors. They are the idiomatic Java 16+ alternative to Lombok `@Value`.

**Why fail-fast on startup?**  
If the countries dataset cannot be loaded, every subsequent routing request will fail. Failing at startup (via `@PostConstruct` throwing `IllegalStateException`) surfaces the problem immediately and prevents a degraded service from silently returning errors.

