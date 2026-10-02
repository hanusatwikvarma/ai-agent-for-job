# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Job Application Agent** — a multi-module Spring Boot + React platform that scrapes
jobs, scores them with Claude, tracks applications in MongoDB, auto-fills submissions
via Playwright, and sends email notifications.

- Java **17** (do not bump without discussion)
- Spring Boot 3.4.4, Spring Cloud 2024.0.0
- Kafka + MongoDB + Redis via `docker-compose.yml`
- Group: `com.jobagent`, root artifact: `job-application-agent`

## Modules

| Module                          | Port | Purpose                                   |
| ------------------------------- | ---- | ----------------------------------------- |
| `common`                        | —    | Shared DTOs, events, enums, Kafka topics  |
| `api-gateway`                   | 8080 | Spring Cloud Gateway routing              |
| `job-scraper-service`           | 8081 | Scheduled scrape; publishes `raw-jobs`    |
| `jd-matching-service`           | 8082 | Claude scorer; writes `job-matches`       |
| `application-tracker-service`   | 8083 | REST pipeline + profile; MongoDB-backed   |
| `application-submitter-service` | 8084 | Playwright auto-fill (dry-run default)    |
| `notification-service`          | 8085 | SMTP emails on status changes             |
| `frontend/`                     | 5173 | React + Vite + TanStack Query             |

## Kafka topics (single source of truth: `com.jobagent.common.kafka.KafkaTopics`)

- `raw-jobs` — scraper → matcher
- `matched-jobs` — matcher → tracker
- `application-events` — tracker → submitter + notification
- `notification-events` — tracker/submitter → notification

## Build & run

```bash
# Infra
docker compose up -d

# Build everything
./mvnw -DskipTests package

# Run a specific service
./mvnw -pl job-scraper-service spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

## Conventions

- Use Lombok (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`) for boilerplate.
- Use `JobSource` interface for any new job portal — never hard-code calls.
- The scraper's **MockJobSource** is enabled by default so the pipeline can run
  without external calls. Keep it that way for tests.
- The submitter must stay in **dry-run** unless `SUBMITTER_DRY_RUN=false` is set
  explicitly in env — never commit a change that flips the default.
- All MongoDB collections live in the single `jobagent` database.

## Legal

Scraping LinkedIn / Naukri violates their ToS. The scraper has guards (sources
disabled by default) and the Mock source for safe local use. For production,
swap in an official API via a new `JobSource` implementation.