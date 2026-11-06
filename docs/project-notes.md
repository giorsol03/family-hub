# Family Hub — Project Notes (handover)

Read this first in a new session. It records what has been built, the decisions taken and why,
and **how to deploy** (section 6, self-contained).

Last updated: 2026-11-06. Repo: https://github.com/giorsol03/family-hub (branch `main`).

Related docs: `CLAUDE.md` (stack, conventions, rules) · `docs/requirements.md` (what the app must do) ·
`docs/plan.md` (milestone table) · `docs/deploy.md` (short deploy checklist; section 6 here is the full version).

---

## 1. Status at a glance

| # | Milestone | Commit | State |
|---|-----------|--------|-------|
| 1 | Skeleton, members, login (server-rendered, later replaced) | `aa4ae70` | superseded by 1b |
| 1b | Switch to REST API + React (Vite, TS, Mantine) | `8ba2c71` | ✅ |
| 2 | Grocery list + unit-aware merging + favourites | `ca1af4d` | ✅ |
| 3 | Chores, templates, recurrence, Today/Week/Overdue | `f02707f` | ✅ |
| 4 | Recipes (ingredients, tags, search, photo URL) | `82bd94e` | ✅ |
| 5 | Weekly meal plan + "add ingredients to groceries?" popup | `2592109` | ✅ |
| 6 | Calendar (month/week, events, meal-plan entries) | `538ec3a` | ✅ |
| 7 | Daily goals habit tracker | `b6895a3` | ✅ |
| 8 | Production profile, Postgres test, Dockerfile, Render blueprint | `8f6d207` | ✅ code — **first deploy not done yet** |

**Checks at last commit:** backend 160 tests pass (5 skipped = Postgres container test, no Docker on this PC);
frontend lint clean, 74 tests pass, build OK.

**Next step:** deploy (section 6). After that, ideas in section 8.

---

## 2. Decisions taken (and why)

- **Spring Boot 4.1.2** (not 3.x) — chosen by the owner at milestone 1. Boot 4 uses modular starters
  (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, …) and Jackson 3.
- **REST API + React SPA** instead of server-rendered Thymeleaf — changed after milestone 1 for a nicer UI.
  Spring serves `/api/**` (JSON) and, in production, the built React files.
- **Mantine** for UI ("better UI but not too excessive"): defaults, one teal accent, auto light/dark, mobile-first
  (bottom tab bar on phones, sidebar on desktop).
- **H2 (PostgreSQL mode) locally, Neon Postgres in production.** Migrations written in portable SQL.
- **Windows / PowerShell** dev machine: use `.\mvnw.cmd`, PowerShell syntax. Node 26 (installed via winget) is at
  `C:\Program Files\nodejs`; Java 24 JDK in `C:\Users\giorg\.jdks\openjdk-24.0.1` (project targets Java 21).
  Gotcha: rebuilding PATH from the machine setting puts an old Java 18 first → only *prepend* Node:
  `$env:Path = "C:\Program Files\nodejs;" + $env:Path`.
- **Everyone sees everything**: any member can edit any item, tick anyone's goal, etc. (household app).

---

## 3. Architecture

### Backend (repo root, package `com.solomonidis.familyhub`)
One package per feature: `member`, `grocery`, `chore`, `recipe`, `mealplan`, `calendar`, `goal`, plus `common`.
Each has entity, repository, service (business logic), `*ApiController`, and DTO records. Controllers never return entities.

`common` contains:
- `config/SecurityConfig` — session cookie + remember-me (30 days); `POST /api/auth/login` (form-encoded
  `username`, `password`, `remember-me` → 204/401); `POST /api/auth/logout` → 204; anonymous `/api/**` → 401;
  CSRF via `XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header; everything outside `/api` is public (the SPA).
- `config/ClockConfig` — one `Clock` bean in `app.timezone` (Europe/Nicosia). Always inject `Clock`, never `LocalDate.now()`.
- `config/ProductionEnvironmentCheck` — prod only: validates env vars, converts Neon's `postgresql://` string to JDBC.
- `config/StaticResourceConfig` — `/assets/**` cached 1 year (hashed names); `index.html` always revalidated.
- `audit/AuditableEntity` — `id`, `createdAt`, `createdBy` filled automatically.
- `web/ApiExceptionHandler` — errors as RFC 7807 `ProblemDetail` with a `fieldErrors` map (400/404/409).
- `web/SpaForwardController` — deep links like `/goals` → `index.html`.
- `auth/AuthController` — `/api/auth/me`, `/api/auth/csrf`.
- `BootstrapMemberInitializer` (in `member`) — creates the first member from `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD`
  when the member table is empty.

### Database (Flyway, `src/main/resources/db/migration`)
| File | Tables |
|---|---|
| V1 | `member` |
| V2 | `grocery_default` (9 seeds), `grocery_item` |
| V3 | `chore_template` (8 seeds), `chore` |
| V4 | `recipe`, `recipe_ingredient`, `recipe_tag` |
| V5 | `meal_plan_entry` |
| V6 | `calendar_event` (local wall-clock times), `calendar_event_attendee` |
| V7 | `goal`, `goal_check` (unique goal + date) |

