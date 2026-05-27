package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExplainSqlPreprocessor 单元测试
 */
@DisplayName("ExplainSqlPreprocessor 单元测试")
class ExplainSqlPreprocessorTest {

    private ExplainSqlPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new ExplainSqlPreprocessor();
    }

    @Nested
    @DisplayName("skip 规则")
    class SkipTests {

        @Test
        @DisplayName("遇到 ${} 占位符应跳过 EXPLAIN")
        void should_skip_when_contains_dollar_placeholder() {
            String sql = "SELECT * FROM users ORDER BY ${orderBy}";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isTrue();
            assertThat(result.reason()).contains("${}");
        }

        @Test
        @DisplayName("非 DML/查询语句应跳过 EXPLAIN")
        void should_skip_non_dml_statement() {
            String sql = "CREATE TABLE users (id INT)";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isTrue();
            assertThat(result.reason()).contains("仅对可执行 EXPLAIN");
        }

        @Test
        @DisplayName("变更语句应跳过 EXPLAIN，避免误执行")
        void should_skip_mutating_statement() {
            String sql = "UPDATE users SET name = #{name} WHERE id = #{id}";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isTrue();
            assertThat(result.reason()).contains("只读");
        }
    }

    @Nested
    @DisplayName("占位符替换")
    class PlaceholderReplacementTests {

        @Test
        @DisplayName("IN #{...} 应替换为 IN (1)")
        void should_replace_in_placeholder() {
            String sql = "SELECT * FROM users WHERE id IN #{ids}";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isFalse();
            assertThat(normalizeSql(result.sql())).contains("IN (1)");
        }

        @Test
        @DisplayName("#{...} 应替换为 1")
        void should_replace_hash_placeholder() {
            String sql = "SELECT * FROM users WHERE name = #{name} AND id = #{id}";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isFalse();
            assertThat(normalizeSql(result.sql())).contains("name = 1");
            assertThat(normalizeSql(result.sql())).contains("id = 1");
        }

        @Test
        @DisplayName("INCLUDE/BIND 注释应被处理")
        void should_handle_include_and_bind_comments() {
            String sql = "SELECT /* INCLUDE:Base_Column_List */ FROM users /* BIND:x=1 */";

            ExplainSqlPreprocessor.PreprocessResult result = preprocessor.preprocess(sql);

            assertThat(result.skipped()).isFalse();
            assertThat(result.sql()).doesNotContain("BIND:");
            assertThat(normalizeSql(result.sql())).contains("SELECT * FROM users");
        }
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
