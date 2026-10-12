# Security Policy

This foundation build does not yet implement authentication or authorization and must not be exposed to an untrusted network.

Report vulnerabilities privately to the maintainers rather than opening a public issue. A project security contact must be added before the first public release.

Security-sensitive areas include:

- Cross-user access and object-level authorization.
- Sensitive profile or food-log data in logs.
- Imports of malicious or oversized data.
- Prompt injection in future AI adapters.
- Secret handling and external model data retention.
- Backup, export, deletion, and restore behavior.
