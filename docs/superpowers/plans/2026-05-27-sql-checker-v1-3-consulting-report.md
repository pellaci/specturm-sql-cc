# SQL Checker v1.3 Consulting Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the v1.3 consulting-style SQL health assessment report: executive summary, remediation campaigns, evidence confidence, acceptance checklists, and professional report UX.

**Architecture:** Extend the existing `DiagnosticReport` view model and keep HTML/JSON on the same contract. `DiagnosticReportFactory` remains the aggregation boundary, `DiagnosticReportJsonSerializer` remains the dependency-free JSON boundary, and `diagnostic-report.pebble` remains the single-file offline HTML report.

**Tech Stack:** Java 17, Spring Boot 3.2, Lombok DTOs, Pebble templates, AssertJ/JUnit 5, Maven, existing SQL Checker scan/report DTOs.

---

## File Structure

- Modify `src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java`
  - Add nested DTOs for `ExecutiveSummary`, `Confidence`, `Methodology`, `RemediationCampaign`, and `CampaignScope`.
- Modify `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java`
  - Build executive summary, methodology, report-level confidence, and remediation campaigns from existing findings.
- Modify `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java`
  - Serialize the new fields so HTML and JSON stay aligned.
- Modify `src/main/resources/templates/diagnostic-report.pebble`
  - Rework the top half into a consulting report flow and render campaigns, confidence badges, glossary, and anchors.
- Create `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java`
  - Keep new aggregation tests out of the already-large `ReportServiceImplTest`.
- Modify `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`
  - Add focused HTML/JSON rendering assertions for the new contract.
- Modify `src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java`
  - Assert fixture CLI/report output contains the new JSON sections.
- Modify `src/test/resources/fixtures/mixed-repo/src/main/resources/mapper/UserMapper.xml`
  - Add one small dynamic SQL sample so the fixture deterministically triggers a P0 campaign.

---

### Task 1: Extend the Report Contract

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java`
- Create: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java`

- [ ] **Step 1: Write failing DTO aggregation smoke test**

Add this test class:

```java
package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticReportFactoryConsultingReportTest {

    @Test
    @DisplayName("should expose consulting report sections")
    void should_expose_consulting_report_sections() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-consulting")
                .status(ScanStatus.COMPLETED)
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(SqlStatementDto.builder()
                        .id("sql-risk")
                        .sqlType(SqlType.SELECT)
                        .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
                        .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
                        .validity(ValidityStatus.VALID)
                        .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                        .severity(SeverityLevel.CRITICAL)
                        .score(60)
                        .build()))
                .build());

        assertThat(report.getExecutiveSummary()).isNotNull();
        assertThat(report.getCampaigns()).isNotNull();
        assertThat(report.getConfidence()).isNotNull();
        assertThat(report.getMethodology()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: compilation fails because `DiagnosticReport` does not yet have `getExecutiveSummary`, `getCampaigns`, `getConfidence`, or `getMethodology`.

- [ ] **Step 3: Add DTO fields and nested classes**

In `DiagnosticReport`, add fields after `private Diagnostics diagnostics;`:

```java
private ExecutiveSummary executiveSummary;
private List<RemediationCampaign> campaigns;
private Confidence confidence;
private Methodology methodology;
```

Add nested DTOs before `Diagnostics`:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class ExecutiveSummary {
    private String riskConclusion;
    private List<String> topDrivers;
    private List<String> recommendedActions;
    private String confidenceSummary;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class RemediationCampaign {
    private String id;
    private String priority;
    private String theme;
    private String title;
    private String summary;
    private CampaignScope scope;
    private String evidenceLevel;
    private List<String> recommendations;
    private List<String> acceptanceChecklist;
    private List<String> findingIds;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class CampaignScope {
    private int sqlCount;
    private int fileCount;
    private List<String> files;
    private List<String> examples;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class Confidence {
    private String level;
    private List<String> evidenceSources;
    private List<String> limitations;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class Methodology {
    private List<String> scoring;
    private List<String> severityDefinitions;
    private List<String> coverageDefinitions;
    private List<String> knownLimits;
}
```

- [ ] **Step 4: Add temporary empty builders in factory**

In `DiagnosticReportFactory.from`, add these fields to the `DiagnosticReport.builder()` call after `.diagnostics(...)`:

```java
.executiveSummary(DiagnosticReport.ExecutiveSummary.builder()
        .riskConclusion("")
        .topDrivers(List.of())
        .recommendedActions(List.of())
        .confidenceSummary("")
        .build())
.campaigns(List.of())
.confidence(DiagnosticReport.Confidence.builder()
        .level("NEEDS_REVIEW")
        .evidenceSources(List.of())
        .limitations(List.of())
        .build())
.methodology(defaultMethodology())
```

Add this helper near other private helpers:

```java
private static DiagnosticReport.Methodology defaultMethodology() {
    return DiagnosticReport.Methodology.builder()
            .scoring(List.of("Score starts from SQL-level diagnostic scores and is capped by critical or warning issue concentration."))
            .severityDefinitions(List.of(
                    "CRITICAL: safety, destructive DML, injection, or production-impacting risk.",
                    "WARNING: performance, maintainability, or correctness risk that should enter remediation planning.",
                    "INFO: evidence or optimization signal that does not by itself block release."))
            .coverageDefinitions(List.of(
                    "Parse coverage measures SQL statements accepted by preprocessing and classification.",
                    "EXPLAIN coverage measures eligible SQL with successful execution-plan evidence.",
                    "Manual review marks dynamic templates, skipped EXPLAIN, and evidence gaps."))
            .knownLimits(List.of(
                    "Static analysis cannot prove runtime parameter values.",
                    "Database-free reports do not include real optimizer evidence.",
                    "Dynamic SQL templates may require manual review even when parse coverage is 100%."))
            .build();
}
```

- [ ] **Step 5: Run test and commit**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: test passes and the consulting report contract exists. Task 3 will strengthen campaign assertions from non-null to content-specific.

Commit:

```bash
git add src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java
git commit -m "Add consulting report contract"
```

---

### Task 2: Build Executive Summary, Confidence, and Methodology

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java`

- [ ] **Step 1: Add failing executive summary tests**

Append these tests to `DiagnosticReportFactoryConsultingReportTest`:

```java
@Test
@DisplayName("should generate executive summary from risk counts and diagnostics")
void should_generate_executive_summary_from_risk_counts_and_diagnostics() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-commodity")
            .totalFiles(2)
            .filesScanned(2)
            .sqlFound(2)
            .uniqueSqlFound(2)
            .sqlStatements(List.of(dynamicInjectionSql(), skippedExplainSql()))
            .build());

    assertThat(report.getExecutiveSummary().getRiskConclusion())
            .contains("CRITICAL")
            .contains("installment-commodity");
    assertThat(report.getExecutiveSummary().getTopDrivers())
            .anySatisfy(driver -> assertThat(driver).contains("SQL_INJECTION_RISK"));
    assertThat(report.getExecutiveSummary().getRecommendedActions())
            .anySatisfy(action -> assertThat(action).contains("P0"));
    assertThat(report.getExecutiveSummary().getConfidenceSummary())
            .contains("Manual review");
}

@Test
@DisplayName("should classify report confidence from evidence coverage")
void should_classify_report_confidence_from_evidence_coverage() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-commodity")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(skippedExplainSql()))
            .build());

    assertThat(report.getConfidence().getLevel()).isEqualTo("NEEDS_REVIEW");
    assertThat(report.getConfidence().getLimitations())
            .anySatisfy(limit -> assertThat(limit).contains("EXPLAIN"));
    assertThat(report.getMethodology().getKnownLimits())
            .anySatisfy(limit -> assertThat(limit).contains("Static analysis"));
}
```

Add helper methods in the same test class:

```java
private static SqlStatementDto dynamicInjectionSql() {
    return SqlStatementDto.builder()
            .id("dynamic-order")
            .sqlType(SqlType.SELECT)
            .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
            .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
            .validity(ValidityStatus.VALID)
            .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
            .severity(SeverityLevel.CRITICAL)
            .score(55)
            .staticAnalysis(org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto.builder()
                    .issues(List.of(org.spectrum.sqlchecker.application.analysis.dto.StaticIssue.builder()
                            .type(org.spectrum.sqlchecker.domain.shared.enumeration.IssueType.SQL_INJECTION_RISK)
                            .severity(SeverityLevel.CRITICAL)
                            .message("存在 ${} 动态拼接")
                            .suggestion("改为参数绑定或白名单映射")
                            .build()))
                    .build())
            .build();
}

