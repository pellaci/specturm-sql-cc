package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import org.spectrum.sqlchecker.application.analysis.dto.ExplainPlan;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Build ExplainPlan summary from plan nodes.
 */
@Component
public class ExplainPlanBuilder {

    public ExplainPlan build(List<PlanNode> nodes) {
        long totalRows = nodes.stream()
                .filter(n -> n.getRows() != null)
                .mapToLong(PlanNode::getRows)
                .sum();

        boolean hasFullTableScan = nodes.stream().anyMatch(PlanNode::isFullTableScan);
        boolean hasTemporary = nodes.stream().anyMatch(PlanNode::isUsingTemporary);
        boolean hasFilesort = nodes.stream().anyMatch(PlanNode::isUsingFilesort);

        return ExplainPlan.builder()
                .nodes(nodes)
                .totalRows(totalRows)
                .hasFullTableScan(hasFullTableScan)
                .hasTemporary(hasTemporary)
                .hasFilesort(hasFilesort)
                .build();
    }
}
