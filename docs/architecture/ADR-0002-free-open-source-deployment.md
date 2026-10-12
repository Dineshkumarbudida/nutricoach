# ADR-0002: Free and open-source reference deployment

- **Status:** Accepted
- **Date:** 2026-10-12

## Context

NutriCoach must remain self-hostable and useful without a paid AI API, proprietary platform-as-a-service, managed database, or commercial deployment control plane. The foundation also lacks application-level authentication, so deployment defaults must minimize accidental exposure.

## Decision

1. The reference runtime is a Linux host using open-source OCI tooling and Docker Compose-compatible configuration.
2. PostgreSQL, the Spring Boot backend, and the React/Nginx web service run as separate containers on an internal network.
3. Database credentials are generated locally and mounted with Compose secrets rather than stored as plaintext environment variables.
4. Host publishing defaults to loopback for the web and backend services.
5. Trusted-LAN mode may publish only the web service to all interfaces; the backend remains loopback-only and the database remains unpublished.
6. Public access uses Caddy, pinned to a reviewed release, for automatic TLS and an interim Basic Authentication gate.
7. The Caddy admin API is disabled, the Actuator path is blocked publicly, the outer Authorization header is removed before proxying, and logs are rotated locally.
8. Direct Compose deployment remains the canonical path. Optional dashboards must never become a requirement.
9. Automated CI must start and exercise both local and HTTPS deployment modes.

## Consequences

### Positive

- No paid deployment service is required.
- The stack can run on a spare Linux computer, home server, or operator-controlled virtual machine.
- Secrets are separated from source control.
- Public TLS setup is reproducible and does not require a proprietary reverse proxy.
- The deployment remains portable and inspectable.

### Negative

- The operator is responsible for hardware, OS patching, DNS, firewall rules, backups, and availability.
- Building images on small hardware may be slow.
- Caddy Basic Authentication is only an outer single-instance gate; it does not provide per-user authorization.
- Public multi-user hosting remains blocked until application authentication and authorization are implemented.

## Alternatives considered

- **Managed free tiers:** rejected as the reference path because limits and availability can change and the platforms are proprietary.
- **A self-hosted PaaS dashboard:** not required; it adds memory use and operational complexity without improving the core foundation.
- **Kubernetes:** rejected for the initial deployment because it is unnecessary for a small modular monolith.
- **Exposing the web service directly:** retained only for loopback and trusted-LAN modes; public exposure requires the Caddy gate.
