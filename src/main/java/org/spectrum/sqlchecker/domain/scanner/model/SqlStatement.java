package org.spectrum.sqlchecker.domain.scanner.model;

import lombok.Getter;
import org.spectrum.sqlchecker.domain.shared.aggregate.AggregateRoot;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash;
import org.spectrum.sqlchecker.domain.shared.valueobject.AbstractSql;
import org.spectrum.sqlchecker.domain.shared.valueobject.OriginalSql;
import org.spectrum.sqlchecker.domain.scanner.model.SqlLocation;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQL 语句聚合根
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class SqlStatement extends AggregateRoot<SqlStatement> {

    /**
     * 原始 SQL（值对象）
     */
    private final OriginalSql originalSql;

    /**
     * 抽象 SQL（参数化模板，值对象）
     */
    private AbstractSql abstractSql;

    /**
     * SQL 类型
     */
    private SqlType sqlType;

    /**
     * SQL 哈希（用于去重，值对象）
     */
    private SqlHash sqlHash;

    /**
     * 出现位置列表
     */
    private final List<SqlLocation> locations = new ArrayList<>();

    /**
     * 构造函数
     *
     * @param originalSql 原始 SQL
     * @param abstractSql 抽象 SQL
     * @param sqlType SQL 类型
     */
    public SqlStatement(String originalSql, String abstractSql, SqlType sqlType) {
        this.originalSql = new OriginalSql(originalSql);
        this.abstractSql = new AbstractSql(abstractSql);
        this.sqlType = sqlType != null ? sqlType : SqlType.fromSql(originalSql);
        this.sqlHash = SqlHash.fromAbstract(abstractSql);
    }

    /**
     * 构造函数（自动推断类型）
     *
     * @param originalSql 原始 SQL
     * @param abstractSql 抽象 SQL
     */
    public SqlStatement(String originalSql, String abstractSql) {
        this(originalSql, abstractSql, SqlType.fromSql(originalSql));
    }

    /**
     * 设置抽象 SQL
     *
     * @param abstractSql 抽象 SQL
     */
    public void setAbstractSql(String abstractSql) {
        this.abstractSql = new AbstractSql(abstractSql);
    }

    /**
     * 设置 SQL 类型
     *
     * @param sqlType SQL 类型
     */
    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
        markAsUpdated();
    }

    /**
     * 添加位置
     *
     * @param location 位置信息
     */
    public void addLocation(SqlLocation location) {
        if (location != null && !hasLocation(location)) {
            this.locations.add(location);
            markAsUpdated();
        }
    }

    /**
     * 批量添加位置
     *
     * @param newLocations 位置列表
     */
    public void addLocations(List<SqlLocation> newLocations) {
        if (newLocations != null) {
            for (SqlLocation location : newLocations) {
                addLocation(location);
            }
        }
    }

    /**
     * 判断是否已有位置
     */
    private boolean hasLocation(SqlLocation location) {
        return locations.stream()
            .anyMatch(l -> l.getFilePath().equals(location.getFilePath())
                && l.getStartLine() == location.getStartLine());
    }

    /**
     * 获取不可变的位置列表
     */
    public List<SqlLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    /**
     * 是否为相同 SQL（用于去重）
     *
     * @param other 另一条 SQL
     * @return 是否相同
     */
    public boolean isSameSql(SqlStatement other) {
        return other != null && this.sqlHash.equals(other.sqlHash);
    }

    /**
     * 合并位置信息
     *
     * @param other 另一条 SQL
     */
    public void mergeLocations(SqlStatement other) {
        if (other != null) {
            addLocations(other.getLocations());
            markAsUpdated();
        }
    }

    /**
     * 获取原始 SQL 值
     */
    public String getOriginalSql() {
        return originalSql.getValue();
    }

    /**
     * 获取抽象 SQL 值
     */
    public String getAbstractSql() {
        return abstractSql.getValue();
    }

    /**
     * 获取 SQL 哈希值
     */
    public String getSqlHash() {
        return sqlHash.getValue();
    }

    @Override
    public String toString() {
        return "SqlStatement{" +
            "id='" + getId() + '\'' +
            ", sqlType=" + sqlType +
            ", abstractSql='" + abstractSql.getValue() + '\'' +
            ", locationCount=" + locations.size() +
            '}';
    }
}
