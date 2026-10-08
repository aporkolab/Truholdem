# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.x.x   | :white_check_mark: |
| 1.x.x   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in TruHoldem, please report it responsibly:

1. **Do NOT** create a public GitHub issue
2. Email the details to: security@aporkolab.com
3. Include:
   - Type of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

- **Response Time**: Within 48 hours
- **Resolution**: Security patches will be prioritized
- **Disclosure**: We follow responsible disclosure practices

## Security Measures

TruHoldem implements several security measures:

- JWT-based authentication with refresh tokens
- Password hashing with BCrypt
- Rate limiting on API endpoints
- Input validation and sanitization
- CORS configuration
- SQL injection prevention via JPA/Hibernate
- XSS protection in Angular

## Dependencies

We regularly update dependencies to patch known vulnerabilities:
- Dependabot monitors and creates PRs for updates
- Security scanning runs on every PR via Trivy
- The full npm dependency tree is audited in CI; high or critical findings fail the frontend job.

### Temporary npm overrides

- `shell-quote` is pinned to `1.11.0` to fix GHSA-pqg4-j6r4-53mv.
  `concurrently@9.2.4` otherwise pins the vulnerable `1.9.0`. Remove the override
  when the resolved parent dependencies require a patched version.
- `@istanbuljs/load-nyc-config@1.1.0` uses `js-yaml@4.3.2`, whose `load()` API
  supports its YAML configuration loader. This replaces the `js-yaml 3 →
  argparse 1 → sprintf-js` development dependency chain. There is no patched
  `sprintf-js` release for GHSA-hp3w-g68c-fv3c. Remove this override once the
  upstream loader no longer depends on `js-yaml 3`.

### Removed vulnerable build dependencies

The frontend uses `@angular/build` instead of `@angular-devkit/build-angular`.
This removes the Webpack dev-server → `http-proxy-middleware` → `micromatch` →
`braces` chain affected by GHSA-vfj7-8cjw-p6xm. There is no patched `braces`
release at the time of this migration; no audit suppression is used.

Thank you for helping keep TruHoldem secure!
