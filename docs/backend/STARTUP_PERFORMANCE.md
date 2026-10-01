# Startup Performance Measurement

Peladinhas logs bounded startup timing information during production startup.
The goal is to locate cold-start time spent inside Spring Boot, database
connection setup, Flyway, Hibernate/JPA, repository initialization, and the web
server without exposing secrets or adding a public diagnostics endpoint.

## Render Cold-Start Measurement

1. Deploy the backend normally.
2. Let the Render free web service suspend.
3. Trigger the service with `GET /health`.
4. In Render logs, find entries beginning with `startup.performance`.
5. Record the `summary`, each `phase`, and the `slowStepRank` entries.
6. Repeat at least five cold starts before comparing changes.

Useful lines look like:

```text
startup.performance summary applicationReadyMs=150000 recordedSteps=420 recordedDurationMs=185000 webServerPort=10000
startup.performance phase=datasource steps=8 durationMs=9000
startup.performance phase=flyway steps=3 durationMs=6000
startup.performance phase=jpa steps=24 durationMs=39000
startup.performance phase=repositories steps=22 durationMs=7000
startup.performance phase=web-server steps=9 durationMs=5000
startup.performance slowStepRank=1 durationMs=17000 name=spring.beans.instantiate tags=beanName=entityManagerFactory
```

## What Is Measured

- `applicationReadyMs`: elapsed time reported by Spring from application start
  until the application is ready to serve traffic.
- `recordedSteps`: number of Spring startup steps kept in the bounded startup
  buffer.
- `recordedDurationMs`: sum of recorded Spring startup step durations. Some
  steps can overlap, so this is diagnostic rather than a wall-clock total.
- `phase`: grouped startup steps whose names or safe tags mention datasource,
  Flyway, JPA/Hibernate, repositories, or the web server.
- `slowStepRank`: the slowest individual Spring startup steps with safe step
  names and bean-style tags.

## Safety and Limits

The instrumentation does not log environment variables, database URLs,
credentials, JSON Web Tokens, user data, SQL statements, or request payloads.
It does not add an endpoint and does not change `/health`.

The startup buffer is capped at 4096 steps. Logging happens once when the
application is ready. The overhead is limited to Spring's supported
`BufferingApplicationStartup` recording plus a one-time summary pass.

The phase totals are approximations based on Spring startup step names and tags.
Use them to identify where to investigate next, not as accounting-grade timing.
