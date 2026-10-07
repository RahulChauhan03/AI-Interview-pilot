# Current System — AI Interview Pilot (audit 2026-10-06)

## Architecture
Monorepo, two deployables, one local AI runtime:

```
Angular 21 SPA (interview-pilot-ai, :4200)
        │  REST + JWT (Bearer)
Spring Boot 3.5 monolith (interview-pilot-backend, :8080)
   ├── MySQL 9.6  (users, resumes, job descriptions, matches, interviews, applications) — Flyway V1..V2, ddl-auto=validate
   ├── MongoDB    (parsed resumes: raw/clean text + structured AI extraction)
   ├── Local disk (uploads/resume — original files)
   ├── SMTP       (password-reset mail only)
   └── Ollama     (llama3.2:1b, http://localhost:11434) via OllamaService only
```

Backend is a package-by-feature monolith: `auth, user, admin, resume, jobdescription, interview, application, security, exception, common` (+ dead `ai` scaffolding).

## Tech stack
| Area | Version |
|---|---|
| Java / Spring Boot | 17 / 3.5.6 (Web, Security, Data JPA, Data MongoDB, Validation, Mail, AOP) |
| DB | MySQL 9.6 + Flyway 11.7.2; MongoDB |
| Auth | jjwt 0.12.6 (HS256), BCrypt |
| AI resilience | Resilience4j 2.3.0 (retry + circuit breaker "ollama") |
| Documents | Apache Tika 3.2.3 (parsing), PDFBox 3.0.5 (PDF generation) |
| Frontend | Angular 21.2, Angular Material 21.2, RxJS 7.8, TypeScript 5.9, Vitest 4 |
| AI model | Ollama `llama3.2:1b` (configurable `OLLAMA_MODEL`) |

## Existing features (working)
- Auth: register, login (JWT), forgot/reset password by email, roles USER/ADMIN, route guards.
- Resume: upload PDF/DOC/DOCX (10 MB), async parsing (Tika → AI extraction → Mongo), recovery job for stuck parses, library + detail view.
- Job descriptions: CRUD; JD is the centre of an **application workspace** (overview, match, tailored resume, cover letter, interview prep, application).
- Resume ↔ JD match: AI score 0–100, strengths, missing skills, recommendations; result reused while inputs unchanged.
- Tailored resume + cover letter: AI rewording guarded by `ClaimGuard` (no invented numbers, missing skills or names); PDF + ZIP downloads with safe filenames.
- AI mock interview: question generation per JD/resume, per-answer scoring 0–100 with feedback, completion + result page.
- Application tracker (8 statuses), skill-gap analysis (per job and overall), candidate dashboard, admin dashboard/users/activity, light/dark theme.
- Error envelope `{status,message,timestamp,data,path}`, global exception mapping (404/409/502/503…), secrets via env vars, dev/prod profiles.

## What does not work well
- `llama3.2:1b` sometimes generates until the 3000-token cap → 503 (seen in the E2E: tailored resume attempt 1). Also weak scoring (e.g. matchScore 0 with good strengths) and echoing prompt text.
- AI calls run synchronously inside HTTP requests (20 s – 4 min).
- No Docker, no CI pipeline, no health/metrics endpoints.
- Older parsed resumes (pre-Phase 4 format) have sparse structure; tailoring asks for a re-upload.

## Important files
| Module | Path |
|---|---|
| Security / JWT | `backend/.../security/config/SecurityConfig.java`, `security/filter/JwtAuthenticationFilter.java` |
| AI gateway | `backend/.../resume/ai/OllamaService.java`, `AiResponseParser.java`, `resources/ollama/*.json` |
| Resume pipeline | `resume/service/impl/ResumeAsyncProcessingServiceImpl.java`, `ResumeProcessingRecoveryJob.java` |
| Matching | `jobdescription/service/impl/JobDescriptionServiceImpl.java`, `resume/service/impl/ResumeMatchingServiceImpl.java` |
| Interview | `interview/service/impl/InterviewServiceImpl.java`, `AnswerScoringServiceImpl.java`, `resume/service/impl/InterviewGenerationServiceImpl.java` |
| Applications / documents | `application/**` (`ClaimGuard`, `ResumeFacts`, `PdfWriter`, `DocumentRenderer`) |
| Migrations | `backend/src/main/resources/db/migration/V1__initial_schema.sql`, `V2__job_applications.sql` |
| Frontend routing / shell | `ai/src/app/app.routes.ts`, `shared/layout/app-shell.component.ts` |
| Frontend API layer | `core/http/api.service.ts`, `api-client.service.ts`, `api-endpoints.ts`, `core/interceptors/*` |

Tests: backend 168 (JUnit 5, Mockito, WebMvcTest, 1 context test), frontend 21 (Vitest).
