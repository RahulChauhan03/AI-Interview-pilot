# Changelog

All notable changes to **HireFlow — AI Career & Interview Assistant** (formerly AI Interview Pilot).
Versions follow the project's development phases; newest first.

---

## [0.9.0] — Repository cleanup — 2026-10-07

### Changed
- README now carries the technical documentation: application workspace, tech stack, repository layout,
  local setup, AI configuration, tests, known limitations and roadmap.

### Removed
- Temporary audit documents (`CURRENT_SYSTEM.md`, `GAP_ANALYSIS.md`, `PRODUCTION_READINESS.md`, `ROADMAP.md`);
  their still-relevant content moved to the README.
- Root `.vscode/` editor settings; `.vscode/` is now git-ignored.

---

## [0.8.0] — UI modernization & ATS score — 2026-10-07

### Added
- **ATS Friendliness Score** on Resume Details: an animated gradient score ring (0–100%) with a rating
  (Excellent 90–100 · Good 75–89 · Needs improvement 60–74 · Needs attention below 60), a
  "How is this calculated?" explanation, a per-check breakdown and the top improvement tips.
  Calculated only from the parsed resume: contact details, summary, skills, experience, dates, education,
  standard sections and keyword signals. Nothing is assumed or invented.
- **Lucide icon set**: one consistent stroke style with distinct metaphors per concept (dashboard, jobs, resume,
  match, tailored resume, cover letter, interview, applications, skills, AI and more), registered with Angular
  Material's icon registry. No icon font or icon package is loaded at runtime.
- Stat cards with tinted icon tiles on the candidate and admin dashboards.
- Tooltips on icon-only buttons; accessible labels on password show/hide toggles.

### Changed
- Refreshed visual design: softer borders and layered shadows, rounder cards and controls, clearer typography and
  table headers, refined status badges, empty and error states with icon tiles, subtle hover and focus
  transitions (reduced motion respected).
- Compact sidebar with an active indicator, gradient brand mark and a translucent top bar.
- Links underline on hover only; correct singular/plural wording throughout.

### Fixed
- Job workspace tabs were clipped on the left at laptop widths; all tabs now fit and wrap on narrow screens.
- Interview Prep and Overview columns were squeezed: routed pages and the workspace panel are now real
  full-width blocks, and the two-column layout responds to the workspace width (stacks when narrow).
- Long file names and URLs no longer overflow their cards.
- Data tables are readable on phones (rows become stacked cards with visible actions).
- Spacing between dashboard sections and other small alignment issues.

---

## [0.7.0] — Stabilization — 2026-10-06

### Added
- **Duplicate prevention** (enforced by the backend). Repeated or concurrent requests (double-click, retry) reuse
  the existing record instead of creating another:
  - Job description: identical company, title and text returns the existing one.
  - Resume match: one analysis per user, job and resume while neither has changed.
  - Mock interview: an interview in progress for the same job and resume is returned instead of a new one.
  - Application: one per job description.

  Responses include `reused: true`, and the UI explains that the existing record was opened.
- **Delete** for applications (with their generated documents), job matches and interviews (with questions and
  answers), with confirmation dialogs in the UI. Only the owner can delete; other users get 404.

### Changed
- **AI reliability**: every AI task has its own output budget, AI responses are size-bounded (list lengths and
  text lengths), and scores are generated after the evidence. Runaway generations from the small local model
  are prevented; a tailored resume now completes in seconds instead of timing out. Retry and circuit-breaker
  behaviour is unchanged.
- Visible branding is now **HireFlow — AI Career & Interview Assistant** (page title, navigation, sign-in page,
  password-reset email). Code packages and folders keep their names.

### Fixed
- Application list and skill gaps no longer run several database queries per item (N+1); related data is
  loaded once per user.

### Removed
- Unused backend AI scaffolding (empty `/api/v1/ai` endpoint and unused repositories) and unused frontend
  configuration files.

### Developer experience
- `./mvnw test` always runs against separate `interview_pilot_test` databases and never touches development data.

---

## [0.6.0] — Job application workspace

### Added
- A workspace for every job description: Overview, Match, Tailored resume, Cover letter, Interview prep and
  Application tabs, with progress steps based on real data.
- **Tailored resume**: the AI rewords and reorders the candidate's own resume for the job; it never adds
  experience, skills, numbers, employers or education. A claim guard removes unsupported AI text and shows what
  was left out.
- **Cover letter**: written from the resume and job description, addressed to the hiring manager, editable.
- **Downloads**: tailored resume PDF, cover letter PDF and a ZIP application package with safe file names.
- **Application tracker** with statuses Saved, Preparing, Applied, Assessment, Interview, Offer, Rejected and
  Withdrawn, plus an applications list and detail page.
- **Skill gaps**: strong, developing and missing skills per job and across all jobs, with recommendations.
- Dashboard career metrics and "Use for application" in the resume library.
- Database migration `V2__job_applications.sql`.

---

## [0.5.0] — Product UI and admin area

### Added
- Separate candidate and admin areas with role-based navigation and guards.
- Admin dashboard, user list and activity view.
- Light/dark theme following the system setting, with a manual toggle.
- Consistent loading, empty and error states across pages.

---

## [0.4.0] — Core career features

### Added
- Job description management (create, edit, delete).
- Resume ↔ job matching: 0–100 score, strengths, missing skills and recommendations; the previous result is
  reused while the resume and job are unchanged.
- AI mock interviews: questions generated from the resume and job, per-answer evaluation (0–100 with
  correctness, relevance, feedback, strengths and improvements) and an overall result.

---

## [0.3.0] — AI pipeline reliability

### Added
- A single Ollama client with connect/read timeouts, retries and a circuit breaker.
- Structured JSON output via schemas, with strict parsing and validation of every AI response.
- Clear errors when the AI is unavailable (503) or returns invalid output (502).
- Default model `llama3.2:1b`, configurable through environment variables.

---

## [0.2.0] — Production foundations

### Added
- Flyway database migrations with Hibernate schema validation (no automatic schema changes).
- Secrets from environment variables (database, JWT, mail), with `dev` and `prod` profiles that fail fast when
  configuration is missing.
- Consistent API error responses and validation messages.

---

## [0.1.0] — Initial release

### Added
- User registration, login (JWT) and password reset by email.
- Resume upload (PDF, DOC, DOCX) with text extraction and AI parsing into structured data.
- Spring Boot backend (MySQL and MongoDB) and Angular frontend.
