package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.analysis.ExplainAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainPlan;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * EXPLAIN 分析服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
public class ExplainAnalysisServiceImpl implements ExplainAnalysisService {

    @Override
    public ExplainAnalysisDto analyze(String sqlId, String sql, String connectionId) {
        // 模拟 EXPLAIN 分析
        List<ExplainIssue> issues = new ArrayList<>();
        List<PlanNode> nodes = new ArrayList<>();

        // 模拟执行计划节点
        nodes.add(PlanNode.builder()
                .id(1)
                .selectType("SIMPLE")
                .type("ALL")
                .table("users")
                .possibleKeys(null)
                .key(null)
                .keyLen(null)
                .ref(null)
                .rows(10000L)
                .extra("Using where")
                .build());

        // 检查问题
        String upperSql = sql.toUpperCase();
        if (upperSql.contains("SELECT *")) {
            issues.add(ExplainIssue.builder()
                    .type("FULL_TABLE_SCAN")
                    .severity(SeverityLevel.CRITICAL)
                    .message("检测到全表扫描")
                    .suggestion("添加适当的索引或优化查询条件")
                    .tableName("users")
                    .build());
        }

        ExplainPlan plan = ExplainPlan.builder()
                .nodes(nodes)
                .build();

        return ExplainAnalysisDto.builder()
                .sqlId(sqlId)
                .plan(plan)
                .issues(issues)
                .severity(issues.isEmpty() ? SeverityLevel.INFO : SeverityLevel.CRITICAL)
                .durationMs(100)
                .build();
    }

    @Override
    public ExplainAnalysisDto analyzeWithDefaultConnection(String sqlId, String sql) {
        return analyze(sqlId, sql, "default");
    }
}
