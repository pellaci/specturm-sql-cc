package org.spectrum.sqlchecker.infrastructure.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("JavaScriptSqlExtractor 单元测试")
class JavaScriptSqlExtractorTest {

    private JavaScriptSqlExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JavaScriptSqlExtractor();
    }

    @Test
    @DisplayName("应该支持 ts 文件")
    void should_support_ts_file() {
        assertThat(extractor.supports(FileType.fromPath("query.ts"))).isTrue();
        assertThat(extractor.getSourceType()).isEqualTo(SqlSourceType.JAVASCRIPT);
    }

    @Test
    @DisplayName("应该从模板字符串中提取 SQL")
    void should_extract_sql_from_template_literal() throws Exception {
        String content = """
                const sql = `SELECT id, status FROM orders WHERE status = 'NEW'`;
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).containsExactly("SELECT id, status FROM orders WHERE status = 'NEW'");
    }

    @Test
    @DisplayName("应该从普通字符串中提取 SQL")
    void should_extract_sql_from_string_literal() throws Exception {
        String content = """
                const sql = "UPDATE users SET name = 'alice' WHERE id = 1";
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).containsExactly("UPDATE users SET name = 'alice' WHERE id = 1");
    }

    @Test
    @DisplayName("应该从多行模板字符串中提取 SQL")
    void should_extract_multiline_template_literal() throws Exception {
        String content = """
                const sql = `
                    SELECT id, name
                    FROM users
                    WHERE status = 'ACTIVE'
                `;
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).hasSize(1);
        assertThat(sqls.get(0)).contains("SELECT id, name", "FROM users", "WHERE status = 'ACTIVE'");
    }

    @Test
    @DisplayName("应该支持小写 SQL")
    void should_extract_lowercase_sql() throws Exception {
        String content = "const sql = 'select id from users where id = 1';";

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).containsExactly("select id from users where id = 1");
    }

    @Test
    @DisplayName("应该处理转义引号")
    void should_extract_sql_with_escaped_quotes() throws Exception {
        String content = """
                const sql = "SELECT id FROM users WHERE name = \\"alice\\"";
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).containsExactly("SELECT id FROM users WHERE name = \\\"alice\\\"");
    }

    @Test
    @DisplayName("不应该提取非 SQL 字符串")
    void should_ignore_non_sql_strings() throws Exception {
        String content = """
                const message = "please select a user from the dropdown";
                const label = 'display more options';
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls).isEmpty();
    }

    @Test
    @DisplayName("应该提取 WITH 和 SHOW 语句")
    void should_extract_keywords_supported_by_validator() throws Exception {
        String content = """
                const cte = `WITH active_users AS (SELECT id FROM users) SELECT * FROM active_users`;
                const showTables = "SHOW TABLES";
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls)
                .containsExactly(
                        "WITH active_users AS (SELECT id FROM users) SELECT * FROM active_users",
                        "SHOW TABLES"
                );
    }

    @Test
    @DisplayName("注释中的 SQL 字符串按当前字符串提取策略处理")
    void should_lock_current_comment_string_behavior() throws Exception {
        String content = """
                // const sql = "SELECT * FROM commented_out";
                const activeSql = "SELECT id FROM active_users";
                """;

        List<String> sqls = extractor.extract(content);

        assertThat(sqls)
                .containsExactly("SELECT * FROM commented_out", "SELECT id FROM active_users");
    }

    @Test
    @DisplayName("长非 SQL 字符串不应该触发正则栈溢出")
    void should_handle_long_non_sql_string_without_stack_overflow() {
        String content = "const text = \"" + "a".repeat(6_000) + "\";";

        assertThatCode(() -> assertThat(extractor.extract(content)).isEmpty())
                .doesNotThrowAnyException();
    }
}
