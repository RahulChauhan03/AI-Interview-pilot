# Changelog

## Phase 1 — Stabilize (2026-10-06)
### AI reliability
- Per-task output budgets: each Ollama schema sets `x-max-output-tokens` (match 700, answer evaluation 600,
  interview questions 1500, tailored resume 1800, cover letter 900, resume analysis 3000), capped by
  `resume.ollama.max-output-tokens`; the key is stripped before the schema is sent.
- All schemas are bounded (`maxItems` on every array, `maxLength` on free-text strings) so a small model cannot
  generate endless lists or text.
- Scores are generated last (`matchScore`, answer `score`) after the evidence; the match prompt asks for a score
  consistent with the listed strengths and gaps.
- Retry and circuit-breaker behaviour unchanged.
### Performance
- Application list and overall skill gaps load matches, interviews and document dates once per user instead of
  several queries per item; document dates come from a projection (no document content loaded).
- `hibernate.default_batch_fetch_size=50` batches lazy loading.
### Tests
- `./mvnw test` always uses the new `test` profile (separate `interview_pilot_test` databases, dummy JWT/mail values);
  a test asserts the development database is never configured.
- New tests: per-task token budget, bounded schemas, batched application list.
### Cleanup
- Removed unused backend `com.interviewpilot.ai` package (empty `/api/v1/ai` controller, unused Mongo documents and
  repositories) and unused frontend files `core/constants/api.constants.ts`, `core/config/api.config.ts`,
  `core/config/app.config.ts`, plus legacy `API_ENDPOINTS.INTERVIEW` / `PROFILE`.
### Branding
- Visible product name is now **HireFlow — AI Career & Interview Assistant** (page title, sidebar, sign-in page,
  password-reset email). Packages and folders keep their names.
### Duplicate prevention (backend-enforced)
- Repeated or concurrent requests for the same operation are serialized per user and target (`KeyedLocks`, a
  striped in-process lock) and then reuse the existing record:
  - **Job description:** identical company, title and text (whitespace-insensitive) returns the existing one (200).
  - **Match:** one analysis per user + job + resume while neither changed (existing reuse rule, now race-free).
  - **Interview:** while an interview for the same job + resume is in progress, it is returned (200) instead of
    generating another; completed or deleted interviews allow a new one.
  - **Application:** one per job description (existing unique key); the lock is held until the transaction commits.
- Responses carry `reused: true`; the UI shows a short notice and opens the existing record.
### Delete
- `DELETE /api/applications/{id}` (with its documents), `DELETE /api/matches/{id}`, `DELETE /api/interviews/{id}`
  (with questions, answers and logs). Another user's record returns 404; no login returns 401.
- Delete buttons with a confirmation dialog on the applications list/detail/workspace, matches list/detail and
  interviews list/results pages.
### API and database
- No breaking changes and no new migration: three new DELETE endpoints and an additive `reused` field.

## Phase 5.6 — Job application workspace
- Workspace per job description, tailored resume, cover letter, PDF/ZIP downloads, application tracker, skill gaps
  (Flyway `V2__job_applications.sql`).
