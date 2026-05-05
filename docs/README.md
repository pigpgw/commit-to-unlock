# Commit-to-Unlock Documentation

This directory is the active planning and evidence set for Commit-to-Unlock. Historical drafts were removed after their still-valid decisions were folded into the documents below; use git history only when old context is required.

## Read First

| Order | Document | Use it for |
| --- | --- | --- |
| 1 | [MVP execution plan](mvp-execution-plan.md) | Current build order, gate status, and what not to build yet. |
| 2 | [Product and security hardening plan](product-security-hardening-plan.md) | Non-negotiable product, privacy, security, and platform-policy boundaries. |
| 3 | [Competitive service review](competitive-service-review.md) | Market positioning, paid moat, and competitor lessons. |
| 4 | [MVP gap analysis](mvp-gap-analysis.md) | Known gaps, risk register, and next fixes. |
| 5 | [Android dogfood runbook](android-dogfood-runbook.md) | Real-device smoke, 14-day dogfood protocol, and evidence template. |
| 6 | [Decision log](decision-log.md) | Decisions that should not be reopened without new evidence. |

## Design And Runtime Specs

| Document | Use it for |
| --- | --- |
| [App design](app-design.md) | Product concept, brand tone, visual direction, and architecture. |
| [Control and account design](control-account-design.md) | Selected-target blocking, account paths, deletion, and user control. |
| [Proof policy MVP](proof-policy-mvp.md) | Quest, proof, exception, weekday, holiday, and override behavior. |
| [Security and logic review](security-and-logic-review.md) | Logic invariants, threat model, and security checks. |
| [GitHub Sprint 4 entry spec](github-sprint4-entry.md) | HMAC, dedupe, retention, and GitHub runtime entry criteria. |

## Current Source Of Truth

The current MVP is Android-first and evidence-gated:

```text
MVP-A: prove selected-app local enforcement on real Android devices.
MVP-B: only after that, connect verified developer proof to a credit ledger.
```

If documents conflict, follow this order:

1. Platform policy and law.
2. [Product and security hardening plan](product-security-hardening-plan.md).
3. [MVP execution plan](mvp-execution-plan.md).
4. [Decision log](decision-log.md).
5. More specific implementation docs.

## Do Not Build Yet

- GitHub scoring runtime.
- Payments, money stake, fines, or subscription flows.
- Parent, school, child, MDM, or supervised-device mode.
- AccessibilityService, Device Admin, uninstall prevention, or whole-phone lock.
- Full private-diff storage or full-diff LLM judging.
- Leaderboards or shame-driven social comparison.

## Evidence Locations

| Evidence | Location |
| --- | --- |
| Android screenshots | [assets/screenshots/android](assets/screenshots/android) |
| Dogfood exports | `artifacts/android-dogfood/` locally, intentionally ignored by git |
| Policy golden fixtures | [../fixtures/policy-golden.json](../fixtures/policy-golden.json) |
| Android package docs | [../apps/android/README.md](../apps/android/README.md) |
| iOS package docs | [../apps/ios/README.md](../apps/ios/README.md) |
