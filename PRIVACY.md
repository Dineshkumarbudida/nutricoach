# Privacy for self-hosted NutriCoach

NutriCoach is designed so the operator can run the core application on infrastructure they control.

## Foundation behavior

- No telemetry or analytics service is configured by default.
- No external AI provider is required or contacted by the foundation build.
- Meal and profile data are stored in the operator's PostgreSQL database.
- The reference reverse proxy does not log request bodies.
- Container logs may contain technical metadata such as timestamps, paths, status codes, and errors; operators must avoid adding sensitive values to URLs.
- Database dumps, `.env`, and generated secret files remain under the operator's control.

## Operator responsibilities

A self-hosting operator determines who can access the service, how long data and backups are retained, where backups are copied, and whether the host is reachable from the internet. Operators should:

- Use local mode unless remote access is genuinely needed.
- Keep the host OS and container images patched.
- Protect `.env`, `secrets/`, database volumes, and backups.
- Encrypt off-host backups where appropriate.
- Avoid public multi-user use until application authentication and per-user authorization exist.
- Explain any added telemetry, external AI, email, monitoring, or backup provider to users before enabling it.

## Current limitation

The foundation is not a complete consumer service and does not yet implement account export, deletion, consent management, or per-user access control. Those controls must be implemented and tested before operating a multi-user instance.
