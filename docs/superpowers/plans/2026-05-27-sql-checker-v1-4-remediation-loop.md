# SQL Checker v1.4 Remediation Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a remediation-loop report layer that turns SQL diagnostics into task-sized repair work with recipes, confidence labels, acceptance checks, JSON output, HTML presentation, and CLI task counts.

**Architecture:** Extend the existing `DiagnosticReport` contract with a `remediation` object rather than replacing v1.3 executive summary and campaigns. `DiagnosticReportFactory` remains the aggregation boundary, `DiagnosticReportJsonSerializer` remains the dependency-free JSON boundary, `diagnostic-report.pebble` renders the same model, and CLI summary reads counts from the report model.

**Tech Stack:** Java 17, Spring Boot 3.2, Lombok DTOs, Pebble templates, dependency-free JSON serializer, JUnit 5, AssertJ, Maven.

---

## File Structure

- Modify `src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java`
  - Add `private Remediation remediation;`.
  - Add nested DTOs: `Remediation`, `RemediationSummary`, `RemediationTask`, `RepairRecipe`.
- Modify `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java`
  - Generate remediation summary, tasks, and recipes from existing findings/campaigns/diagnostics.
  - Keep all remediation semantics out of Pebble.
- Modify `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java`
  - Serialize `remediation.summary`, `remediation.tasks`, and `remediation.recipes`.
- Modify `src/main/resources/templates/diagnostic-report.pebble`
  - Add governance brief metrics, repair recipe library, and task detail.
  - Keep clean SQL hidden from HTML.
- Modify `src/main/java/org/spectrum/sqlchecker/cli/command/ProgressDisplay.java`
  - Add remediation task counts to simple result output.
- Modify `src/main/java/org/spectrum/sqlchecker/cli/command/ScanCommand.java`
  - Pass remediation task counts to `ProgressDisplay`.
- Create `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java`
  - Focused unit tests for remediation model generation.
- Modify `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`
  - HTML and JSON rendering assertions.
- Modify `src/test/java/org/spectrum/sqlchecker/cli/command/ScanCommandTest.java`
  - CLI summary count assertions.
- Modify `src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java`
  - Fixture report JSON contains remediation sections.

---

### Task 1: Add the Remediation Report Contract

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java`
- Create: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java`

- [ ] **Step 1: Write the failing remediation contract test**

Create `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java`:

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

class DiagnosticReportFactoryRemediationTest {

    @Test
    @DisplayName("should expose remediation summary tasks and recipes")
    void should_expose_remediation_summary_tasks_and_recipes() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-remediation")
                .status(ScanStatus.COMPLETED)
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(SqlStatementDto.builder()
                        .id("sql-dynamic-order")
                        .sqlType(SqlType.SELECT)
                        .originalSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .normalizedSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .abstractSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .validity(ValidityStatus.VALID)
                        .explainEligibility(ExplainEligibility.SKIPPED)
                        .severity(SeverityLevel.CRITICAL)
                        .score(60)
                        .build()))
                .build());

        assertThat(report.getRemediation()).isNotNull();
        assertThat(report.getRemediation().getSummary()).isNotNull();
        assertThat(report.getRemediation().getTasks()).isNotNull();
        assertThat(report.getRemediation().getRecipes()).isNotNull();
    }
}
```

- [ ] **Step 2: Run the new test and verify it fails**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryRemediationTest test
```

Expected: compilation fails because `DiagnosticReport#getRemediation()` does not exist.

- [ ] **Step 3: Add remediation DTOs**

In `src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java`, add this field after `private Methodology methodology;`:

```java
private Remediation remediation;
```

Add these nested classes before `Diagnostics`:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class Remediation {
    private RemediationSummary summary;
    private List<RemediationCampaign> campaigns;
    private List<RemediationTask> tasks;
    private List<RepairRecipe> recipes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class RemediationSummary {
    private int campaignCount;
    private int taskCount;
    private int confirmedTaskCount;
    private int likelyTaskCount;
    private int reviewTaskCount;
    private String estimatedFirstPassFocus;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class RemediationTask {
    private String id;
    private String title;
    private String priority;
    private String severity;
    private String theme;
    private String confidence;
    private Location location;
    private String campaignId;
    private List<String> ruleTypes;
    private String impact;
    private String repairRecipeId;
    private String recommendation;
    private String acceptanceCheck;
    private String evidence;
    private SqlText sql;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class RepairRecipe {
    private String id;
    private String title;
    private List<String> appliesToRules;
    private String unsafePattern;
    private String safePattern;
    private List<String> steps;
    private String verification;
    private List<String> knownLimits;
}
```

- [ ] **Step 4: Add temporary empty remediation model in the factory**

In `DiagnosticReportFactory.from`, add a local empty model before the `return DiagnosticReport.builder()` call:

```java
DiagnosticReport.Remediation remediation = DiagnosticReport.Remediation.builder()
        .summary(DiagnosticReport.RemediationSummary.builder()
                .campaignCount(0)
                .taskCount(0)
                .confirmedTaskCount(0)
                .likelyTaskCount(0)
                .reviewTaskCount(0)
                .estimatedFirstPassFocus("暂无修复任务。")
                .build())
        .campaigns(List.of())
        .tasks(List.of())
        .recipes(List.of())
        .build();
```

Add `.remediation(remediation)` to the report builder after `.methodology(defaultMethodology())`:

```java
.methodology(defaultMethodology())
.remediation(remediation)
.build();
```

- [ ] **Step 5: Run the contract test and verify it passes**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryRemediationTest test
```

Expected: build success, 1 test passes.

- [ ] **Step 6: Commit the contract**

Run:

```bash
git add src/main/java/org/spectrum/sqlchecker/application/report/dto/DiagnosticReport.java src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java
git commit -m "Add remediation report contract"
```

---

### Task 2: Generate Repair Recipes and Remediation Tasks

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java`

- [ ] **Step 1: Add failing task generation tests**

Append these imports to `DiagnosticReportFactoryRemediationTest`:

```java
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
```

Append these tests to the class:

```java
@Test
@DisplayName("should create P0 remediation task for dynamic SQL injection risk")
void should_create_p0_remediation_task_for_dynamic_sql_injection_risk() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-trade")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(dynamicSql()))
            .build());

    assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
    assertThat(report.getRemediation().getSummary().getReviewTaskCount()).isEqualTo(1);
    assertThat(report.getRemediation().getTasks())
            .singleElement()
            .satisfies(task -> {
                assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:42 · SQL_INJECTION_RISK");
                assertThat(task.getPriority()).isEqualTo("P0");
                assertThat(task.getTheme()).isEqualTo("SAFETY");
                assertThat(task.getConfidence()).isEqualTo("NEEDS_REVIEW");
                assertThat(task.getRepairRecipeId()).isEqualTo("dynamic-value-binding");
                assertThat(task.getRecommendation()).contains("参数绑定").contains("白名单");
                assertThat(task.getAcceptanceCheck()).contains("P0").contains("重新扫描");
            });
}

@Test
@DisplayName("should create P1 remediation task for unbounded query")
void should_create_p1_remediation_task_for_unbounded_query() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-trade")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(unboundedSql()))
            .build());

    assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
    assertThat(report.getRemediation().getSummary().getLikelyTaskCount()).isEqualTo(1);
    assertThat(report.getRemediation().getTasks())
            .singleElement()
            .satisfies(task -> {
                assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:88 · SELECT_WITHOUT_WHERE");
                assertThat(task.getPriority()).isEqualTo("P1");
                assertThat(task.getTheme()).isEqualTo("PERFORMANCE");
                assertThat(task.getConfidence()).isEqualTo("LIKELY");
                assertThat(task.getRepairRecipeId()).isEqualTo("unbounded-query-containment");
            });
}

