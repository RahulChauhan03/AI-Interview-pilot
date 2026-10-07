# Production Readiness — score 0–10

| Area | Score | Why |
|---|---|---|
| Architecture | 6 | Clean feature-packaged monolith, single AI gateway. Dead `ai` package, two databases, AI work on request threads. |
| Security | 5 | Env secrets, BCrypt, JWT, ownership checks, CORS allow-list. No rate limiting, no token revocation, extension-only upload check, token readable by JS (sessionStorage). |
| Database | 7 | Flyway + `validate`, FKs and indexes. Mongo not versioned; no backup/restore procedure documented. |
| APIs | 7 | Consistent envelope, validation, error mapping. No OpenAPI docs, no pagination on lists, long synchronous AI endpoints. |
| Testing | 6 | 168 backend + 21 frontend tests. No DB integration tests, few component tests, no automated E2E; `./mvnw test` uses the real dev DB unless overridden. |
| AI reliability | 4 | Retry, circuit breaker, timeouts, schema JSON, strict parsing, claim guard. 1B model hits token cap / gives unstable scores; single global `num_predict`. |
| Performance | 5 | Fine for one user. Application/skill-gap lists do per-item queries (N+1); sync AI calls hold threads for minutes. |
| Configuration | 7 | Profiles, required env vars, fail-fast. No documented prod checklist. |
| Docker/CI/CD | 0 | Nothing exists. |
| Observability | 2 | Logs only; no health, metrics or correlation IDs. |
| UI/UX | 7 | Consistent Material UI, dark mode, empty/error/loading states. Initial bundle 636 kB (> 500 kB budget); long AI waits only show a spinner. |
| **Overall** | **5 / 10** | Feature-rich MVP; not yet deployable/operable. |

## Critical risks
1. **Synchronous AI inside HTTP requests** (up to ~4 min): thread exhaustion and proxy timeouts in any real deployment.
2. **AI model quality** (`llama3.2:1b`): token-cap runaways → 503s; unreliable match scores.
3. **No brute-force protection** on login / forgot-password (also email abuse).
4. **No CI/Docker**: builds and tests are not enforced; deployment is manual.
5. **Large uncommitted working tree** (many phases since the last commit): one bad operation loses work. Committing is the user's decision.
6. **Dev-profile tests hit the real DB**, and the IntelliJ devtools server applies new migrations to the real DB immediately.
7. Uploads on local disk: single-instance only; needs a backup plan.

## Technical debt / dead code (found)
- Backend `ai/` package (15 files, ~200 lines): empty `AiController` at `/api/v1/ai`, unused Mongo documents/repositories (`ai_conversations`, `document_chunks`, `llm_requests`…), `Ai` entity — unused scaffolding.
- `ServletInitializer` (WAR support) while packaging a JAR.
- Frontend: unused `core/constants/api.constants.ts`, `core/config/api.config.ts`, `core/config/app.config.ts`; legacy `API_ENDPOINTS.INTERVIEW`/`PROFILE`; duplicate auth files under `features/auth` (`services/auth.ts`, `models/api-response.model.ts`, `interfaces/auth.interface.ts`) next to `core/services/auth.service.ts`.
- Duplicate `.github/modernize` scaffolding at repo root and in the backend.
- No TODO/FIXME markers; no hardcoded secrets found (all from env / git-ignored `.env`).
