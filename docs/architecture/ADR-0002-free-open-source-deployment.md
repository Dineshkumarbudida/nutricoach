# ADR-0002: Free and open-source reference deployment

- **Status:** Accepted
- **Date:** 2026-10-12

## Context

NutriCoach must remain self-hostable and useful without a paid AI API, proprietary platform-as-a-service, managed database, or commercial deployment control plane. The foundation also lacks application-level authentication, so deployment defaults must minimize accidental exposure.

## Decision

1. The reference runtime is a Linux host using open-source OCI tooling and Docker Compose-compatible configuration.
2. PostgreSQL, the Spring Boot backend, and the React/Nginx web service run as separate containers.
3. PostgreSQL and the backend share an internal data network. The backend and web service share a separate application network so loopback or LAN port publication works without publishing the database.
4. Database credentials are generated locally and mounted with Compose secrets rather than stored as plaintext environment variables.
5. Host publishing defaults to loopback for the web and backend services.
6. Trusted-LAN mode may publish only the web service to all interfaces; the backend remains loopback-only and the database remains unpublished.
7. Public access uses Caddy, pinned to a reviewed release, for automatic TLS and an interim Basic Authentication gate.
8. The Caddy admin API is disabled, the Actuator path is blocked publicly, the outer Authorization header is removed before proxying, and logs are rotated locally.
9. Direct Compose deployment remains the canonical path. Optional dashboards must never become a requirement.
10. Automated CI must start and exercise both local and HTTPS deployment modes.

## Consequences

### Positive

- No paid deployment service is required.
- The stack can run on a spare Linux computer, home server, or operator-controlled virtual machine.
- Secrets are separated from source control.
- The database remains isolated from published application ports.
- Public TLS setup is reproducible and does not require a proprietary reverse proxy.
- The deployment remains portable and inspectable.

### Negative

- The operator is responsible for hardware, OS patching, DNS, firewall rules, backups, and availability.
- Building images on small hardware may be slow.
- The application network permits normal container networking for the web and backend; it is not a substitute for host firewall policy.
- Caddy Basic Authentication is only an outer single-instance gate; it does not provide per-user authorization.
- Public multi-user hosting remains blocked until application authentication and authorization are implemented.

## Alternatives considered

- **Managed free tiers:** rejected as the reference path because limits and availability can change and the platforms are proprietary.
- **A self-hosted PaaS dashboard:** not required; it adds memory use and operational complexity without improving the core foundation.
- **Kubernetes:** rejected for the initial deployment because it is unnecessary for a small modular monolith.
- **One internal network for every service:** rejected because Docker host port publication was not reliable for the local deployment path; data isolation and application reachability require separate networks.
- **Exposing the web service directly:** retained only for loopback and trusted-LAN modes; public exposure requires the Caddy gate.
