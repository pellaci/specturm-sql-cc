package org.spectrum.sqlchecker.domain.scanner.service;

import org.spectrum.sqlchecker.domain.scanner.model.SqlStatement;

import java.util.List;

/**
 * SQL 去重器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlDeduplicator {

    /**
     * 对 SQL 语句去重
     *
     * @param statements 原始 SQL 列表
     * @return 去重后的 SQL 列表
     */
    List<SqlStatement> deduplicate(List<SqlStatement> statements);

    /**
     * 统计重复情况
     *
     * @param statements SQL 列表
     * @return 重复统计信息
     */
    DeduplicationStats stats(List<SqlStatement> statements);

    /**
     * 去重统计信息
     */
    record DeduplicationStats(
        int total,
        int unique,
        int duplicates,
        double duplicateRate
    ) {}
}
