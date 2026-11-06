# Family Hub

Personal household website (chores, groceries, recipes/meal plan, calendar, daily goals).
Full requirements: @docs/requirements.md

## Stack
- Java 21, Spring Boot 3.x, Maven (use the Maven wrapper: `./mvnw` / `mvnw.cmd`)
- Spring Web, Spring Data JPA, Spring Security (form login), Validation, Thymeleaf, Actuator
- PostgreSQL (hosted on Neon) with Flyway migrations in `src/main/resources/db/migration`
- Thymeleaf + Bootstrap 5 via CDN; small amounts of htmx / vanilla JS. No SPA framework.
- Tests: JUnit 5, Spring Boot Test; Testcontainers or H2 for repository tests

## Structure
Package per feature under `com.solomonidis.familyhub`:
`member`, `chore`, `grocery`, `recipe`, `mealplan`, `calendar`, `goal`, `common`.
Each feature has entity, repository, service, controller, and Thymeleaf templates in `templates/<feature>/`.
Business logic lives in services, not controllers.

## Config
- DB from env vars: `DATABASE_URL` (jdbc:postgresql://...?sslmode=require), `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `server.port=${PORT:8080}`
- `spring.jpa.hibernate.ddl-auto=validate` — schema changes only via new Flyway migrations, never edit an applied migration
- Never commit secrets. Local secrets go in `.env` (gitignored)

## Rules
- Build features one at a time; after each, run `./mvnw test` and make sure it passes before moving on
- Grocery merging logic (unit conversion G↔KG, ML↔L) must have unit tests
- Keep the UI mobile-friendly
