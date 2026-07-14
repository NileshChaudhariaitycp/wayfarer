# How to Run & Test This Project

Quick-reference for starting the project from scratch and testing it —
whether it's your first time today or you're picking it back up after a
break. For deeper explanations of *why* things work this way, see
[README.md](README.md) and [`docs/adr/`](docs/adr/).

---

## ✅ Prerequisites

**Always needed, no matter which path below you use:**

| Tool | Why |
|---|---|
| **Git** | Clone/pull the repo |
| **JDK 17** | Compiles and runs every service |
| **Maven 3.9+** | Builds the project, runs `mvn spring-boot:run` |

A code editor (VS Code or otherwise) is just a convenience — any terminal
works.

**Only needed for the Docker Compose path:**

| Tool | Why |
|---|---|
| **Docker Desktop** | Runs Postgres, Redis, Kafka, Zipkin, Prometheus, and all 11 services as containers |

**Only needed for the Kubernetes path** (not covered step-by-step below —
see README's "Run it on Kubernetes instead" section):

| Tool | Why |
|---|---|
| **Docker Desktop** | `kind` runs cluster nodes *as* Docker containers |
| **kind** | Creates the local Kubernetes cluster |
| **kubectl** | Talk to the cluster |
| **Helm** | Installs the parameterized chart |

**Only needed for testing, not for running:**

| Tool | Why |
|---|---|
| **Bruno** (or curl/Postman/Insomnia) | Hitting the API endpoints — any HTTP client works, Bruno's just what the ready-made collection in `bruno/` targets |

**The practical takeaway:** the plain-Maven path below only needs Git +
JDK 17 + Maven — nothing else. It's the most dependency-light and most
reliable option if Docker Desktop is ever acting up.

---

## ⭐ Fastest path: Docker Compose

**Use this if Docker Desktop is running.**

```bash
docker compose up --build -d
```

Wait 1-2 minutes, then confirm everything is up:

```bash
docker compose ps
```

Every service should show `Up`. Skip to **[Testing with Bruno](#-testing-with-bruno)** below.

To stop everything:

```bash
docker compose down
```

---

## 🔧 Fallback path: Plain Maven in VS Code (no Docker needed)

**Use this if Docker isn't available or is acting up.** Uses in-memory H2
databases — nothing else needs to be installed or running.

### Step 1 — Open the project

1. Open VS Code → **File → Open Folder** → select the project folder
2. Open a terminal: **Terminal → New Terminal** (or `` Ctrl+` ``)

### Step 2 — Start services in order (one terminal tab per service)

> **Important:** each command below **keeps running** — don't press Ctrl+C
> after it starts. Open a **new terminal tab** for each one (click the
> **`+`** icon in the terminal panel), and leave every tab open.

**2a. Start these two first, one at a time, waiting for each to finish
booting** (watch for `Started ...Application` in the log):

```bash
mvn spring-boot:run -pl discovery-server
```
```bash
mvn spring-boot:run -pl config-server
```

**2b. Then start these 8, in any order, each in its own new terminal tab:**

```bash
mvn spring-boot:run -pl auth-service
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl flight-service
mvn spring-boot:run -pl hotel-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl loyalty-service
mvn spring-boot:run -pl booking-service
mvn spring-boot:run -pl notification-service
```

**2c. Start this one last** (it routes to everything above, so it needs
them registered with `discovery-server` first):

```bash
mvn spring-boot:run -pl api-gateway
```

### Step 3 — Confirm it's up

```bash
curl http://localhost:8080/actuator/health
```

Should return `{"status":"UP"}`. Everything is now reachable at
**`http://localhost:8080`**.

> **Known cosmetic quirk:** `flight-service`/`hotel-service`'s own
> `/actuator/health` shows `DOWN` in this mode — that's just an unused
> Redis health check failing (no Redis running locally), not a real
> problem. Search/booking work completely normally regardless. See
> README's "Run it locally instead" section for why.

### To stop everything

Go to each terminal tab and press `Ctrl+C`, or just close the VS Code
window.

---

## 🧪 Testing with Bruno

1. Open the **Bruno** app (separate desktop app, not inside VS Code)
2. **Open Collection** → select the `bruno/` folder inside this project
3. Top-right dropdown → select the **local** environment
4. Click through the collection folders in this order:

| Step | Folder → Request | What happens |
|---|---|---|
| 1 | **Auth → Login (Customer)** (or Register) | Click **Send** — JWT auto-saved, nothing to copy |
| 2 | **Flights → Search Flights** | Click **Send** — public, no login needed |
| 3 | **Bookings → Book Flight** | Click **Send** — runs the full Saga (reserve seat → pay → earn points) |
| 4 | **Bookings → Get Booking By Id** | Click **Send** — auto-uses the booking id from step 3 |
| 5 | **Loyalty → My Loyalty Balance** | Click **Send** — see points credited |
| 6 | **Bookings → Cancel Booking** | Click **Send** — refunds + reverses points + releases the seat |
| 7 | **Auth → Login (Admin)** | Click **Send** — separate admin token, doesn't overwrite your customer one |
| 8 | **Admin → List All Bookings** / **Create Flight** | Click **Send** — admin-only endpoints |

Every request has a **Docs** tab with extra notes (e.g. how to test a
declined payment with `cardToken: "FAIL_CARD"`).

---

## Things worth trying once it's running

- Book with `"cardToken": "FAIL_CARD"` → payment declines, seat is
  automatically released
- Try **Admin** requests with a CUSTOMER token instead of `{{adminToken}}`
  → expect `403`
- Cancel the same booking twice → second call is a clean no-op
- Check the `notification-service` terminal output after a booking for a
  `[MOCK EMAIL] ...` line (Kafka event was consumed)
