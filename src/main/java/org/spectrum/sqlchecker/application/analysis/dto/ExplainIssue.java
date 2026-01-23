package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

/**
 * EXPLAIN 分析问题
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainIssue {

    /**
     * 问题类型
     */
    private String type;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 问题描述
     */
    private String message;

    /**
     * 建议修改
     */
    private String suggestion;

    /**
     * 相关表名
     */
    private String tableName;
}
