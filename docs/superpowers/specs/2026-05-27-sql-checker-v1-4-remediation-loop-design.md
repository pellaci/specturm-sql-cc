# SQL Checker v1.4 Remediation Loop Design

Date: 2026-05-27

## Product Thesis

SQL Checker v1.4 should evolve from a consulting-style SQL diagnosis report into a remediation-loop product.

The v1.3 report already answers "where is the risk" and "what should be fixed first" at a project level. v1.4 must make the next step operational: convert diagnosis into repair tasks that can be assigned, verified, and rescanned without turning the tool into a full project-management system.

The primary reader remains a technical owner reviewing a messy Java/MyBatis codebase. The secondary users are engineers who receive focused SQL remediation tasks and need enough context to fix them safely.

## Target Outcome

After scanning a project, a technical owner should be able to:

1. Identify the top remediation campaigns.
2. Export or copy task-sized repair items.
3. Understand why each task matters.
4. Apply a concrete repair pattern.
5. Re-run the scanner and verify risk reduction.

The product should shift the report language from raw findings to a governance workflow:

- Finding: "This SQL triggered a rule."
- Task: "Fix this specific query pattern in this file, using this repair recipe, then verify with this acceptance condition."
- Campaign: "This group of related tasks should be handled together because they share risk, ownership, and verification logic."

## Scope

### In Scope

- A remediation-oriented report section built from the existing `DiagnosticReport`.
- Task-level view model derived from findings and campaigns.
- Repair recipes for the highest-value recurring issue classes.
- Task confidence labels that separate confirmed issues from manual-review items.
- Acceptance checks for every campaign and task.
- JSON output for remediation tasks so downstream tooling can consume them.
- Report UX improvements that make task triage, filtering, copying, and verification easier.

### Out of Scope

- Issue tracker integration.
- Automatic code modification.
- Historical trend dashboards.
- Full DBA-grade index design.
- AI-generated explanations.
- Runtime production telemetry ingestion.

## Experience Principles

### Action Before Exhaustiveness

The HTML report should show the work that matters first. Complete raw diagnostic data belongs in JSON. The report should not make a reviewer read clean SQL or generic evidence gaps before seeing actionable risk.

### Evidence Honesty

Every task must state its evidence level:

- `CONFIRMED`: parsed SQL plus deterministic static rule or successful EXPLAIN evidence.
- `LIKELY`: parsed SQL plus strong template inference, but missing runtime evidence.
- `NEEDS_REVIEW`: dynamic template, unknown rule, syntax uncertainty, or missing context.

Static analysis must not pretend to be runtime proof.

### Repair Pattern Over Generic Advice

Recommendations should be written as concrete repair recipes:

- Unsafe `${}` dynamic value -> bound parameter or whitelist mapping.
- Dynamic `ORDER BY` -> enum-backed column map.
- Unbounded read query -> add business filter, pagination, or explicit limit.
- `SELECT *` -> explicit field list aligned with caller needs.
- Dangerous DML -> require WHERE guard, bounded condition, or explicit allowlist.

### Verification Built In

Each task should include a "done means" section. A task is not useful if it only says what is wrong. It should define what the next scan should show after the fix.

## Information Architecture

The v1.4 report should use this reading order:

1. **Executive Governance Brief**
   - Risk level.
   - Recommended remediation order.
   - Number of campaigns and tasks.
   - Confirmed vs review-needed task counts.

2. **Remediation Campaign Board**
   - P0/P1/P2 campaign columns or stacked sections.
   - Each campaign shows task count, files, top affected modules, evidence level, and acceptance checklist.
   - Campaign cards link to filtered task detail.

3. **Repair Recipe Library**
   - Short, reusable recipes keyed by rule family.
   - Each recipe includes unsafe pattern, safe pattern, example guidance, and verification method.

4. **Task Detail**
   - Only issue-bearing SQL tasks.
   - Task title uses file and rule, not internal hash ids.
   - Each task includes location, rule, SQL excerpt, impact, repair recipe, confidence, and acceptance check.

5. **Evidence and Diagnostics Appendix**
   - Parse failures.
   - Manual-review reasons.
   - EXPLAIN coverage limitations.
   - Known limits and methodology.

## Data Model Additions

Extend the report model without making Pebble infer product semantics.

### `remediation`

- `summary`
  - `campaignCount`
  - `taskCount`
  - `confirmedTaskCount`
  - `likelyTaskCount`
  - `reviewTaskCount`
  - `estimatedFirstPassFocus`
- `campaigns`
  - reuses or extends existing campaign objects.
- `tasks`
  - one task per actionable finding or grouped finding cluster.
- `recipes`
  - reusable repair recipes keyed by rule or rule family.

### `RemediationTask`

Fields:

