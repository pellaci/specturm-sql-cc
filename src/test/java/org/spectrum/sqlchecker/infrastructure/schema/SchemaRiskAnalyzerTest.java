package org.spectrum.sqlchecker.infrastructure.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spectrum.sqlchecker.application.scan.dto.SchemaAnalysisDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaRiskAnalyzerTest {

    @Test
    @DisplayName("should correlate SQL predicates with DDL indexes")
    void should_correlate_sql_predicates_with_ddl_indexes(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("schema.sql"), """
                CREATE TABLE t_order (
                  id BIGINT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  status VARCHAR(16),
                  created_at TIMESTAMP,
                  KEY idx_user_id (user_id)
                );
                """);
        SchemaRiskAnalyzer analyzer = new SchemaRiskAnalyzer(new DdlExtractor());

        SchemaAnalysisDto analysis = analyzer.analyze(tempDir, List.of(SqlStatementDto.builder()
                .id("sql-order")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM t_order WHERE user_id = ? AND status = ? ORDER BY created_at")
                .normalizedSql("SELECT id FROM t_order WHERE user_id = ? AND status = ? ORDER BY created_at")
                .validity(ValidityStatus.VALID)
                .locations(List.of(location("mapper/OrderMapper.xml")))
                .build()));

        assertThat(analysis.isDdlDetected()).isTrue();
        assertThat(analysis.getSchemaPath()).isEqualTo(tempDir.toAbsolutePath().normalize().toString());
        assertThat(analysis.getTableCount()).isEqualTo(1);
        assertThat(analysis.getCoveredTableCount()).isEqualTo(1);
        assertThat(analysis.getUnindexedPredicateCount()).isEqualTo(1);
        assertThat(analysis.getRisks())
                .anySatisfy(risk -> {
                    assertThat(risk.getRiskType()).isEqualTo("UNINDEXED_PREDICATE");
                    assertThat(risk.getTableName()).isEqualTo("t_order");
                    assertThat(risk.getIndexedPredicateColumns()).contains("user_id");
                    assertThat(risk.getMissingIndexColumns()).contains("status", "created_at");
                    assertThat(risk.getLocations()).contains("mapper/OrderMapper.xml:12");
                });
    }

    @Test
    @DisplayName("should report missing schema path as evidence warning")
    void should_report_missing_schema_path_as_evidence_warning(@TempDir Path tempDir) {
        Path missingSchemaPath = tempDir.resolve("missing-ddl");
        SchemaRiskAnalyzer analyzer = new SchemaRiskAnalyzer(new DdlExtractor());

        SchemaAnalysisDto analysis = analyzer.analyze(missingSchemaPath, List.of(SqlStatementDto.builder()
                .id("sql-user")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users WHERE status = ?")
                .normalizedSql("SELECT id FROM users WHERE status = ?")
                .validity(ValidityStatus.VALID)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build()));

        assertThat(analysis.isDdlDetected()).isFalse();
        assertThat(analysis.getSchemaPath()).isEqualTo(missingSchemaPath.toAbsolutePath().normalize().toString());
        assertThat(analysis.getWarnings()).anySatisfy(warning -> assertThat(warning).contains("DDL 证据路径不存在"));
    }

    @Test
    @DisplayName("should report SQL tables missing from project DDL")
    void should_report_sql_tables_missing_from_project_ddl(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("schema.sql"), """
                CREATE TABLE users (
                  id BIGINT PRIMARY KEY,
                  name VARCHAR(64)
                );
                """);
        SchemaRiskAnalyzer analyzer = new SchemaRiskAnalyzer(new DdlExtractor());

        SchemaAnalysisDto analysis = analyzer.analyze(tempDir, List.of(SqlStatementDto.builder()
                .id("sql-payment")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM payment_order WHERE id = ?")
                .normalizedSql("SELECT id FROM payment_order WHERE id = ?")
                .validity(ValidityStatus.VALID)
                .locations(List.of(location("PaymentDao.java")))
                .build()));

        assertThat(analysis.getMissingDdlTableCount()).isEqualTo(1);
        assertThat(analysis.getWarnings()).anySatisfy(warning -> assertThat(warning).contains("部分 SQL 引用的表"));
        assertThat(analysis.getRisks())
                .anySatisfy(risk -> {
                    assertThat(risk.getRiskType()).isEqualTo("SCHEMA_GAP");
                    assertThat(risk.getTableName()).isEqualTo("payment_order");
                    assertThat(risk.getRecommendation()).contains("schema-path");
                });
    }

    @Test
    @DisplayName("should keep missing DDL as a global evidence gap when no DDL exists")
    void should_keep_missing_ddl_as_global_evidence_gap_when_no_ddl_exists(@TempDir Path tempDir) {
        SchemaRiskAnalyzer analyzer = new SchemaRiskAnalyzer(new DdlExtractor());

        SchemaAnalysisDto analysis = analyzer.analyze(tempDir, List.of(SqlStatementDto.builder()
                .id("sql-user")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users WHERE status = ?")
                .normalizedSql("SELECT id FROM users WHERE status = ?")
                .validity(ValidityStatus.VALID)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build()));

        assertThat(analysis.isDdlDetected()).isFalse();
        assertThat(analysis.getMissingDdlTableCount()).isEqualTo(1);
        assertThat(analysis.getRisks()).isEmpty();
        assertThat(analysis.getWarnings()).anySatisfy(warning -> assertThat(warning).contains("未在扫描范围内发现"));
    }

    private SqlLocationDto location(String filePath) {
        return SqlLocationDto.builder()
                .filePath(filePath)
                .fileName(Path.of(filePath).getFileName().toString())
                .startLine(12)
                .endLine(12)
                .sourceType(SqlSourceType.MYBATIS)
                .build();
    }
}
