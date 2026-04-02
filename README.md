<div align="center">

# 🚀 ProStriver

### A Production-Grade Productivity & Spaced Repetition Platform

**Learn smarter. Revise on time. Stay consistent.**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Live](https://img.shields.io/badge/Live-prostriver.me-00C853?style=for-the-badge&logo=vercel&logoColor=white)](https://prostriver.me)

---
🌐 **Go explore** [https://prostriver.me](https://prostriver.me) 

</div>

---

## 📸 Screenshots

<!-- Add your screenshots below. Replace the placeholder URLs with actual image paths. -->

| Dashboard | Revisions |
|:---------:|:------:|
|<img width="640" height="360" alt="Screenshot 2026-04-02 105826" src="https://github.com/user-attachments/assets/2a6923d4-a98c-43d5-bc42-a18124fa53b7" />|<img width="720" height="480" alt="Screenshot 2026-04-02 112517" src="https://github.com/user-attachments/assets/9f701aeb-bb88-47ce-859f-75b4ea86df86" />
|

| Challenge Streak | Email Reminder |
|:----------------:|:--------------:|
|<img width="640" height="360" alt="Screenshot 2026-04-02 104404" src="https://github.com/user-attachments/assets/44b36e02-0deb-407c-9e3d-506f8d6b4656" /> | <img width="638" height="470" alt="Screenshot 2026-04-02 104643" src="https://github.com/user-attachments/assets/67e84b2b-0b10-4472-b641-2b88b4698a6c" /> |

---

## 💡 What is ProStriver?

ProStriver is a **full-stack productivity platform** built for students and self-learners who want to **track what they study**, **never forget what they learn**, and **build consistency through challenges**.

The backend is designed as a **production-ready, cloud-deployed Java application** with a clean separation between the API server and background worker — both running from a single codebase using Spring Profiles.

### 🎯 Key Problems It Solves

- **Forgetting what you studied** → Automated spaced repetition reminders via email
- **No visibility into learning habits** → Daily/monthly analytics with streak tracking
- **Lack of accountability** → Lock-in challenges (30-day, 100-day, 365-day)
- **Scattered topic management** → Centralized topic CRUD with search, filter & pagination

---

## ✨ Features

### 🔐 Authentication & Security
- Email/password signup with **OTP verification** (6-digit, time-limited)
- **JWT access tokens** (short-lived) + **HTTP-only cookie refresh tokens** (long-lived, rotating)
- Forgot password / reset password flow with OTP
- Change password for authenticated users
- Logout (single device) and **logout-all-devices** (revokes every refresh token)
- Role-based access control (**USER** / **ADMIN**)
- Brute-force protection: OTP rate limiting, device fingerprinting

### 📚 Topic Management
- Full CRUD: create, read, update, soft-delete, archive
- Paginated listing with **search** (title, subject, notes), **status filter**, and **date filter**
- Assign a **revision plan** (admin-defined) or a **manual reminder pattern** (e.g., `2,3,7,14,30`)
- ML-summary endpoint for downstream integrations

### 🔁 Spaced Repetition Engine
- Automatic scheduling of revision dates based on plan or manual pattern
- **Daily email digest** at 8:00 AM IST via [Brevo (Sendinblue) Transactional API](https://www.brevo.com/)
- Per-user notification preferences (EMAIL / NONE)
- Today's revisions endpoint + upcoming revisions endpoint
- Mark revision as complete → auto-completes topic when all revisions are done
- **End-of-day canceller**: missed revisions auto-cancel at midnight IST

### 📊 Analytics & Insights
- **Daily progress** snapshots: topics created, revisions due/completed/cancelled
- **Monthly summary** with month-to-date aggregations
- Analytics overview API for frontend dashboards

### 🏆 Lock-In Challenges
- Configurable challenge types (e.g., 30-day, 100-day, 365-day)
- Daily streak tracking via scheduled job
- Start, view status, and quit challenges
- Rule engine for challenge validation

### 👤 User Profile
- View and update profile (display name, notification preferences)
- Admin endpoints for user management and revision plan CRUD

### 🛡️ Admin Panel
- Admin-only revision plan management (create, update, delete, list)
- Admin user listing and management
- Health check endpoint

---

## 🏗️ Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                        GitHub Actions CI/CD                       │
│          Build Docker Image → Push to Docker Hub → Deploy         │
└───────��──────┬────────────────────────────┬───────────────────────┘
               │                            │
               ▼                            ▼
    ┌─────────────────────┐     ┌─────────────────────────┐
    │   AWS EC2 Instance  │     │  DigitalOcean Droplet   │
    │   ┌───────────────┐ │     │  ┌────────────────────┐ │
    │   │  API Server   │ │     │  │  Background Worker │ │
    │   │  (profile:api)│ │     │  │  (profile:worker)  │ │
    │   │  Port: 8080   │ │     │  │  Scheduled Jobs    │ │
    │   └───────┬───────┘ │     │  └─────────┬──────────┘ │
    └───────────┼─────────┘     └────────────┼────────────┘
                │                            │
                ▼                            ▼
    ┌─────────────────────────────────────────────┐
    │              PostgreSQL (Shared)             │
    │          Managed / Self-hosted DB            │
    └─────────────────────────────────────────────┘
                         │
                         ▼
    ┌─────────────────────────────────────────────┐
    │           Brevo Transactional Email          │
    │         OTP Emails • Revision Digests        │
    └─────────────────────────────────────────────┘
```

### Dual-Profile Design

A single Spring Boot application runs in **two modes** from the same Docker image:

| Profile | Role | Deployed On | Responsibility |
|---------|------|-------------|----------------|
| `api` | REST API Server | AWS EC2 | Handles all HTTP requests, JWT auth, CRUD operations |
| `worker` | Background Worker | DigitalOcean | Runs scheduled jobs (email digests, streak tracking, analytics, cleanup) |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.12 |
| **Security** | Spring Security 6.3 + JWT (jjwt 0.11.5) |
| **ORM** | Spring Data JPA + Hibernate 6.5 |
| **Database** | PostgreSQL |
| **Email** | Brevo (Sendinblue) Transactional API |
| **API Docs** | SpringDoc OpenAPI 2.5 (Swagger UI) |
| **Validation** | Jakarta Bean Validation |
| **Build** | Maven + Maven Wrapper |
| **Containerization** | Docker (multi-stage build) |
| **Orchestration** | Docker Compose |
| **CI/CD** | GitHub Actions |
| **Cloud** | AWS EC2 (API) + DigitalOcean (Worker) |
| **Code Generation** | Lombok |

---

## 📁 Project Structure

```
src/main/java/com/ProStriver/
├── ProStriverApplication.java        # Entrypoint
│
├── auth/                             # Authentication module
│   ├── AuthController.java           # Signup, login, refresh, logout, password flows
│   ├── AuthService.java              # Business logic for auth
│   ├── RefreshTokenCookieService.java
│   └── dto/                          # Request/response DTOs
│
├── topic/                            # Core topic & revision module
│   ├── TopicController.java          # CRUD + search + pagination
│   ├── TopicService.java             # Topic lifecycle, revision scheduling
│   ├── RevisionController.java       # Today's & upcoming revisions, mark complete
│   ├── RevisionService.java          # Revision business logic
│   ├── RevisionEmailScheduler.java   # Daily email digest (worker)
│   ├── RevisionEndOfDayCanceller.java# Auto-cancel missed revisions (worker)
│   └── dto/
│
├── analytics/                        # Analytics & progress tracking
│   ├── AnalyticsController.java      # Overview endpoint
│   ├── AnalyticsService.java         # Aggregations
│   ├── DailyProgressScheduler.java   # Nightly progress snapshot (worker)
│   ├── MonthlySummaryScheduler.java  # MTD summary updater (worker)
│   └── dto/
│
├── challenge/                        # Lock-in challenge system
│   ├── ChallengeController.java      # Plans, select, status, quit
│   ├── ChallengeService.java         # Challenge lifecycle
│   ├── ChallengeRules.java           # Validation rules
│   ├── ChallengeStreakScheduler.java  # Daily streak evaluation (worker)
│   └── dto/
│
├── user/                             # User profile management
│   ├── UserController.java           # Get/update profile
│   ├── UserService.java
│   └── dto/
│
├── admin/                            # Admin-only endpoints
│   ├── AdminRevisionPlanController.java
│   ├── AdminRevisionPlanService.java
│   ├── AdminUserController.java
│   ├── AdminUserService.java
│   ├── HealthCheck.java
│   └── dto/
│
├── notification/                     # Email integration
│   ├── EmailService.java             # Brevo transactional email client
│   └── BrevoHealthIndicator.java     # Health check for email provider
│
├── security/                         # Security infrastructure
│   ├── JwtAuthenticationFilter.java  # OncePerRequest JWT filter
│   ├── SecurityProperties.java       # Externalized security config
│   └── ...
│
├── config/                           # Application configuration
│   ├── CorsConfig.java               # CORS policy
│   ├── OpenApiConfig.java            # Swagger/OpenAPI setup
│   ├── SchedulingConfig.java         # @EnableScheduling
│   ├── TimeConfig.java               # Clock bean (IST)
│   └── ...
│
├── entity/                           # JPA entities
│   ├── User.java
│   ├── Topic.java
│   ├── RevisionSchedule.java
│   ├── RevisionPlan.java
│   ├── DailyProgress.java
│   ├── MonthlySummary.java
│   ├── LockInChallenge.java
│   ├── Badge.java
│   ├── OtpCode.java
│   ├── RefreshToken.java
│   └── enums/
│
├── repository/                       # Spring Data JPA repositories
│
└── common/                           # Shared utilities
    └── exception/                    # Global exception handler
```

## ⏰ Scheduled Jobs (Worker Profile)

| Job | Schedule (IST) | Description |
|-----|---------------|-------------|
| **RevisionEmailScheduler** | Daily 8:00 AM | Sends revision digest emails to users |
| **RevisionEndOfDayCanceller** | Daily 12:00 AM | Cancels missed (un-completed) revisions |
| **DailyProgressScheduler** | Daily 12:00 AM | Snapshots daily progress per user |
| **MonthlySummaryScheduler** | Daily 12:30 AM | Updates month-to-date summary |
| **ChallengeStreakScheduler** | Daily | Evaluates and updates challenge streaks |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (or later)
- **Maven 3.9+** (or use the included `mvnw`)
- **PostgreSQL 15+**
- **Docker & Docker Compose** (for containerized deployment)

### Local Development

```bash
# 1. Clone the repository
git clone https://github.com/pranaypanakanti/ProStriver_Backend.git
cd ProStriver_Backend

# 2. Set up environment variables (create .env or set in application.yml)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/prostriver
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret_key
export BREVO_API_KEY=your_brevo_api_key

# 3. Run with API profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=api

# 4. (In another terminal) Run worker profile for scheduled jobs
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker
```

### Docker Deployment

```bash
# Build the image
docker build -t prostriver:latest .

# Run API server
docker compose -f docker-compose.api.yml up -d

# Run background worker
docker compose -f docker-compose.worker.yml up -d
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | DB username | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | DB password | ✅ |
| `JWT_SECRET` | Secret key for JWT signing | ✅ |
| `BREVO_API_KEY` | Brevo transactional email API key | ✅ |
| `SPRING_PROFILES_ACTIVE` | `api` or `worker` | ✅ |
| `SERVER_PORT` | Server port (default: 8080) | ❌ |

---

## 🔄 CI/CD Pipeline

Every push to `main` triggers the GitHub Actions pipeline:

```
Push to main
    │
    ▼
┌─────────────────────┐
│  Build Docker Image  │
│  Push to Docker Hub  │
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
┌─────────┐ ┌──────────┐
│ Deploy  │ │  Deploy  │
│   API   │ │  Worker  │
│ (EC2)   │ │  (DO)    │
└─────────┘ └──────────┘
```

- **Docker Hub image:** `pranaypanakanti/prostriver:latest`
- **API deployment:** AWS EC2 via SSH
- **Worker deployment:** DigitalOcean Droplet via SSH
- **Zero-downtime** rolling updates with Docker Compose

---

## 🗃️ Database Schema (Entities)

| Entity | Description |
|--------|-------------|
| `User` | User accounts with roles and notification preferences |
| `Topic` | Learning topics with subject, title, notes, status |
| `RevisionPlan` | Admin-defined revision day templates |
| `RevisionSchedule` | Individual scheduled revision per topic per day |
| `DailyProgress` | Daily snapshot of user activity metrics |
| `MonthlySummary` | Month-to-date aggregated statistics |
| `LockInChallenge` | Active challenge tracking with streaks |
| `Badge` | Gamification badges |
| `OtpCode` | Time-limited OTP codes for verification |
| `RefreshToken` | Hashed refresh tokens with device fingerprinting |

---

## 🧑‍💻 Author

<div align="center">

**Pranay Panakanti**

[![GitHub](https://img.shields.io/badge/GitHub-pranaypanakanti-181717?style=for-the-badge&logo=github)](https://github.com/pranaypanakanti)

</div>

---

<div align="center">

**⭐ If this project helped or inspired you, give it a star!**

</div>
