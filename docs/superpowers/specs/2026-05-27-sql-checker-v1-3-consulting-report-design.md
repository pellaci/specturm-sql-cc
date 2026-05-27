# SQL Checker v1.3 Consulting Report Design

Date: 2026-05-27

## Product Direction

SQL Checker v1.3 should evolve from a SQL scanning report into a consulting-style SQL health assessment report.

The primary user is a technical owner who needs to evaluate a messy Java/MyBatis project quickly. The report must answer four questions without requiring the reader to inspect every SQL detail:

1. Is this project's SQL risk acceptable?
2. Where is the risk concentrated?
3. What should be fixed first?
4. How do we prove that the fix reduced risk?

The v1.3 scope is intentionally report-centered. It should not expand into historical trends, broad multi-database support, or AI-generated explanations. Those can come later after the one-off assessment experience is strong.

## Target Scenarios

v1.3 serves three related scenarios, with consulting-style project health assessment as the product shell:

- Project health assessment: help a CTO, architect, or technical lead understand overall SQL risk and immediate priorities.
- Takeover assessment: help a new team understand risk hotspots before maintaining or refactoring an inherited project.
- Performance triage: surface likely performance risks and evidence, while keeping deep DBA-grade optimization as a later phase.

## Experience Principles

The report should feel like a professional technical audit deliverable, not a raw scanner dump.

- Three-level reading path:
  - 10 seconds: executive conclusion, score, risk level, and top actions.
  - 3 minutes: risk map, remediation campaigns, and evidence confidence.
  - 30 minutes: SQL-level findings, evidence, recommendations, and verification steps.
- Professional language:
  - Use consistent terms: risk judgment, impact scope, evidence, recommended action, verification method.
  - Avoid exaggerated or absolute claims when evidence is static or incomplete.
  - Clearly distinguish confirmed evidence from manual-review items.
- Visual hierarchy:
  - Prefer restrained technical-report styling, strong section structure, dense but readable tables, and clear severity hierarchy.
  - Avoid decorative UI that makes the report feel like a marketing page.
- Operator workflow:
  - A reader should be able to jump from summary to campaign, from campaign to SQL evidence, and from evidence to copyable repair context.
  - Filters and anchors should preserve orientation and make findings reproducible.
- Credibility layer:
  - Every high-level conclusion should show whether it is backed by static AST analysis, template inference, EXPLAIN evidence, or manual review.

## v1.3 Feature Modules

### 1. Executive Summary

Generate a concise summary from the existing `DiagnosticReport` data.

The summary should include:

- One-sentence risk conclusion.
- Top 3 drivers of risk.
- Top impacted modules or file groups.
- Recommended first actions.
- Evidence confidence overview.

Example tone:

> Current SQL risk is High. Risk is concentrated in mapper-level dynamic SQL and unbounded read queries. Start with the P0 dynamic SQL safety campaign, then reduce full-scan candidates in the commodity query path.

### 2. Remediation Campaigns

Group scattered findings into fixable campaigns.

Each campaign should include:

- Campaign id and title.
- Priority: P0, P1, or P2.
- Risk theme: safety, performance, maintainability, or correctness.
- Scope: files, modules, SQL count, and top examples.
- Why it matters.
- Recommended repair pattern.
- Acceptance checklist.
- Related findings.

Initial campaign types:

- Dynamic SQL safety stopgap: `${}` interpolation, unsafe string concatenation, and injection-risk templates.
- Unbounded query containment: `SELECT` without `WHERE`, `ORDER BY` without `LIMIT`, and high-risk full scans.
- Index and access-path review: missing index, full scan, no index used, high row estimates.
- Mapper template cleanup: duplicate SQL and large dynamic templates that require manual review.
- Destructive DML guardrail: unsafe `UPDATE`, `DELETE`, `DROP`, or `TRUNCATE` patterns when detected.

### 3. Evidence Confidence

Add confidence metadata to high-level conclusions and findings.

Suggested confidence levels:

- Strong: parsed SQL plus static AST rule, or parsed SQL plus successful EXPLAIN evidence.
- Medium: parsed SQL with template inference or partial static evidence.
- Needs Review: dynamic template, skipped EXPLAIN, unsupported SQL, or configuration gap.
- Failed: true parse failure or tool execution error.

The report should make evidence limits visible instead of hiding them. `Parse failures = 0` must not be confused with `all SQL is semantically verified`.

### 4. Review and Acceptance Checklist

