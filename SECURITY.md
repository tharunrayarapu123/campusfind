# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

The CampusFind team takes platform security and student privacy seriously.

If you believe you have discovered a security vulnerability in CampusFind, please do **NOT** open a public GitHub issue.

Instead, please report it privately:
1. Contact the security team via email at `security@campusfind.edu` or campus system administration.
2. Provide a detailed summary with reproducible steps or Proof of Concept (PoC).
3. We will acknowledge receipt of your report within 48 hours and work with you to deploy a fix promptly.

## Security Practices in CampusFind

* **Strict Profile Separation:** The `prod` profile strictly prohibits development secrets, enforces minimum 32-character `JWT_SECRET`, disables Swagger UI, and uses Cloudinary over local storage.
* **Student Privacy Safeguards:** Personal phone numbers, full email addresses, and student IDs are never exposed on public item feeds.
* **Anti-Theft Verification:** Found items require security question verification before any claim or handoff can proceed.
* **Rate Limiting:** Token-bucket rate limiting restricts brute-force attempts and chat spamming (30 messages per minute maximum).
* **Defensive Error Handling:** Stack traces and internal database errors are suppressed from API responses.