Rules: `ddl-auto=validate`; **never edit an applied migration** — add `V8__…sql`. Enums stored as VARCHAR names.

### Frontend (`frontend/`)
React 19 + Vite + TypeScript (strict), Mantine 9, React Router, TanStack Query, `@mantine/form`, dayjs, Tabler icons.
Vitest + Testing Library; oxlint.
- `src/api/` — `client.ts` (CSRF, `ApiError` with field errors) + one file per feature.
- `src/features/<feature>/` — pages, components, query hooks.
- `src/components/` — `AppLayout` (header, sidebar, phone bottom tabs), `navItems.ts`, `MemberAvatar`, `MemberSelect`.
- Dev: Vite on 5173 proxies `/api` to Spring on 8080 (same origin, no CORS).

### Profiles
| Profile | DB | When |
|---|---|---|
| `dev` (default) | H2 file in `./data/` (gitignored) | `.\mvnw.cmd spring-boot:run` |
| `test` | H2 in memory, admin/admin-pass | tests |
| `prod` | Neon Postgres from env vars | Docker / Render (`SPRING_PROFILES_ACTIVE=prod`) |

---

## 4. Features and business rules

- **Members** — name, username, colour, password; deactivate/re-activate; first member from env vars.
- **Groceries** — grouped by merchant → category; tick bought (instant), "Clear bought"; favourites
  (defaults) with deactivate/re-activate. **Merging:** same name (case-insensitive) + compatible unit + not bought →
  quantities add up. G↔KG and ML↔L convert (shown as kg/l at ≥ 1000); PCS only with PCS, PACK only with PACK;
  otherwise a separate line. Logic in `grocery/Quantity`, unit-tested in `QuantityTest`.
- **Chores** — templates (8 seeds) with recurrence none/daily/weekly/every N days/monthly. Finishing a recurring chore
  creates the next one, keeping its rhythm and never in the past. Tabs Today / This week / Overdue; filter by person.
- **Recipes** — ingredients (ordered), lower-cased tags, search, optional photo URL. Can't delete a recipe planned for
  today or later (409).
- **Meal plan** — Mon–Sun × Breakfast/Lunch/Dinner (stacked days on phones). After planning, popup asks which
  ingredients to add to groceries (all ticked, select all/none); added with merging, source RECIPE, note
  "For <recipe>"; PCS/PACK rounded up; category/shop borrowed from a matching favourite.
- **Calendar** — month and week views; tap a day to add; event types MEAL/EVENT/NIGHT_OUT/OTHER; attendees;
  all-day events; meal-plan entries shown read-only; member filter (their events + events with no attendees + meals).
- **Daily goals** — month grid (goal rows × day columns), sticky name column, scrolls to today on phones;
  tap to tick in the owner's colour; future days and days before start are locked. Per goal: 🔥 streak (doesn't break
  until the day is over), % this month (only days so far since start), "x/y wk" if a weekly target is set.
  "Everyone" groups by person. Remove = deactivate (history kept; "Show removed goals" → Restore).

---

## 5. Everyday commands (PowerShell)

```powershell
# backend (port 8080, H2 file DB)
$env:APP_ADMIN_USERNAME="giorgos"; $env:APP_ADMIN_PASSWORD="choose-one"; .\mvnw.cmd spring-boot:run
# frontend (port 5173) — new terminal
$env:Path = "C:\Program Files\nodejs;" + $env:Path; cd frontend; npm run dev
# all checks (must pass before every commit)
.\mvnw.cmd test
cd frontend; npm run lint; npm test; npm run build
```
Open http://localhost:5173. The admin env vars only matter while the member table is empty
(delete `./data/` to start from scratch).

---

## 6. 🚀 How to deploy (do this later — everything needed is here)

### 6.1 What is already done in the code
- `Dockerfile` (multi-stage): Node builds `frontend/` → Maven builds the jar with the React app inside
  `static/` → small Java 21 JRE image, non-root, `TZ=Europe/Nicosia`, `SPRING_PROFILES_ACTIVE=prod`.
  Tests are *not* run in the image build — run them locally before pushing.
- `render.yaml` — Render "Blueprint": Docker web service, free plan, Frankfurt, health check
  `/actuator/health`, auto-deploy on every push to `main`.
- `application-prod.properties` — Neon datasource, small connection pool (lets Neon sleep), Secure/HttpOnly/SameSite
  cookies behind Render's HTTPS proxy, gzip.
- `ProductionEnvironmentCheck` — accepts Neon's connection string as is; if something is missing the log starts with
  `Production configuration is incomplete:` and lists it.
- `.env.example` — template for running the prod build locally.

### 6.2 Who does what
| | Who |
|---|---|
| Postgres server (hardware, upgrades, backups, uptime) | **Neon** (hosted) |
| Creating/updating tables | **The app** — Flyway runs migrations on every start |
| Accounts, secrets, pasting them into Render | **You** (once) |
| Future schema changes | New migration files in the code |

