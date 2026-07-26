# TrueTrace Banking + AML Backend

[![Git Clones](https://badgen.net/https/cdn.jsdelivr.net/gh/Little-Boy-s-TrueTrace/truetrace-deployment@main/truetrace-bank-backend-clone-badge.json)](https://github.com/Little-Boy-s-TrueTrace/truetrace-deployment)
[![Unique Cloners](https://badgen.net/https/cdn.jsdelivr.net/gh/Little-Boy-s-TrueTrace/truetrace-deployment@main/truetrace-bank-backend-uniques-badge.json)](https://github.com/Little-Boy-s-TrueTrace/truetrace-deployment)
[![Release Downloads](https://badgen.net/https/cdn.jsdelivr.net/gh/Little-Boy-s-TrueTrace/truetrace-deployment@main/downloads-badge.json)](https://github.com/Little-Boy-s-TrueTrace/truetrace-deployment/releases)

> **Part of the [Little Boy's TrueTrace](https://github.com/Little-Boy-s-TrueTrace) project** -- an end-to-end AI-powered banking security platform.

Spring Boot REST API for the TrueTrace demonstration. It provides customer authentication, accounts, transfers, AML compliance checks, audit logs, JWT enforcement, IP blocking, and Kafka fraud-event publication.

## Capabilities

- User registration, login, logout, and JWT revocation
- Account detail lookup and AML checks
- Transaction execution and history search
- Concurrency and idempotency protections around transfers
- Security log and banned-IP management
- Kafka publication of telemetry
- PostgreSQL runtime persistence and H2-backed tests

## Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 16
- Optional Kafka broker
- Docker for container builds

## Configuration

| Variable | Required | Purpose |
|---|---:|---|
| `SPRING_DATASOURCE_PASSWORD` | yes | Database password |
| `JWT_SECRET` | yes | High-entropy JWT signing secret |
| `TRUETRACE_SECURITY_SYNC_TOKEN` | yes | Internal service authentication token |
| `KAFKA_BOOTSTRAP_SERVERS` | no | Broker list; default `localhost:9094` |

## Run Locally

```bash
createdb -h localhost -U postgres truetrace
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/health
```

## Docker

```bash
docker build -t truetrace-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/truetrace \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD='<database-password>' \
  -e JWT_SECRET='<jwt-secret>' \
  -e TRUETRACE_SECURITY_SYNC_TOKEN='<internal-token>' \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9094 \
  truetrace-backend
```

## Tests and Build

```bash
mvn test
mvn clean verify
mvn clean package
```

## Security Boundaries

- Never use seeded users in production.
- Use TLS at the gateway and rotate JWT/internal tokens independently.
- Treat logs as sensitive: they may contain compliance payload context and customer identifiers.
