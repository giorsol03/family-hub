# Family Hub

Personal household website (chores, groceries, recipes/meal plan, calendar, daily goals).
Full requirements: @docs/requirements.md
Build plan / milestones: @docs/plan.md

## Stack
- **Backend** (repo root): Java 21, Spring Boot 4.x, Maven (use the wrapper: `.\mvnw.cmd` on Windows, `./mvnw` elsewhere)
  - Spring Web MVC (REST/JSON only — no server-rendered pages), Spring Data JPA, Spring Security, Validation, Actuator
  - PostgreSQL (hosted on Neon) with Flyway migrations in `src/main/resources/db/migration`; H2 (PostgreSQL mode) locally and in tests
  - Tests: JUnit 5, Spring Boot Test + MockMvc; H2 for repository tests, Testcontainers Postgres for migration checks
- **Frontend** (`frontend/`): React + Vite + TypeScript (strict)
  - UI components: Mantine (core, hooks, form, dates, notifications) + Tabler icons. No other CSS framework.
  - Routing: React Router. Server state: TanStack Query. Forms: `@mantine/form`.
  - Tests: Vitest + React Testing Library. Lint: ESLint. Package manager: npm.

## Structure
Backend — package per feature under `com.solomonidis.familyhub`:
`member`, `chore`, `grocery`, `recipe`, `mealplan`, `calendar`, `goal`, `common`.
Each feature has entity, repository, service, REST controller and request/response DTOs (Java records).
Business logic lives in services, not controllers. Controllers never return JPA entities.

Frontend — `frontend/src/`:
- `api/` — fetch client (`client.ts`, handles CSRF + errors) and one file per feature with typed calls
- `features/<feature>/` — pages, components and TanStack Query hooks for that feature
- `components/` — shared UI (app shell, member badge, …); `routes.tsx` — route table

## API conventions
- All endpoints under `/api/**`, JSON in/out. Resource style: `GET /api/members`, `POST /api/members`, `PUT /api/members/{id}`, `POST /api/members/{id}/deactivate`.
- Validation errors and business-rule errors return RFC 7807 `ProblemDetail` (400/404/409) with a `fieldErrors` map; the frontend shows them on the form fields.
- Auth: Spring Security session cookie (+ remember-me). `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`. Unauthenticated `/api/**` → 401 JSON (never a redirect).
- CSRF: cookie-based (`XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header), sent by `api/client.ts`.
- Dates as ISO strings (`LocalDate` → `2026-11-06`, instants in UTC). Household timezone: Europe/Nicosia (inject `Clock`).

## Running
- Dev: backend `.\mvnw.cmd spring-boot:run` (port 8080) + `cd frontend; npm run dev` (Vite on 5173, proxies `/api` to 8080 — same origin, no CORS).
- Prod: one deployable. Dockerfile builds `frontend/` with Node, copies `dist/` into Spring's `static/`, and Spring serves the SPA (non-`/api` routes forward to `index.html`).

## Config
- DB from env vars: `DATABASE_URL` (jdbc:postgresql://...?sslmode=require), `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `server.port=${PORT:8080}`
- `spring.jpa.hibernate.ddl-auto=validate` — schema changes only via new Flyway migrations, never edit an applied migration
- First member from `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` when the member table is empty
- Never commit secrets. Local secrets go in `.env` (gitignored)

## Rules
- Build features one at a time; after each, all checks must pass before moving on:
  `.\mvnw.cmd test` and, in `frontend/`, `npm run lint`, `npm test`, `npm run build` (includes `tsc`)
- Grocery merging logic (unit conversion G↔KG, ML↔L) must have unit tests
- Keep the UI mobile-first (it is mostly used on phones) and simple: Mantine defaults, one accent colour, no heavy animations
