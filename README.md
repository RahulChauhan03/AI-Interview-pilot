# AI Interview Pilot

AI Interview Pilot is a full-stack AI-powered platform designed to help candidates prepare for job applications, analyze resumes against job descriptions, practice interviews, and receive AI-generated feedback.

The project is built with Java Spring Boot, Angular, MySQL, MongoDB, and Ollama.

---

## 🚀 Features

### 🔐 Authentication & Security
- User registration and login
- JWT-based authentication
- Role-based authorization
- Secure environment-based configuration
- Protected APIs
- Password reset functionality

### 📄 Resume Management
- Upload PDF resumes
- Resume file validation
- Resume text extraction using Apache Tika
- AI-powered resume parsing
- Parsed resume data stored in MongoDB
- Resume processing status tracking
- Resume ownership validation

### 💼 Job Description Management
- Create job descriptions
- View job descriptions
- Update job descriptions
- Delete job descriptions
- User-specific job descriptions

### 🎯 Resume & Job Matching
- Match resumes against job descriptions
- AI-powered match analysis
- Match score
- Matched skills
- Missing skills
- Strengths and gaps
- AI-generated matching summary

### 🎤 AI Interview
- Generate interview sessions from resume + job description
- AI-generated interview questions
- Interview session management
- Answer submission
- AI-powered answer evaluation
- Interview scoring
- Feedback and improvement suggestions

### 🤖 AI Processing
- Local AI processing using Ollama
- Configurable AI model
- Currently optimized for `llama3.2:1b`
- Structured AI responses
- AI response validation
- Timeout handling
- Retry handling
- Circuit breaker
- Controlled AI failure handling

### 🗄️ Database
- MySQL for transactional application data
- MongoDB for parsed resume/AI data
- Flyway database migrations
- Hibernate schema validation
- Foreign key constraints and indexes

---

## 🏗️ Architecture

```text
                    ┌─────────────────────┐
                    │      Angular UI     │
                    │     Frontend        │
                    └──────────┬──────────┘
                               │
                               │ REST API
                               ▼
                    ┌─────────────────────┐
                    │   Spring Boot API   │
                    │      Backend        │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
       ┌───────────┐     ┌───────────┐    ┌───────────┐
       │   MySQL   │     │  MongoDB  │    │  Ollama   │
       │           │     │           │    │    AI     │
       │ App Data  │     │ Resume AI │    │ Processing │
       └───────────┘     └───────────┘    └───────────┘
```

---

## 🧭 Application Workspace (HireFlow)

Each job description opens as a workspace that takes an application from job description to offer:

- **Overview** – real progress steps (match, tailored resume, cover letter, interview practice, applied)
- **Tailored resume** – AI rewording and reordering of the candidate's own resume; it never adds experience, skills, numbers or employers (unsupported AI text is removed by a claim guard)
- **Cover letter** – written from the resume and the job description, addressed to the hiring manager, editable
- **Downloads** – resume PDF, cover letter PDF and a ZIP application package (generated on demand)
- **Interview prep** – likely topics from skill gaps, recent questions, and mock interviews for the job
- **Application tracking** – Saved → Preparing → Applied → Assessment → Interview → Offer / Rejected / Withdrawn
- **Skill gaps** – strong, developing and missing skills across all jobs and interviews
- **ATS friendliness score** – on Resume Details, calculated from the parsed resume with a breakdown and tips

Repeated create requests (double-click, retry) reuse the existing job description, match, interview in progress or application instead of creating duplicates. Users can delete their own applications, matches and interviews; other users' records always return 404.

---

## 🧰 Tech Stack

| Area | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Security, Data JPA, Data MongoDB, Validation, Mail) |
| Databases | MySQL 8+ with Flyway migrations (`ddl-auto=validate`), MongoDB for parsed resumes |
| Security | JWT (jjwt 0.12), BCrypt, ownership checks on every resource |
| AI | Ollama (`llama3.2:1b` by default), Resilience4j retry + circuit breaker |
| Documents | Apache Tika 3.2 (parsing), Apache PDFBox 3.0 (PDF generation) |
| Frontend | Angular 21, Angular Material 21, RxJS, Vitest; Lucide icons (vendored SVGs) |

