# Job Application Agent

An AI-powered, multi-service pipeline that:

1. **Scrapes** job listings every 3 hours from pluggable sources (Mock / Naukri / LinkedIn).
2. **Scores** each job against your resume with the Claude API (`claude-sonnet-4-6`).
3. **Tracks** your application pipeline in MongoDB.
4. **Submits** applications automatically via Playwright (dry-run by default).
5. **Notifies** you by email on every status change.

## Architecture

```
                    +---------------------+
    cron(3h)  --->  | job-scraper-service | --Kafka[raw-jobs]--+
                    +---------------------+                    |
                                                               v
                                                    +---------------------+
                                                    | jd-matching-service | --MongoDB(job-matches)
                                                    +---------------------+
                                                               |
                                                   Kafka[matched-jobs]
                                                               v
                                                    +----------------------------+
                                                    | application-tracker-service| --MongoDB(applications, profile)
                                                    +----------------------------+
                                                               |
                                   Kafka[application-events] <-+-> Kafka[application-events]
                                                               |
                     +----------------------------+   +---------------------+
                     | application-submitter-svc  |   | notification-service| --SMTP--> Email
                     +----------------------------+   +---------------------+

                                 +-------------+          +----------+
                 Browser --->    | api-gateway |  <--->   | services |
                                 +-------------+          +----------+
                                        ^
                                        |
                                  +-----------+
                                  | frontend  |  (React + Vite, :5173)
                                  +-----------+
```

## Modules

| Module                          | Port | Description                                  |
| ------------------------------- | ---- | -------------------------------------------- |
| `common`                        | —    | Shared DTOs, events, enums, Kafka topics     |
| `api-gateway`                   | 8080 | Spring Cloud Gateway, CORS, routing          |
| `job-scraper-service`           | 8081 | Scheduled scraping; publishes to `raw-jobs`  |
| `jd-matching-service`           | 8082 | Claude-powered fit scoring; writes MongoDB   |
| `application-tracker-service`   | 8083 | REST pipeline + user profile; MongoDB-backed |
| `application-submitter-service` | 8084 | Playwright auto-fill/submit (dry-run)        |
| `notification-service`          | 8085 | SMTP + Thymeleaf email on status changes     |
| `frontend/`                     | 5173 | React + Vite dashboard                       |

## Prereqs

- Java 17 (`sdk install java 17.0.13-tem`)
- Maven 3.9+ (or use `./mvnw`)
- Docker + Docker Compose
- Node 20+ (frontend)
- An Anthropic API key

## Quick start

```bash
cp .env.example .env       # then edit values, especially ANTHROPIC_API_KEY
docker compose up -d        # Kafka, MongoDB, Redis, Mailhog, Mongo-Express, Kafka UI
./mvnw -pl common install   # publish the shared library to local repo
./mvnw -pl job-scraper-service spring-boot:run

# in another terminal
cd frontend && npm install && npm run dev
```

Open:

- Frontend: http://localhost:5173
- Kafka UI: http://localhost:8090
- Mongo-Express: http://localhost:8091
- Mailhog UI: http://localhost:8092

## Scraping legal/ToS notice

Scraping LinkedIn and Naukri violates their Terms of Service and both actively block
scrapers. The scraper is built around a pluggable `JobSource` interface — for anything
beyond local testing, swap in an **official job-board API** (LinkedIn Jobs API,
RapidAPI JSearch, Adzuna, etc.). The `MockJobSource` is enabled by default so you can
run the whole pipeline end-to-end without touching real sites.

## Auto-submit safety

The submitter service runs in **dry-run mode by default** (`SUBMITTER_DRY_RUN=true`):
it fills forms with your profile data and takes a screenshot but does **not** click
submit. Flip to false only when you've verified the pipeline on specific job sites.