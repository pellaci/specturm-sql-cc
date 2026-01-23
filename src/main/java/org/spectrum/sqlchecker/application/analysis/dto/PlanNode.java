package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

/**
 * 执行计划节点
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanNode {

    /**
     * 节点 ID
     */
    private Integer id;

    /**
     * select_type
     */
    private String selectType;

    /**
     * type
     */
    private String type;

    /**
     * 表名
     */
    private String table;

    /**
     * 分区
     */
    private String partitions;

    /**
     * 可能使用的索引
     */
    private String possibleKeys;

    /**
     * 实际使用的索引
     */
    private String key;

    /**
     * 索引长度
     */
    private String keyLen;

    /**
     * 引用列
     */
    private String ref;

    /**
     * 扫描行数
     */
    private Long rows;

    /**
     * 额外信息
     */
    private String extra;

    /**
     * 解析结果（友好说明）
     */
    private String explanation;

    /**
     * 是否为全表扫描
     */
    public boolean isFullTableScan() {
        return "ALL".equalsIgnoreCase(type);
    }

    /**
     * 是否使用索引
     */
    public boolean isUsingIndex() {
        return key != null && !key.isEmpty() && !"".equals(key) && !"NULL".equalsIgnoreCase(key);
    }

    /**
     * 是否使用临时表
     */
    public boolean isUsingTemporary() {
        return extra != null && extra.toUpperCase().contains("USING TEMPORARY");
    }

    /**
     * 是否使用文件排序
     */
    public boolean isUsingFilesort() {
        return extra != null && extra.toUpperCase().contains("USING FILESORT");
    }
}