---

## 📁 Repository Layout

| Path | Contents |
|---|---|
| `interview-pilot-backend/` | Spring Boot API |
| `interview-pilot-backend/src/main/resources/db/migration/` | Flyway migrations (`V1__initial_schema.sql`, `V2__job_applications.sql`) |
| `interview-pilot-backend/src/main/resources/ollama/` | JSON schemas for every AI task (bounded output, per-task token budget) |
| `interview-pilot-ai/` | Angular frontend |
| `CHANGELOG.md` | Changes per phase |

Key backend modules (package `com.interviewpilot`): `auth`, `user`, `admin`, `resume` (upload, parsing, `resume.ai.OllamaService` – the only Ollama client), `jobdescription` (job descriptions, matching), `interview`, `application` (workspace, documents, PDFs, skill gaps), `security`, `exception`.

---

## ⚙️ Run Locally

Prerequisites: Java 17, Node 20+, MySQL 8+, MongoDB, [Ollama](https://ollama.com) with `ollama pull llama3.2:1b`.

1. **Backend** – create a git-ignored `interview-pilot-backend/.env` with `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME` and `MAIL_PASSWORD` (see `application-example.properties`), then:
   ```bash
   cd interview-pilot-backend && ./mvnw spring-boot:run
   ```
   Runs on port 8080 with the `dev` profile (database `interview_pilot_db`). Flyway applies migrations on startup.
2. **Frontend**:
   ```bash
   cd interview-pilot-ai && npm install && npx ng serve
   ```
   Opens on http://localhost:4200.

### AI configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_MODEL` | `llama3.2:1b` | Model used for all AI tasks |
| `OLLAMA_ENDPOINT` | `http://localhost:11434/api/generate` | Ollama endpoint |
| `OLLAMA_MAX_OUTPUT_TOKENS` | `3000` | Global output cap; each schema sets a lower per-task budget (`x-max-output-tokens`) |

AI output is never trusted directly: it is schema-constrained JSON, strictly parsed, validated, and checked against the resume before it is saved.

---

## ✅ Tests

- **Backend:** `cd interview-pilot-backend && ./mvnw test` – always runs with the `test` profile (`src/test/resources/application-test.properties`), using separate `interview_pilot_test` databases in MySQL and MongoDB, created automatically. It never uses the development database. Override with `TEST_DB_URL` / `TEST_MONGODB_URI`.
- **Frontend:** `cd interview-pilot-ai && npx ng test --watch=false`

---

## ⚠️ Known Limitations

- AI calls run synchronously inside HTTP requests (matching ~10–20 s, generation up to a few minutes with a local model).
- `llama3.2:1b` output quality is limited (e.g. a skill listed as both strength and gap); guards keep content truthful but cannot make it perfect.
- No rate limiting on login / registration / password reset yet; logout does not revoke tokens server-side.
- Duplicate-request protection works within one backend instance.
- No Docker setup, CI pipeline, health endpoint or metrics yet.
- The Angular initial bundle (~650 kB) exceeds its 500 kB budget warning.

---

## 🗺️ Roadmap

1. ~~Stabilize~~ – AI output limits, dead-code removal, N+1 fixes, test isolation, duplicate prevention, delete (done)
2. Security hardening – rate limiting, failed-login lockout, token revocation, upload content checks
3. Background AI jobs – long AI work as jobs with status polling
4. Job description analyzer – structured skills, seniority and responsibilities reused by matching, tailoring and interviews
5. Resume & tailoring – re-parse older resumes, edit tailored content
6. Interview AI & evaluation – questions from the JD analysis, category reports and trends
7. Applications & notifications – notes, follow-up dates, reminders
8. Testing & observability – health checks, request IDs, metrics, E2E smoke tests, API docs
9. Docker, CI/CD & deployment
