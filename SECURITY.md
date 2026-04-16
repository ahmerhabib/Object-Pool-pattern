# Security Policy

## Supported Versions

This repository currently supports security fixes on the `main` branch.

## Reporting a Vulnerability

If you discover a security issue, please do **not** open a public issue.

Instead:
1. Email the maintainer directly (or use GitHub private security reporting if enabled).
2. Include clear reproduction steps, affected files/paths, and impact.
3. Share any proof-of-concept in a safe, non-destructive format.

You can expect:
- Initial acknowledgement within 3 business days.
- Triage and severity assessment as quickly as possible.
- Coordinated disclosure once a fix is available.

## Secure Development Notes

- Never commit secrets, tokens, private keys, or `.env` files.
- Use local environment variables for credentials.
- Generated build artifacts (`bin/`, `target/`, `*.class`) should stay out of version control.