@Test
@DisplayName("should expose initial repair recipes")
void should_expose_initial_repair_recipes() {
    DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
            .scanPath("/repo/installment-trade")
            .totalFiles(1)
            .filesScanned(1)
            .sqlFound(1)
            .uniqueSqlFound(1)
            .sqlStatements(List.of(dynamicSql()))
            .build());

    assertThat(report.getRemediation().getRecipes())
            .extracting(DiagnosticReport.RepairRecipe::getId)
            .contains(
                    "dynamic-value-binding",
                    "dynamic-order-by-whitelist",
                    "unbounded-query-containment",
                    "select-star-field-list",
                    "dangerous-dml-guardrail",
                    "template-review-normalization");
}
```

Add these helpers to the same test class:

```java
private static SqlStatementDto dynamicSql() {
    return SqlStatementDto.builder()
            .id("sql-dynamic")
            .sqlType(SqlType.SELECT)
            .originalSql("SELECT id FROM orders ORDER BY ${orderBy}")
            .normalizedSql("SELECT id FROM orders ORDER BY ${orderBy}")
            .abstractSql("SELECT id FROM orders ORDER BY ${orderBy}")
            .validity(ValidityStatus.VALID)
            .explainEligibility(ExplainEligibility.SKIPPED)
            .severity(SeverityLevel.CRITICAL)
            .score(55)
            .locations(List.of(location("OrderMapper.xml", 42)))
            .staticAnalysis(StaticAnalysisDto.builder()
                    .issues(List.of(StaticIssue.builder()
                            .type(IssueType.SQL_INJECTION_RISK)
                            .severity(SeverityLevel.CRITICAL)
                            .message("动态 SQL 存在注入风险")
                            .suggestion("使用参数绑定或白名单")
                            .build()))
                    .build())
            .build();
}

private static SqlStatementDto unboundedSql() {
    return SqlStatementDto.builder()
            .id("sql-unbounded")
            .sqlType(SqlType.SELECT)
            .originalSql("SELECT id, status FROM orders")
            .normalizedSql("SELECT id, status FROM orders")
            .abstractSql("SELECT id, status FROM orders")
            .validity(ValidityStatus.VALID)
            .explainEligibility(ExplainEligibility.SKIPPED)
            .severity(SeverityLevel.WARNING)
            .score(80)
            .locations(List.of(location("OrderMapper.xml", 88)))
            .staticAnalysis(StaticAnalysisDto.builder()
                    .issues(List.of(StaticIssue.builder()
                            .type(IssueType.SELECT_WITHOUT_WHERE)
                            .severity(SeverityLevel.WARNING)
                            .message("SELECT 缺少 WHERE 条件")
                            .suggestion("补充业务过滤条件或分页限制")
                            .build()))
                    .build())
            .build();
}

