package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * SQL 类型
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum SqlType {

    /**
     * SELECT
     */
    SELECT,

    /**
     * INSERT
     */
    INSERT,

    /**
     * UPDATE
     */
    UPDATE,

    /**
     * DELETE
     */
    DELETE,

    /**
     * CREATE
     */
    CREATE,

    /**
     * ALTER
     */
    ALTER,

    /**
     * DROP
     */
    DROP,

    /**
     * 其他（TRUNCATE、SHOW 等）
     */
    OTHER;

    /**
     * 从 SQL 语句推断类型
     */
    public static SqlType fromSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return OTHER;
        }

        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) {
            return SELECT;
        } else if (trimmed.startsWith("INSERT")) {
            return INSERT;
        } else if (trimmed.startsWith("UPDATE")) {
            return UPDATE;
        } else if (trimmed.startsWith("DELETE")) {
            return DELETE;
        } else if (trimmed.startsWith("CREATE")) {
            return CREATE;
        } else if (trimmed.startsWith("ALTER")) {
            return ALTER;
        } else if (trimmed.startsWith("DROP")) {
            return DROP;
        }
        return OTHER;
    }
}