private static SqlStatementDto skippedExplainSql() {
    return SqlStatementDto.builder()
            .id("skipped-query")
            .sqlType(SqlType.SELECT)
            .originalSql("SELECT * FROM orders")
            .abstractSql("SELECT * FROM orders")
            .validity(ValidityStatus.VALID)
            .explainEligibility(ExplainEligibility.SKIPPED)
            .severity(SeverityLevel.WARNING)
            .score(80)
            .build();
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: assertions fail because executive summary fields are still blank.

- [ ] **Step 3: Implement executive summary and confidence helpers**

In `DiagnosticReportFactory.from`, compute after `summary`:

```java
DiagnosticReport.Diagnostics diagnostics = DiagnosticReport.Diagnostics.builder()
        .parseFailures(parseFailures(sqlStatements))
        .skippedExplain(skippedExplain(sqlStatements))
        .manualReview(manualReview(sqlStatements))
        .configWarnings(explainFailures(sqlStatements))
        .build();
DiagnosticReport.Confidence confidence = buildConfidence(summary, diagnostics);
```

Replace the inline diagnostics builder with `.diagnostics(diagnostics)` and set:

```java
.executiveSummary(buildExecutiveSummary(result, summary, confidence, sqlStatements))
.confidence(confidence)
```

Add helpers:

```java
private static DiagnosticReport.ExecutiveSummary buildExecutiveSummary(
        ScanResult result,
        DiagnosticReport.Summary summary,
        DiagnosticReport.Confidence confidence,
        List<SqlStatementDto> sqlStatements) {
    List<String> topRules = countByRule(sqlStatements).entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(entry -> entry.getKey() + " × " + entry.getValue())
            .toList();
    List<String> actions = new ArrayList<>();
    if (hasRule(sqlStatements, IssueType.SQL_INJECTION_RISK.name())) {
        actions.add("P0: 先处理动态 SQL 安全止血，替换 ${} 或字符串拼接。");
    }
    if (hasRule(sqlStatements, IssueType.SELECT_WITHOUT_WHERE.name())
            || hasRule(sqlStatements, IssueType.ORDER_BY_WITHOUT_LIMIT.name())) {
        actions.add("P1: 收敛无边界读取和排序分页风险。");
    }
    if (actions.isEmpty()) {
        actions.add("P2: 保留当前报告作为治理基线，持续关注人工复核项。");
    }
    String projectName = resolveProjectName(result.getScanPath());
    String conclusion = projectName + " 当前 SQL 风险等级为 " + summary.getRiskLevel()
            + "，共发现 " + summary.getCounts().getTotalIssues() + " 个问题，建议按修复战役推进。";
    return DiagnosticReport.ExecutiveSummary.builder()
            .riskConclusion(conclusion)
            .topDrivers(topRules)
            .recommendedActions(actions)
            .confidenceSummary("Evidence confidence: " + confidence.getLevel()
                    + " · Manual review " + manualReview(sqlStatements).size()
                    + " · EXPLAIN skipped " + skippedExplain(sqlStatements).size())
            .build();
}

private static DiagnosticReport.Confidence buildConfidence(
        DiagnosticReport.Summary summary,
        DiagnosticReport.Diagnostics diagnostics) {
    List<String> sources = new ArrayList<>();
    sources.add("Static AST rules");
    if (summary.getCoverage().getExplainCoverage() > 0) {
        sources.add("EXPLAIN evidence");
    }
    List<String> limits = new ArrayList<>();
    if (!diagnostics.getSkippedExplain().isEmpty()) {
        limits.add("EXPLAIN evidence is incomplete because some SQL was skipped.");
    }
    if (!diagnostics.getManualReview().isEmpty()) {
        limits.add("Manual review is required for dynamic templates or evidence gaps.");
    }
    if (!diagnostics.getParseFailures().isEmpty()) {
        limits.add("Some SQL could not be parsed and should be fixed before relying on aggregate conclusions.");
    }
    String level = diagnostics.getParseFailures().isEmpty()
            && diagnostics.getManualReview().isEmpty()
            && diagnostics.getConfigWarnings().isEmpty()
            ? "STRONG"
            : "NEEDS_REVIEW";
    return DiagnosticReport.Confidence.builder()
            .level(level)
            .evidenceSources(sources)
            .limitations(limits)
            .build();
}

private static boolean hasRule(List<SqlStatementDto> sqlStatements, String rule) {
    for (SqlStatementDto sql : sqlStatements) {
        if (containsIssue(sql, rule)) {
            return true;
        }
    }
    return false;
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: all tests pass.

Commit:

```bash
git add src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java
git commit -m "Generate consulting executive summary"
```

---

### Task 3: Generate Remediation Campaigns

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java`

- [ ] **Step 1: Add failing campaign tests**

Add:

```java
@Test
@DisplayName("should group injection findings into P0 remediation campaign")
void should_group_injection_findings_into_p0_remediation_campaign() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-commodity")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(dynamicInjectionSql()))
            .build());

    assertThat(report.getCampaigns())
            .anySatisfy(campaign -> {
                assertThat(campaign.getId()).isEqualTo("p0-dynamic-sql-safety");
                assertThat(campaign.getPriority()).isEqualTo("P0");
                assertThat(campaign.getTheme()).isEqualTo("SAFETY");
                assertThat(campaign.getFindingIds()).contains("dynamic-order");
                assertThat(campaign.getAcceptanceChecklist())
                        .anySatisfy(item -> assertThat(item).contains("重新扫描"));
            });
}

@Test
@DisplayName("should group skipped explain findings into review campaign")
void should_group_skipped_explain_findings_into_review_campaign() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-commodity")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(skippedExplainSql()))
            .build());

    assertThat(report.getCampaigns())
            .anySatisfy(campaign -> {
                assertThat(campaign.getId()).isEqualTo("p2-evidence-completion");
                assertThat(campaign.getPriority()).isEqualTo("P2");
                assertThat(campaign.getEvidenceLevel()).isEqualTo("NEEDS_REVIEW");
            });
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: campaign assertions fail because campaigns are empty.

- [ ] **Step 3: Implement campaign generation**

In `DiagnosticReportFactory.from`, replace `.campaigns(List.of())` with:

```java
.campaigns(buildCampaigns(findings, sqlStatements))
```

Add helpers:

```java
private static List<DiagnosticReport.RemediationCampaign> buildCampaigns(
        List<DiagnosticReport.Finding> findings,
        List<SqlStatementDto> sqlStatements) {
    List<DiagnosticReport.RemediationCampaign> campaigns = new ArrayList<>();
    addCampaign(campaigns, campaignForRule(
            "p0-dynamic-sql-safety",
            "P0",
            "SAFETY",
            "动态 SQL 安全止血",
            "集中处理 ${}、字符串拼接和潜在注入风险，先消除可导致越权查询或 SQL 注入的入口。",
            "STRONG",
            findings,
            List.of("SQL_INJECTION_RISK", "DYNAMIC_SQL"),
            List.of("把 ${} 改为参数绑定；动态列名和排序字段使用白名单映射。"),
            List.of("重新扫描并确认该战役 finding 数为 0。", "为动态排序/筛选分支补充单元或集成测试。")));
    addCampaign(campaigns, campaignForRule(
            "p1-unbounded-query-containment",
            "P1",
            "PERFORMANCE",
            "无边界查询和排序风险收敛",
            "处理 SELECT 无 WHERE、ORDER BY 无 LIMIT、潜在全表扫描等会扩大数据库负载的查询。",
            "MEDIUM",
            findings,
            List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN"),
            List.of("补充可命中索引的过滤条件、分页边界，并用 EXPLAIN 验证访问路径。"),
            List.of("重新扫描确认 P1 高风险项下降。", "对关键查询执行 EXPLAIN 并记录访问类型、行数和索引命中。")));
    List<DiagnosticReport.Finding> reviewFindings = findings.stream()
            .filter(finding -> finding.getExplain() != null && !"SUPPORTED".equals(finding.getExplain().getEligibility()))
            .toList();
    if (!reviewFindings.isEmpty()) {
        campaigns.add(campaign(
                "p2-evidence-completion",
                "P2",
                "MAINTAINABILITY",
                "证据补齐与模板复核",
                "补齐 EXPLAIN 证据和人工复核动态模板，降低报告中的不可证明项。",
                "NEEDS_REVIEW",
                reviewFindings,
                List.of("为关键查询配置安全 EXPLAIN；对动态模板记录白名单和业务边界。"),
                List.of("重新扫描并确认 manual review 和 skipped EXPLAIN 数下降。")));
    }
    return campaigns;
}

private static DiagnosticReport.RemediationCampaign campaignForRule(
        String id,
        String priority,
        String theme,
        String title,
        String summary,
        String evidenceLevel,
        List<DiagnosticReport.Finding> findings,
        List<String> rules,
        List<String> recommendations,
        List<String> checklist) {
    List<DiagnosticReport.Finding> matched = findings.stream()
            .filter(finding -> finding.getIssues() != null && finding.getIssues().stream()
                    .anyMatch(issue -> rules.contains(issue.getType())))
            .toList();
    return matched.isEmpty() ? null : campaign(id, priority, theme, title, summary, evidenceLevel, matched, recommendations, checklist);
}

private static DiagnosticReport.RemediationCampaign campaign(
        String id,
        String priority,
        String theme,
        String title,
        String summary,
        String evidenceLevel,
        List<DiagnosticReport.Finding> findings,
        List<String> recommendations,
        List<String> checklist) {
    List<String> files = findings.stream()
            .flatMap(finding -> finding.getLocations().stream())
            .map(DiagnosticReport.Location::getFilePath)
            .filter(path -> path != null && !path.isBlank())
            .distinct()
            .limit(6)
            .toList();
    List<String> examples = findings.stream()
            .map(DiagnosticReport.Finding::getId)
            .limit(5)
            .toList();
    return DiagnosticReport.RemediationCampaign.builder()
            .id(id)
            .priority(priority)
            .theme(theme)
            .title(title)
            .summary(summary)
            .scope(DiagnosticReport.CampaignScope.builder()
                    .sqlCount(findings.size())
                    .fileCount(files.size())
                    .files(files)
                    .examples(examples)
                    .build())
            .evidenceLevel(evidenceLevel)
            .recommendations(recommendations)
            .acceptanceChecklist(checklist)
            .findingIds(findings.stream().map(DiagnosticReport.Finding::getId).toList())
            .build();
}

private static void addCampaign(List<DiagnosticReport.RemediationCampaign> campaigns,
                                DiagnosticReport.RemediationCampaign campaign) {
    if (campaign != null) {
        campaigns.add(campaign);
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest test
```

Expected: campaign tests pass.

Commit:

```bash
git add src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryConsultingReportTest.java
git commit -m "Group findings into remediation campaigns"
```

---

### Task 4: Serialize the v1.3 JSON Contract

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`

- [ ] **Step 1: Add failing JSON assertions**

In `ReportServiceImplTest.should_generate_structured_diagnostic_json`, add:

```java
assertThat(json).contains("\"executiveSummary\"");
assertThat(json).contains("\"campaigns\"");
assertThat(json).contains("\"confidence\"");
assertThat(json).contains("\"methodology\"");
assertThat(json).contains("\"acceptanceChecklist\"");
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_generate_structured_diagnostic_json test
```

Expected: test fails because the JSON serializer does not include new fields.

- [ ] **Step 3: Serialize new fields**

In `toJson`, add after diagnostics serialization:

```java
first = fieldObject(sb, first, "diagnostics", safeReport.getDiagnostics(), DiagnosticReportJsonSerializer::appendDiagnostics);
first = fieldObject(sb, first, "executiveSummary", safeReport.getExecutiveSummary(), DiagnosticReportJsonSerializer::appendExecutiveSummary);
first = fieldList(sb, first, "campaigns", safeReport.getCampaigns(), DiagnosticReportJsonSerializer::appendCampaign);
first = fieldObject(sb, first, "confidence", safeReport.getConfidence(), DiagnosticReportJsonSerializer::appendConfidence);
fieldObject(sb, first, "methodology", safeReport.getMethodology(), DiagnosticReportJsonSerializer::appendMethodology);
```

Then add appenders:

```java
private static void appendExecutiveSummary(StringBuilder sb, DiagnosticReport.ExecutiveSummary summary) {
    boolean first = beginObject(sb);
    first = fieldString(sb, first, "riskConclusion", summary.getRiskConclusion());
    first = fieldStringList(sb, first, "topDrivers", summary.getTopDrivers());
    first = fieldStringList(sb, first, "recommendedActions", summary.getRecommendedActions());
    fieldString(sb, first, "confidenceSummary", summary.getConfidenceSummary());
    endObject(sb);
}

private static void appendCampaign(StringBuilder sb, DiagnosticReport.RemediationCampaign campaign) {
    boolean first = beginObject(sb);
    first = fieldString(sb, first, "id", campaign.getId());
    first = fieldString(sb, first, "priority", campaign.getPriority());
    first = fieldString(sb, first, "theme", campaign.getTheme());
    first = fieldString(sb, first, "title", campaign.getTitle());
    first = fieldString(sb, first, "summary", campaign.getSummary());
    first = fieldObject(sb, first, "scope", campaign.getScope(), DiagnosticReportJsonSerializer::appendCampaignScope);
    first = fieldString(sb, first, "evidenceLevel", campaign.getEvidenceLevel());
    first = fieldStringList(sb, first, "recommendations", campaign.getRecommendations());
    first = fieldStringList(sb, first, "acceptanceChecklist", campaign.getAcceptanceChecklist());
    fieldStringList(sb, first, "findingIds", campaign.getFindingIds());
    endObject(sb);
}

private static void appendCampaignScope(StringBuilder sb, DiagnosticReport.CampaignScope scope) {
    boolean first = beginObject(sb);
    first = fieldNumber(sb, first, "sqlCount", scope.getSqlCount());
    first = fieldNumber(sb, first, "fileCount", scope.getFileCount());
    first = fieldStringList(sb, first, "files", scope.getFiles());
    fieldStringList(sb, first, "examples", scope.getExamples());
    endObject(sb);
}

private static void appendConfidence(StringBuilder sb, DiagnosticReport.Confidence confidence) {
    boolean first = beginObject(sb);
    first = fieldString(sb, first, "level", confidence.getLevel());
    first = fieldStringList(sb, first, "evidenceSources", confidence.getEvidenceSources());
    fieldStringList(sb, first, "limitations", confidence.getLimitations());
    endObject(sb);
}

private static void appendMethodology(StringBuilder sb, DiagnosticReport.Methodology methodology) {
    boolean first = beginObject(sb);
    first = fieldStringList(sb, first, "scoring", methodology.getScoring());
    first = fieldStringList(sb, first, "severityDefinitions", methodology.getSeverityDefinitions());
    first = fieldStringList(sb, first, "coverageDefinitions", methodology.getCoverageDefinitions());
    fieldStringList(sb, first, "knownLimits", methodology.getKnownLimits());
    endObject(sb);
}
```

- [ ] **Step 4: Run JSON tests and commit**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_generate_structured_diagnostic_json test
```

Expected: test passes and JSON includes all v1.3 contract fields.

Commit:

```bash
git add src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java
git commit -m "Serialize consulting report contract"
```

---

### Task 5: Redesign the HTML Report Top Half

**Files:**
- Modify: `src/main/resources/templates/diagnostic-report.pebble`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`

- [ ] **Step 1: Add failing HTML rendering test**

Add this test to `ReportServiceImplTest`:

```java
@Test
@DisplayName("html report should render consulting summary campaigns confidence and methodology")
void should_render_consulting_summary_campaigns_confidence_and_methodology(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
    org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
            new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
    Path output = tempDir.resolve("report.html");

    service.generateHtmlReport(createInsightScanResult(), output.toString());

    String html = Files.readString(output);
    assertThat(html).contains("Executive Conclusion");
    assertThat(html).contains("Remediation Campaigns");
    assertThat(html).contains("Evidence Confidence");
    assertThat(html).contains("Methodology");
    assertThat(html).contains("acceptance-checklist");
    assertThat(html).contains("href=\"#finding-");
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_render_consulting_summary_campaigns_confidence_and_methodology test
```

Expected: test fails because these labels/classes do not exist yet.

- [ ] **Step 3: Render executive summary and confidence**

In `diagnostic-report.pebble`, replace the hard-coded executive copy inside `section id="brief"` with fields from `report.executiveSummary`.

Use this structure:

```html
<p class="kicker">Executive Conclusion</p>
<h2>执行摘要</h2>
<p class="verdict-copy">{{ report.executiveSummary.riskConclusion }}</p>
<div class="driver-list">
    {% for driver in report.executiveSummary.topDrivers %}
        <span class="badge">{{ driver }}</span>
    {% else %}
        <span class="muted">未发现主要风险驱动项</span>
    {% endfor %}
</div>
<h3 class="next-steps-title">建议动作</h3>
<ol class="next-steps">
    {% for action in report.executiveSummary.recommendedActions %}
        <li>{{ action }}</li>
    {% endfor %}
</ol>
```

Add a confidence panel:

```html
<div class="executive-panel">
    <p class="kicker">Evidence Confidence</p>
    <h2>证据可信度</h2>
    <span class="badge">{{ report.confidence.level }}</span>
    <p class="section-subtitle">{{ report.executiveSummary.confidenceSummary }}</p>
    <div class="field"><span>证据来源</span>
        <div>{% for source in report.confidence.evidenceSources %}{{ source }}{% if not loop.last %} · {% endif %}{% endfor %}</div>
    </div>
    <div class="field"><span>限制</span>
        <ul>{% for limit in report.confidence.limitations %}<li>{{ limit }}</li>{% else %}<li>未发现明显证据缺口。</li>{% endfor %}</ul>
    </div>
</div>
```

- [ ] **Step 4: Render campaigns before SQL detail queue**

Add a new section before `section id="queue"`:

```html
<section id="campaigns" class="section">
    <div class="section-head">
        <div>
            <p class="kicker">Remediation Campaigns</p>
            <h2>修复战役</h2>
            <p class="section-subtitle">把散点 SQL 风险组织成可派单、可验收的治理动作。</p>
        </div>
    </div>
    <div class="campaign-list">
        {% for campaign in report.campaigns %}
            <article class="campaign-card" id="campaign-{{ campaign.id }}">
                <div class="priority-title">
                    <span class="badge">{{ campaign.priority }}</span>
                    <span class="badge">{{ campaign.theme }}</span>
                    <span class="badge">{{ campaign.evidenceLevel }}</span>
                </div>
                <h3>{{ campaign.title }}</h3>
                <p>{{ campaign.summary }}</p>
                <div class="field"><span>范围</span><div>{{ campaign.scope.sqlCount }} 条 SQL · {{ campaign.scope.fileCount }} 个文件</div></div>
                <div class="field"><span>建议</span><ul>{% for item in campaign.recommendations %}<li>{{ item }}</li>{% endfor %}</ul></div>
                <div class="field acceptance-checklist"><span>验收</span><ul>{% for item in campaign.acceptanceChecklist %}<li>{{ item }}</li>{% endfor %}</ul></div>
                <div class="field"><span>证据</span>
                    <div>
                        {% for findingId in campaign.findingIds %}
                            <a class="action-link" href="#finding-{{ findingId }}">{{ findingId }}</a>{% if not loop.last %} · {% endif %}
                        {% endfor %}
                    </div>
                </div>
            </article>
        {% else %}
            <div class="empty">未形成修复战役。建议保留本次报告作为基线。</div>
        {% endfor %}
    </div>
</section>
```

Add CSS:

```css
.campaign-list { display:grid; gap:16px; }
.campaign-card { border:1px solid var(--line); border-radius:8px; padding:18px; background:#fff; }
.driver-list { display:flex; flex-wrap:wrap; gap:8px; margin:14px 0; }
.acceptance-checklist ul { margin:6px 0 0 18px; }
```

- [ ] **Step 5: Render methodology appendix**

Add near the bottom before `</main>`:

```html
<section id="methodology" class="section">
    <div class="section-head">
        <div>
            <p class="kicker">Methodology</p>
            <h2>方法与口径</h2>
        </div>
    </div>
    <div class="grid columns">
        <div class="panel"><h3>评分</h3><ul>{% for item in report.methodology.scoring %}<li>{{ item }}</li>{% endfor %}</ul></div>
        <div class="panel"><h3>严重级别</h3><ul>{% for item in report.methodology.severityDefinitions %}<li>{{ item }}</li>{% endfor %}</ul></div>
        <div class="panel"><h3>覆盖率</h3><ul>{% for item in report.methodology.coverageDefinitions %}<li>{{ item }}</li>{% endfor %}</ul></div>
        <div class="panel"><h3>已知限制</h3><ul>{% for item in report.methodology.knownLimits %}<li>{{ item }}</li>{% endfor %}</ul></div>
    </div>
</section>
```

Update TOC to include `href="#campaigns"` and `href="#methodology"`.

- [ ] **Step 6: Run HTML test and commit**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_render_consulting_summary_campaigns_confidence_and_methodology test
```

Expected: test passes.

Commit:

```bash
git add src/main/resources/templates/diagnostic-report.pebble src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java
git commit -m "Render consulting report experience"
```

---

### Task 6: Verify End-to-End Report Contract

**Files:**
- Modify: `src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java`
- Modify: `src/test/resources/fixtures/mixed-repo/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: Add fixture E2E assertions**

In `ScanOrchestratorFixtureE2ETest`, add JSON assertions to the existing report-generation test or create a new test:

```java
assertThat(json).contains("\"executiveSummary\"");
assertThat(json).contains("\"campaigns\"");
assertThat(json).contains("\"confidence\"");
assertThat(json).contains("\"methodology\"");
```

The current fixture mapper only contains a safe parameterized query, so add this deterministic campaign sample:

```xml
<select id="findUsersOrdered" resultType="map">
    SELECT id, name FROM users ORDER BY ${orderBy}
</select>
```

Then assert:

```java
assertThat(json).contains("p0-dynamic-sql-safety");
```

- [ ] **Step 2: Run E2E test**

Run:

```bash
mvn -Dtest=ScanOrchestratorFixtureE2ETest test
```

Expected: fixture scan test passes and JSON contains the v1.3 sections.

- [ ] **Step 3: Run focused report test suite**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryConsultingReportTest,ReportServiceImplTest test
```

Expected: all focused report tests pass.

- [ ] **Step 4: Run full verification**

Run:

```bash
mvn test
mvn -q -DskipTests package
java -jar target/sql-checker-1.2.0.jar scan -p /Users/chenpengfei/installment-commodity -o target/installment-commodity-review/report.html --no-progress
```

Expected:

- `mvn test` passes.
- package command creates `target/sql-checker-1.2.0.jar`.
- scan creates `target/installment-commodity-review/report.html` and `target/installment-commodity-review/report.json`.
- JSON contains `executiveSummary`, `campaigns`, `confidence`, and `methodology`.
- HTML first viewport contains executive conclusion and remediation campaigns.

- [ ] **Step 5: Commit verification updates**

Commit:

```bash
git add src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java src/test/resources/fixtures/mixed-repo/src/main/resources/mapper/UserMapper.xml
git commit -m "Verify consulting report end to end"
```

---

### Task 7: Manual Product Review Before Release

**Files:**
- No required code changes unless review finds issues.

- [ ] **Step 1: Open generated report**

Open:

```text
/Users/chenpengfei/specturm-sql-cc/target/installment-commodity-review/report.html
```

Review the first viewport manually:

- It states project risk and top actions before raw metrics.
- It shows evidence confidence and limitations.
- It shows remediation campaigns before SQL-level findings.
- It does not overclaim when EXPLAIN is unavailable.

- [ ] **Step 2: Check JSON contract**

Run:

```bash
rg -n "\"executiveSummary\"|\"campaigns\"|\"confidence\"|\"methodology\"" target/installment-commodity-review/report.json
```

Expected: all four sections are present.

- [ ] **Step 3: Check static HTML interaction still initializes**

Run the existing Node DOM-mock script used in v1.2 verification, or repeat the equivalent static script check. Expected output should include:

```text
interactive-ready
```

- [ ] **Step 4: Final commit if review fixes were needed**

If Step 1-3 required fixes, commit them:

```bash
git add src/main/resources/templates/diagnostic-report.pebble src/main/java/org/spectrum/sqlchecker/infrastructure/report src/test/java/org/spectrum/sqlchecker/infrastructure/report
git commit -m "Polish consulting report release experience"
```

If no fixes were needed, do not create an empty commit.

---

## Self-Review Notes

- Spec coverage: executive summary is Task 2, remediation campaigns are Task 3, evidence confidence and methodology are Tasks 2 and 5, acceptance checklist is Tasks 3 and 5, UX/professional delivery is Task 5, E2E/report validation is Tasks 6 and 7.
- Scope control: the plan does not add history comparison, owner workflows, LLM generation, server-side report hosting, or new database dialect expansion.
- Type consistency: field names match the approved spec and are referenced consistently across DTO, factory, JSON serializer, and template.
- Test strategy: each behavior starts with a failing test, then implementation, then focused verification, then full Maven/package/real scan verification.