private static SqlLocationDto location(String fileName, int line) {
    return SqlLocationDto.builder()
            .filePath("/repo/src/main/resources/mapper/" + fileName)
            .fileName(fileName)
            .startLine(line)
            .endLine(line)
            .sourceType(SqlSourceType.MYBATIS)
            .build();
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryRemediationTest test
```

Expected: assertions fail because task generation still returns empty lists.

- [ ] **Step 3: Replace the temporary empty remediation model with real generation**

In `DiagnosticReportFactory.from`, replace the temporary `DiagnosticReport.Remediation remediation = ...` block and the direct `.campaigns(buildCampaigns(findings))` builder call by creating campaigns once:

```java
List<DiagnosticReport.RemediationCampaign> campaigns = buildCampaigns(findings);
DiagnosticReport.Remediation remediation = buildRemediation(findings, campaigns);
```

Then update the builder to reuse the already-built campaign list:

```java
.campaigns(campaigns)
```

The relevant builder tail should become:

```java
.campaigns(campaigns)
.confidence(confidence)
.methodology(defaultMethodology())
.remediation(remediation)
.build();
```

- [ ] **Step 4: Add remediation helpers**

Add these helpers in `DiagnosticReportFactory` after `buildCampaigns`:

```java
private static DiagnosticReport.Remediation buildRemediation(
        List<DiagnosticReport.Finding> findings,
        List<DiagnosticReport.RemediationCampaign> campaigns) {
    List<DiagnosticReport.RemediationTask> tasks = findings.stream()
            .filter(finding -> finding.getIssues() != null && !finding.getIssues().isEmpty())
            .map(DiagnosticReportFactory::toRemediationTask)
            .toList();
    int confirmed = (int) tasks.stream().filter(task -> "CONFIRMED".equals(task.getConfidence())).count();
    int likely = (int) tasks.stream().filter(task -> "LIKELY".equals(task.getConfidence())).count();
    int review = (int) tasks.stream().filter(task -> "NEEDS_REVIEW".equals(task.getConfidence())).count();
    return DiagnosticReport.Remediation.builder()
            .summary(DiagnosticReport.RemediationSummary.builder()
                    .campaignCount(campaigns.size())
                    .taskCount(tasks.size())
                    .confirmedTaskCount(confirmed)
                    .likelyTaskCount(likely)
                    .reviewTaskCount(review)
                    .estimatedFirstPassFocus(firstPassFocus(tasks))
                    .build())
            .campaigns(campaigns)
            .tasks(tasks)
            .recipes(defaultRepairRecipes())
            .build();
}

private static DiagnosticReport.RemediationTask toRemediationTask(DiagnosticReport.Finding finding) {
    DiagnosticReport.Issue primaryIssue = finding.getIssues().get(0);
    DiagnosticReport.Location location = primaryLocation(finding);
    String ruleType = safe(primaryIssue.getType(), "UNKNOWN");
    String priority = priorityForRule(ruleType, finding.getSeverity());
    String theme = themeForRule(ruleType);
    String confidence = confidenceForTask(finding, ruleType);
    String recipeId = recipeForRule(ruleType);
    return DiagnosticReport.RemediationTask.builder()
            .id("task-" + finding.getId())
            .title(taskTitle(location, ruleType, finding.getSqlType()))
            .priority(priority)
            .severity(finding.getSeverity())
            .theme(theme)
            .confidence(confidence)
            .location(location)
            .campaignId(campaignIdForPriorityAndTheme(priority, theme, ruleType))
            .ruleTypes(finding.getIssues().stream().map(DiagnosticReport.Issue::getType).distinct().toList())
            .impact(impactForRule(ruleType))
            .repairRecipeId(recipeId)
            .recommendation(recommendationForRecipe(recipeId))
            .acceptanceCheck(acceptanceForTask(priority, ruleType))
            .evidence(primaryIssue.getMessage())
            .sql(finding.getSql())
            .build();
}

private static DiagnosticReport.Location primaryLocation(DiagnosticReport.Finding finding) {
    if (finding.getLocations() == null || finding.getLocations().isEmpty()) {
        return DiagnosticReport.Location.builder()
                .filePath("")
                .fileName("unknown")
                .startLine(0)
                .endLine(0)
                .sourceType("UNKNOWN")
                .build();
    }
    return finding.getLocations().get(0);
}

private static String taskTitle(DiagnosticReport.Location location, String ruleType, String sqlType) {
    String label = location.getFileName() != null && !location.getFileName().isBlank()
            ? location.getFileName() + ":" + location.getStartLine()
            : safe(sqlType, "SQL") + " SQL";
    return label + " · " + ruleType;
}

private static String priorityForRule(String ruleType, String severity) {
    if (List.of("SQL_INJECTION_RISK", "DYNAMIC_SQL", "DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE").contains(ruleType)) {
        return "P0";
    }
    if (List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN").contains(ruleType)) {
        return "P1";
    }
    if ("CRITICAL".equals(severity)) {
        return "P0";
    }
    return "P2";
}

private static String themeForRule(String ruleType) {
    if (List.of("SQL_INJECTION_RISK", "DYNAMIC_SQL", "DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE").contains(ruleType)) {
        return "SAFETY";
    }
    if (List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN").contains(ruleType)) {
        return "PERFORMANCE";
    }
    if (List.of("SQL_SYNTAX_ERROR", "UNKNOWN").contains(ruleType)) {
        return "MAINTAINABILITY";
    }
    return "CORRECTNESS";
}

private static String confidenceForTask(DiagnosticReport.Finding finding, String ruleType) {
    if (List.of("UNKNOWN", "SQL_SYNTAX_ERROR").contains(ruleType)) {
        return "NEEDS_REVIEW";
    }
    if (finding.getExplain() != null && finding.getExplain().isExecuted()) {
        return "CONFIRMED";
    }
    if ("P0".equals(priorityForRule(ruleType, finding.getSeverity()))) {
        return "NEEDS_REVIEW";
    }
    return "LIKELY";
}

private static String recipeForRule(String ruleType) {
    if ("SQL_INJECTION_RISK".equals(ruleType)) {
        return "dynamic-value-binding";
    }
    if ("DYNAMIC_SQL".equals(ruleType)) {
        return "dynamic-order-by-whitelist";
    }
    if (List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN").contains(ruleType)) {
        return "unbounded-query-containment";
    }
    if ("SELECT_STAR".equals(ruleType)) {
        return "select-star-field-list";
    }
    if (List.of("DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE").contains(ruleType)) {
        return "dangerous-dml-guardrail";
    }
    return "template-review-normalization";
}

private static String campaignIdForPriorityAndTheme(String priority, String theme, String ruleType) {
    if ("P0".equals(priority) && "SAFETY".equals(theme)) {
        return "p0-dynamic-sql-safety";
    }
    if ("P1".equals(priority) && "PERFORMANCE".equals(theme)) {
        return "p1-unbounded-query-containment";
    }
    return "p2-template-review";
}

private static String impactForRule(String ruleType) {
    return switch (ruleType) {
        case "SQL_INJECTION_RISK", "DYNAMIC_SQL" -> "动态 SQL 可能绕过参数绑定或白名单边界，优先消除安全入口。";
        case "SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT" -> "查询缺少业务边界，数据量增长后容易扩大数据库负载。";
        case "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN" -> "访问路径可能无法命中有效索引，需要结合表规模和 EXPLAIN 复核。";
        case "SELECT_STAR" -> "字段范围不明确，增加网络传输、反序列化和后续变更风险。";
        case "DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE" -> "变更语句缺少足够保护，存在大范围数据影响风险。";
        default -> "该 SQL 需要人工复核后归类为明确规则或修复项。";
    };
}

private static String recommendationForRecipe(String recipeId) {
    return switch (recipeId) {
        case "dynamic-value-binding" -> "把动态值改为参数绑定；动态字段名、排序字段必须使用白名单映射。";
        case "dynamic-order-by-whitelist" -> "将 ORDER BY 输入映射到枚举或固定字段表，禁止直接拼接请求参数。";
        case "unbounded-query-containment" -> "补充业务 WHERE 条件、分页边界或 LIMIT，并对关键查询补 EXPLAIN。";
        case "select-star-field-list" -> "改为调用方实际需要的字段清单，并检查 DTO 映射是否仍完整。";
        case "dangerous-dml-guardrail" -> "补充 WHERE 保护、业务状态限制或显式 allowlist，并增加回归测试。";
        default -> "补齐最小 SQL 样例，确认动态分支、方言和占位符后再派修。";
    };
}

private static String acceptanceForTask(String priority, String ruleType) {
    return priority + " 任务完成后重新扫描，确认 " + ruleType + " 对应任务数下降；如果修改 SQL 语义，补充查询路径测试。";
}

private static String firstPassFocus(List<DiagnosticReport.RemediationTask> tasks) {
    long p0 = tasks.stream().filter(task -> "P0".equals(task.getPriority())).count();
    if (p0 > 0) {
        return "第一轮先处理 " + p0 + " 个 P0 安全或破坏性风险任务。";
    }
    long p1 = tasks.stream().filter(task -> "P1".equals(task.getPriority())).count();
    if (p1 > 0) {
        return "第一轮先处理 " + p1 + " 个 P1 性能边界任务。";
    }
    if (!tasks.isEmpty()) {
        return "第一轮先复核 " + tasks.size() + " 个 P2 模板或维护性任务。";
    }
    return "暂无修复任务。";
}
```

- [ ] **Step 5: Add default repair recipes**

Add this helper after `firstPassFocus`:

```java
private static List<DiagnosticReport.RepairRecipe> defaultRepairRecipes() {
    return List.of(
            recipe(
                    "dynamic-value-binding",
                    "动态值参数绑定",
                    List.of("SQL_INJECTION_RISK"),
                    "${value} 或字符串拼接直接进入 SQL",
                    "#{value} 参数绑定；字段名和排序字段使用白名单",
                    List.of("定位动态值来源。", "把普通值改为绑定参数。", "无法绑定的字段名改为白名单映射。"),
                    "重新扫描确认 SQL_INJECTION_RISK 任务数下降。",
                    List.of("表名、字段名和排序方向不能直接用参数绑定，需要白名单。")),
            recipe(
                    "dynamic-order-by-whitelist",
                    "动态排序白名单",
                    List.of("DYNAMIC_SQL"),
                    "ORDER BY ${orderBy}",
                    "ORDER BY <server-side whitelist column>",
                    List.of("定义允许排序字段枚举。", "将外部输入映射到枚举。", "拒绝未登记字段。"),
                    "补充排序字段映射测试，并重新扫描确认动态 SQL 风险下降。",
                    List.of("业务确实需要动态排序时，报告仍可能要求人工复核白名单完整性。")),
            recipe(
                    "unbounded-query-containment",
                    "无边界查询收敛",
                    List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN"),
                    "SELECT 查询缺少 WHERE、LIMIT 或有效访问路径",
                    "补充业务过滤条件、分页边界和可验证的索引访问路径",
                    List.of("确认调用场景的数据范围。", "补充 WHERE 或 LIMIT。", "关键查询补 EXPLAIN 验证访问路径。"),
                    "重新扫描确认 P1 任务下降；启用 EXPLAIN 时确认访问类型或行数改善。",
                    List.of("静态扫描无法知道真实表规模，必须结合业务数据量判断优先级。")),
            recipe(
                    "select-star-field-list",
                    "SELECT 星号字段清单化",
                    List.of("SELECT_STAR"),
                    "SELECT *",
                    "SELECT id, name, status",
                    List.of("确认调用方实际读取字段。", "替换为显式字段清单。", "检查 DTO/ResultMap 映射。"),
                    "重新扫描确认 SELECT_STAR 任务下降，并运行相关 mapper 测试。",
                    List.of("字段清单应跟随调用方需求，不要机械复制全字段。")),
            recipe(
                    "dangerous-dml-guardrail",
                    "危险 DML 防护",
                    List.of("DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE"),
                    "UPDATE/DELETE/DROP/TRUNCATE 缺少保护边界",
                    "增加 WHERE、状态约束、allowlist 或显式运维确认",
                    List.of("确认语句是否允许批量影响。", "补充业务边界。", "增加回归测试覆盖误删误改路径。"),
                    "重新扫描确认 P0 DML 任务下降，并保留变更审查记录。",
                    List.of("DDL/DML 风险需要业务上下文，工具只给出风险入口。")),
            recipe(
                    "template-review-normalization",
                    "模板复核与归一化",
                    List.of("UNKNOWN", "SQL_SYNTAX_ERROR"),
                    "动态模板、方言或占位符导致无法明确派修",
                    "补齐最小可解析样例或把模板归类到明确规则",
                    List.of("定位模板分支。", "提取最小 SQL 样例。", "确认是误报、方言缺口还是真实问题。"),
                    "重新扫描确认 UNKNOWN/SQL_SYNTAX_ERROR 下降或转为明确规则。",
                    List.of("该类任务默认需要人工复核。")));
}

private static DiagnosticReport.RepairRecipe recipe(
        String id,
        String title,
        List<String> appliesToRules,
        String unsafePattern,
        String safePattern,
        List<String> steps,
        String verification,
        List<String> knownLimits) {
    return DiagnosticReport.RepairRecipe.builder()
            .id(id)
            .title(title)
            .appliesToRules(appliesToRules)
            .unsafePattern(unsafePattern)
            .safePattern(safePattern)
            .steps(steps)
            .verification(verification)
            .knownLimits(knownLimits)
            .build();
}
```

- [ ] **Step 6: Run the remediation tests and verify they pass**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryRemediationTest test
```

Expected: all remediation factory tests pass.

- [ ] **Step 7: Commit task generation**

Run:

```bash
git add src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactory.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportFactoryRemediationTest.java
git commit -m "Generate remediation tasks and recipes"
```

---

### Task 3: Serialize Remediation JSON

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`

- [ ] **Step 1: Add failing JSON assertions**

In `ReportServiceImplTest`, find `should_generate_json_report_with_stable_contract` and append these assertions after the existing `"methodology"` assertion:

```java
assertThat(json).contains("\"remediation\"");
assertThat(json).contains("\"summary\"");
assertThat(json).contains("\"taskCount\"");
assertThat(json).contains("\"tasks\"");
assertThat(json).contains("\"recipes\"");
assertThat(json).contains("\"repairRecipeId\"");
```

- [ ] **Step 2: Run the JSON test and verify it fails**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_generate_json_report_with_stable_contract test
```

Expected: test fails because `remediation` is not serialized.

- [ ] **Step 3: Wire remediation into top-level serializer**

In `DiagnosticReportJsonSerializer.toJson`, replace the final methodology line:

```java
fieldObject(sb, first, "methodology", safeReport.getMethodology(), DiagnosticReportJsonSerializer::appendMethodology);
```

with:

```java
first = fieldObject(sb, first, "methodology", safeReport.getMethodology(), DiagnosticReportJsonSerializer::appendMethodology);
fieldObject(sb, first, "remediation", safeReport.getRemediation(), DiagnosticReportJsonSerializer::appendRemediation);
```

- [ ] **Step 4: Add remediation serializers**

Add these methods after `appendMethodology`:

```java
private static void appendRemediation(StringBuilder sb, DiagnosticReport.Remediation remediation) {
    boolean first = beginObject(sb);
    first = fieldObject(sb, first, "summary", remediation.getSummary(), DiagnosticReportJsonSerializer::appendRemediationSummary);
    first = fieldList(sb, first, "campaigns", remediation.getCampaigns(), DiagnosticReportJsonSerializer::appendCampaign);
    first = fieldList(sb, first, "tasks", remediation.getTasks(), DiagnosticReportJsonSerializer::appendRemediationTask);
    fieldList(sb, first, "recipes", remediation.getRecipes(), DiagnosticReportJsonSerializer::appendRepairRecipe);
    endObject(sb);
}

private static void appendRemediationSummary(StringBuilder sb, DiagnosticReport.RemediationSummary summary) {
    boolean first = beginObject(sb);
    first = fieldNumber(sb, first, "campaignCount", summary.getCampaignCount());
    first = fieldNumber(sb, first, "taskCount", summary.getTaskCount());
    first = fieldNumber(sb, first, "confirmedTaskCount", summary.getConfirmedTaskCount());
    first = fieldNumber(sb, first, "likelyTaskCount", summary.getLikelyTaskCount());
    first = fieldNumber(sb, first, "reviewTaskCount", summary.getReviewTaskCount());
    fieldString(sb, first, "estimatedFirstPassFocus", summary.getEstimatedFirstPassFocus());
    endObject(sb);
}

private static void appendRemediationTask(StringBuilder sb, DiagnosticReport.RemediationTask task) {
    boolean first = beginObject(sb);
    first = fieldString(sb, first, "id", task.getId());
    first = fieldString(sb, first, "title", task.getTitle());
    first = fieldString(sb, first, "priority", task.getPriority());
    first = fieldString(sb, first, "severity", task.getSeverity());
    first = fieldString(sb, first, "theme", task.getTheme());
    first = fieldString(sb, first, "confidence", task.getConfidence());
    first = fieldObject(sb, first, "location", task.getLocation(), DiagnosticReportJsonSerializer::appendLocation);
    first = fieldString(sb, first, "campaignId", task.getCampaignId());
    first = fieldStringList(sb, first, "ruleTypes", task.getRuleTypes());
    first = fieldString(sb, first, "impact", task.getImpact());
    first = fieldString(sb, first, "repairRecipeId", task.getRepairRecipeId());
    first = fieldString(sb, first, "recommendation", task.getRecommendation());
    first = fieldString(sb, first, "acceptanceCheck", task.getAcceptanceCheck());
    first = fieldString(sb, first, "evidence", task.getEvidence());
    fieldObject(sb, first, "sql", task.getSql(), DiagnosticReportJsonSerializer::appendSqlText);
    endObject(sb);
}

private static void appendRepairRecipe(StringBuilder sb, DiagnosticReport.RepairRecipe recipe) {
    boolean first = beginObject(sb);
    first = fieldString(sb, first, "id", recipe.getId());
    first = fieldString(sb, first, "title", recipe.getTitle());
    first = fieldStringList(sb, first, "appliesToRules", recipe.getAppliesToRules());
    first = fieldString(sb, first, "unsafePattern", recipe.getUnsafePattern());
    first = fieldString(sb, first, "safePattern", recipe.getSafePattern());
    first = fieldStringList(sb, first, "steps", recipe.getSteps());
    first = fieldString(sb, first, "verification", recipe.getVerification());
    fieldStringList(sb, first, "knownLimits", recipe.getKnownLimits());
    endObject(sb);
}
```

- [ ] **Step 5: Run JSON/report tests**

Run:

```bash
mvn -Dtest=ReportServiceImplTest test
```

Expected: all report service tests pass.

- [ ] **Step 6: Commit JSON serialization**

Run:

```bash
git add src/main/java/org/spectrum/sqlchecker/infrastructure/report/DiagnosticReportJsonSerializer.java src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java
git commit -m "Serialize remediation report data"
```

---

### Task 4: Render Remediation Workflow in HTML

**Files:**
- Modify: `src/main/resources/templates/diagnostic-report.pebble`
- Modify: `src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java`

- [ ] **Step 1: Add failing HTML assertions**

In `ReportServiceImplTest`, find `should_render_consulting_summary_campaigns_confidence_and_methodology` and append:

```java
assertThat(html).contains("治理简报");
assertThat(html).contains("修复任务");
assertThat(html).contains("Repair Recipes");
assertThat(html).contains("修复配方");
assertThat(html).contains("Task Detail");
assertThat(html).contains("复制任务摘要");
assertThat(html).contains("复制验收条件");
```

- [ ] **Step 2: Run the HTML test and verify it fails**

Run:

```bash
mvn -Dtest=ReportServiceImplTest#should_render_consulting_summary_campaigns_confidence_and_methodology test
```

Expected: test fails because the new remediation sections are not rendered.

- [ ] **Step 3: Add sidebar anchors**

In `diagnostic-report.pebble`, find the sidebar links around `#campaigns`. Replace the links between `#brief` and `#queue` with:

```html
<a href="#brief">治理简报</a>
<a href="#campaigns">修复战役</a>
<a href="#recipes">修复配方</a>
<a href="#tasks">修复任务</a>
<a href="#queue">优先队列</a>
```

- [ ] **Step 4: Add remediation summary block after overview metrics**

Insert this section after the `#overview` metrics section:

```html
<section id="governance" class="section">
    <div class="section-head">
        <div>
            <p class="kicker">Governance Brief</p>
            <h2>治理简报</h2>
            <p class="section-subtitle">{{ report.remediation.summary.estimatedFirstPassFocus }}</p>
        </div>
    </div>
    <div class="grid metrics">
        <div class="metric"><div class="value">{{ report.remediation.summary.taskCount }}</div><div class="label">修复任务</div></div>
        <div class="metric"><div class="value">{{ report.remediation.summary.campaignCount }}</div><div class="label">修复战役</div></div>
        <div class="metric"><div class="value">{{ report.remediation.summary.confirmedTaskCount }}</div><div class="label">确定问题</div></div>
        <div class="metric"><div class="value">{{ report.remediation.summary.likelyTaskCount }}</div><div class="label">高可信推断</div></div>
        <div class="metric"><div class="value">{{ report.remediation.summary.reviewTaskCount }}</div><div class="label">需人工复核</div></div>
    </div>
</section>
```

- [ ] **Step 5: Add repair recipe library before findings detail**

Insert this section before the existing `<section id="details" class="section">`:

```html
<section id="recipes" class="section">
    <div class="section-head">
        <div>
            <p class="kicker">Repair Recipes</p>
            <h2>修复配方</h2>
            <p class="section-subtitle">把重复出现的问题转成稳定修复模式，减少每条 SQL 从零判断。</p>
        </div>
    </div>
    <div class="grid columns">
        {% for recipe in report.remediation.recipes %}
            <article class="panel">
                <h3>{{ recipe.title }}</h3>
                <div class="field"><span>适用规则</span><div>{{ recipe.appliesToRules|join(', ') }}</div></div>
                <div class="field"><span>风险模式</span><div>{{ recipe.unsafePattern }}</div></div>
                <div class="field"><span>安全模式</span><div>{{ recipe.safePattern }}</div></div>
                <div class="field"><span>步骤</span><ul>{% for step in recipe.steps %}<li>{{ step }}</li>{% endfor %}</ul></div>
                <div class="field"><span>验收</span><div>{{ recipe.verification }}</div></div>
            </article>
        {% endfor %}
    </div>
</section>
```

- [ ] **Step 6: Add remediation task detail section before SQL finding detail**

Insert this section before `<section id="details" class="section">`:

```html
<section id="tasks" class="section">
    <div class="section-head">
        <div>
            <p class="kicker">Task Detail</p>
            <h2>修复任务</h2>
            <p class="section-subtitle">任务面向派单和验收；完整 SQL 诊断仍保留在问题明细和 JSON。</p>
        </div>
    </div>
    {% if report.remediation.tasks is not empty %}
        {% for task in report.remediation.tasks %}
            <article id="{{ task.id }}" class="finding" data-severity="{{ task.severity }}" data-rules="{{ task.ruleTypes|join(' ') }}">
                <div class="finding-head">
                    <div>
                        <span class="badge">{{ task.priority }}</span>
                        <span class="badge sev-{{ task.severity }}">{{ task.severity }}</span>
                        <span class="badge">{{ task.theme }}</span>
                        <span class="badge">{{ task.confidence }}</span>
                        <h3>{{ task.title }}</h3>
                        <div class="locations">{{ task.location.filePath }}:{{ task.location.startLine }} · {{ task.location.sourceType }}</div>
                    </div>
                    <div class="mini-score"><span>Recipe</span><strong>{{ task.repairRecipeId }}</strong></div>
                </div>
                <div class="issue">
                    <div class="field"><span>影响</span><div>{{ task.impact }}</div></div>
                    <div class="field"><span>建议</span><div>{{ task.recommendation }}</div></div>
                    <div class="field"><span>验收</span><div>{{ task.acceptanceCheck }}</div></div>
                    <div class="field"><span>证据</span><div>{{ task.evidence }}</div></div>
                    <div class="sql-tools">
                        <button class="copy-task-summary" type="button" data-task-summary="{{ task.title }} | {{ task.priority }} | {{ task.recommendation }}">复制任务摘要</button>
                        <button class="copy-acceptance" type="button" data-acceptance="{{ task.acceptanceCheck }}">复制验收条件</button>
                    </div>
                </div>
            </article>
        {% endfor %}
    {% else %}
        <div class="empty">未生成修复任务。完整扫描结果仍保留在 JSON。</div>
    {% endif %}
</section>
```

- [ ] **Step 7: Add copy handlers for task summary and acceptance**

In the existing script inside `onReady(function () { ... })`, after the SQL copy button loop, add:

```javascript
document.querySelectorAll('.copy-task-summary').forEach(function (button) {
    button.addEventListener('click', function () {
        copyText(button.getAttribute('data-task-summary') || '', button);
    });
});
document.querySelectorAll('.copy-acceptance').forEach(function (button) {
    button.addEventListener('click', function () {
        copyText(button.getAttribute('data-acceptance') || '', button);
    });
});
```

Use the existing `copyText` helper if present. If only SQL copy has inline clipboard code, extract that code into:

```javascript
function copyText(text, button) {
    if (!navigator.clipboard) {
        return;
    }
    navigator.clipboard.writeText(text).then(function () {
        var original = button.textContent;
        button.textContent = '已复制';
        setTimeout(function () {
            button.textContent = original;
        }, 1200);
    });
}
```

- [ ] **Step 8: Run report rendering tests**

Run:

```bash
mvn -Dtest=ReportServiceImplTest test
```

Expected: all report service tests pass.

- [ ] **Step 9: Commit HTML remediation workflow**

Run:

```bash
git add src/main/resources/templates/diagnostic-report.pebble src/test/java/org/spectrum/sqlchecker/infrastructure/report/ReportServiceImplTest.java
git commit -m "Render remediation workflow in reports"
```

---

### Task 5: Add Remediation Counts to CLI Output

**Files:**
- Modify: `src/main/java/org/spectrum/sqlchecker/cli/command/ProgressDisplay.java`
- Modify: `src/main/java/org/spectrum/sqlchecker/cli/command/ScanCommand.java`
- Modify: `src/test/java/org/spectrum/sqlchecker/cli/command/ScanCommandTest.java`

- [ ] **Step 1: Add failing CLI verification**

In `ScanCommandTest`, update both `verify(progressDisplay).showSimpleResult(...)` calls to include remediation task counts after skipped explain:

```java
verify(progressDisplay).showSimpleResult(eq(1), eq(1), eq(0), eq(0), eq(1), eq(1), eq(100.0), eq(0),
        eq(0), eq(1), eq(0), eq(0), eq(0), eq(0), eq(0), eq(0), eq(10L), anyString(), anyString());
```

The new count order is:

```text
manualReview, skippedExplain, remediationTasks, p0Tasks, p1Tasks, p2Tasks, critical, warning, info
```

- [ ] **Step 2: Run CLI test and verify it fails**

Run:

```bash
mvn -Dtest=ScanCommandTest test
```

Expected: compilation fails because the new `showSimpleResult` signature does not exist.

- [ ] **Step 3: Add CLI display overload**

In `ProgressDisplay`, change the longest `showSimpleResult` signature from:

```java
public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int uniqueSql,
                              double parseRate, int parseFailures, int manualReview, int skippedExplain,
                              int critical, int warning, int info, long duration, String reportPath, String jsonReportPath) {
```

to:

```java
public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int uniqueSql,
                              double parseRate, int parseFailures, int manualReview, int skippedExplain,
                              int remediationTasks, int p0Tasks, int p1Tasks, int p2Tasks,
                              int critical, int warning, int info, long duration, String reportPath, String jsonReportPath) {
```

Inside the method, after `EXPLAIN skipped`, print:

```java
out.println("  Remediation tasks: " + remediationTasks + " (P0 " + p0Tasks + " / P1 " + p1Tasks + " / P2 " + p2Tasks + ")");
```

Update the shorter overload that currently calls the longest method to pass zeros:

```java
showSimpleResult(totalFiles, javaFiles, xmlFiles, sqlFiles, sqlFound, uniqueSql, parseRate, parseFailures,
        0, 0, 0, 0, 0, 0, critical, warning, info, duration, reportPath, jsonReportPath);
```

- [ ] **Step 4: Pass task counts from `ScanCommand`**

In `ScanCommand.showResult`, compute counts after `skippedExplain`:

```java
DiagnosticReport.Remediation remediation = report.getRemediation();
int remediationTasks = remediation != null && remediation.getSummary() != null
        ? remediation.getSummary().getTaskCount()
        : 0;
int p0Tasks = countTasksByPriority(remediation, "P0");
int p1Tasks = countTasksByPriority(remediation, "P1");
int p2Tasks = countTasksByPriority(remediation, "P2");
```

Add this helper in `ScanCommand`:

```java
private int countTasksByPriority(DiagnosticReport.Remediation remediation, String priority) {
    if (remediation == null || remediation.getTasks() == null) {
        return 0;
    }
    return (int) remediation.getTasks().stream()
            .filter(task -> priority.equals(task.getPriority()))
            .count();
}
```

Pass the new values to `progressDisplay.showSimpleResult` between `skippedExplain` and issue counts:

```java
manualReview,
skippedExplain,
remediationTasks,
p0Tasks,
p1Tasks,
p2Tasks,
counts.getCriticalIssues(),
counts.getWarningIssues(),
counts.getInfoIssues(),
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
mvn -Dtest=ScanCommandTest test
```

Expected: all 7 ScanCommand tests pass.

- [ ] **Step 6: Commit CLI summary**

Run:

```bash
git add src/main/java/org/spectrum/sqlchecker/cli/command/ProgressDisplay.java src/main/java/org/spectrum/sqlchecker/cli/command/ScanCommand.java src/test/java/org/spectrum/sqlchecker/cli/command/ScanCommandTest.java
git commit -m "Summarize remediation tasks in CLI output"
```

---

### Task 6: Add End-to-End Acceptance and Regenerate Reports

**Files:**
- Modify: `src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java`
- Optionally modify: `src/test/resources/fixtures/mixed-repo/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: Add fixture JSON remediation assertions**

In `ScanOrchestratorFixtureE2ETest`, locate the test that generates report JSON. Add these assertions after reading the JSON file:

```java
assertThat(json).contains("\"remediation\"");
assertThat(json).contains("\"tasks\"");
assertThat(json).contains("\"recipes\"");
assertThat(json).contains("\"taskCount\"");
```

If the existing fixture has no actionable SQL and task count remains zero, keep these structural assertions and add a separate factory unit test for non-zero tasks. Do not force noisy fixture data unless the current fixture already has dynamic SQL or unbounded query examples.

- [ ] **Step 2: Run fixture E2E test**

Run:

```bash
mvn -Dtest=ScanOrchestratorFixtureE2ETest test
```

Expected: fixture E2E tests pass and report JSON contains remediation sections.

- [ ] **Step 3: Run focused report and CLI tests**

Run:

```bash
mvn -Dtest=DiagnosticReportFactoryRemediationTest,DiagnosticReportFactoryConsultingReportTest,ReportServiceImplTest,ScanCommandTest test
```

Expected: all focused report and CLI tests pass.

- [ ] **Step 4: Run full regression**

Run:

```bash
mvn test
```

Expected: all tests pass.

- [ ] **Step 5: Package the jar**

Run:

```bash
mvn -q -DskipTests package
```

Expected: package command exits 0 and `target/sql-checker-1.2.0.jar` exists.

- [ ] **Step 6: Regenerate installment report suite sequentially**

Run:

```bash
for d in /Users/chenpengfei/installment-coupon /Users/chenpengfei/installment-spike /Users/chenpengfei/installment-fws /Users/chenpengfei/installment-campaign /Users/chenpengfei/installment-thirdparty /Users/chenpengfei/installment-trade /Users/chenpengfei/installment-commodity /Users/chenpengfei/installment-common /Users/chenpengfei/installment-search; do
  name=$(basename "$d")
  java -jar target/sql-checker-1.2.0.jar scan -p "$d" -o "target/installment-review/$name/report.html" --no-progress
done
```

Expected: each module emits `report.html` and `report.json`. Run sequentially to avoid H2 lock contention.

- [ ] **Step 7: Verify HTML task counts match JSON task counts**

Run:

```bash
node -e "const fs=require('fs'); const path=require('path'); const root='target/installment-review'; const rows=[]; for (const name of fs.readdirSync(root).sort()) { const jsonPath=path.join(root,name,'report.json'); const htmlPath=path.join(root,name,'report.html'); if (!fs.existsSync(jsonPath)) continue; const r=JSON.parse(fs.readFileSync(jsonPath,'utf8')); const html=fs.readFileSync(htmlPath,'utf8'); const taskCount=r.remediation.summary.taskCount; const taskCards=(html.match(/id=\"task-/g)||[]).length; rows.push({module:name, taskCount, taskCards}); } console.table(rows); const bad=rows.filter(r=>r.taskCount!==r.taskCards); if (bad.length) { console.error('task card mismatch', bad); process.exit(1); }"
```

Expected: table prints every module and exits 0.

- [ ] **Step 8: Browser QA on installment-trade**

Open or reload:

```text
http://127.0.0.1:51477/installment-trade/report.html
```

Verify:

- `治理简报` is visible.
- `修复配方` is visible.
- `修复任务` count matches JSON `remediation.summary.taskCount`.
- `复制任务摘要` and `复制验收条件` buttons exist.
- Long SQL hash ids are not visible as primary labels.
- Existing filtering still works for problem SQL.

- [ ] **Step 9: Commit acceptance coverage**

Run:

```bash
git add src/test/java/org/spectrum/sqlchecker/application/scan/impl/ScanOrchestratorFixtureE2ETest.java
git commit -m "Verify remediation report end to end"
```

---

## Final Verification

After all tasks are complete, run:

```bash
git status --short --branch
mvn test
mvn -q -DskipTests package
java -jar target/sql-checker-1.2.0.jar scan -p /Users/chenpengfei/installment-trade -o target/installment-review/installment-trade/report.html --no-progress
node -e "const fs=require('fs'); const r=JSON.parse(fs.readFileSync('target/installment-review/installment-trade/report.json','utf8')); console.log(JSON.stringify({taskCount:r.remediation.summary.taskCount, recipes:r.remediation.recipes.length, campaigns:r.remediation.summary.campaignCount}, null, 2)); if (!r.remediation || !Array.isArray(r.remediation.tasks) || !Array.isArray(r.remediation.recipes)) process.exit(1);"
```

Expected:

- Working tree contains only intended changes before final commit.
- `mvn test` passes.
- package succeeds.
- installment-trade report and JSON regenerate.
- JSON prints remediation task and recipe counts.