### 6.3 Step 1 — Neon (database), ~5 min
1. Sign up at https://neon.tech (free plan).
2. **New project**: name `family-hub`, Postgres **17**, region **AWS Europe Central (Frankfurt)**.
3. Database name `familyhub` (or keep the default `neondb`).
4. Dashboard → **Connect** → copy the **direct** connection string (*not* "Pooled"). It looks like
   `postgresql://familyhub_owner:npg_xxxx@ep-something-123456.eu-central-1.aws.neon.tech/familyhub?sslmode=require`
5. Keep it somewhere private (password manager). **Never commit it.**

### 6.4 Step 2 — Render (hosting), ~10 min
1. Sign up at https://render.com with GitHub; give it access to the `family-hub` repo.
2. **New → Blueprint** → choose the repo. Render reads `render.yaml` and asks for:
   | Variable | Value |
   |---|---|
   | `DATABASE_URL` | the Neon string from 6.3 step 4, pasted as is |
   | `APP_ADMIN_USERNAME` | your login, e.g. `giorgos` |
   | `APP_ADMIN_PASSWORD` | a strong password |
   | `APP_ADMIN_NAME` | preset `Admin` — change to your name if you like |
   | `REMEMBER_ME_KEY` | generated by Render automatically |
   | `SPRING_PROFILES_ACTIVE` | preset `prod` |
3. **Apply.** First build ~5–8 min. In the logs look for:
   - `Successfully applied 7 migrations` → tables created on Neon
   - `Started FamilyHubApplication` → app is up
4. Open `https://family-hub-xxxx.onrender.com`, log in, add the other family members under **Members**.

### 6.5 Step 3 — after the first login
- Render → service → **Environment**: delete `APP_ADMIN_PASSWORD` (only used when there are no members).
- Check the time zone: Goals highlights today's column; Chores "Today" shows today's Cyprus date.
- On each phone: open the site, log in with **Remember me**, and "Add to Home screen".

### 6.6 If something goes wrong
| Symptom in Render log | Fix |
|---|---|
| `Production configuration is incomplete: …` | Add the listed variables in Environment, then **Manual Deploy**. |
| `DATABASE_URL must start with postgresql://` | Re-copy the string from Neon → Connect. |
| Connection refused / timeout to Neon | Check the Neon project isn't deleted/suspended permanently; use the direct (non-pooled) host. |
| Flyway / SQL error in a migration | Paste the log to Claude. **Don't** edit an applied migration; fix with a new one. The first Postgres run is the first real test of the SQL (Docker wasn't available locally). |
| Build fails at `npm ci` or Maven | Paste the log; run all checks locally first. |
| Login works but logs out after a while | Make sure you ticked **Remember me**. Free Render restarts end sessions. |

Optional before deploying: put the Neon string in a local `.env` (gitignored) and ask Claude to run the
migrations against it from this PC — catches SQL problems before Render does.

### 6.7 Good to know
- **Free plans sleep:** Render free stops after ~15 min idle → next visit ~1 min to wake. Neon free suspends too
  (wakes in < 1 s). Render Starter (~$7/month) stays awake.
- **Updating the app:** push to `main` → Render rebuilds and redeploys; new migrations run automatically.
- **Backups:** Neon point-in-time restore (short window on free). For long-term copies, now and then:
  `pg_dump "<neon connection string>" -Fc -f familyhub-YYYY-MM-DD.dump`.
- **Changing `REMEMBER_ME_KEY`** logs everyone out.
- **Run the production image locally** (needs Docker Desktop): copy `.env.example` → `.env`, fill in, then
  `docker build -t family-hub .` and `docker run -p 8080:8080 --env-file .env family-hub`.
  With Docker installed, `.\mvnw.cmd test` also runs `PostgresMigrationTest` (all migrations on real Postgres 17).

---

## 7. Gotchas learned along the way
- **Jackson 3** rejects a missing value for a primitive in a request record → use `Boolean`/`Integer` with `@NotNull`.
- **Bean cycle** recipe ↔ meal plan solved with a separate `MealPlanRecipeUsage` (`RecipeUsage` hook).
- Bulk JPQL deletes leave stale managed entities → use derived `deleteBy…` methods.
- `@mantine/form` uncontrolled mode passes `defaultValue` → `MemberSelect` supports both.
- Tests: Mantine needs `env="test"` in the provider; jsdom needs `matchMedia`, `ResizeObserver`, `document.fonts` polyfills
  (`frontend/src/test/setup.ts`).
- PowerShell: commit with several `-m` flags (here-strings piped to `Out-Null` silently failed);
  write files without a BOM when generating Java.
- `src/main/resources/static/` is build output (gitignored) — don't commit it.

---

## 8. Possible next steps (not started)
- First deploy (section 6), then real-phone testing.
- Lazy-load pages to shrink the ~500 KB JS bundle (faster first load on phones).
- Install Docker Desktop so the Postgres migration test runs locally.
- A GitHub Actions workflow running all checks on every push.
- Make it installable as an app (PWA manifest + icons).
