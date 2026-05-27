package org.spectrum.sqlchecker.infrastructure.preprocess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MyBatisSqlFixer 测试")
class MyBatisSqlFixerTest {

    private final MyBatisSqlFixer fixer = new MyBatisSqlFixer();
    private final JSqlParserValidator validator = new JSqlParserValidator();

    @Test
    @DisplayName("应该把 MyBatis #{} 参数归一化为可解析 SQL")
    void should_normalize_hash_parameters_to_parseable_sql() {
        String sql = """
                SELECT id, name
                FROM users
                WHERE status = #{status,jdbcType=INTEGER}
                  AND id IN #{ids}
                """;

        String fixed = fixer.fix(sql);

        assertThat(fixed).doesNotContain("#{");
        assertThat(fixed).contains("status = 1");
        assertThat(fixed).contains("id IN (1)");
        assertThat(validator.validate(fixed).valid()).isTrue();
    }

    @Test
    @DisplayName("应该保守修复 MyBatis 动态片段占位符")
    void should_normalize_template_fragment_placeholders() {
        String sql = "SELECT * FROM users WHERE ? ORDER BY id";

        String fixed = fixer.fix(sql);

        assertThat(fixed).contains("WHERE 1 = 1");
        assertThat(validator.validate(fixed).valid()).isTrue();
    }

    @Test
    @DisplayName("应该支持字符串模板分类，降低 Java 字符串中的 MyBatis 占位符噪声")
    void should_support_placeholder_template_category() {
        assertThat(fixer.supports(SqlCategory.PLACEHOLDER_TEMPLATE)).isTrue();
    }

    @Test
    @DisplayName("不应替换 ${} 文本替换占位符，避免把动态拼接伪装成安全 SQL")
    void should_keep_dollar_placeholders_for_risk_classification() {
        String sql = "SELECT * FROM users ORDER BY ${orderBy}";

        String fixed = fixer.fix(sql);

        assertThat(fixed).contains("${orderBy}");
    }

    @Test
    @DisplayName("应该压缩连续逗号以继续解析并保留上游语法问题证据")
    void should_collapse_duplicate_commas_for_parse_continuation() {
        String sql = """
                INSERT INTO s_sku (id, ware_qd,,, market_price)
                VALUES (1, 1, 1)
                """;

        String fixed = fixer.fix(sql);

        assertThat(fixed).contains("ware_qd, market_price");
        assertThat(fixed).doesNotContain(",,,");
        assertThat(validator.validate(fixed).valid()).isTrue();
    }

    @Test
    @DisplayName("应该将 from 后误写的 and 条件归一化为 where 以继续诊断")
    void should_normalize_from_and_condition_for_parse_continuation() {
        String sql = "SELECT id FROM s_native_config_index AND delete_flag = 0 LIMIT 1";

        String fixed = fixer.fix(sql);

        assertThat(fixed).contains("FROM s_native_config_index WHERE delete_flag = 0");
        assertThat(validator.validate(fixed).valid()).isTrue();
    }
}
