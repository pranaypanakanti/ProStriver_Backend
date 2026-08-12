<div align="center">

# 🚀 ProStriver

### Production Spring Boot backend for study planning, spaced repetition & AI-generated learning plans

**Designed for scale. Deployed lean. Debugged in production.**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Live](https://img.shields.io/badge/Live-prostriver.me-00C853?style=for-the-badge&logo=vercel&logoColor=white)](https://prostriver.me)

🌐 **Live:** [prostriver.me](https://prostriver.me) &nbsp;

</div>

---

## 📑 Table of Contents

[Overview](#-overview) · [Engineering Highlights](#-engineering-highlights) · [Architecture](#-architecture) · [The Async AI Pipeline](#-the-async-ai-pipeline) · [Tech Stack](#-tech-stack) · [Features](#-features) · [Design Decisions](#-design-decisions) · [Production Lessons](#-production-lessons) · [Project Structure](#-project-structure) · [Getting Started](#-getting-started) · [CI/CD](#-cicd) · [Roadmap](#-roadmap)

---

## 💡 Overview

ProStriver is a backend for students and self-learners: track what you study, get spaced-repetition reminders, stay consistent through streak challenges, and generate **AI-powered, structured study plans** you can work through subtopic by subtopic.

It's two things at once — a **live product** with real users, and a **systems-engineering project** built to demonstrate end-to-end backend design under real constraints. It's deliberately provisioned small (a single 1 GB box), so almost every decision was a genuine tradeoff: stateless and horizontally scalable in *shape*, async-decoupled for slow work, polyglot in its storage, and hardened by lessons from real production incidents.

> **Scale:** ~200–300 users · 10–15 concurrent · running on AWS `t3.micro` + a DigitalOcean droplet.

---

## ⭐ Engineering Highlights

- **Async LLM pipeline over Kafka** — AI study-plan generation runs off the request thread through a Confluent Kafka queue with at-least-once delivery, idempotent consumers, and client polling.
- **Polyglot persistence by data shape** — PostgreSQL for relational data, MongoDB for nested plan documents, Redis for cache + coordination — each chosen for what its data actually *is*.
- **Modular monolith, two roles from one artifact** — a single fat JAR runs as an `api` node or a `worker` node via Spring profiles; the heavy AI subsystem is gated entirely off the worker box.
- **Distributed rate limiting** — Bucket4j token buckets backed by Redis, correct across instances, capping the expensive LLM endpoint per user.
- **Resilient by design** — graceful Redis degradation (cache is never a single point of failure), per-item transactional isolation in batch jobs, atomic idempotent document updates, JVM tuning + swap for a memory-constrained host.
- **Secure by default** — stateless JWT (access + rotating refresh), OTP verification, RBAC, and object-level authorization (ownership checks returning `404`, not `403`).

---

## 🏗️ Architecture

A single Spring Boot artifact, deployed as **two roles** selected by profile. All state is external, so the app instances are disposable and replaceable.

```
                         ┌──────────────────────────────────────┐
                         │         GitHub Actions  (CI/CD)        │
                         │   build fat JAR → Docker Hub → deploy   │
                         └───────────────┬───────────┬───────────┘
                                         │           │
                  ┌──────────────────────▼──┐     ┌──▼───────────────────────┐
                  │   AWS EC2 t3.micro       │     │  DigitalOcean 512 MB      │
                  │   profile: api           │     │  profile: worker          │
                  │   • REST API · JWT       │     │  • nightly schedulers     │
                  │   • Kafka producer+consumer│   │  • digests · streaks      │
                  │   • Bucket4j rate limit  │     │  • (no Kafka/Mongo/LLM)   │
                  │   • Gemini (Spring AI)    │    └──────────┬────────────────┘
                  └───┬─────────┬─────────┬──┘                │
       cache/limit ▲  │         │         │  plan docs        │ progress · emails
                   │  ▼         ▼         ▼                   ▼
            ┌──────┴──┐  ┌───────────┐  ┌──────────┐   ┌────────────────────┐
            │  Redis  │  │ PostgreSQL │  │ MongoDB  │   │ PostgreSQL · Brevo │
            │  Cloud  │  │ (Supabase) │  │ (Atlas)  │   │  (shared)          │
            └─────────┘  └───────────┘  └──────────┘   └────────────────────┘
                                  ▲
                        ┌─────────┴──────────┐
                        │  Confluent Kafka    │  study-plan-jobs (SASL_SSL)
                        │  2 partitions       │  async LLM job queue
                        └────────────────────┘
```

| Profile | Host | Responsibility |
|---|---|---|
| `api` | AWS EC2 `t3.micro` | All HTTP traffic, JWT auth, CRUD, the async AI pipeline (Kafka producer + consumer) |
| `worker` | DigitalOcean 512 MB | Nightly scheduled jobs only — digests, streaks, analytics, cleanup |

---

## 🤖 The Async AI Pipeline

Generating a plan calls an LLM (several seconds), so it never blocks a request thread:

```
POST /api/study-plan ─▶ rate-limit (Redis) ─▶ create job QUEUED (Mongo)
                     ─▶ publish to Kafka (keyed by userId) ─▶ 202 { jobId }   ← returns instantly

   Kafka consumer ─▶ idempotency guard (skip if DONE) ─▶ PROCESSING
                  ─▶ Gemini via Spring AI (multi-key rotation + Redis cooldown)
                  ─▶ DONE + plan written in one atomic document update

   Client polls GET /api/study-plan/{jobId} until status = DONE
```

Once generated, the plan is interactive: `PATCH /{jobId}/start` begins it, and `PATCH /{jobId}/subtopic/{subtopicId}` ticks a subtopic done/undone via a single **atomic, idempotent** MongoDB positional update that keeps the progress counter exact even under concurrent clicks.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language / Framework** | Java 21 · Spring Boot 3.5 |
| **Security** | Spring Security 6 · JWT (jjwt) · OTP · RBAC |
| **Relational** | PostgreSQL (Supabase) · Spring Data JPA / Hibernate |
| **Document store** | MongoDB (Atlas) · Spring Data MongoDB |
| **Cache / coordination** | Redis (Redis Cloud) · Lettuce |
| **Messaging** | Apache Kafka (Confluent Cloud, SASL_SSL) · Spring Kafka |
| **AI** | Spring AI · Google Gemini (key rotation) |
| **Rate limiting** | Bucket4j (Redis-backed token bucket) |
| **Email** | Brevo transactional API |
| **Build** | Maven (multi-module) |
| **Containerization / CI/CD** | Docker (multi-stage) · GitHub Actions |
| **Cloud** | AWS EC2 + DigitalOcean |
| **API docs** | SpringDoc OpenAPI (Swagger UI) |

---

## ✨ Features

- **🔐 Auth & Security** — email/password signup with **OTP**, **JWT** access tokens + **rotating refresh** tokens (HTTP-only cookie), forgot/reset/change password, logout-all-devices, role-based access control.
- **📚 Topics & Spaced Repetition** — full topic CRUD with search/filter/pagination, automatic revision scheduling, daily **8 AM IST email digests** (Brevo), mark-complete with auto-completion, end-of-day cancellation of missed revisions.
- **🤖 AI Study Plans** — async, structured plans (`mainTopics → subTopics` with resources, key concepts, common mistakes, tips), per-subtopic completion tracking with an exact server-owned progress counter.
- **🏆 Lock-In Challenges** — 30/100/365-day streak challenges evaluated nightly by a rule engine.
- **📊 Analytics** — daily progress snapshots and month-to-date summaries.
- **🛡️ Admin** — revision-plan management, user management, health checks.

---

## 🧩 Design Decisions

| Decision | Choice | Why |
|---|---|---|
| Monolith vs microservices | **Modular monolith** (2 Maven modules, 1 JAR) | 10–15 concurrent users; two JVMs won't fit 1 GB. Clean module boundaries without the cost of separate deployments. |
| Slow LLM work | **Async via Kafka + polling** | A multi-second call must not hold a request thread; web and work tiers scale independently. |
| Kafka placement | **Confluent Cloud (managed)** | Keeps the broker off the 1 GB box, adds SASL_SSL + replication, mirrors how teams actually run it. Honestly: at this scale Kafka is learning-driven — a DB queue would also work. |
| Storage | **Polyglot** | Plans are document-shaped → Mongo; relational/ACID data → Postgres; ephemeral fast state → Redis. |
| Job lifecycle | **One Mongo document** (status + plan) | Completion is a single atomic write, not a cross-database update with a consistency gap. |
| Deployment | **Spring profiles** (`api` / `worker`) | One image, two roles; heavy AI subsystem gated off the small worker. |
| User-data deletes | **Soft delete** (`deletedAt`) | Sidesteps FK-ordering hazards and preserves history. |

---

## 🔧 Production Lessons

Real incidents debugged on the live system — the most interview-worthy part of the project:

- **TLS handshake failures** → drive transport security from the connection-string *scheme* (`rediss://` vs `redis://`), never hardcode it. The same idea later made one Kafka config work locally and on Confluent.
- **"Works, then fails after idle"** → managed services close idle connections; configure keepalive, timeouts, and reconnect deliberately.
- **A nightly job that silently rolled back its whole batch** → once a statement fails, the transaction is poisoned; per-item work needs `REQUIRES_NEW` in a **separate bean** (a self-call bypasses the proxy).
- **Foreign-key delete ordering** → isolation makes a batch job *honest*; clearing children (or soft-deleting) makes it *work* — two different fixes.
- **OOM on 1 GB** → swap, heap caps, profile-gated subsystems, and ultimately a managed broker.

---

## 📁 Project Structure

Maven multi-module — `prostriver-app` (the runnable API + workers) depends on `prostriver-planner` (the AI study-plan library):

```
prostriver-parent/                       ← parent POM (packaging: pom)
├── prostriver-app/                       ← Spring Boot app → fat JAR  (com.proStriver)
│   └── src/main/java/com/proStriver/
│       ├── auth/  topic/  challenge/     ← core domains (controllers, services, schedulers)
│       ├── analytics/  user/  admin/
│       ├── studyplan/                    ← secured study-plan controller (principal-based)
│       ├── security/                     ← JWT filter, security config, user details
│       ├── config/                       ← profiles, Redis, scheduling, planner wiring
│       ├── entity/  repository/          ← JPA entities & Spring Data repos
│       └── common/                       ← global exception handler, Redis service, crypto
│
└── prostriver-planner/                   ← AI library module  (com.springAi)
    └── src/main/java/com/springAi/
        ├── studyPlanner/                 ← Gemini study-plan service + entities + job model
        ├── kafka/                        ← producer, consumer, topic & error-handler config
        ├── ratelimit/                    ← Bucket4j (Redis) rate limiting
        └── gemini/                       ← Gemini key rotation + cooldown manager
```

---

## 🚀 Getting Started

**Prerequisites:** Java 21, Maven 3.9+ (or the bundled `./mvnw`), Docker. The app expects external Postgres, Redis, MongoDB, and a Kafka cluster (or local equivalents).

```bash
# Clone
git clone https://github.com/pranaypanakanti/ProStriver_Backend.git
cd ProStriver_Backend

# Configure (env vars — never commit secrets)
export DB_URL=jdbc:postgresql://localhost:5432/prostriver
export DB_USER=...      DB_PASSWORD=...
export REDIS_URI=redis://localhost:6379
export MONGODB_URI=mongodb://localhost:27017/prostriver
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export JWT_SECRET_BASE64=...   BREVO_API_KEY=...   GEMINI_API_KEYS=key1,key2

# Build the whole reactor (produces the app fat JAR)
./mvnw clean package

# Run the API node
java -jar prostriver-app/target/*.jar --spring.profiles.active=api

# Run the worker node (scheduled jobs)
java -jar prostriver-app/target/*.jar --spring.profiles.active=worker
```

**Key environment variables:** `DB_URL` · `DB_USER` · `DB_PASSWORD` · `REDIS_URI` · `MONGODB_URI` · `KAFKA_BOOTSTRAP_SERVERS` · `KAFKA_API_KEY` · `KAFKA_API_SECRET` · `JWT_SECRET_BASE64` · `BREVO_API_KEY` · `GEMINI_API_KEYS` · `SPRING_PROFILES_ACTIVE`.

> The `api` profile loads Kafka, MongoDB, and Gemini; the `worker` profile excludes them and runs only the schedulers.

---

## 🔄 CI/CD

Every push to `main` triggers GitHub Actions:

```
push → build multi-stage Docker image → push to Docker Hub
     → SSH deploy to EC2 (api)  +  SSH deploy to DigitalOcean (worker)
     → docker compose pull / up · prune
```

Both nodes run the **same image** (`pranaypanakanti/prostriver:latest`), differentiated only by `SPRING_PROFILES_ACTIVE`.

---

## 🗺️ Roadmap

- [x] Async AI study-plan pipeline (Kafka + Mongo + Gemini)
- [x] Per-subtopic completion with atomic, idempotent updates
- [x] Multi-module merge + api/worker profile split + managed Kafka
- [ ] Google / GitHub **OAuth 2.0**
- [ ] **Pro tier** — virtual credits ledger + OpenAI routing + Stripe (sandbox-first)
- [ ] Dead-letter topic processing for failed jobs
- [ ] Observability — structured logging + correlation IDs across the async pipeline
- [ ] Automated test suite (Testcontainers for Postgres/Mongo, embedded Kafka)
- [ ] SSE / WebSocket push (upgrade from polling)

---

## 🧑‍💻 Author

<div align="center">

**Pranay Panakanti**

[![GitHub](https://img.shields.io/badge/GitHub-pranaypanakanti-181717?style=for-the-badge&logo=github)](https://github.com/pranaypanakanti)

⭐ *If this project taught you something or sparked an idea, drop a star.*

</div>
