# ADR-0005: Circuit breaker and quota linkage

- **Status**: Accepted — see R-003 in `openstrata-meta/contracts/adr-resolutions.md`
- **Date**: 2026-07-17
- **Suggested by**: OpenStrata Architecture Group
- **Repository**: ai-billing-service
- **Source**: `design/DESIGN.md` §16 Open Issue
- **Association**: (within this repository)

##Context

The circuit breaker granularity of `BudgetExceeded → Quota circuit breaker` (tenant-level QPS or application-level) is to be determined. It is recommended that tenant-level + application-level options be selected. --- > Change Record > | Version | Date | Description | > | --- | --- | --- | > | v1.0-Draft | 2026-07-17 | Initial detailed design, covering skeletal skeleton, 16 sections complete | > Traceability Matrix (this document section ↔ Architectural Design Document § Number) > | Section | Architecture Document § | > | --- | --- | > | 1 Domain Context | §8.3 / §4.7.2 / §12.4 | > | 2 Responsibility list | §4.7.2 / §8.3 | > | 3 Domain model | §15.5.2 / §8.3 | > | 4 Application layer use cases | §15.5.2 ② | > | 5 Domain service rules | §4.7.2 / §8.2 / §14.5 | > | 6 SPI ports and adapters | §10.3 / §10.4 / §15.5.4 | > | 7 External API contract | §8.3 / §16.4 | > | 8 Data model | §8.2 / §16 base | > | 9 Business process timing | §8.3 / §15.5.2.2 | > | 10 Configuration and Profile | §12.1 / §12.2 / §12.4 | > | 11 Integration points | §4.7.2 / §15.2 / bom.yaml | > | 12 Security and multi-tenancy | §8 / §14.3 / §4.7.4 | > | 13 Observability | §4.8 | > | 14 Deployment and resiliency | §9.2 | > | 15 Testing Strategy | §15.5.5 | > | 16 Open Questions | — |

## Decision Options (Options Considered)

1. **Maintain status quo / conservative default**: Maintain current behavior, controlled by configuration switches or explicit parameters, and do not introduce destructive changes.
2. **Unified implementation after cross-repository alignment**: Agree on a clear contract with the relevant service (`corresponding governance service`) before implementation.
3. **Phased introduction**: Leave a placeholder/default switch in the current stage, and solidify it in subsequent stages after the dependent capabilities are ready (see Related Architecture §).

## Recommended decision (Decision)

This ADR solidifies "circuit breaker and quota linkage" into an architectural decision record and incorporates it into `design/adr/` for continuous tracking. This issue stems from the `design/DESIGN.md` §16 open issue and is still open.

**Conservative Default Principle**: Before the final decision is made, the "minimum available + explicit configuration switch" shall prevail, maintain the current behavior, and not destroy the existing contract and cross-repository SPI interface; this ADR status will be written back after review by the relevant team.



## To be aligned / Follow-ups (Follow-ups)

- Associated architecture documents §10.3 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §10.4 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §12.1 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §12.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §12.4 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §14.3 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §14.5 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §15.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §15.5.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §15.5.2.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §15.5.4 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §15.5.5 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §16 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §16.4 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §4.7.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §4.7.4 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §4.8 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §8 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §8.2 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §8.3 (as a basis for decision-making and a source of consistency verification).
- Associated architecture documents §9.2 (as a basis for decision-making and a source of consistency verification).
- **Resolution (R-003)**: Accepted — the `BudgetExceeded → Quota circuit breaker` trips on tenant-level QPS with an application-level (global) ceiling as backstop; both thresholds are configuration-driven. See `openstrata-meta/contracts/adr-resolutions.md`.

## Traceback

- Upstream design: `design/DESIGN.md` §16 Open issue
- Relevance index: see `design/adr/README.md`
