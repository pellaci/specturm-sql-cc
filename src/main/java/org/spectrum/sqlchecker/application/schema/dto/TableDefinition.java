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
     * 是否是推断的（非 DDL 文件提取）
     */
    private boolean inferred;

    /**
     * 来源文件路径
     */
    private String sourceFile;
}
