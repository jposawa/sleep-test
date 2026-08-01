# Sleep Logger API

REST service for logging sleep and reporting sleep statistics, built on the
template in this repository.

## Running

```bash
docker-compose up --build
```

Postgres and the API start together and the Flyway migrations are applied on
boot. The API listens on `http://localhost:8080`, and describes itself at
`http://localhost:8080/swagger-ui.html`.

## Endpoints

All of them identify the caller with an `X-User-Id` header. The migrations seed
one user, `1`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/sleep-logs` | Log a sleep |
| `GET` | `/api/v1/sleep-logs/last-night` | The most recent sleep, or `404` when there is none |
| `GET` | `/api/v1/sleep-logs/averages` | Averages over the last 30 days |

```bash
curl -X POST http://localhost:8080/api/v1/sleep-logs \
  -H 'X-User-Id: 1' -H 'Content-Type: application/json' \
  -d '{"bedStart":"2026-11-13T22:53:00-03:00",
       "bedEnd":"2026-11-14T07:05:00-03:00",
       "morningFeeling":"GOOD"}'
```

## Design

**The interval is the only stored temporal fact.** Total time in bed and the
date of the sleep are both derived from `[bed_start, bed_end]`, so they can
never contradict it. Neither is a column.

**Times cross the wire as instants, never as formatted dates.** Resolving a
calendar date requires the caller's timezone, which this API deliberately does
not take: a client that knows its own zone renders the date. The same applies to
the window reported by the averages endpoint - its bounds are instants.

Average bedtime and wake time are the exception in kind, because a time of day
has no date. They are reported in UTC, and a client shifts them into its own
offset. That is sound because the average is a circular mean, which is
equivariant under rotation - shifting every input shifts the result by the same
amount.

**Clock times are averaged on a circle.** The arithmetic mean of 23:50 and 00:10
is noon, which is wrong. Each time is treated as an angle over 24 hours, the
unit vectors are averaged, and the result is converted back.

**The morning feeling is a database type**, not a string with a constraint. It
is a closed set of three values, so the column cannot hold anything else.

**Nothing limits a user to one sleep per night.** Naps are legitimate entries,
and the requirements do not ask for the restriction.

## Assumptions

The requirements describe a sleep log as carrying "the date of the sleep
(today)", while the wireframe shows a card headed "November, 13th" next to
"10:53 pm - 7:05 am" - times that belong to the evening of the 13th and the
morning of the 14th. The two readings cannot both hold.

This implementation anchors a night on `bed_start`: a night belongs to the
evening it begins on, which is how the wireframe reads. In practice the API
returns both instants and lets the client decide what to display; the anchor
only governs ordering and the 30-day window.

## Known limitations

- **Daylight saving.** Averages of times of day are computed in UTC. Because a
  circular mean is rotation-equivariant, a client can shift the result into a
  fixed offset and get the same answer. In a zone that changes offset inside the
  30-day window, the two differ by up to an hour.
- **`docker-compose.yml` passes `SPRING_DATASOURCE_USER`**, which Spring binds
  to `spring.datasource.user` - a property nothing reads. The connection works
  because `spring.datasource.username`, which the datasource configuration does
  read, defaults to the same value in `application.properties`. Left as it
  ships: the assignment asks not to spend time on the provided configuration,
  and the values agree.

## Tests

```bash
./gradlew test              # unit tests, no database needed
./gradlew testIntegration   # repository tests, needs the stack running
```

Repository tests are tagged `integration` and excluded from `test`, so that
`gradlew build` - which the Dockerfile runs while no database exists - keeps
working unchanged.

Two ways to exercise the running API:

```bash
./scripts/test-api.sh
```

walks the whole flow and fails on any unexpected status. It expects a clean
database, since one of the things it asserts is the empty state.

`scripts/sleep-logger.postman_collection.json` covers the same requests for
Postman.
