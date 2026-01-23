package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.analysis.StaticAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * StaticAnalysisServiceImpl 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaticAnalysisServiceImpl 单元测试")
class StaticAnalysisServiceImplTest {

    private StaticAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new StaticAnalysisServiceImpl();
    }

    // ==================== analyze 方法测试 ====================

    @Nested
    @DisplayName("analyze 方法测试")
    class AnalyzeTests {

        @Test
        @DisplayName("应该分析干净的 SQL 并返回满分")
        void should_analyze_clean_sql_with_full_score() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id, name FROM users WHERE id = 1 LIMIT 1");

            assertThat(result.getSqlId()).isEqualTo("sql-1");
            assertThat(result.getScore()).isEqualTo(100);
            assertThat(result.getIssues()).isEmpty();
            assertThat(result.getSeverity()).isEqualTo(SeverityLevel.INFO);
        }

        @Test
        @DisplayName("应该检测到 SELECT * 问题")
        void should_detect_select_star() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users LIMIT 10");

            // SELECT * 扣 10 分，由于有 LIMIT 不检测 MISSING_WHERE
            assertThat(result.getScore()).isEqualTo(90);
            assertThat(result.getIssues()).hasSize(1);

            StaticIssue issue = result.getIssues().get(0);
            assertThat(issue.getType()).isEqualTo(IssueType.SELECT_STAR);
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.WARNING);
            assertThat(issue.getMessage()).contains("SELECT *");
            assertThat(issue.getSuggestion()).contains("明确列出");
        }

        @Test
        @DisplayName("应该检测到缺少 WHERE 子句")
        void should_detect_missing_where() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id, name FROM users");

            // 缺少 WHERE 扣 20 分
            assertThat(result.getScore()).isEqualTo(80);
            assertThat(result.getIssues()).hasSize(1);

            StaticIssue issue = result.getIssues().get(0);
            assertThat(issue.getType()).isEqualTo(IssueType.SELECT_WITHOUT_WHERE);
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.WARNING);
            assertThat(issue.getMessage()).contains("WHERE");
            assertThat(issue.getSuggestion()).contains("LIMIT");
        }

        @Test
        @DisplayName("应该检测到 LIKE 以通配符开头")
        void should_detect_like_leading_wildcard() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id, name FROM users WHERE name LIKE '%test' LIMIT 10");

            // LIKE % 扣 30 分
            assertThat(result.getScore()).isEqualTo(70);
            assertThat(result.getIssues()).hasSize(1);

            StaticIssue issue = result.getIssues().get(0);
            assertThat(issue.getType()).isEqualTo(IssueType.LIKE_LEADING_WILDCARD);
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
            assertThat(issue.getMessage()).contains("索引");
        }

        @Test
        @DisplayName("应该检测到 LIKE 以通配符开头（双引号）")
        void should_detect_like_leading_wildcard_double_quote() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id, name FROM users WHERE name LIKE \"%test\" LIMIT 10");

            // LIKE % 扣 30 分
            assertThat(result.getScore()).isEqualTo(70);
            assertThat(result.getIssues()).hasSize(1);

            StaticIssue issue = result.getIssues().get(0);
            assertThat(issue.getType()).isEqualTo(IssueType.LIKE_LEADING_WILDCARD);
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
        }

        @Test
        @DisplayName("应该同时检测多个问题")
        void should_detect_multiple_issues() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users WHERE name LIKE '%test'");

            // 两个问题：SELECT * 和 LIKE %
            assertThat(result.getScore()).isEqualTo(60);
            assertThat(result.getIssues()).hasSize(2);

            List<IssueType> types = result.getIssues().stream().map(StaticIssue::getType).toList();
            assertThat(types).contains(IssueType.SELECT_STAR, IssueType.LIKE_LEADING_WILDCARD);
        }

        @Test
        @DisplayName("应该在有 CRITICAL 问题时返回 CRITICAL 级别")
        void should_return_critical_severity_for_critical_issues() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users WHERE name LIKE '%test'");

            assertThat(result.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
        }

        @Test
        @DisplayName("应该在没有问题时返回 INFO 级别")
        void should_return_info_severity_for_no_issues() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id FROM users WHERE id = 1 LIMIT 1");

            assertThat(result.getSeverity()).isEqualTo(SeverityLevel.INFO);
        }

        @Test
        @DisplayName("应该在有 WARNING 问题时返回 WARNING 级别")
        void should_return_warning_severity_for_warning_issues() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users");

            assertThat(result.getSeverity()).isEqualTo(SeverityLevel.WARNING);
        }

        @Test
        @DisplayName("分数应该不低于 0")
        void should_not_score_below_zero() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users WHERE name LIKE '%test'");

            // 即使扣分超过 100，分数也不应该低于 0
            assertThat(result.getScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("应该设置分析耗时")
        void should_set_analysis_duration() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT id FROM users");

            assertThat(result.getDurationMs()).isEqualTo(50);
        }

        @Test
        @DisplayName("应该处理小写 SQL")
        void should_handle_lowercase_sql() {
            StaticAnalysisDto result = service.analyze("sql-1", "select * from users LIMIT 10");

            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getType()).isEqualTo(IssueType.SELECT_STAR);
        }

        @Test
        @DisplayName("应该处理混合大小写 SQL")
        void should_handle_mixed_case_sql() {
            StaticAnalysisDto result = service.analyze("sql-1", "SeLeCt * FrOm users LIMIT 10");

            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getType()).isEqualTo(IssueType.SELECT_STAR);
        }

        @Test
        @DisplayName("应该不检测带 LIMIT 的无 WHERE 查询")
        void should_not_detect_select_without_where_with_limit() {
            StaticAnalysisDto result = service.analyze("sql-1", "SELECT * FROM users LIMIT 10");

            // 应该只检测 SELECT *，不检测缺少 WHERE
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getType()).isEqualTo(IssueType.SELECT_STAR);
        }

        @Test
        @DisplayName("应该检测不带 SELECT 的 LIKE")
        void should_detect_like_outside_select() {
            StaticAnalysisDto result = service.analyze("sql-1", "UPDATE users SET name = 'test' WHERE name LIKE '%test'");

            // 实际上检测到了 LIKE %（即使不在 SELECT 中）
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getType()).isEqualTo(IssueType.LIKE_LEADING_WILDCARD);
        }
    }

    // ==================== analyzeBatch 方法测试 ====================

    @Nested
    @DisplayName("analyzeBatch 方法测试")
    class AnalyzeBatchTests {

        @Test
        @DisplayName("应该批量分析多个 SQL")
        void should_analyze_multiple_sqls() {
            List<String> sqls = List.of(
                    "SELECT id FROM users WHERE id = 1 LIMIT 1",
                    "SELECT * FROM users LIMIT 10",
                    "SELECT id FROM users WHERE name LIKE '%test' LIMIT 10"
            );

            List<StaticAnalysisDto> results = service.analyzeBatch(sqls);

            assertThat(results).hasSize(3);

            assertThat(results.get(0).getSqlId()).isEqualTo("sql-0");
            assertThat(results.get(0).getScore()).isEqualTo(100);

            assertThat(results.get(1).getSqlId()).isEqualTo("sql-1");
            assertThat(results.get(1).getScore()).isEqualTo(90);

            assertThat(results.get(2).getSqlId()).isEqualTo("sql-2");
            assertThat(results.get(2).getScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("应该处理空的 SQL 列表")
        void should_handle_empty_sql_list() {
            List<StaticAnalysisDto> results = service.analyzeBatch(List.of());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("应该为每个 SQL 生成唯一 ID")
        void should_generate_unique_ids_for_each_sql() {
            List<String> sqls = List.of(
                    "SELECT * FROM users",
                    "SELECT * FROM orders",
                    "SELECT * FROM products"
            );

            List<StaticAnalysisDto> results = service.analyzeBatch(sqls);

            List<String> ids = results.stream().map(StaticAnalysisDto::getSqlId).toList();
            assertThat(ids).containsExactly("sql-0", "sql-1", "sql-2");
        }
    }

    // ==================== isValidSyntax 方法测试 ====================

    @Nested
    @DisplayName("isValidSyntax 方法测试")
    class IsValidSyntaxTests {

        @Test
        @DisplayName("应该验证 SELECT 语法有效")
        void should_validate_select_syntax() {
            assertThat(service.isValidSyntax("SELECT * FROM users")).isTrue();
            assertThat(service.isValidSyntax("select id from users")).isTrue();
            assertThat(service.isValidSyntax("SELECT id, name FROM users WHERE id = 1")).isTrue();
        }

        @Test
        @DisplayName("应该验证 INSERT 语法有效")
        void should_validate_insert_syntax() {
            assertThat(service.isValidSyntax("INSERT INTO users VALUES (1, 'test')")).isTrue();
            assertThat(service.isValidSyntax("insert into users set name = 'test'")).isTrue();
        }

        @Test
        @DisplayName("应该验证 UPDATE 语法有效")
        void should_validate_update_syntax() {
            assertThat(service.isValidSyntax("UPDATE users SET name = 'test'")).isTrue();
            assertThat(service.isValidSyntax("update users set name = 'test' where id = 1")).isTrue();
        }

        @Test
        @DisplayName("应该验证 DELETE 语法有效")
        void should_validate_delete_syntax() {
            assertThat(service.isValidSyntax("DELETE FROM users WHERE id = 1")).isTrue();
            assertThat(service.isValidSyntax("delete from users")).isTrue();
        }

        @Test
        @DisplayName("应该拒绝 null 输入")
        void should_reject_null_input() {
            assertThat(service.isValidSyntax(null)).isFalse();
        }

        @Test
        @DisplayName("应该拒绝空字符串")
        void should_reject_empty_string() {
            assertThat(service.isValidSyntax("")).isFalse();
        }

        @Test
        @DisplayName("应该拒绝空白字符串")
        void should_reject_blank_string() {
            assertThat(service.isValidSyntax("   ")).isFalse();
            assertThat(service.isValidSyntax("\t\n")).isFalse();
        }

        @Test
        @DisplayName("应该拒绝不以关键字开头的语句")
        void should_reject_non_keyword_start() {
            assertThat(service.isValidSyntax("WITH cte AS (SELECT * FROM users) SELECT * FROM cte")).isFalse();
            assertThat(service.isValidSyntax("-- comment\nSELECT * FROM users")).isFalse();
        }
    }
}
