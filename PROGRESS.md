# Job Application Agent — Progress Report

A plain-English explanation of what has been built in this repo so far, what works today, and what is still a stub.

---

## 1. The big picture

We are building a **multi-service AI job-application agent**. The idea is a pipeline where each stage does one job and hands off to the next through a message queue (Kafka). The stages are:

```
   ┌──────────────────────────┐
   │  Scrape job listings     │  (every 3 hrs via cron)
   └──────────────┬───────────┘
                  ▼  Kafka topic: raw-jobs
   ┌──────────────────────────┐
   │  Score fit with Claude   │  (resume vs JD → 0–100)
   └──────────────┬───────────┘
                  ▼  Kafka topic: matched-jobs
   ┌──────────────────────────┐
   │  Track in pipeline       │  (FOUND / APPLIED / INTERVIEW / …)
   └──────────────┬───────────┘
                  ▼  Kafka topic: application-events
   ┌──────────────────────────┐    ┌────────────────────────────┐
   │  Auto-fill & submit      │    │  Email you on changes      │
   │  (Playwright, dry-run)   │    │  (SMTP + Thymeleaf)        │
   └──────────────────────────┘    └────────────────────────────┘
```

A React dashboard sits on top via a Spring Cloud **API Gateway**.

---

## 2. What is in the repo (file-by-file)

### 2.1 Root files

| File | Purpose |
|---|---|
| `pom.xml` | Multi-module parent POM. Pins Spring Boot 3.4.4, Spring Cloud 2024.0.0, Java 17, Lombok, MapStruct, Jsoup, Playwright, Testcontainers. Lists all 7 child modules. |
| `docker-compose.yml` | Spins up all infra containers (Kafka+Zookeeper, MongoDB+Mongo-Express, Redis, Mailhog, Kafka UI). |
| `.env.example` → `.env` | Config values: Mongo creds, Anthropic API key, mail server, scrape schedule, user email, submitter dry-run flag. |
| `.gitignore` | Standard Maven/IDE/Node/Playwright ignores plus `.env` to keep secrets out of git. |
| `README.md` | Public-facing readme (quick start, architecture diagram). |
| `CLAUDE.md` | Guidance file for Claude Code (conventions, module table, build commands). |
| `mvnw`, `.mvn/` | Maven wrapper so you don't have to install Maven globally. |

### 2.2 The 7 Maven modules

| Module | Port | What it is today | Lines of real code |
|---|---|---|---|
| `common` | — | Shared library. Contains `JobDto`, `MatchResultDto`, `ApplicationStatusEvent`, enums (`ApplicationStatus`, `EmploymentType`, `JobPortal`), and the `KafkaTopics` constants. | **Done** — reusable building blocks. |
| `job-scraper-service` | 8081 | **Fully built.** See §3. | **Done** |
| `jd-matching-service` | 8082 | Only a `main` method and `application.yml` with Mongo + Kafka + Anthropic config wired. No consumer, no scorer yet. | **Stub** |
| `application-tracker-service` | 8083 | Only a `main` method and `application.yml`. No controllers, no Mongo documents yet. | **Stub** |
| `application-submitter-service` | 8084 | Only a `main` method. Playwright dependency declared but no browser automation yet. | **Stub** |
| `notification-service` | 8085 | Only a `main` method. Mail server config wired to Mailhog. No consumer, no templates yet. | **Stub** |
| `api-gateway` | 8080 | **Fully built.** Spring Cloud Gateway with routes for all backend services + CORS. | **Done** |

### 2.3 Frontend

| File | Purpose |
|---|---|
| `frontend/package.json` | React 18, Vite 5, TanStack Query, Axios, React Router. |
| `frontend/vite.config.ts` | Dev server on `:5173`. Proxies `/api/**` to the gateway at `:8080`. |
| `frontend/index.html`, `src/main.tsx`, `src/App.tsx` | A one-screen dashboard with a "Run now" button that POSTs to `/api/scraper/run` and shows the result. |
| `frontend/src/styles.css` | Minimal styling. |

---

## 3. What the Job Scraper Service actually does

This is the one backend service that is fully implemented. Here is how it works:

### 3.1 Startup
`JobScraperApplication.java` boots Spring Boot with `@EnableScheduling` and `@EnableConfigurationProperties`.

### 3.2 Configuration (`ScraperProperties.java`)
Reads from `application.yml` / env vars:
- `scraper.keywords` — e.g. `java, spring boot, backend`
- `scraper.locations` — e.g. `remote, bangalore`
- `scraper.max-results-per-query` — how many jobs per keyword × location
- `scraper.sources.mock-enabled` / `naukri-enabled` / `linkedin-enabled` — flags
- `scraper.cron` — defaults to `0 0 */3 * * *` (every 3 hours at HH:00)
- `scraper.dedup.ttl` — Redis dedup TTL (14 days)

### 3.3 Pluggable sources
There is a `JobSource` interface so new portals can be added as drop-in beans:

- **`MockJobSource`** (enabled by default). Generates 5 fake jobs per query so you can run the whole pipeline without hitting any external site. Great for local testing.
- **`NaukriJobSource`** (disabled by default). Uses Jsoup to hit `naukri.com` search URLs and parse the result HTML. Fragile — the DOM changes.
- **`LinkedInJobSource`** (disabled by default). Uses the public "guest" job endpoint. Heavily rate-limited and often blocked.

> **Why Mock is the default:** scraping LinkedIn / Naukri violates their ToS and gets you IP-banned quickly. For production you would add a `JobSource` implementation that calls a legitimate API (Adzuna, JSearch, LinkedIn Jobs API, etc.).

