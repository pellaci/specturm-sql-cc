package org.spectrum.sqlchecker.application.schema.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 表定义 DTO
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Data
@Builder
public class TableDefinition {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 原始 DDL 语句
     */
    private String originalDdl;

    /**
     * DDL 中声明的列
     */
    private java.util.List<String> columns;

    /**
     * 主键列
     */
    private java.util.List<String> primaryKeyColumns;

    /**
     * 索引覆盖列（包含主键和普通索引首列/成员列）
     */
    private java.util.List<String> indexedColumns;

    /**
     * 是否是推断的（非 DDL 文件提取）
     */
    private boolean inferred;

    /**
     * 来源文件路径
     */
    private String sourceFile;
}
