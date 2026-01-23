package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

/**
 * 静态分析问题
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticIssue {

    /**
     * 问题类型
     */
    private IssueType type;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 问题描述
     */
    private String message;

    /**
     * 位置描述（如 "line 15"）
     */
    private String location;

    /**
     * 建议修改
     */
    private String suggestion;

    /**
     * 参考链接
     */
    private String referenceUrl;

    /**
     * 问题代码片段
     */
    private String codeSnippet;
}
