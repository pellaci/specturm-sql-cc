package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import lombok.RequiredArgsConstructor;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detect performance issues from EXPLAIN plan nodes.
 */
@Component
@RequiredArgsConstructor
public class ExplainIssueDetector {

    private final ExplainAnalysisSettings settings;

    public List<ExplainIssue> detect(List<PlanNode> nodes) {
        List<ExplainIssue> issues = new ArrayList<>();
        long rowsThreshold = settings.getRowsThreshold();

        for (PlanNode node : nodes) {
            String tableName = node.getTable() != null ? node.getTable() : "<unknown>";
            long rows = node.getRows() != null ? node.getRows() : 0L;
            boolean hasRows = node.getRows() != null;
            boolean highRows = hasRows && rows > rowsThreshold;

            if (node.isFullTableScan()) {
                issues.add(ExplainIssue.builder()
                        .type("FULL_TABLE_SCAN")
                        .severity(highRows ? SeverityLevel.CRITICAL : SeverityLevel.WARNING)
                        .message("检测到全表扫描" + (hasRows ? ("，预计扫描行数: " + rows) : ""))
                        .suggestion("建议在 " + tableName + " 表上添加适当的索引以避免全表扫描")
                        .tableName(node.getTable())
                        .build());
            }

            if (!node.isUsingIndex()) {
                issues.add(ExplainIssue.builder()
                        .type("NO_INDEX_USED")
                        .severity(highRows ? SeverityLevel.WARNING : SeverityLevel.INFO)
                        .message("查询未使用索引" + (hasRows ? ("，预计扫描行数: " + rows) : ""))
                        .suggestion("建议在 " + tableName + " 表上为 WHERE 条件字段添加索引")
                        .tableName(node.getTable())
                        .build());
            }

            if (highRows) {
                issues.add(ExplainIssue.builder()
                        .type("HIGH_ROWS")
                        .severity(SeverityLevel.WARNING)
                        .message("预计扫描行数过多: " + rows + " 行")
                        .suggestion("建议优化查询条件或添加复合索引以减少扫描行数")
                        .tableName(node.getTable())
                        .build());
            }

            if (node.isUsingTemporary()) {
                issues.add(ExplainIssue.builder()
                        .type("USING_TEMPORARY")
                        .severity(highRows ? SeverityLevel.WARNING : SeverityLevel.INFO)
                        .message("查询使用了临时表")
                        .suggestion("临时表创建会影响性能，建议优化 GROUP BY 或 ORDER BY，或考虑添加索引")
                        .tableName(node.getTable())
                        .build());
            }

            if (node.isUsingFilesort()) {
                issues.add(ExplainIssue.builder()
                        .type("USING_FILESORT")
                        .severity(highRows ? SeverityLevel.WARNING : SeverityLevel.INFO)
                        .message("查询使用了文件排序")
                        .suggestion("建议为 ORDER BY 字段添加索引以避免文件排序")
                        .tableName(node.getTable())
                        .build());
            }
        }

        return issues;
    }
}
