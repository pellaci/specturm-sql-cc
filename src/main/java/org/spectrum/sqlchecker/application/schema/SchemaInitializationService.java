package org.spectrum.sqlchecker.application.schema;

import org.spectrum.sqlchecker.application.schema.dto.SchemaInitializationResult;

import java.nio.file.Path;
import java.util.List;

/**
 * Schema 初始化服务接口
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public interface SchemaInitializationService {

    /**
     * 初始化数据库 Schema
     * <p>
     * 扫描指定路径下的 DDL 文件，提取 CREATE TABLE 语句并执行。
     * 如果没有 DDL 文件，则从 DML 语句推断表结构。
     *
     * @param scanPath     扫描路径
     * @param connectionId 数据库连接 ID
     * @return 初始化结果
     */
    SchemaInitializationResult initialize(Path scanPath, String connectionId);

    /**
     * 从 DML 语句推断并初始化表结构
     *
     * @param dmlStatements DML 语句列表
     * @param connectionId  数据库连接 ID
     * @return 初始化结果
     */
    SchemaInitializationResult initializeFromDml(List<String> dmlStatements, String connectionId);
}
