# FOREP AI Workforce Intelligence Platform

Backend monolith for workforce analytics, task integrations, time tracking, and AI-powered workforce insights.

## Stack

- Java 21
- Spring Boot 3.2
- PostgreSQL + Flyway
- JWT Security
- LangChain4j + Google Gemini

## Run locally

```bash
docker compose up -d
cd backend
mvn spring-boot:run
```

The local API runs on `http://localhost:8080`.

Required for AI insight generation:

```bash
GEMINI_API_KEY=<your-key>
```
