package org.spectrum.sqlchecker.infrastructure.scan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SqlScanSupport 单元测试
 */
@DisplayName("SqlScanSupport 单元测试")
class SqlScanSupportTest {

    @Test
    @DisplayName("looksLikeSql should recognize common statements")
    void looks_like_sql() {
        assertThat(SqlScanSupport.looksLikeSql("SELECT * FROM users")).isTrue();
        assertThat(SqlScanSupport.looksLikeSql("UPDATE users SET name='a'" )).isTrue();
        assertThat(SqlScanSupport.looksLikeSql("delete from users" )).isTrue();
        assertThat(SqlScanSupport.looksLikeSql("update:sku:enlandy:")).isFalse();
    }

    @Test
    @DisplayName("extractSqlFromJavaWithLocations should return candidates")
    void extract_sql_from_java() {
        String content = "String sql = \"SELECT * FROM users WHERE id = 1\";";
        List<SqlScanSupport.SqlCandidate> candidates = SqlScanSupport.extractSqlFromJavaWithLocations(content);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).sql()).contains("SELECT * FROM users");
        assertThat(candidates.get(0).line()).isEqualTo(1);
    }

    @Test
    @DisplayName("abstractSql should normalize literals")
    void abstract_sql() {
        String normalized = SqlScanSupport.abstractSql("select * from t where id = 10 and name = 'abc'");
        assertThat(normalized).contains("?");
        assertThat(normalized).doesNotContain("10");
        assertThat(normalized).doesNotContain("abc");
    }
}
