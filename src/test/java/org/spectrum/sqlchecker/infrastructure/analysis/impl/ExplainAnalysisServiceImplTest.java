package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainIssueDetector;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainPlanBuilder;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainPlanExecutor;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainSqlPreprocessor;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExplainAnalysisServiceImpl 单元测试")
class ExplainAnalysisServiceImplTest {

    @Mock
    private ExplainPlanExecutor planExecutor;

    @Mock
    private ExplainIssueDetector issueDetector;

    @Mock
    private ExplainPlanBuilder planBuilder;

    @Test
    @DisplayName("SQL 方言 EXPLAIN 失败应转为诊断，不计入 SQL 风险问题")
    void should_return_diagnostic_error_without_risk_issue_when_explain_sql_fails() throws Exception {
        ExplainAnalysisServiceImpl service = new ExplainAnalysisServiceImpl(
                planExecutor,
                issueDetector,
                planBuilder,
                new ExplainSqlPreprocessor()
        );
        when(planExecutor.execute(anyString(), eq("default")))
                .thenThrow(new SQLException("bad syntax"));

        ExplainAnalysisDto result = service.analyze("sql-1", "SELECT id FROM users", "default");

        assertThat(result.getSeverity()).isEqualTo(SeverityLevel.INFO);
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.hasIssues()).isFalse();
        assertThat(result.getErrorMessage()).contains("EXPLAIN 执行失败").contains("bad syntax");
        verifyNoInteractions(issueDetector, planBuilder);
    }
}
