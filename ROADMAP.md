# HireFlow — Target Architecture & Roadmap

## Target architecture (keep it a modular monolith)
```
Angular SPA ──REST/JWT──► Spring Boot modular monolith
                           auth │ user │ jd (+analysis) │ resume │ ai │ interview │ applications │ notifications
                           ai module = the only Ollama client:
                             prompt templates + JSON schema → OllamaService (timeout, retry, circuit breaker,
                             per-task token limit) → strict parser → validation/ClaimGuard → domain service
                           ai_jobs table + executor: long AI work runs async; UI polls job status
                ├── MySQL (system of record, Flyway)
                ├── MongoDB (parsed resume documents — keep; no migration needed now)
                ├── Disk/volume (original uploads)
                └── Ollama (container or host)
```
No microservices: one user-facing workload, one database of record, the AI runtime is already a separate process.
LLM output is never trusted: schema-constrained JSON, strict parsing, domain validation, fallbacks.

## Phases
### P1 — Stabilize (recommended next)
- **Goal:** make what exists reliable and clean before adding features.
- **Changes:** per-task AI output limits + schema field ordering (score last) to cut runaways and fix 0-scores; remove dead `ai` package, unused frontend files and legacy endpoint constants (after approval); fix N+1 in application/skill-gap lists; safe test profile so `./mvnw test` never uses the real DB; rename user-visible branding to HireFlow (UI title, PDF metadata) — package names unchanged; README + CHANGELOG.
- **Files:** `OllamaService`, `resources/ollama/*.json`, services calling `generateJson`, `application/service/impl/*`, `ai/**` (delete), `src/test/resources/application-test.properties`, frontend `core/constants|config`, `index.html`.
- **DB/API:** none (no migration). API unchanged.
- **Tests:** unit tests for per-task limits; repeat the tailored-resume + match E2E steps 3× with `llama3.2:1b`.
- **Acceptance:** all tests green; build clean; no references to removed code; tailored resume succeeds without 503 in 3 of 3 runs; match scores non-zero when strengths exist.

### P2 — Security hardening
- Rate limiting for login/register/forgot-password (in-memory, no new dependency), failed-login lockout, token revocation on logout/password change (V3 `token_revocations` or `users.token_version`), magic-byte upload check via Tika, security headers review.
- Tests: WebMvc tests for 429/lockout/revocation; upload with fake extension rejected. Acceptance: OWASP top issues covered for auth/upload.

### P3 — Async AI jobs
- V4 `ai_jobs` (id, user, type, target, status, error, timestamps); endpoints return 202 + job id; `GET /api/ai-jobs/{id}`; frontend polling with progress states; recovery of stuck jobs.
- Acceptance: no HTTP request holds a thread for an AI call; refresh-safe generation.

### P4 — JD Analyzer
- V5 `jd_analyses` (per JD version: required/nice skills, seniority, responsibilities, keywords); analysis on create/update (async); shown in workspace; reused by matching, tailoring, interview.
- Acceptance: analysis is stored once per JD version; match uses it; tests for parsing/validation.

### P5 — Resume & tailoring
- Re-parse action for old resumes, user edits of tailored resume content, one ATS-friendly template polish.
### P6 — Interview AI & evaluation
- Questions from JD analysis + gaps, retry of failed evaluations, category breakdown report, trend across interviews.
### P7 — Applications & notifications
- Notes, follow-up dates, status history (V-migration), opt-in email reminders via existing mail setup.
### P8 — Testing & observability
- Actuator health/readiness (DB, Mongo, Ollama), request IDs in logs, AI latency/failure metrics, DB integration tests, scripted E2E smoke, OpenAPI docs.
### P9 — Docker, CI/CD, deployment
- Dockerfiles (backend, frontend/nginx), docker-compose (MySQL, Mongo, Ollama), GitHub Actions (build, test, frontend build), prod configuration checklist, backup/restore notes.

Every phase: tests + build, DB/API/UI/security verification, docs + CHANGELOG, then stop for approval. No commits/pushes/deploys without explicit request.
