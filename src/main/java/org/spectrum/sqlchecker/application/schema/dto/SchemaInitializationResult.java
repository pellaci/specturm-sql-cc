package org.spectrum.sqlchecker.application.schema.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Schema 初始化结果
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Data
@Builder
public class SchemaInitializationResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 创建的表数量
     */
    private int tablesCreated;

    /**
     * 跳过的表数量（已存在）
     */
    private int tablesSkipped;

    /**
     * 失败的表数量
     */
    private int tablesFailed;

    /**
     * 详细信息列表
     */
    private List<TableCreationDetail> details;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 表创建详情
     */
    @Data
    @Builder
    public static class TableCreationDetail {
        /**
         * 表名
         */
        private String tableName;

        /**
         * 来源类型：DDL 或 INFERRED
         */
        private String source;

        /**
         * 来源文件
         */
        private String sourceFile;

        /**
         * 是否创建成功
         */
        private boolean created;

        /**
         * 是否跳过（表已存在）
         */
        private boolean skipped;

        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