Each campaign should have a practical verification path.

Checklist examples:

- Re-run `sql-checker scan` and confirm P0 campaign count is zero.
- Replace unsafe dynamic values with bound parameters or whitelisted field mappings.
- Run EXPLAIN for changed high-risk queries and confirm access type or row estimate improves.
- Add focused integration tests for query paths touched by the campaign.
- Confirm manual-review count drops for the targeted module or mapper.

### 5. UX and Professional Delivery

v1.3 must improve both readability and trust.

Required report improvements:

- Add an executive opening section before metrics.
- Add campaign cards or table directly after the risk map.
- Add confidence badges to summary claims and campaigns.
- Add stable anchors from campaign rows to underlying findings.
- Improve filter states so active filters are obvious and shareable by hash or visible state text.
- Add a report glossary for score, risk level, parse coverage, EXPLAIN coverage, manual review, and skipped EXPLAIN.
- Keep HTML single-file and offline-friendly.

## Report Information Architecture

Recommended order:

1. Executive conclusion
2. Risk scorecard
3. Evidence confidence and coverage
4. Risk hotspot map
5. Remediation campaigns
6. Takeover assessment signals
7. Performance evidence signals
8. Findings detail
9. Appendix: diagnostics, methodology, and glossary

This order prioritizes decisions before evidence. Detailed SQL evidence remains available, but it should no longer be the primary reading path.

## Data Model Extensions

Extend the report model rather than letting the template infer product semantics.

Proposed additions:

- `executiveSummary`
  - `riskConclusion`
  - `topDrivers`
  - `recommendedActions`
  - `confidenceSummary`
- `campaigns`
  - `id`
  - `priority`
  - `theme`
  - `title`
  - `summary`
  - `scope`
  - `evidenceLevel`
  - `recommendations`
  - `acceptanceChecklist`
  - `findingIds`
- `confidence`
  - `level`
  - `evidenceSources`
  - `limitations`
- `methodology`
  - `scoring`
  - `severityDefinitions`
  - `coverageDefinitions`
  - `knownLimits`

HTML and JSON must continue to share the same model.

## Non-Goals

v1.3 should not include:

- Historical comparison dashboards.
- Team owner assignment workflows.
- Full DBA-grade index design automation.
- New database dialect expansion beyond safety corrections.
- LLM-based report writing.
- Server-side report viewer.

## Success Metrics

The release is successful if:

- A technical owner can identify the top three remediation priorities within 3 minutes.
- Each high-priority recommendation links to concrete SQL evidence.
- Each campaign includes a verification checklist.
- Report language distinguishes evidence-backed facts from manual-review claims.
- Existing static report generation still works without a database.
- JSON contains the same executive summary and campaign data as HTML.

## Acceptance Tests

Automated checks:

- Fixture scan generates `executiveSummary`, `campaigns`, `confidence`, and `methodology` in JSON.
- HTML renders executive conclusion, remediation campaigns, confidence badges, glossary, and detail anchors.
- Dynamic SQL injection fixture appears in a P0 campaign with an acceptance checklist.
- EXPLAIN-disabled scan still produces complete campaigns and marks evidence limitations.
- EXPLAIN failure is shown as a diagnostic/config limitation, not a SQL issue.

Manual UX checks:

- Open the report and verify the first viewport communicates project risk and top actions.
- From a P0 campaign, navigate to SQL details and back without losing context.
- Confirm active filters are visible and resettable.
- Confirm professional tone: no exaggerated certainty, no noisy implementation jargon in executive sections.

## Release Path

Recommended iteration sequence:

1. Add report model fields and aggregation logic.
2. Generate executive summary and campaign data from current findings.
3. Redesign report top half around decision flow.
4. Add confidence badges and methodology/glossary.
5. Add tests and rerun real scan on `installment-commodity`.
6. Do a manual report-read review before release.

## Product Decisions

- Risk labels should keep the existing `Critical/High/Medium/Low` vocabulary for machine-readable consistency. The executive layer may add action-oriented wording, such as `Immediate Attention`, but it should not replace the canonical risk label.
- Campaign priority should combine severity, affected SQL count, affected file/module concentration, and evidence confidence. Severity alone is not enough because one isolated high-severity issue and one concentrated module-level pattern require different remediation planning.
- v1.3 should not estimate remediation effort numerically. It should provide repair patterns and acceptance checks, because effort estimation would create false precision without repository ownership and runtime context.
