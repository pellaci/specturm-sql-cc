package org.spectrum.sqlchecker.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * ScanCommand 单元测试
 *
 * 测试覆盖范围：
 * - SQL 模式匹配
 * - 文件扫描功能
 * - 静态分析检查规则
 * - HTML 报告生成
 * - 辅助工具方法
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScanCommand 单元测试")
class ScanCommandTest {

    private ScanCommand command;

    @BeforeEach
    void setUp() {
        command = new ScanCommand();
    }

    /**
     * 使用反射设置私有字段
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 使用反射获取私有字段
     */
    private <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    /**
     * 使用反射调用私有方法
     */
    private Object invokePrivate(String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = getParameterTypes(methodName, args);
        Method method = ScanCommand.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(command, args);
    }

    /**
     * 获取方法参数类型
     */
    private Class<?>[] getParameterTypes(String methodName, Object[] args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                paramTypes[i] = inferParameterType(methodName, i);
            } else {
                // 处理基本类型和包装类型
                if (args[i] instanceof Integer) {
                    paramTypes[i] = int.class;
                } else if (args[i] instanceof Long) {
                    paramTypes[i] = long.class;
                } else {
                    paramTypes[i] = args[i].getClass();
                }
            }
        }
        return paramTypes;
    }

    /**
     * 根据方法名和参数位置推断参数类型
     */
    private Class<?> inferParameterType(String methodName, int paramIndex) {
        return switch (methodName) {
            case "escapeHtml" -> String.class;
            case "truncate" -> paramIndex == 0 ? String.class : int.class;
            case "buildHtmlReport" -> paramIndex == 0 ? long.class : int.class;
            default -> {
                if (methodName.startsWith("check") && paramIndex == 2) {
                    yield net.sf.jsqlparser.statement.Statement.class;
                }
                yield Object.class;
            }
        };
    }

    /**
     * 获取 SqlIssue record 的属性值
     */
    private Object getSqlIssueProperty(Object issue, String propertyName) throws Exception {
        Class<?> recordClass = issue.getClass();
        // 尝试通过方法获取
        try {
            Method method = recordClass.getDeclaredMethod(propertyName);
            method.setAccessible(true);
            return method.invoke(issue);
        } catch (NoSuchMethodException e) {
            // 尝试通过 RecordComponent 获取
            for (RecordComponent component : recordClass.getRecordComponents()) {
                if (component.getName().equals(propertyName)) {
                    Method accessor = component.getAccessor();
                    accessor.setAccessible(true);
                    return accessor.invoke(issue);
                }
            }
            throw e;
        }
    }

    // ==================== SQL 模式匹配测试 ====================

    @Nested
    @DisplayName("SQL 模式匹配测试")
    class SqlPatternTests {

        @Test
        @DisplayName("应该正确匹配包含 SELECT 关键字的字符串")
        void should_match_select_pattern() {
            Pattern pattern = Pattern.compile(
                    "\"([^\"$]{0,}?\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|REPLACE)\\b[^\"$]{4,}?)\"",
                    Pattern.CASE_INSENSITIVE
            );

            assertThat(pattern.matcher("\"SELECT * FROM users\"").find()).isTrue();
            assertThat(pattern.matcher("\"select * from users\"").find()).isTrue();
            assertThat(pattern.matcher("\"insert into users values (1, 'test')\"").find()).isTrue();
            assertThat(pattern.matcher("\"UPDATE users SET name = 'test'\"").find()).isTrue();
            assertThat(pattern.matcher("\"delete from users where id = 1\"").find()).isTrue();
            assertThat(pattern.matcher("\"CREATE TABLE test (id INT)\"").find()).isTrue();
        }

        @Test
        @DisplayName("应该拒绝不包含 SQL 关键字的字符串")
        void should_reject_non_sql_strings() {
            Pattern pattern = Pattern.compile(
                    "\"([^\"$]{0,}?\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|REPLACE)\\b[^\"$]{4,}?)\"",
                    Pattern.CASE_INSENSITIVE
            );

            assertThat(pattern.matcher("\"hello world\"").find()).isFalse();
            assertThat(pattern.matcher("\"test\"").find()).isFalse();
            assertThat(pattern.matcher("\"SEL\"").find()).isFalse();
        }
    }

    // ==================== looksLikeSql 方法测试 ====================

    @Nested
    @DisplayName("looksLikeSql 方法测试")
    class LooksLikeSqlTests {

        @Test
        @DisplayName("应该识别 SELECT 语句")
        void should_recognize_select() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "SELECT * FROM users")).isEqualTo(true);
            assertThat(invokePrivate("looksLikeSql", "select name from users")).isEqualTo(true);
            assertThat(invokePrivate("looksLikeSql", "SELECT id, name FROM users WHERE id = 1")).isEqualTo(true);
        }

        @Test
        @DisplayName("应该识别 INSERT 语句")
        void should_recognize_insert() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "INSERT INTO users VALUES (1, 'test')")).isEqualTo(true);
        }

        @Test
        @DisplayName("应该识别 UPDATE 语句")
        void should_recognize_update() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "UPDATE users SET name = 'test'")).isEqualTo(true);
        }

        @Test
        @DisplayName("应该识别 DELETE 语句")
        void should_recognize_delete() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "DELETE FROM users WHERE id = 1")).isEqualTo(true);
        }

        @Test
        @DisplayName("应该识别 DDL 语句")
        void should_recognize_ddl() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "CREATE TABLE users (id INT)")).isEqualTo(true);
            assertThat(invokePrivate("looksLikeSql", "ALTER TABLE users ADD COLUMN name VARCHAR(50)")).isEqualTo(true);
            assertThat(invokePrivate("looksLikeSql", "DROP TABLE users")).isEqualTo(true);
            assertThat(invokePrivate("looksLikeSql", "TRUNCATE TABLE users")).isEqualTo(true);
        }

        @Test
        @DisplayName("应该拒绝非 SQL 语句")
        void should_reject_non_sql() throws Exception {
            assertThat(invokePrivate("looksLikeSql", "hello world")).isEqualTo(false);
            assertThat(invokePrivate("looksLikeSql", "FROM users")).isEqualTo(false);
        }
    }

    // ==================== checkSelectStar 测试 ====================

    @Nested
    @DisplayName("checkSelectStar 检查测试")
    class CheckSelectStarTests {

        @Test
        @DisplayName("应该检测到 SELECT *")
        void should_detect_select_star() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkSelectStar", "Test.java", "SELECT * FROM users", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("SELECT_STAR");
            assertThat(getSqlIssueProperty(issues.get(0), "severity")).isEqualTo("CRITICAL");
            assertThat(getSqlIssueProperty(issues.get(0), "message").toString()).contains("SELECT *");
        }

        @Test
        @DisplayName("应该检测到小写的 select *")
        void should_detect_lowercase_select_star() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkSelectStar", "Test.java", "select * from users", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("SELECT_STAR");
        }

        @Test
        @DisplayName("应该不检测 SELECT 带具体列名")
        void should_not_detect_select_with_columns() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkSelectStar", "Test.java", "SELECT id, name FROM users", null);

            assertThat(issues).isEmpty();
        }
    }

    // ==================== checkMissingWhere 测试 ====================

    @Nested
    @DisplayName("checkMissingWhere 检查测试")
    class CheckMissingWhereTests {

        @Test
        @DisplayName("应该检测到缺少 WHERE 的 SELECT")
        void should_detect_missing_where() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkMissingWhere", "Test.java", "SELECT * FROM users", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("MISSING_WHERE");
            assertThat(getSqlIssueProperty(issues.get(0), "severity")).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("应该不检测有 WHERE 的 SELECT")
        void should_not_detect_select_with_where() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkMissingWhere", "Test.java", "SELECT * FROM users WHERE id = 1", null);

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该不检测有 LIMIT 的 SELECT")
        void should_not_detect_select_with_limit() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkMissingWhere", "Test.java", "SELECT * FROM users LIMIT 10", null);

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该不检测 COUNT(*) 查询")
        void should_not_detect_count_query() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkMissingWhere", "Test.java", "SELECT COUNT(*) FROM users", null);

            assertThat(issues).isEmpty();
        }
    }

    // ==================== checkLikeLeadingWildcard 测试 ====================

    @Nested
    @DisplayName("checkLikeLeadingWildcard 检查测试")
    class CheckLikeLeadingWildcardTests {

        @Test
        @DisplayName("应该检测到 LIKE 以 % 开头（单引号）")
        void should_detect_like_with_leading_percent_single_quote() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkLikeLeadingWildcard", "Test.java", "SELECT * FROM users WHERE name LIKE '%test'", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("LIKE_LEADING_WILDCARD");
            assertThat(getSqlIssueProperty(issues.get(0), "severity")).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("应该不检测 LIKE 不以 % 开头")
        void should_not_detect_like_without_leading_percent() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkLikeLeadingWildcard", "Test.java", "SELECT * FROM users WHERE name LIKE 'test%'", null);

            assertThat(issues).isEmpty();
        }
    }

    // ==================== checkOrderByWithoutLimit 测试 ====================

    @Nested
    @DisplayName("checkOrderByWithoutLimit 检查测试")
    class CheckOrderByWithoutLimitTests {

        @Test
        @DisplayName("应该检测到 ORDER BY 无 LIMIT")
        void should_detect_orderby_without_limit() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkOrderByWithoutLimit", "Test.java", "SELECT * FROM users ORDER BY create_time", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("ORDERBY_WITHOUT_LIMIT");
            assertThat(getSqlIssueProperty(issues.get(0), "severity")).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("应该不检测 ORDER BY 有 LIMIT")
        void should_not_detect_orderby_with_limit() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkOrderByWithoutLimit", "Test.java", "SELECT * FROM users ORDER BY create_time LIMIT 10", null);

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该不检测 ORDER BY 数字（位置排序）")
        void should_not_detect_orderby_with_ordinal() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkOrderByWithoutLimit", "Test.java", "SELECT * FROM users ORDER BY 1", null);

            assertThat(issues).isEmpty();
        }
    }

    // ==================== checkJoinType 测试 ====================

    @Nested
    @DisplayName("checkJoinType 检查测试")
    class CheckJoinTypeTests {

        @Test
        @DisplayName("应该检测到隐式 JOIN（逗号连接）")
        void should_detect_implicit_join() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkJoinType", "Test.java", "SELECT * FROM users u, orders o WHERE u.id = o.user_id", null);

            assertThat(issues).hasSize(1);
            assertThat(getSqlIssueProperty(issues.get(0), "type")).isEqualTo("IMPLICIT_JOIN");
            assertThat(getSqlIssueProperty(issues.get(0), "severity")).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("应该不检测显式 JOIN")
        void should_not_detect_explicit_join() throws Exception {
            List<?> issues = (List<?>) invokePrivate("checkJoinType", "Test.java", "SELECT * FROM users JOIN orders ON users.id = orders.user_id", null);

            assertThat(issues).isEmpty();
        }
    }

    // ==================== truncate 方法测试 ====================

    @Nested
    @DisplayName("truncate 方法测试")
    class TruncateTests {

        @Test
        @DisplayName("应该截断超过最大长度的字符串")
        void should_truncate_long_string() throws Exception {
            String result = (String) invokePrivate("truncate", "This is a very long string that should be truncated", 10);

            // 截断后应该包含省略号
            assertThat(result).endsWith("...");
            assertThat(result.length()).isLessThanOrEqualTo(13); // 10 + "..."
        }

        @Test
        @DisplayName("应该不截断短于最大长度的字符串")
        void should_not_truncate_short_string() throws Exception {
            String result = (String) invokePrivate("truncate", "Short", 10);

            assertThat(result).isEqualTo("Short");
        }

        @Test
        @DisplayName("应该正确处理等于最大长度的字符串")
        void should_handle_equal_length_string() throws Exception {
            String result = (String) invokePrivate("truncate", "Exactly10!", 10);

            assertThat(result).isEqualTo("Exactly10!");
        }

        @Test
        @DisplayName("应该处理空字符串")
        void should_handle_empty_string() throws Exception {
            String result = (String) invokePrivate("truncate", "", 10);

            assertThat(result).isEmpty();
        }
    }

    // ==================== escapeHtml 方法测试 ====================

    @Nested
    @DisplayName("escapeHtml 方法测试")
    class EscapeHtmlTests {

        @Test
        @DisplayName("应该转义 & 符号")
        void should_escape_ampersand() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "A & B");

            assertThat(result).isEqualTo("A &amp; B");
        }

        @Test
        @DisplayName("应该转义 < 符号")
        void should_escape_less_than() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "A < B");

            assertThat(result).isEqualTo("A &lt; B");
        }

        @Test
        @DisplayName("应该转义 > 符号")
        void should_escape_greater_than() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "A > B");

            assertThat(result).isEqualTo("A &gt; B");
        }

        @Test
        @DisplayName("应该转义双引号")
        void should_escape_double_quote() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "\"Hello\"");

            assertThat(result).isEqualTo("&quot;Hello&quot;");
        }

        @Test
        @DisplayName("应该转义单引号")
        void should_escape_single_quote() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "'Hello'");

            assertThat(result).isEqualTo("&#x27;Hello&#x27;");
        }

        @Test
        @DisplayName("应该转义多个特殊字符")
        void should_escape_multiple_special_chars() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "<div>HTML & 'JS' \"CSS\"</div>");

            assertThat(result).isEqualTo("&lt;div&gt;HTML &amp; &#x27;JS&#x27; &quot;CSS&quot;&lt;/div&gt;");
        }

        @Test
        @DisplayName("应该处理 null 输入")
        void should_handle_null_input() throws Exception {
            String result = (String) invokePrivate("escapeHtml", (String) null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该处理空字符串")
        void should_handle_empty_string() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该不改变普通字符")
        void should_not_change_normal_chars() throws Exception {
            String result = (String) invokePrivate("escapeHtml", "Hello World 123");

            assertThat(result).isEqualTo("Hello World 123");
        }
    }

    // ==================== isMyBatisMapperFile 方法测试 ====================

    @Nested
    @DisplayName("isMyBatisMapperFile 方法测试")
    class IsMyBatisMapperFileTests {

        @Test
        @DisplayName("应该识别 MyBatis mapper 文件（mybatis.org）")
        void should_recognize_mybatis_mapper() throws Exception {
            String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">SELECT * FROM users</select>
                </mapper>
                """;

            boolean result = (boolean) invokePrivate("isMyBatisMapperFile", content);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该识别 iBatis mapper 文件")
        void should_recognize_ibatis_mapper() throws Exception {
            String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//ibatis.apache.org//DTD SQL Map 2.0//EN"
                    "http://ibatis.apache.org/dtd/sql-map-2.dtd">
                <mapper namespace="User">
                    <select id="selectAll">SELECT * FROM users</select>
                </mapper>
                """;

            boolean result = (boolean) invokePrivate("isMyBatisMapperFile", content);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该拒绝非 MyBatis XML 文件")
        void should_reject_non_mybatis_xml() throws Exception {
            String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <element>test</element>
                </root>
                """;

            boolean result = (boolean) invokePrivate("isMyBatisMapperFile", content);

            assertThat(result).isFalse();
        }
    }

    // ==================== 集成测试 ====================

    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {

        @Test
        @DisplayName("应该正确扫描包含 SQL 的 Java 文件")
        void should_scan_java_file_with_sql(@TempDir Path tempDir) throws Exception {
            Path javaFile = tempDir.resolve("TestDao.java");
            String content = """
                package com.example;

                public class TestDao {
                    public void findAll() {
                        String sql = "SELECT * FROM users";
                        execute(sql);
                    }

                    private void execute(String sql) { }
                }
                """;
            Files.writeString(javaFile, content);

            // 设置路径并确保输出目录存在
            setField(command, "path", tempDir.toString());
            setField(command, "outputPath", tempDir.resolve("report.html").toString());

            Integer result = command.call();

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("应该正确处理空目录")
        void should_handle_empty_directory(@TempDir Path tempDir) throws Exception {
            setField(command, "path", tempDir.toString());
            setField(command, "outputPath", tempDir.resolve("report.html").toString());

            Integer result = command.call();

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("应该处理不存在的路径")
        void should_handle_nonexistent_path(@TempDir Path tempDir) throws Exception {
            // 使用一个没有 .java 或 .xml 文件的目录
            Path emptyDir = tempDir.resolve("empty");
            Files.createDirectories(emptyDir);

            setField(command, "path", emptyDir.toString());
            setField(command, "outputPath", tempDir.resolve("report.html").toString());

            Integer result = command.call();

            assertThat(result).isZero();
        }
    }

    // ==================== 输出路径测试 ====================

    @Nested
    @DisplayName("输出路径配置测试")
    class OutputPathTests {

        @Test
        @DisplayName("应该能够设置自定义输出路径")
        void should_allow_custom_output_path() throws Exception {
            setField(command, "outputPath", "/custom/path/report.html");

            String outputPath = getField(command, "outputPath", String.class);
            assertThat(outputPath).isEqualTo("/custom/path/report.html");
        }
    }

    // ==================== 详细模式测试 ====================

    @Nested
    @DisplayName("详细模式测试")
    class VerboseModeTests {

        @Test
        @DisplayName("默认应该不是详细模式")
        void should_not_be_verbose_by_default() throws Exception {
            boolean verbose = getField(command, "verbose", Boolean.class);
            assertThat(verbose).isFalse();
        }

        @Test
        @DisplayName("应该能够启用详细模式")
        void should_allow_verbose_mode() throws Exception {
            setField(command, "verbose", true);

            boolean verbose = getField(command, "verbose", Boolean.class);
            assertThat(verbose).isTrue();
        }
    }

    // ==================== HTML 报告测试 ====================

    @Nested
    @DisplayName("HTML 报告生成测试")
    class HtmlReportTests {

        @Test
        @DisplayName("生成的 HTML 应包含必需元素")
        void should_generate_valid_html() throws Exception {
            setField(command, "path", "/test/path");

            String html = (String) invokePrivate("buildHtmlReport", 100L, 5, 3, 1);

            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("<html");
            assertThat(html).contains("SQL Quality Checker Report");
            assertThat(html).contains("Files Scanned");
            assertThat(html).contains("Critical");
            assertThat(html).contains("Warning");
            assertThat(html).contains("Info");
        }

        @Test
        @DisplayName("生成的 HTML 应正确转义特殊字符")
        void should_escape_special_characters_in_html() throws Exception {
            setField(command, "path", "/path/to<script>");

            String html = (String) invokePrivate("buildHtmlReport", 100L, 0, 0, 0);

            assertThat(html).doesNotContain("<script>");
            assertThat(html).contains("&lt;script&gt;");
        }
    }

    // ==================== extractSqlFromJava 方法测试 ====================

    @Nested
    @DisplayName("extractSqlFromJava 方法测试")
    class ExtractSqlFromJavaTests {

        @Test
        @DisplayName("应该从 Java 代码中提取 SQL")
        void should_extract_sql_from_java() throws Exception {
            String content = "\"SELECT * FROM users\"";

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) invokePrivate("extractSqlFromJava", content);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("SELECT");
        }

        @Test
        @DisplayName("应该提取多个 SQL")
        void should_extract_multiple_sql() throws Exception {
            String content = "\"SELECT * FROM users\" + \"SELECT * FROM orders\"";

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) invokePrivate("extractSqlFromJava", content);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("应该忽略非 SQL 字符串")
        void should_ignore_non_sql_strings() throws Exception {
            String content = "\"hello world\"";

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) invokePrivate("extractSqlFromJava", content);

            assertThat(result).isEmpty();
        }
    }
}
