# Gap Analysis — Interview Pilot → HireFlow

| Feature | Current | Required for HireFlow | Priority |
|---|---|---|---|
| Authentication | JWT login/register, BCrypt, password reset, USER/ADMIN roles. Token in sessionStorage, no server logout/revocation, no rate limit. | Rate-limited login/register/reset, token revocation on logout/password change, lockout after failed attempts. | High |
| Resume | Upload PDF/DOC/DOCX, async AI parsing, recovery job. Extension-only validation; old parses sparse. | Content-type (magic byte) validation, re-parse action for old resumes, edit parsed facts by the user. | Medium |
| JD Analyzer | **Missing.** JD is free text; only the match produces skills. | Structured JD analysis (required/nice-to-have skills, seniority, responsibilities, keywords) stored once per JD version, shown in workspace, reused by match/tailoring/interview. | High |
| Resume Matching | AI score + strengths/gaps + recommendations, reused when unchanged. Score unreliable with 1B model. | Use JD analysis; deterministic skill-overlap component + AI explanation; stable scores. | High |
| Resume Tailoring | Done (reword/reorder only, ClaimGuard, fallback to original). Fails on token cap. | Per-task token limits, user edits of tailored content, versions per application. | Medium |
| PDF | PDFBox resume, cover letter, JD PDF + ZIP, rendered on demand. | One polished template (ATS-friendly), optional DOCX later. | Low |
| AI Interview | Question generation, per-answer scoring, completion. Synchronous AI calls. | Questions driven by JD analysis + gaps; async generation/scoring; retry for a failed evaluation. | High |
| Evaluation | Per-answer 0–100 + feedback; overall score; skill gaps from categories. | Category breakdown report, trend across interviews, readiness summary per application. | Medium |
| Application Tracker | Done: 8 statuses, documents, interview links, applied date. | Notes, follow-up dates/reminders, status history, notifications. | Medium |
| Security | Env-var secrets, CORS allow-list, ownership checks (404), admin role on `/api/admin/**`. | Rate limiting, security headers/CSP review, upload sniffing, audit log, dependency scanning. | High |
| Testing | 168 backend unit/WebMvc tests, 21 frontend tests, manual E2E scripts. | Repository/integration tests on real MySQL (Testcontainers optional), component tests, scripted E2E smoke, CI enforcement. | High |
| Docker/CI/CD | **None** (only Copilot "modernize" scaffolding under `.github/`). | Dockerfiles, docker-compose (MySQL, Mongo, Ollama, app), GitHub Actions build+test, release notes. | High |
| Observability | Default logging only. | Actuator health/readiness (incl. Ollama), request IDs, structured logs, AI latency/failure metrics. | Medium |
| Notifications | Password-reset email only. | Email reminders for follow-ups/interview practice (opt-in). | Low |
