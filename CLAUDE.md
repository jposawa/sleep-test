# CLAUDE.md — Engineering Rules & Project Guide

Base instructions for any agent (or human) working in this repository. These are
durable rules; they are versioned. The evolving, task-specific attack plan lives
in `specs/PLANO_DE_ATAQUE.md` (git-ignored working doc — read it for current
strategy, PR sequence, and open decisions).

---

## 0. Hard rules (non-negotiable)

1. **Code is English. Always.** Regardless of the language used in chat or in
   planning docs, ALL resulting code — identifiers, type names, API fields, DB
   objects, log messages, commit messages, PR descriptions, and **every code
   comment** — MUST be written in English. No Portuguese (or any other language)
   in source, tests, migrations, or config.

2. **No scope drift.** Build exactly what the requirements ask — no more, no
   less. Do not add frameworks, endpoints, columns, abstractions, or "nice to
   haves" that the assignment did not request. The README (line 42) explicitly
   says the provided server/DB/build defaults are good enough; do not tweak
   infrastructure beyond what is strictly needed to make the required features
   work. When tempted to add something, check it against the README's functional
   requirements first — if it is not there, do not build it.

3. **Read requirements carefully; clear every doubt.** Re-read the `README.md`
   and re-check the wireframe (`resources/wireframes.png`) before and during each
   piece of work. Any doubt — even a small one — MUST be resolved by re-reading
   the requirements and/or asking the user. Never guess a requirement, never
   assume intent from a field name, and never "fill the gap" silently. If two
   readings are possible, ask.

4. **Commits and pushes belong to the user.** For safety, agents do NOT run
   `git commit`, `git push`, `git merge`, or any history-rewriting command on
   their own. Stage and prepare the work, then hand it over — propose the commit
   message and let the user run it. Act only with **express, per-action approval**
   from the user; approval granted once does not carry over to the next commit or
   push. This applies with extra force to anything that leaves the machine
   (`push`, PR creation) and to destructive operations (`reset --hard`,
   `push --force`, branch deletion).

---

## 1. What this project is

REST API for a **sleep logger**, to be integrated into Noom's web interface.
Three functional requirements, mapping to the three wireframe panels:

- **REQ 1 (create + fetch last night):** create a sleep log for last night;
  fetch last night's sleep. Empty state (wireframe REQ 1C) when none exists.
- **REQ 3 (30-day averages):** date range, average time in bed, average
  bedtime/wake time, and frequency of each morning feeling `[BAD, OK, GOOD]`.

Auth is out of scope, but the API must be **aware of the concept of a user**.

Authoritative source of requirements: `README.md`. If this file and the README
ever disagree, the README wins — and flag the discrepancy.

## 2. Stack & conventions

- **Kotlin + Spring Boot 2.7 + Java 17** (follow the template's grain — do not
  switch languages).
- Data access via **`NamedParameterJdbcTemplate`** (already configured). **No
  JPA/Hibernate.** SQL stays explicit.
- **Flyway** for all schema changes — never mutate the schema outside a
  versioned migration.
- **PostgreSQL** (provided via `docker-compose`).
- Layering: **Controller → Service → Repository**. Controllers do HTTP only;
  business logic lives in services; SQL lives in repositories.
- Naming: standard Kotlin/Spring conventions. DTOs are explicit; do not leak
  DB rows straight to the wire.

## 3. Testing

- **Repository:** integration tests against the compose Postgres with Flyway
  migrations. **Do not** add Testcontainers or other infra (README line 42).
- **Service / business logic:** plain unit tests (JUnit + AssertJ, already on
  the classpath). Cover the logic that carries risk: average computations,
  feeling frequencies, circular time-of-day averaging, "last night" resolution,
  validation. Do not test trivial getters.
- Keep the existing `unittest` profile for tests that need no DB.

## 4. Git & PR discipline

- Work in small, focused branches; merge via PRs. One coherent change per PR.
- Commit messages and PR descriptions: production-level, English, explain the
  *why*.
- **The user owns commits and pushes** — see hard rule §0.4. Agents prepare and
  propose; the user executes, unless expressly approved for that specific action.
- **Never commit build artifacts** (`build/`, `.gradle/`), the docker DB volume
  (`db/`), or the working specs (`specs/`). `.gitignore` enforces this.
- Base branch: `main`.

## 5. How to run

- Full stack: `docker-compose up --build` (needs Docker Desktop; ports 5432 +
  8080). On Windows, ensure Docker Desktop is running first.
- The compose file builds `./sleep` and starts Postgres with a healthcheck.

## 6. Working docs

- `specs/PLANO_DE_ATAQUE.md` — current attack plan (git-ignored). Read it for the
  live PR sequence, architecture decisions, identified gotchas, and the
  done-when checklist. Keep it updated as decisions change.
- Base/durable instructions (this file, coding standards, patterns) are
  versioned. Whitelist any versioned specs under `specs/standards/` in
  `.gitignore` as they are added.
