package org.spectrum.sqlchecker.infrastructure.database.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostgreSqlAdapter 单元测试")
class PostgreSqlAdapterTest {

    @Test
    @DisplayName("should build read-only explain without analyze")
    void should_build_read_only_explain_without_analyze() {
        PostgreSqlAdapter adapter = new PostgreSqlAdapter();

        String explainSql = adapter.buildExplainSql("SELECT * FROM users WHERE id = 1");

        assertThat(explainSql).startsWith("EXPLAIN ");
        assertThat(explainSql.toUpperCase()).doesNotContain("ANALYZE");
    }
}
