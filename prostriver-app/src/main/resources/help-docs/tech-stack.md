---
title: Tech Stack
slug: tech-stack
summary: What ProStriver is built with, backend and frontend.
questions:
  - what is prostriver built with
  - what tech stack does prostriver use
  - what database does it use
  - is prostriver built with spring boot
  - what ai model does it use
  - where is prostriver hosted
  - what is the frontend built in
  - what is on the roadmap
  - what features are coming next
  - is there google sign in
  - what is the help assistant
  - can the help assistant see my study plans
  - does prostriver use openai or gemini
---

# Tech Stack

ProStriver is a full-stack platform built with a modern, production-grade toolset across both its backend and frontend.

## Backend

| Layer | Technology |
|---|---|
| Language & Framework | Java 21, Spring Boot |
| Relational Database | PostgreSQL (Supabase) |
| Document Database | MongoDB (Atlas) |
| Cache & Rate Limiting | Redis (Redis Cloud) |
| Messaging | Apache Kafka (Confluent Cloud) |
| AI | Google Gemini (study plans) and OpenAI (help assistant), both via Spring AI |
| Email | Brevo |
| Authentication | JWT (access + refresh tokens) |
| Containerization | Docker |
| CI/CD | GitHub Actions |
| Hosting | AWS EC2 + Heroku |
| Observability | Spring Boot Actuator, Micrometer, Prometheus, Grafana Cloud |

The backend is a modular, multi-module Java build that runs as two distinct roles from the same underlying codebase — one handling live API traffic, the other running scheduled background jobs like email digests and analytics — allowing each to scale and be reasoned about independently.

Study plan generation runs asynchronously: your request is queued through Kafka and processed in the background, so submitting a plan doesn't block on the AI model's response time.

ProStriver uses two AI providers, each picked for a different job. Google Gemini generates study plans in the background, where a longer response time costs you nothing because you are not sitting and waiting on it. OpenAI powers the help assistant, which has to answer while you wait and is set up for a single fast response instead.

## Frontend

| Layer | Technology |
|---|---|
| Framework | React 19 |
| Build Tool | Vite |
| Styling | Tailwind CSS |
| Routing | React Router |
| Icons | Lucide |
| Hosting | Vercel |

The frontend is a fully responsive single-page application, built to work cleanly across desktop and mobile browsers.

## Help Assistant

The help assistant answers questions about ProStriver using this documentation as its only source.

It works one question at a time. Each question is answered on its own with no memory of what came before, so there is no conversation to continue. Ask a follow-up as a complete question rather than assuming it remembers the last one.

It reads only the documentation, never your account. It has no access to your topics, study plans, or revision history, so it cannot tell you what is on your schedule this week or how far through a topic you are.

When the documentation does not cover something, it says so rather than guessing. A question outside ProStriver's scope is answered as out of scope, and a question the documentation touches on but does not actually answer is flagged as such instead of being filled in with an invented response.

## Infrastructure Philosophy

Wherever possible, ProStriver uses managed cloud services (for the database, cache, message queue, and observability) rather than self-hosting everything on a single server. This keeps the core application servers lean and focused purely on serving the product, while specialized infrastructure concerns are handled by providers built specifically for them.

## Roadmap

**Shipped:**
- Full observability stack — Spring Boot Actuator, Micrometer, Prometheus, and Grafana Cloud dashboards, monitoring the live system in real time.

**Upcoming:**
- **More resilient AI plan generation** — additional safeguards around the study-plan generation pipeline so a failed generation is handled and recoverable rather than simply dropped.
- **Sign in with Google** — an additional, faster way to create an account and log in, alongside email/password.

