package org.spectrum.sqlchecker.domain.scanner.model;

import lombok.Getter;
import org.spectrum.sqlchecker.domain.shared.aggregate.AggregateRoot;
import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.scanner.service.extractor.SqlExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 源文件聚合根
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class SourceFile extends AggregateRoot<SourceFile> {

    /**
     * 文件路径（值对象）
     */
    private final FilePath path;

    /**
     * 文件类型（值对象）
     */
    private final FileType type;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 提取的 SQL 语句
     */
    private final List<SqlStatement> sqlStatements = new ArrayList<>();

    /**
     * 行数
     */
    private int lineCount;

    /**
     * 字符数
     */
    private int charCount;

    /**
     * 构造函数
     *
     * @param path 文件路径
     * @param type 文件类型
     * @param content 文件内容
     */
    public SourceFile(FilePath path, FileType type, String content) {
        this.path = path;
        this.type = type;
        this.content = content;
        this.lineCount = content != null ? content.split("\n").length : 0;
        this.charCount = content != null ? content.length() : 0;
    }

    /**
     * 构造函数（自动检测文件类型）
     *
     * @param path 文件路径
     * @param content 文件内容
     */
    public SourceFile(FilePath path, String content) {
        this(path, FileType.fromPath(path.getValue()), content);
    }

    /**
     * 设置文件内容
     *
     * @param content 文件内容
     */
    public void setContent(String content) {
        this.content = content;
        this.lineCount = content != null ? content.split("\n").length : 0;
        this.charCount = content != null ? content.length() : 0;
        markAsUpdated();
    }

    /**
     * 添加 SQL 语句
     *
     * @param sqlStatement SQL 语句
     */
    public void addSqlStatement(SqlStatement sqlStatement) {
        if (sqlStatement != null) {
            this.sqlStatements.add(sqlStatement);
            markAsUpdated();
        }
    }

    /**
     * 批量添加 SQL 语句
     *
     * @param statements SQL 语句列表
     */
    public void addSqlStatements(List<SqlStatement> statements) {
        if (statements != null) {
            this.sqlStatements.addAll(statements);
            markAsUpdated();
        }
    }

    /**
     * 获取不可变的 SQL 语句列表
     *
     * @return SQL 语句列表
     */
    public List<SqlStatement> getSqlStatements() {
        return Collections.unmodifiableList(sqlStatements);
    }

    /**
     * 提取 SQL（委托给提取器）
     *
     * @param extractor SQL 提取器
     * @return 提取的 SQL 语句列表
     * @throws SqlExtractionException 提取失败
     */
    public List<String> extractSql(SqlExtractor extractor) throws SqlExtractionException {
        List<String> extracted = extractor.extract(this.content);
        // 将提取的 SQL 字符串转换为 SqlStatement 对象并添加
        for (String sql : extracted) {
            SqlStatement statement = new SqlStatement(sql, sql);
            SqlLocation location = SqlLocation.builder()
                    .filePath(this.path)
                    .startLine(0)
                    .endLine(0)
                    .startColumn(0)
                    .endColumn(0)
                    .sourceType(extractor.getSourceType())
                    .build();
            statement.addLocation(location);
            addSqlStatement(statement);
        }
        return extracted;
    }

    /**
     * 是否为 XML 文件
     *
     * @return 是否为 XML
     */
    public boolean isXml() {
        return type.isXml();
    }

    /**
     * 是否为 Java 文件
     *
     * @return 是否为 Java
     */
    public boolean isJava() {
        return type.isJava();
    }

    /**
     * 是否为 SQL 文件
     *
     * @return 是否为 SQL
     */
    public boolean isSql() {
        return type.isSql();
    }

    /**
     * 获取文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return path.getFileName();
    }

    /**
     * 获取文件扩展名
     *
     * @return 扩展名
     */
    public String getExtension() {
        return path.getExtension();
    }

    @Override
    public String toString() {
        return "SourceFile{" +
            "path='" + path.getValue() + '\'' +
            ", type=" + type +
            ", lineCount=" + lineCount +
            ", sqlCount=" + sqlStatements.size() +
            '}';
    }
}
