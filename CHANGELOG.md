# Changelog

All notable changes to this project. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Dependabot: weekly, grouped minor and patch updates for Maven, the Docker base image and GitHub Actions, so CI checks them together. Major upgrades are left
  for deliberate, hand-made changes.

## [1.0.0] - 2026-09-30

### Added
- AI support assistant for gateway merchants on Spring Boot 3.5 + Spring AI, running on Amazon Bedrock or local
  Ollama models.
- Read-only tool calling with the merchant's own API key (payments, refunds, payouts, settlements).
- Retrieval-augmented answers over the gateway documentation, with sources, and incremental document ingestion.
- Per-merchant conversation memory, rate limiting and token usage reporting.

### Fixed
- Spring now uses the intended `RateLimiter` constructor.

[Unreleased]: https://github.com/amirizalrahmat0799/payment-assistant/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/amirizalrahmat0799/payment-assistant/releases/tag/v1.0.0
