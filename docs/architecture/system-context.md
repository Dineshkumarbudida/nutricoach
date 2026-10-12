# System context

```text
User / caregiver / administrator
              |
              v
       React web client
              |
              v
       Spring Boot API
              |
      +-------+--------+
      |                |
Deterministic       PostgreSQL
calculation and     versioned data,
recommendation      user records,
domain              provenance
      |
      +---- optional AI adapter ---- local/self-hosted/external model
```

The optional AI adapter calls application services. It does not access the database as an authority and does not bypass validation.
