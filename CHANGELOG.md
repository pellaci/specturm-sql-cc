# Changelog

## 1.2.0 - 2026-05-27

Release focus: make SQL Checker usable as a professional diagnostic-report tool for large Java/MyBatis codebases.

### Added

- Added a stable `DiagnosticReport` model shared by HTML and JSON output.
- Added automatic JSON output next to the requested HTML report.
- Added a single-file technical HTML report with overview, hotspots, insights, priority queue, filters, SQL evidence, and recommendations.
- Added report diagnostics for parse failures, manual review items, skipped EXPLAIN, and configuration warnings.
- Added Java/MyBatis XML/JS/TS/SQL fixture coverage for the real scan pipeline.
- Added CI workflow for Maven test execution.

### Changed

- CLI scan output now distinguishes SQL occurrences, unique SQL, parse coverage, parse failures, manual review, and EXPLAIN skipped.
- HTML details default to problem SQL and keep clean SQL bodies in JSON only, reducing report noise and file size.
- MyBatis extraction now preserves line locations for mapper statements.
- Unknown rule ids no longer fall back to unrelated issue types.
- PostgreSQL EXPLAIN generation uses read-only EXPLAIN and avoids ANALYZE-style execution.

### Fixed

- Fixed misleading parse-failure reporting for dynamic SQL that is parseable but still requires manual review.
- Fixed HTML escaping and interaction script regressions for local static reports.
- Fixed SQL injection recommendations so dynamic `${}` evidence gets actionable guidance.

### Verification

- `mvn test`
- `mvn -q -DskipTests package`
- CLI scan of `/Users/chenpengfei/installment-commodity`
- Static HTML script execution check

### Known Limitations

- Expert-rule analysis is still a placeholder.
- EXPLAIN coverage depends on a configured and reachable database.
- Manual review count is expected for dynamic SQL and skipped EXPLAIN paths; it is not a parser failure count.