- `id`: stable machine id, hidden by default in HTML.
- `title`: human-readable title, usually `<file>:<line> · <rule family>`.
- `priority`: `P0`, `P1`, or `P2`.
- `severity`: existing diagnostic severity.
- `theme`: safety, performance, correctness, or maintainability.
- `confidence`: `CONFIRMED`, `LIKELY`, or `NEEDS_REVIEW`.
- `location`: primary file, line, source type.
- `campaignId`: owning remediation campaign.
- `ruleTypes`: triggered rule types.
- `impact`: concise reason this matters.
- `repairRecipeId`: linked recipe.
- `recommendation`: task-specific repair instruction.
- `acceptanceCheck`: task-level verification condition.
- `evidence`: static issue message, EXPLAIN summary, or manual-review reason.
- `sql`: original and normalized SQL text.

### `RepairRecipe`

Fields:

- `id`
- `title`
- `appliesToRules`
- `unsafePattern`
- `safePattern`
- `steps`
- `verification`
- `knownLimits`

Initial recipes:

- `dynamic-value-binding`
- `dynamic-order-by-whitelist`
- `unbounded-query-containment`
- `select-star-field-list`
- `dangerous-dml-guardrail`
- `template-review-normalization`

## Task Generation Rules

Task generation should be deterministic and explainable.

### Priority Mapping

- P0:
  - SQL injection risk.
  - unsafe dynamic SQL interpolation.
  - dangerous DML or destructive statement.
- P1:
  - unbounded read query.
  - missing limit with order by.
  - full scan or no-index evidence when available.
  - high concentration of warning findings in one file.
- P2:
  - unknown or syntax-review findings.
  - duplicate SQL cleanup.
  - maintainability-oriented template cleanup.

### Confidence Mapping

- `CONFIRMED`:
  - deterministic static rule on valid parsed SQL.
  - or successful EXPLAIN issue.
- `LIKELY`:
  - valid SQL with skipped EXPLAIN.
  - valid template after normalization but missing runtime evidence.
- `NEEDS_REVIEW`:
  - unknown issue type.
  - parse failure.
  - dynamic SQL branch that cannot be reduced safely.
  - placeholder or database context prevents confident classification.

### Grouping Rules

Tasks are usually one finding each. Group only when all conditions are true:

- same file,
- same rule family,
- same repair recipe,
- same confidence level.

The first v1.4 implementation may skip grouping and still be acceptable if task output is clean and filterable.

## Report UX Requirements

- HTML must remain single-file and offline-friendly.
- The report must not render clean SQL tasks.
- Internal hash ids may appear in anchors or JSON, but not as primary visible labels.
- Campaign-to-task navigation must work through anchors or filters.
- Active filters must be visible.
- Copy actions should support:
  - copy SQL,
  - copy task summary,
  - copy acceptance check.
- Empty states should be useful:
  - no P0 tasks,
  - no issue SQL,
  - no EXPLAIN evidence configured.

## JSON Requirements

`report.json` should include full remediation data:

```json
{
  "remediation": {
    "summary": {},
    "campaigns": [],
    "tasks": [],
    "recipes": []
  }
}
```

HTML and JSON must be generated from the same model. No HTML-only remediation logic.

## CLI Requirements

The existing scan command should remain compatible:

```bash
java -jar target/sql-checker-1.2.0.jar scan -p <project> -o target/report.html --no-progress
```

For v1.4, CLI output should reduce raw noise:

- Keep `Manual review`.
- Rename or contextualize `EXPLAIN skipped` as an evidence limitation.
- Add `Remediation tasks` count.
- Add `P0/P1/P2 tasks` count.

## Testing Strategy

### Unit Tests

- Remediation task generation from static findings.
- Priority mapping for P0/P1/P2.
- Confidence mapping for confirmed, likely, and review-needed cases.
- Repair recipe selection by rule type.
- JSON serialization of remediation summary, tasks, and recipes.

### Integration Tests

- Fixture scan generates HTML and JSON with remediation sections.
- Dynamic SQL sample creates P0 task and dynamic binding recipe.
- Unbounded query sample creates P1 task and containment recipe.
- Unknown/template issue creates P2 review task.
- Clean SQL appears in JSON findings but not in HTML remediation tasks.

### Manual Acceptance

- Run on `installment-commodity`, `installment-trade`, and `installment-search`.
- Verify a technical owner can identify first-pass remediation work within 3 minutes.
- Verify every P0 campaign links to concrete tasks.
- Verify copy task summary produces enough context for an engineer to act.
- Verify report language does not overstate static evidence.

## Release Criteria

v1.4 is releasable when:

- JSON contains deterministic remediation tasks and repair recipes.
- HTML renders campaign board, recipe library, and task detail.
- CLI summarizes task counts without adding noise.
- Existing v1.3 executive summary and campaign sections remain compatible.
- `mvn test` passes.
- A fresh scan of the installment suite produces reports whose HTML task count matches JSON remediation task count.

## Future Iterations

### v1.5: Baseline and CI Gate

- Store baseline report.
- Show new vs existing tasks.
- Provide non-zero exit behavior for new P0 tasks.
- Generate PR-friendly summary.

### v1.6: Evidence Depth

- Safer EXPLAIN workflows.
- Database profile support.
- Index and access-path explanation improvements.
- Better MyBatis dynamic branch modeling.

### v1.7: Assisted Fix Workflow

- Optional patch suggestions.
- IDE-friendly task export.
- Rule suppression with rationale.
- Owner/module mapping.
