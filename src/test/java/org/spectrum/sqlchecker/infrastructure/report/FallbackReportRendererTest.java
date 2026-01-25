package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainPlan;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanSqlEntry;
import org.spectrum.sqlchecker.application.scan.dto.ScanStatistics;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FallbackReportRenderer 单元测试
 */
@DisplayName("FallbackReportRenderer 单元测试")
class FallbackReportRendererTest {

    @Test
    @DisplayName("should render preprocess info and explain plan")
    void should_render_preprocess_and_explain() {
        FallbackReportRenderer renderer = new FallbackReportRenderer();

        PreprocessResult preprocess = PreprocessResult.builder()
                .category(SqlCategory.MYBATIS_XML_STATIC)
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SUPPORTED)
                .normalizedSql("select * from t where id = ?")
                .explainSql("explain select * from t where id = 1")
                .build();

        PlanNode node = PlanNode.builder()
                .selectType("SIMPLE")
                .type("ALL")
                .table("t")
                .key("idx_id")
                .rows(100L)
                .extra("Using where")
                .build();

        ExplainAnalysisDto explain = ExplainAnalysisDto.builder()
                .plan(ExplainPlan.builder().nodes(List.of(node)).build())
                .build();

        ScanSqlEntry entry = ScanSqlEntry.builder()
                .id("sql-1")
                .abstractSql("select * from t where id = ?")
                .originalSql("select * from t where id = 1")
                .preprocessResult(preprocess)
                .explainAnalysis(explain)
                .build();

        ScanExecutionResult result = ScanExecutionResult.builder()
                .scanPath(".")
                .statistics(ScanStatistics.builder()
                        .totalFiles(1)
                        .javaFiles(1)
                        .xmlFiles(0)
                        .sqlFiles(0)
                        .filesScanned(1)
                        .sqlFound(1)
                        .sqlParsed(1)
                        .durationMs(10)
                        .criticalIssues(0)
                        .warningIssues(0)
                        .infoIssues(0)
                        .build())
                .sqlEntries(List.of(entry))
                .build();

        String html = renderer.render(result);

        assertThat(html).contains("分类:");
        assertThat(html).contains("Explain SQL");
        assertThat(html).contains("执行计划节点");
        assertThat(html).contains("Using where");
    }
}