### 3.4 Dedup (`JobDedupService.java`)
Before publishing a job, we check Redis with `setIfAbsent` on `jobagent:seen:<externalId>` with a 14-day TTL. If the key already exists, the job is dropped. This prevents re-sending the same job through the pipeline every 3 hours.

### 3.5 Publisher (`JobPublisher.java`)
Uses `KafkaTemplate<String, JobDto>` to publish each new job to the `raw-jobs` topic. The producer is configured with `acks=all` and idempotence enabled.

### 3.6 Orchestrator (`JobScraperService.java`)
Loops through every enabled source × every keyword × every location, fetches jobs, dedups, publishes, and returns a `ScrapeSummary(fetched, published, skipped)`.

### 3.7 Scheduler (`JobScrapeScheduler.java`)
`@Scheduled(cron = "${scraper.cron}")` — fires the orchestrator on the cron. Default is every 3 hours.

### 3.8 Manual trigger (`ScraperController.java`)
`POST /api/scraper/run` runs the orchestrator immediately and returns the summary. Used by the frontend "Run now" button.

---

## 4. Infrastructure (docker-compose)

All third-party systems run as Docker containers via Colima (we installed Colima + Docker CLI + docker-compose plugin earlier).

| Container | Port(s) | What it is | Admin UI |
|---|---|---|---|
| `jobagent-zookeeper` | 2181 | Kafka's coordination layer | — |
| `jobagent-kafka` | 9092, 29092 | Message broker — the backbone of inter-service comms | — |
| `jobagent-kafka-ui` | 8090 | Web UI to browse topics / messages | http://localhost:8090 |
| `jobagent-mongo` | 27017 | Document DB for jobs, matches, applications, profile | — |
| `jobagent-mongo-express` | 8091 | Web UI for Mongo | http://localhost:8091 |
| `jobagent-redis` | 6379 | Used by the scraper for dedup | — |
| `jobagent-mailhog` | 1025 (SMTP), 8092 (UI) | Fake SMTP server; emails show up in its UI instead of being sent for real | http://localhost:8092 |

---

## 5. Kafka topics (defined in `common/KafkaTopics.java`)

| Topic | Produced by | Consumed by | Payload |
|---|---|---|---|
| `raw-jobs` | **scraper** ✅ | matcher (TODO) | `JobDto` |
| `matched-jobs` | matcher (TODO) | tracker (TODO) | `MatchResultDto` |
| `application-events` | tracker (TODO) | submitter + notification (TODO) | `ApplicationStatusEvent` |
| `notification-events` | tracker/submitter (TODO) | notification (TODO) | `ApplicationStatusEvent` |

Only `raw-jobs` has real traffic today.

---

## 6. What we proved working today

1. **Installed Colima + Docker + docker-compose** on your Mac (you only had Java 23, Node, npm, Homebrew before).
2. **`docker compose up -d`** → all 7 infra containers came up healthy.
3. **`./mvnw ... install`** → whole 7-module Maven project built with `BUILD SUCCESS`.
4. **`./mvnw -pl job-scraper-service spring-boot:run`** → scraper started on :8081, loaded 3 beans implementing `JobSource` (Mock + Naukri + LinkedIn), connected to Kafka, connected to Redis.
5. **`POST /api/scraper/run`** returned `{fetched: 30, published: 30, skipped: 0}`:
   - 30 = 3 keywords × 2 locations × 5 mock jobs each
   - All 30 were new, so all 30 published, none skipped
6. **Verified in Kafka UI** — the `raw-jobs` topic contained the 30 valid JSON `JobDto` messages.
7. **Started API Gateway on :8080 and React UI on :5173.**
8. **Clicked "Run now" in the browser** — request flowed `browser → Vite proxy → gateway → scraper → Kafka` and the result appeared on the page. End-to-end chain confirmed.

---

## 7. Known gaps / known not-done

1. The 4 stub services do nothing yet (jd-matching, tracker, submitter, notification).
2. The MockJobSource uses `UUID.randomUUID()` as `externalId`, so dedup can't demo on repeat runs (every mock job looks new).
3. No tests have been written.
4. No user profile / resume storage yet. (This is what you need to supply next for matching + auto-apply to be meaningful.)
5. Scraper's Naukri/LinkedIn parsers assume a specific HTML structure that changes often — they will need maintenance when enabled. Prefer an official API.
6. Playwright not installed yet (it's in the pom but the browsers haven't been downloaded).
7. No auth on the gateway. Fine for localhost; not fine if ever exposed.
8. The notification service's "to" address is currently your email from `.env` — single-user design.

---

## 8. How to start everything again later

```bash
cd /Users/hanusatwikvarma/IdeaProjects/untitled5

# 1. Start infra
colima start                      # if Colima isn't already running
docker compose up -d

# 2. Build (first time only after a clean checkout)
./mvnw -DskipTests install

# 3. Run the services you need
./mvnw -pl job-scraper-service spring-boot:run    # terminal 1
./mvnw -pl api-gateway        spring-boot:run    # terminal 2
cd frontend && npm install && npm run dev        # terminal 3

# 4. Visit http://localhost:5173
```

---

## 9. The next three steps (in order)

1. **Application Tracker + User Profile service** (no API key needed). Stores your personal details, resume, preferred roles. REST API for the pipeline (FOUND → APPLIED → INTERVIEW …).
2. **JD Matching Service** (needs `ANTHROPIC_API_KEY`). Consumes `raw-jobs`, calls Claude with your resume + each JD, writes a score (0–100) + strengths/gaps/recommendation to Mongo, republishes to `matched-jobs`.
3. **Notification Service**. Consumes `application-events`, sends Thymeleaf email through SMTP → Mailhog in dev, real SMTP in prod.

Submitter comes after, since it needs the profile + matching to be useful.
