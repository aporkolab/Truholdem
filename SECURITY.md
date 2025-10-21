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

Thank you for helping keep TruHoldem secure!
