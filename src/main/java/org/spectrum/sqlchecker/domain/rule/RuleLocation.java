package org.spectrum.sqlchecker.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则问题位置
 * <p>
 * 描述问题在 SQL 语句中的位置信息
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleLocation {

    /**
     * SQL ID
     */
    private String sqlId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 起始行号（从 1 开始）
     */
    private int startLine;

    /**
     * 起始列号（从 1 开始）
     */
    private int startColumn;

    /**
     * 结束行号（从 1 开始）
     */
    private int endLine;

    /**
     * 结束列号（从 1 开始）
     */
    private int endColumn;

    /**
     * 创建位置的便捷方法（仅行号）
     *
     * @param sqlId SQL ID
     * @param line  行号
     * @return 位置对象
     */
    public static RuleLocation of(String sqlId, int line) {
        return RuleLocation.builder()
                .sqlId(sqlId)
                .startLine(line)
                .endLine(line)
                .build();
    }

    /**
     * 创建位置的便捷方法（完整位置）
     *
     * @param sqlId   SQL ID
     * @param line    行号
     * @param column  列号
     * @return 位置对象
     */
    public static RuleLocation of(String sqlId, int line, int column) {
        return RuleLocation.builder()
                .sqlId(sqlId)
                .startLine(line)
                .startColumn(column)
                .endLine(line)
                .endColumn(column)
                .build();
    }
}
