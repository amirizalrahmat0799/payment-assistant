# Security

Payment Assistant is a portfolio project that simulates payment systems. It is not affiliated with any real payment scheme, bank or card network, and it is not meant to handle real money or real customer data.

## Reporting a vulnerability

Please report security issues privately, not in a public issue:

- through GitHub: **Security > Report a vulnerability** on this repository, or
- by email to amirizalrahmat@gmail.com.

Include the steps to reproduce and the version or commit you tested. I will reply within a week.

## Supported versions

Only the latest version on `main` receives fixes.

## Demo credentials

The repository contains credentials that exist only so the demo runs with one command. They are public, so change all of them before running this anywhere other than your own machine:

- The PostgreSQL password `assistant` in `docker-compose.yml` and the application config

## Known limitations

The assistant's tools are read-only: it can explain how to refund a payment but cannot issue one. The rate limiter
and chat memory are in memory, so limits apply per instance (a Redis limiter is on the roadmap).
