package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 扫描执行请求
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanExecutionRequest {

    /**
     * 扫描路径
     */
    private String path;

    /**
     * 是否输出详细信息
     */
    private boolean verbose;

    /**
     * 是否启用 EXPLAIN
     */
    private boolean enableExplain;

    /**
     * 数据库连接名
     */
    private String dbConnection;

    /**
     * 是否初始化 Schema
     */
    private boolean initSchema;

    /**
     * Schema 路径
     */
    private String schemaPath;
}
