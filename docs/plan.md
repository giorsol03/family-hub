# Family Hub — Build Plan

Architecture (changed 2026-11-06): Spring Boot is a **REST API only**; the UI is a **React + Vite + TypeScript**
app in `frontend/` using **Mantine** components. One deployable in production (Spring serves the built SPA).
See `CLAUDE.md` for stack, conventions and rules.

Each milestone = one feature, backend + frontend together, ends with all checks green
(`.\mvnw.cmd test`, `npm run lint`, `npm test`, `npm run build`) and a commit.

## Milestones
| # | Milestone | Status | Main deliverables |
|---|-----------|--------|-------------------|
| 1 | Foundation (server-rendered) | ✅ done (`aa4ae70`) | Boot skeleton, Flyway V1 `member`, login, members CRUD, Thymeleaf layout — **superseded by 1b** |
| 1b | **Switch to REST API + React shell** | ✅ done | see below |
| 2 | Grocery list | ✅ done | `grocery_default`, `grocery_item`; merging service (G↔KG, ML↔L) + unit tests; list grouped by merchant → category; tick bought (optimistic update); clear bought; add from default; defaults page (deactivate / re-activate); seed defaults |
| 3 | Chores | ✅ done | `chore_template`, `chore`; Today / This week / Overdue tabs; assignee filter; recurrence → next instance on completion; seed templates |
| 4 | Recipes | ✅ done | `recipe`, `recipe_ingredient`, tags; list + detail + form with dynamic ingredient rows |
| 5 | Weekly meal plan | ✅ done | `meal_plan_entry`; Mon–Sun × slot grid (stacked days on phones); "add ingredients to grocery list?" modal (checkboxes, select all / none) reusing the M2 merging service, source=RECIPE |
| 6 | Calendar | | `event`, `event_attendee`; month + week views; tap a day to add; meal-plan entries shown read-only; member filter |
| 7 | Daily goals | | `goal`, `goal_check`; monthly habit grid (horizontal scroll on phones), tap to tick; per-person filter / "Everyone" grouping; streak + % this month |
| 8 | Postgres/Neon + deploy | | `prod` profile from env vars; Testcontainers Postgres run of all migrations; multi-stage Dockerfile (Node build → Spring jar); SPA fallback; Render deploy; Europe/Nicosia verified |

## Milestone 1b — REST API + React shell

### Backend changes
- **Remove**: `spring-boot-starter-thymeleaf`, `thymeleaf-extras-springsecurity6` (+ test starter), `templates/`, `static/css`, `static/js`,
  `HomeController`, `GlobalModelAdvice`, `NavItem` (navigation moves to the frontend).
- **Keep as is**: Flyway `V1__members.sql`, `Member`, `MemberRepository`, `MemberService`, `MemberException`, `AuditableEntity`,
  `CurrentMemberProvider`, `ClockConfig`, `BootstrapMemberInitializer`, `MemberUserDetailsService`, `MemberPrincipal`.
- **Members API** (`member/MemberApiController`): `GET /api/members`, `GET /api/members/{id}`, `POST /api/members`,
  `PUT /api/members/{id}`, `POST /api/members/{id}/deactivate`, `POST /api/members/{id}/activate`.
  DTOs: `MemberResponse(id, name, username, color, active)`, `MemberRequest` (replaces `MemberForm`, same validation).
- **Auth API** (`common/auth/AuthController`): `POST /api/auth/login` (form-encoded, handled by Spring's form-login filter),
  `POST /api/auth/logout`, `GET /api/auth/me` → `MemberResponse` (fresh from DB, so name/colour edits show immediately).
- **SecurityConfig**: no form-login page; JSON login via `AuthenticationManager` + `HttpSessionSecurityContextRepository`;
  remember-me kept; `/api/**` unauthenticated → 401 (`HttpStatusEntryPoint`); CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()`
  + SPA token handler; permit `/api/auth/login`, `/actuator/health` and static SPA assets.
- **Errors** (`common/web/ApiExceptionHandler`): `ProblemDetail` for validation (400 + `fieldErrors`), `MemberException`
  (409 for taken username, 400 otherwise), `NotFoundException` (404).
- **SPA fallback** (`common/web/SpaForwardController`): forwards non-`/api`, non-file routes to `/index.html` (used in prod).

### Frontend (`frontend/`)
- Scaffold: `npm create vite@latest frontend -- --template react-ts`; add `@mantine/core @mantine/hooks @mantine/form
  @mantine/notifications @mantine/dates dayjs @tabler/icons-react react-router-dom @tanstack/react-query`;
  dev: `vitest @testing-library/react @testing-library/jest-dom jsdom`, Mantine PostCSS preset.
- `vite.config.ts`: dev proxy `/api` → `http://localhost:8080`; Vitest config (jsdom).
- `src/api/client.ts`: `fetch` wrapper — `credentials: 'same-origin'`, JSON, reads `XSRF-TOKEN` cookie into `X-XSRF-TOKEN`,
  throws `ApiError` carrying `ProblemDetail` + `fieldErrors`; 401 → redirect to `/login`.
- `src/api/auth.ts`, `src/api/members.ts` — typed calls; `src/features/auth/useCurrentMember.ts` (TanStack Query).
- **App shell** (`components/AppShell.tsx`, Mantine `AppShell`):
  - Header: "Family Hub" + current member badge (colour dot + name) + logout menu.
  - Desktop (≥ md): left navbar with all sections.
  - Phone: fixed bottom tab bar — Home · Chores · Groceries · Meals · Calendar · Goals; Recipes + Members in the header menu.
  - Sections not built yet are shown disabled ("Soon") — single `navItems` array in `components/navItems.ts`.
- **Routes**: `/login` (public), and behind a `RequireAuth` guard: `/` (home cards), `/members`, `/members/new`, `/members/:id`.
- **Pages**: Login (centred card, remember-me), Home (section cards grid), Members list (avatar with colour + initial,
  edit / deactivate / re-activate actions, "You" badge), Member form (name, username, Mantine `ColorInput` with swatches,
  password optional on edit) with server field errors mapped onto inputs. Success/error toasts via Mantine notifications.
- Theme: Mantine defaults, one primary colour, auto light/dark from the device.

### Tests
- Backend: rewrite `MemberControllerTest` / `SecurityTest` as JSON API tests (MockMvc + `jsonPath`): 401 when anonymous,
  login success/failure, CSRF required on POST, CRUD + validation `ProblemDetail`s. `MemberServiceTest` and
  `FamilyHubApplicationTests` unchanged.
- Frontend: `client.test.ts` (CSRF header, error parsing), `MemberForm.test.tsx` (validation + server field errors),
  `AppShell.test.tsx` (disabled sections, active tab).

### Verification
1. `.\mvnw.cmd test`; in `frontend/`: `npm run lint`, `npm test`, `npm run build`.
2. Run both: `$env:APP_ADMIN_USERNAME="giorgos"; $env:APP_ADMIN_PASSWORD="..."; .\mvnw.cmd spring-boot:run` and `cd frontend; npm run dev`
   → http://localhost:5173: redirected to login, log in, see home + nav, add a member, log out / in as them.
3. Phone check in DevTools (~390px): bottom tabs visible, no horizontal scroll.
4. Commit and push.
