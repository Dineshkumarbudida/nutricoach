# ADR-0002: Docker Compose is the reference deployment

**Status:** Accepted  
**Date:** 2026-10-11

## Decision

The reference deployment contains:

- React/TypeScript web client served by Nginx.
- Java 21/Spring Boot API.
- PostgreSQL 18 database.
- Persistent database volume.
- Health checks and environment-based configuration.

Core behavior must not require an external model or telemetry endpoint.

## Supported modes

- A private single-user installation.
- A family or organization instance with isolated accounts.
- A community-hosted instance after authentication, authorization, abuse controls, backup, and operational security are complete.

## Security note

The foundation milestone intentionally has no authentication and is local-development only.
