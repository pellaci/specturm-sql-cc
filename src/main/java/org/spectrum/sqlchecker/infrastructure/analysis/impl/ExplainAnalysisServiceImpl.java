package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.analysis.ExplainAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainPlan;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.exception.ConnectionException;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainIssueDetector;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainPlanBuilder;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainPlanExecutor;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainSqlPreprocessor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPLAIN 分析服务实现
 * <p>
 * 连接数据库执行 EXPLAIN 命令，解析执行计划并检测性能问题
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplainAnalysisServiceImpl implements ExplainAnalysisService {

    private final ExplainPlanExecutor planExecutor;
    private final ExplainIssueDetector issueDetector;
    private final ExplainPlanBuilder planBuilder;
    private final ExplainSqlPreprocessor sqlPreprocessor;

    @Override
    public ExplainAnalysisDto analyze(String sqlId, String sql, String connectionId) {
        long startTime = System.currentTimeMillis();

        try {
            // 0. 预处理 SQL，替换未解析的 MyBatis 占位符
            ExplainSqlPreprocessor.PreprocessResult preprocessResult = sqlPreprocessor.preprocess(sql);
            if (preprocessResult.skipped()) {
                log.debug("EXPLAIN skipped: sqlId={}, reason={}", sqlId, preprocessResult.reason());
                return buildSkipResult(sqlId);
            }
            String processedSql = preprocessResult.sql();

            // 1. 执行 EXPLAIN 并解析结果
            ExplainPlanExecutor.ExplainExecutionResult executionResult =
                    planExecutor.execute(processedSql, connectionId);
            List<PlanNode> nodes = executionResult.nodes();

            // 2. 分析执行计划，检测问题
            List<ExplainIssue> issues = issueDetector.detect(nodes);

            // 3. 计算总体严重等级
            SeverityLevel severity = calculateSeverity(issues);

            // 4. 生成执行计划摘要
            ExplainPlan plan = planBuilder.build(nodes);
            plan.setSeverity(severity);

            long durationMs = System.currentTimeMillis() - startTime;

            log.debug("EXPLAIN 分析完成: sqlId={}, database={}, 节点数={}, 耗时={}ms",
                    sqlId, executionResult.databaseName(), nodes.size(), durationMs);

            return ExplainAnalysisDto.builder()
                    .sqlId(sqlId)
                    .plan(plan)
                    .issues(issues)
                    .severity(severity)
                    .durationMs(durationMs)
                    .build();

        } catch (ConnectionException e) {
            log.error("数据库连接失败: connectionId={}", connectionId, e);
            return buildErrorResult(sqlId, "数据库连接失败: " + e.getMessage());
        } catch (SQLTimeoutException e) {
            log.error("EXPLAIN 执行超时: sqlId={}, sql={}", sqlId, sql, e);
            return buildErrorResult(sqlId, "EXPLAIN 执行超时");
        } catch (SQLException e) {
            log.error("EXPLAIN 执行失败: sqlId={}, sql={}", sqlId, sql, e);
            return buildErrorResult(sqlId, "EXPLAIN 执行失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("EXPLAIN 分析异常: sqlId={}", sqlId, e);
            return buildErrorResult(sqlId, "分析异常: " + e.getMessage());
        }
    }

    @Override
    public ExplainAnalysisDto analyzeWithDefaultConnection(String sqlId, String sql) {
        return analyze(sqlId, sql, "default");
    }

    /**
     * 计算总体严重等级
     */
    private SeverityLevel calculateSeverity(List<ExplainIssue> issues) {
        if (issues.isEmpty()) {
            return SeverityLevel.INFO;
        }

        boolean hasCritical = issues.stream().anyMatch(i -> i.getSeverity() == SeverityLevel.CRITICAL);
        if (hasCritical) {
            return SeverityLevel.CRITICAL;
        }

        boolean hasWarning = issues.stream().anyMatch(i -> i.getSeverity() == SeverityLevel.WARNING);
        if (hasWarning) {
            return SeverityLevel.WARNING;
        }

        return SeverityLevel.INFO;
    }

    /**
     * 构建错误结果
     */
    private ExplainAnalysisDto buildErrorResult(String sqlId, String errorMessage) {
        return ExplainAnalysisDto.builder()
                .sqlId(sqlId)
                .plan(ExplainPlan.builder().nodes(new ArrayList<>()).build())
                .issues(List.of(ExplainIssue.builder()
                        .type("ANALYSIS_ERROR")
                        .severity(SeverityLevel.WARNING)
                        .message("无法分析执行计划")
                        .suggestion(errorMessage)
                        .build()))
                .severity(SeverityLevel.WARNING)
                .durationMs(0)
                .build();
    }

    /**
     * 构建跳过结果（不产生问题）
     */
    private ExplainAnalysisDto buildSkipResult(String sqlId) {
        return ExplainAnalysisDto.builder()
                .sqlId(sqlId)
                .plan(ExplainPlan.builder().nodes(new ArrayList<>()).build())
                .issues(List.of())
                .severity(SeverityLevel.INFO)
                .durationMs(0)
                .build();
    }
}
