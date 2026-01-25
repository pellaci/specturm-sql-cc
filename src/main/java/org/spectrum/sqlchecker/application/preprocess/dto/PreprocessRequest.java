package org.spectrum.sqlchecker.application.preprocess.dto;

import lombok.Builder;
import lombok.Data;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;

/**
 * 预处理请求
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class PreprocessRequest {

    /**
     * SQL ID
     */
    private String sqlId;

    /**
     * 原始 SQL
     */
    private String originalSql;

    /**
     * 来源类型
     */
    private SqlSourceType sourceType;

    /**
     * 来源上下文（文件路径/Mapper ID/方法名）
     */
    private String sourceContext;

    /**
     * 是否启用 Explain
     */
    private boolean explainEnabled;
}
