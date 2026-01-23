package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

/**
 * 执行计划
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainPlan {

    /**
     * 执行计划节点列表
     */
    private List<PlanNode> nodes;

    /**
     * 是否有全表扫描
     */
    @Builder.Default
    private boolean hasFullTableScan = false;

    /**
     * 是否使用临时表
     */
    @Builder.Default
    private boolean hasTemporary = false;

    /**
     * 是否使用文件排序
     */
    @Builder.Default
    private boolean hasFilesort = false;

    /**
     * 总扫描行数
     */
    @Builder.Default
    private long totalRows = 0;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 评估等级
     */
    public void evaluate() {
        if (hasFullTableScan) {
            severity = SeverityLevel.max(severity, SeverityLevel.CRITICAL);
        }
        if (totalRows > 10000) {
            severity = SeverityLevel.max(severity, SeverityLevel.WARNING);
        }
        if (hasTemporary || hasFilesort) {
            severity = SeverityLevel.max(severity, SeverityLevel.WARNING);
        }
        if (severity == null) {
            severity = SeverityLevel.INFO;
        }
    }
}
