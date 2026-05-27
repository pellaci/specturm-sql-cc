package org.spectrum.sqlchecker.application.scan.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanOrchestrator;
import org.spectrum.sqlchecker.cli.SqlCheckerApplication;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SqlCheckerApplication.class, properties = "sqlchecker.cli.enabled=false")
@DisplayName("ScanOrchestrator fixture E2E 测试")
class ScanOrchestratorFixtureE2ETest {

    @Autowired
    private ScanOrchestrator scanOrchestrator;

    @Autowired
    private ReportService reportService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("应该扫描真实 fixture，并覆盖 Java、MyBatis、TypeScript")
    void should_scan_fixture_repository_end_to_end() throws IOException {
        Path repoRoot = tempDir.resolve("mixed-repo");
        copyRecursively(Path.of("src/test/resources/fixtures/mixed-repo"), repoRoot);

        ScanExecutionResult result = scanOrchestrator.execute(
                ScanExecutionRequest.builder()
                        .path(repoRoot.toString())
                        .build(),
                null
        );

        assertThat(result.getStatistics().getTotalFiles()).isEqualTo(3);
        assertThat(result.getStatistics().getSqlFound()).isEqualTo(4);
        assertThat(result.getStatistics().getSqlParsed()).isEqualTo(4);
        assertThat(result.getSqlEntries()).hasSize(4);

        Set<SqlSourceType> sourceTypes = result.getSqlEntries().stream()
                .flatMap(entry -> entry.getLocations().stream())
                .map(location -> location.getSourceType())
                .collect(Collectors.toSet());

        assertThat(sourceTypes)
                .contains(SqlSourceType.STRING_LITERAL, SqlSourceType.MYBATIS, SqlSourceType.JAVASCRIPT);

        assertThat(result.getIssueSummary()).containsEntry("SELECT_STAR", 1);

        Path reportPath = tempDir.resolve("mixed-repo-report.json");
        reportService.generateJsonReport(result.getScanResult(), reportPath.toString());

        String json = Files.readString(reportPath);
        assertThat(json).contains("\"executiveSummary\"");
        assertThat(json).contains("\"campaigns\"");
        assertThat(json).contains("\"confidence\"");
        assertThat(json).contains("\"methodology\"");
        assertThat(json).contains("p0-dynamic-sql-safety");
    }

    @Test
    @DisplayName("应该处理混合文件、重复 SQL、忽略目录和解析失败 fallback")
    void should_handle_complex_fixture_cases() throws IOException {
        Path repoRoot = tempDir.resolve("complex-repo");
        createComplexFixture(repoRoot);

        ScanExecutionResult result = scanOrchestrator.execute(
                ScanExecutionRequest.builder()
                        .path(repoRoot.toString())
                        .build(),
                null
        );

        assertThat(result.getStatistics().getTotalFiles()).isEqualTo(5);
        assertThat(result.getStatistics().getJavaFiles()).isEqualTo(1);
        assertThat(result.getStatistics().getXmlFiles()).isEqualTo(1);
        assertThat(result.getStatistics().getSqlFiles()).isEqualTo(1);
        assertThat(result.getStatistics().getSqlFound()).isEqualTo(5);
        assertThat(result.getStatistics().getSqlParsed()).isEqualTo(4);
        assertThat(result.getSqlEntries()).hasSize(4);

        var duplicateEntry = result.getSqlEntries().stream()
                .filter(entry -> entry.getOriginalSql().equals("SELECT id FROM users WHERE id = 1"))
                .findFirst()
                .orElseThrow();

        assertThat(duplicateEntry.getLocations())
                .extracting("sourceType")
                .containsExactlyInAnyOrder(SqlSourceType.STRING_LITERAL, SqlSourceType.JAVASCRIPT);
        assertThat(duplicateEntry.getLocations())
                .extracting("fileName")
                .containsExactlyInAnyOrder("UserQueries.java", "query.ts");

        Set<String> scannedFileNames = result.getSqlEntries().stream()
                .flatMap(entry -> entry.getLocations().stream())
                .map(location -> location.getFileName())
                .collect(Collectors.toSet());

        assertThat(scannedFileNames)
                .contains("UserQueries.java", "UserMapper.xml", "broken.sql", "query.js", "query.ts")
                .doesNotContain("Ignored.java", "ignored.sql");
        var mapperLocation = result.getSqlEntries().stream()
                .flatMap(entry -> entry.getLocations().stream())
                .filter(location -> location.getFileName().equals("UserMapper.xml"))
                .findFirst()
                .orElseThrow();
        assertThat(mapperLocation.getStartLine()).isEqualTo(3);
        assertThat(result.getIssueSummary()).containsEntry("SELECT_STAR", 1);
    }

    @Test
    @DisplayName("应该保留 ORDER BY 缺少 LIMIT 的独立规则类型")
    void should_keep_order_by_without_limit_as_dedicated_issue_type() throws IOException {
        Path repoRoot = tempDir.resolve("order-by-repo");
        write(repoRoot.resolve("sql/order.sql"), "SELECT id FROM users WHERE status = 1 ORDER BY created_at");

        ScanExecutionResult result = scanOrchestrator.execute(
                ScanExecutionRequest.builder()
                        .path(repoRoot.toString())
                        .build(),
                null
        );

        assertThat(result.getIssueSummary()).containsEntry("ORDER_BY_WITHOUT_LIMIT", 1);
        assertThat(result.getScanResult().getSqlStatements())
                .flatExtracting(sql -> sql.getStaticAnalysis().getIssues())
                .extracting(StaticIssue::getType)
                .contains(IssueType.ORDER_BY_WITHOUT_LIMIT)
                .doesNotContain(IssueType.SELECT_WITHOUT_WHERE);
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination);
                }
            }
        }
    }

    private void createComplexFixture(Path repoRoot) throws IOException {
        write(repoRoot.resolve("src/main/java/example/UserQueries.java"), """
                package example;

                class UserQueries {
                    private static final String FIND_USER_SQL = "SELECT id FROM users WHERE id = 1";
                }
                """);
        write(repoRoot.resolve("web/query.ts"), """
                export const sameUserSql = `SELECT id FROM users WHERE id = 1`;
                """);
        write(repoRoot.resolve("web/query.js"), """
                export const auditSql = "SELECT id FROM audit_log WHERE id = 2";
                """);
        write(repoRoot.resolve("src/main/resources/mapper/UserMapper.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <mapper namespace="example.UserMapper">
                    <select id="activeUsers">
                        SELECT id, name FROM users WHERE status = #{status}
                    </select>
                </mapper>
                """);
        write(repoRoot.resolve("sql/broken.sql"), "SELECT * FROM WHERE ;");

        List<Path> ignoredFiles = List.of(
                repoRoot.resolve("target/generated/Ignored.java"),
                repoRoot.resolve("build/generated/Ignored.java"),
                repoRoot.resolve(".git/hooks/ignored.sql"),
                repoRoot.resolve("node_modules/pkg/ignored.sql"),
                repoRoot.resolve(".idea/libraries/ignored.xml")
        );
        for (Path ignoredFile : ignoredFiles) {
            write(ignoredFile, "SELECT * FROM ignored_table;");
        }
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
