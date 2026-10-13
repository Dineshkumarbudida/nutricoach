# Vercel + Supabase deployment

## Target architecture

- Vercel: React + TypeScript + Vite frontend.
- Vercel: Spring Boot API as an OCI container function, listening on the Vercel `PORT`.
- Supabase: managed PostgreSQL for persistence.
- GitHub Actions: CI and deployment verification.
- Docker Compose + PostgreSQL + Caddy remain the self-hosted fallback.

Vercel added Dockerfile/Containerfile support for Functions on Fluid compute in June 2026. The container must start an HTTP server on the supplied `PORT`. See the Vercel documentation for current limits and supported features.

## Supabase connection

For the Spring Boot backend running on Vercel, use the Supabase Shared Pooler in **session mode** (port 5432) when the environment is IPv4-only. Supabase documents the Shared Pooler as IPv4-only and session mode as the option for persistent connections that need IPv4.

Set these Vercel API-project variables:

- `DATABASE_URL`: JDBC URL derived from the Supabase Connect dialog, for example `jdbc:postgresql://POOLER_HOST:5432/postgres?sslmode=require`
- `DATABASE_USER`: exact pooled username from Supabase
- `DATABASE_PASSWORD`: Supabase database password
- `DATABASE_POOL_SIZE`: start at `2`
- `NUTRICOACH_WEB_ALLOWED_ORIGINS`: exact Vercel frontend origin

Do not commit credentials. Do not expose the Supabase database password or service-role key to the browser.

## Flyway

The existing Flyway migrations remain the schema source of truth. Point the API deployment at the new Supabase database and let Flyway create the schema. Do not hand-edit production schema outside migrations.

## Frontend

Vercel project settings:

- Root Directory: `web`
- Framework: Vite
- Build Command: `npm run build`
- Output Directory: `dist`
- Production Branch: `main`

Set:

`VITE_API_BASE_URL=https://<api-project>.vercel.app`

The frontend falls back to the Vite local development proxy when this value is unset.

## API

Vercel project settings:

- Root Directory: `backend`
- Production Branch: `main`

The repository contains `backend/Dockerfile.vercel`. It builds the existing Spring Boot application with Java 21 and starts it on `$PORT`.

## CORS

Only the exact production frontend origin should be allowed by the backend. For previews, use a controlled preview-origin strategy rather than a wildcard.

## Production gate

Do not call the deployment production-ready until these checks pass against the real hosted services:

1. Supabase connection succeeds with TLS.
2. Flyway migration succeeds.
3. Backend starts and `/api/v1/meta` responds.
4. Deterministic nutrition calculation endpoint returns expected test results.
5. Frontend reaches the API from the Vercel domain.
6. CORS rejects an unrelated origin.
7. No Supabase credentials occur in frontend assets.
8. GitHub CI remains green.
9. Authentication and authorization are enabled before real personal profiles are exposed.
10. Privacy, backup, and deletion behavior are reviewed.

## Current integration constraint

This repository can be prepared for Vercel + Supabase entirely in GitHub. Actual creation of the Vercel project and Supabase project requires authorization to those providers. The current ChatGPT environment does not expose a connected Vercel or Supabase action, so no provider account or secret should be fabricated.
