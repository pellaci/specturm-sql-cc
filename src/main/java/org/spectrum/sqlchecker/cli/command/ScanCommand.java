package org.spectrum.sqlchecker.cli.command;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.infrastructure.rule.SqlRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 扫描命令
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
@Command(
        name = "scan",
        description = "Scan a codebase for SQL statements",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "Path to the codebase to scan", defaultValue = ".")
    private String path;

    @Option(names = {"-o", "--output"}, description = "Output file path", defaultValue = "reports/sql-checker-report.html")
    private String outputPath;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    // SQL 匹配模式 - 匹配包含 SQL 关键字的字符串
    private static final Pattern STRING_SQL_PATTERN = Pattern.compile(
            "\"([^\"$]{0,}?\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|REPLACE)\\b[^\"$]{4,}?)\"",
            Pattern.CASE_INSENSITIVE
    );

    // MyBatis XML 中的 SQL 元素标签
    private static final Set<String> SQL_ELEMENT_TAGS = Set.of(
            "select", "insert", "update", "delete", "sql"
    );

    private int totalFiles = 0;
    private int javaFiles = 0;
    private int xmlFiles = 0;
    private int sqlFound = 0;
    private int sqlParsed = 0;
    private final List<SqlIssue> allIssues = new ArrayList<>();
    private final Map<String, Integer> issueSummary = new HashMap<>();

    @Override
    public Integer call() throws Exception {
        long startTime = System.currentTimeMillis();

        printHeader();

        // 执行扫描
        List<File> files = findFiles(new File(path));
        totalFiles = files.size();
        System.out.println("Found " + totalFiles + " files (" + javaFiles + " Java, " + xmlFiles + " XML)");
        System.out.println();

        for (File file : files) {
            if (file.getName().endsWith(".java")) {
                scanJavaFile(file);
            } else if (file.getName().endsWith(".xml")) {
                scanXmlFile(file);
            }
        }

        printResults(startTime);
        return 0;
    }

    private void printHeader() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              SQL Checker - SQL Quality Scanner              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println("  Path: " + new File(path).getAbsolutePath());
        System.out.println();
    }

    private List<File> findFiles(File dir) throws IOException {
        List<File> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".xml"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .filter(p -> !p.toString().contains("/node_modules/"))
                    .forEach(p -> {
                        files.add(p.toFile());
                        if (p.toString().endsWith(".java")) {
                            javaFiles++;
                        } else if (p.toString().endsWith(".xml")) {
                            xmlFiles++;
                        }
                    });
        }
        return files;
    }

    private void scanJavaFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            String relativePath = getRelativePath(file);

            List<String> candidates = extractSqlFromJava(content);

            for (String sql : candidates) {
                sqlFound++;
                analyzeSql(relativePath, sql);
            }

        } catch (IOException e) {
            if (verbose) {
                System.out.println("  [ERROR] Failed to read: " + file.getName());
            }
        }
    }

    private void scanXmlFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            String relativePath = getRelativePath(file);

            // 检查是否是 MyBatis mapper 文件
            if (!isMyBatisMapperFile(content)) {
                return;
            }

            List<String> sqls = extractSqlFromMyBatisXml(content);

            for (String sql : sqls) {
                sqlFound++;
                analyzeSql(relativePath, sql);
            }

        } catch (IOException e) {
            if (verbose) {
                System.out.println("  [ERROR] Failed to read: " + file.getName());
            }
        } catch (Exception e) {
            if (verbose) {
                System.out.println("  [ERROR] Failed to parse XML: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否是 MyBatis mapper 文件
     */
    private boolean isMyBatisMapperFile(String content) {
        return content.contains("<mapper") &&
               (content.contains("mybatis.org") || content.contains("ibatis.apache.org"));
    }

    /**
     * 从 MyBatis XML 文件中提取 SQL
     */
    private List<String> extractSqlFromMyBatisXml(String content) {
        List<String> sqls = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());

            // 查找所有 SQL 元素标签
            for (String tag : SQL_ELEMENT_TAGS) {
                Elements elements = doc.select(tag);
                for (Element element : elements) {
                    String sql = extractSqlFromElement(element);
                    if (!sql.isBlank() && looksLikeSql(sql)) {
                        sqls.add(sql.trim());
                    }
                }
            }

        } catch (Exception e) {
            if (verbose) {
                System.out.println("  [XML_PARSE_FAIL] " + e.getMessage());
            }
        }

        return sqls;
    }

    /**
     * 从 XML 元素中提取 SQL 文本
     */
    private String extractSqlFromElement(Element element) {
        StringBuilder sb = new StringBuilder();

        // 获取直接文本内容
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                sb.append(((TextNode) node).getWholeText().trim()).append(" ");
            } else if (node instanceof Element) {
                Element child = (Element) node;
                String tagName = child.tagName().toLowerCase();

                // 处理 include 标签 - 引用其他 SQL 片段
                if ("include".equals(tagName)) {
                    String refid = child.attr("refid");
                    if (!refid.isEmpty()) {
                        sb.append("/* INCLUDE:").append(refid).append(" */ ");
                    }
                }
                // 处理 if, choose, foreach 等动态 SQL 标签
                else if ("if".equals(tagName) || "choose".equals(tagName) ||
                        "when".equals(tagName) || "otherwise".equals(tagName) ||
                        "foreach".equals(tagName) || "where".equals(tagName) ||
                        "set".equals(tagName) || "trim".equals(tagName)) {
                    // 递归提取子元素内容
                    sb.append(extractSqlFromElement(child)).append(" ");
                }
                // 处理 bind 标签
                else if ("bind".equals(tagName)) {
                    String name = child.attr("name");
                    String value = child.attr("value");
                    sb.append("/* BIND:").append(name).append("=").append(value).append(" */ ");
                }
            }
        }

        return sb.toString().trim();
    }

    private List<String> extractSqlFromJava(String content) {
        List<String> candidates = new ArrayList<>();
        Matcher matcher = STRING_SQL_PATTERN.matcher(content);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (looksLikeSql(sql)) {
                candidates.add(sql);
            }
        }
        return candidates;
    }

    private boolean looksLikeSql(String sql) {
        String upper = sql.toUpperCase();
        return upper.startsWith("SELECT") || upper.startsWith("INSERT") ||
                upper.startsWith("UPDATE") || upper.startsWith("DELETE") ||
                upper.startsWith("CREATE") || upper.startsWith("ALTER") ||
                upper.startsWith("DROP") || upper.startsWith("TRUNCATE");
    }

    private void analyzeSql(String fileName, String sql) {
        sqlParsed++;
        List<SqlIssue> issues = new ArrayList<>();

        try {
            // 使用 JSqlParser 解析 SQL
            Statement stmt = CCJSqlParserUtil.parse(sql);

            // 优先使用 SqlRuleEngine 进行检查（如果可用）
            SqlRuleEngine engine = getRuleEngine();
            if (engine != null) {
                issues.addAll(checkWithRuleEngine(fileName, sql, engine));
            } else {
                // 降级到硬编码检查
                issues.addAll(checkSelectStar(fileName, sql, stmt));
                issues.addAll(checkMissingWhere(fileName, sql, stmt));
                issues.addAll(checkLikeLeadingWildcard(fileName, sql, stmt));
                issues.addAll(checkOrderByWithoutLimit(fileName, sql, stmt));
                issues.addAll(checkJoinType(fileName, sql, stmt));
                issues.addAll(checkSubquery(fileName, sql, stmt));
            }

        } catch (JSQLParserException e) {
            // 解析失败，可能是 SQL 片段或包含占位符
            if (verbose) {
                System.out.println("  [PARSE_FAIL] " + fileName + ": " + truncate(sql, 60));
            }
            // 仍然进行简单的模式检查
            issues.addAll(checkSimplePatterns(fileName, sql));
        }

        if (!issues.isEmpty()) {
            allIssues.addAll(issues);
            if (verbose || allIssues.size() <= 20) {
                for (SqlIssue issue : issues) {
                    printIssue(issue);
                }
            }
        }
    }

    /**
     * 获取 SqlRuleEngine（从 Spring 容器）
     */
    private SqlRuleEngine getRuleEngine() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(SqlRuleEngine.class);
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("[DEBUG] Failed to get SqlRuleEngine: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 使用 SqlRuleEngine 检查 SQL
     */
    private List<SqlIssue> checkWithRuleEngine(String fileName, String sql, SqlRuleEngine engine) {
        List<SqlIssue> issues = new ArrayList<>();

        try {
            List<RuleIssue> ruleIssues = engine.analyze(fileName + "_sql", sql);

            for (RuleIssue ruleIssue : ruleIssues) {
                issues.add(convertToSqlIssue(fileName, sql, ruleIssue));
            }

        } catch (Exception e) {
            if (verbose) {
                System.out.println("  [RULE_ENGINE_ERROR] " + e.getMessage());
            }
        }

        return issues;
    }

    /**
     * 将 RuleIssue 转换为 SqlIssue
     */
    private SqlIssue convertToSqlIssue(String fileName, String sql, RuleIssue ruleIssue) {
        return new SqlIssue(
                fileName,
                sql,
                ruleIssue.getRuleId().toUpperCase().replace("-", "_"),
                severityToString(ruleIssue.getSeverity()),
                ruleIssue.getMessage() + (ruleIssue.getSuggestion() != null ? " 💡 " + ruleIssue.getSuggestion() : "")
        );
    }

    /**
     * 将 SeverityLevel 转换为字符串
     */
    private String severityToString(SeverityLevel severity) {
        if (severity == null) {
            return "WARNING";
        }
        return severity.name();
    }

    // ========== 检查规则 ==========

    private List<SqlIssue> checkSelectStar(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.contains("SELECT *")) {
            if (upper.matches(".*SELECT\\s+\\*\\s+FROM.*")) {
                issues.add(new SqlIssue(fileName, sql, "SELECT_STAR",
                        "CRITICAL", "使用了 SELECT *，会查询所有列"));
            }
        }
        return issues;
    }

    private List<SqlIssue> checkMissingWhere(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.startsWith("SELECT") && !upper.contains("WHERE")) {
            if (!upper.contains("LIMIT") && !upper.contains("COUNT(*)")
                    && !upper.matches(".*SELECT\\s+COUNT\\s*\\(.*\\).*FROM.*")) {
                issues.add(new SqlIssue(fileName, sql, "MISSING_WHERE",
                        "WARNING", "SELECT 语句缺少 WHERE 条件可能导致全表扫描"));
            }
        }
        return issues;
    }

    private List<SqlIssue> checkLikeLeadingWildcard(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.matches(".*LIKE\\s+['\\\"]%.*")) {
            issues.add(new SqlIssue(fileName, sql, "LIKE_LEADING_WILDCARD",
                    "CRITICAL", "LIKE 以通配符开头无法使用索引"));
        }
        return issues;
    }

    private List<SqlIssue> checkOrderByWithoutLimit(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.contains("ORDER BY") && !upper.contains("LIMIT")
                && !upper.matches(".*ORDER\\s+BY\\s+\\d+.*")) {
            issues.add(new SqlIssue(fileName, sql, "ORDERBY_WITHOUT_LIMIT",
                        "WARNING", "ORDER BY 缺少 LIMIT 可能导致大结果集排序"));
        }
        return issues;
    }

    private List<SqlIssue> checkJoinType(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        // 检测隐式 JOIN：FROM 后面有逗号连接的多个表，但没有显式 JOIN 关键字
        if (upper.startsWith("SELECT") && upper.contains(",") && !upper.contains("JOIN")) {
            if (upper.matches(".*FROM\\s+[\\w]+\\s*(?:\\s+\\w+)?\\s*,\\s*[\\w]+.*")) {
                issues.add(new SqlIssue(fileName, sql, "IMPLICIT_JOIN",
                        "WARNING", "检测到隐式 JOIN (逗号连接表)，建议使用显式 JOIN"));
            }
        }
        return issues;
    }

    private List<SqlIssue> checkSubquery(String fileName, String sql, Statement stmt) {
        List<SqlIssue> issues = new ArrayList<>();
        // 简单检查子查询
        long parenthesesCount = sql.chars().filter(ch -> ch == '(').count();
        if (parenthesesCount > 2) {
            issues.add(new SqlIssue(fileName, sql, "COMPLEX_SUBQUERY",
                    "INFO", "检测到复杂的嵌套结构，建议拆分"));
        }
        return issues;
    }

    private List<SqlIssue> checkSimplePatterns(String fileName, String sql) {
        List<SqlIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();

        if (upper.contains("SELECT *")) {
            issues.add(new SqlIssue(fileName, sql, "SELECT_STAR",
                    "CRITICAL", "使用了 SELECT *"));
        }
        if (upper.matches(".*LIKE\\s+['\\\"]%.*")) {
            issues.add(new SqlIssue(fileName, sql, "LIKE_LEADING_WILDCARD",
                    "CRITICAL", "LIKE 以通配符开头"));
        }

        return issues;
    }

    // ========== 输出 ==========

    private void printIssue(SqlIssue issue) {
        String icon = switch (issue.severity) {
            case "CRITICAL" -> "🔴";
            case "WARNING" -> "🟡";
            default -> "🔵";
        };
        System.out.println("  " + icon + " [" + issue.type + "] " + issue.fileName);
        System.out.println("    SQL: " + truncate(issue.sql, 80));
        System.out.println("    💡 " + issue.message);
        System.out.println();
    }

    private void printResults(long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Scan Results");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  Files scanned: " + totalFiles + " (" + javaFiles + " Java, " + xmlFiles + " XML)");
        System.out.println("  SQL found:     " + sqlFound);
        System.out.println("  SQL parsed:    " + sqlParsed);
        System.out.println("  Duration:      " + duration + "ms");
        System.out.println();

        // 统计问题
        int critical = 0;
        int warning = 0;
        int info = 0;
        for (SqlIssue issue : allIssues) {
            issueSummary.put(issue.type, issueSummary.getOrDefault(issue.type, 0) + 1);
            switch (issue.severity) {
                case "CRITICAL" -> critical++;
                case "WARNING" -> warning++;
                default -> info++;
            }
        }

        System.out.println("Issue Summary:");
        System.out.println("  🔴 Critical:  " + critical);
        System.out.println("  🟡 Warning:   " + warning);
        System.out.println("  🔵 Info:      " + info);
        if (critical == 0 && warning == 0 && info == 0) {
            System.out.println("  ✅ No issues found!");
        }
        System.out.println();

        // 问题类型统计
        if (!issueSummary.isEmpty()) {
            System.out.println("Issue Types:");
            issueSummary.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> System.out.println("  - " + e.getKey() + ": " + e.getValue()));
            System.out.println();
        }

        if (allIssues.size() > 20) {
            System.out.println("  ... and " + (allIssues.size() - 20) + " more issues");
            System.out.println("  Use -v for verbose output");
        }
        System.out.println();

        // 生成 HTML 报告
        generateHtmlReport(duration, critical, warning, info);
    }

    /**
     * 生成 HTML 报告
     */
    private void generateHtmlReport(long duration, int critical, int warning, int info) {
        try {
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }

            String html = buildHtmlReport(duration, critical, warning, info);
            Files.writeString(outputFile.toPath(), html);

            System.out.println("📄 HTML report generated: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to generate HTML report: " + e.getMessage());
        }
    }

    /**
     * 构建 HTML 报告内容
     */
    private String buildHtmlReport(long duration, int critical, int warning, int info) {
        StringBuilder sb = new StringBuilder();

        // HTML 头部
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"zh-CN\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>SQL Checker Report</title>\n");
        sb.append("    <style>\n");
        sb.append(getCssStyles());
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        // 顶部统计
        sb.append("    <div class=\"container\">\n");
        sb.append("        <header class=\"header\">\n");
        sb.append("            <h1>🔍 SQL Quality Checker Report</h1>\n");
        sb.append("            <p class=\"subtitle\">Scan Path: ").append(escapeHtml(new File(path).getAbsolutePath())).append("</p>\n");
        sb.append("        </header>\n");

        // 统计卡片
        sb.append("        <div class=\"stats-grid\">\n");
        sb.append("            <div class=\"stat-card\">\n");
        sb.append("                <div class=\"stat-value\">").append(totalFiles).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Files Scanned</div>\n");
        sb.append("                <div class=\"stat-detail\">").append(javaFiles).append(" Java + ").append(xmlFiles).append(" XML</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card\">\n");
        sb.append("                <div class=\"stat-value\">").append(sqlFound).append("</div>\n");
        sb.append("                <div class=\"stat-label\">SQL Found</div>\n");
        sb.append("                <div class=\"stat-detail\">").append(sqlParsed).append(" Parsed</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-critical\">\n");
        sb.append("                <div class=\"stat-value\">").append(critical).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Critical</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-warning\">\n");
        sb.append("                <div class=\"stat-value\">").append(warning).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Warning</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-info\">\n");
        sb.append("                <div class=\"stat-value\">").append(info).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Info</div>\n");
        sb.append("            </div>\n");
        sb.append("        </div>\n");

        // 问题列表
        if (!allIssues.isEmpty()) {
            sb.append("        <div class=\"issues-section\">\n");
            sb.append("            <h2>Issues Found</h2>\n");

            // 按严重程度分组
            Map<String, List<SqlIssue>> bySeverity = new LinkedHashMap<>();
            bySeverity.put("CRITICAL", new ArrayList<>());
            bySeverity.put("WARNING", new ArrayList<>());
            bySeverity.put("INFO", new ArrayList<>());

            for (SqlIssue issue : allIssues) {
                bySeverity.get(issue.severity).add(issue);
            }

            // 渲染问题
            for (Map.Entry<String, List<SqlIssue>> entry : bySeverity.entrySet()) {
                List<SqlIssue> issues = entry.getValue();
                if (!issues.isEmpty()) {
                    String severityClass = entry.getKey().toLowerCase();
                    sb.append("            <div class=\"issue-group issue-").append(severityClass).append("\">\n");
                    sb.append("                <h3>").append(entry.getKey()).append(" (").append(issues.size()).append(")</h3>\n");
                    for (SqlIssue issue : issues) {
                        sb.append("                <div class=\"issue-item\">\n");
                        sb.append("                    <div class=\"issue-header\">\n");
                        sb.append("                    <span class=\"issue-type\">").append(issue.type()).append("</span>\n");
                        sb.append("                    <span class=\"issue-file\">").append(escapeHtml(issue.fileName())).append("</span>\n");
                        sb.append("                    </div>\n");
                        sb.append("                    <pre class=\"issue-sql\">").append(escapeHtml(issue.sql())).append("</pre>\n");
                        sb.append("                    <div class=\"issue-message\">💡 ").append(escapeHtml(issue.message())).append("</div>\n");
                        sb.append("                </div>\n");
                    }
                    sb.append("            </div>\n");
                }
            }

            sb.append("        </div>\n");
        } else {
            sb.append("        <div class=\"success-message\">\n");
            sb.append("            <div class=\"success-icon\">✅</div>\n");
            sb.append("            <h2>No Issues Found!</h2>\n");
            sb.append("            <p>All SQL statements passed the quality checks.</p>\n");
            sb.append("        </div>\n");
        }

        // 问题类型统计
        if (!issueSummary.isEmpty()) {
            sb.append("        <div class=\"summary-section\">\n");
            sb.append("            <h2>Issue Types</h2>\n");
            sb.append("            <div class=\"type-list\">\n");
            issueSummary.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> {
                        sb.append("                <div class=\"type-item\">\n");
                        sb.append("                    <span class=\"type-name\">").append(e.getKey()).append("</span>\n");
                        sb.append("                    <span class=\"type-count\">").append(e.getValue()).append("</span>\n");
                        sb.append("                </div>\n");
                    });
            sb.append("            </div>\n");
            sb.append("        </div>\n");
        }

        // 页脚
        sb.append("        <footer class=\"footer\">\n");
        sb.append("            <p>Generated by SQL Checker v1.0.0</p>\n");
        sb.append("            <p>Scan Duration: ").append(duration).append("ms | ").append(new java.util.Date()).append("</p>\n");
        sb.append("        </footer>\n");
        sb.append("    </div>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    /**
     * 获取 CSS 样式
     */
    private String getCssStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                padding: 20px;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
            }
            .header {
                background: white;
                border-radius: 16px;
                padding: 30px;
                margin-bottom: 24px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                text-align: center;
            }
            .header h1 {
                font-size: 28px;
                color: #333;
                margin-bottom: 8px;
            }
            .subtitle {
                color: #666;
                font-size: 14px;
            }
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                gap: 16px;
                margin-bottom: 24px;
            }
            .stat-card {
                background: white;
                border-radius: 12px;
                padding: 20px;
                text-align: center;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .stat-critical { background: linear-gradient(135deg, #ff6b6b, #ee5a6f); color: white; }
            .stat-warning { background: linear-gradient(135deg, #feca57, #ff9f43); color: white; }
            .stat-info { background: linear-gradient(135deg, #54a0ff, #2e86de); color: white; }
            .stat-value {
                font-size: 32px;
                font-weight: bold;
                margin-bottom: 4px;
            }
            .stat-label {
                font-size: 14px;
                opacity: 0.9;
            }
            .stat-detail {
                font-size: 12px;
                opacity: 0.7;
                margin-top: 4px;
            }
            .issues-section, .summary-section {
                background: white;
                border-radius: 16px;
                padding: 24px;
                margin-bottom: 24px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .issues-section h2, .summary-section h2 {
                font-size: 20px;
                color: #333;
                margin-bottom: 20px;
            }
            .issue-group {
                margin-bottom: 20px;
                padding: 16px;
                border-radius: 12px;
            }
            .issue-group.critical { background: #fee2e2; }
            .issue-group.warning { background: #fef3c7; }
            .issue-group.info { background: #dbeafe; }
            .issue-group h3 {
                font-size: 16px;
                margin-bottom: 12px;
                color: #333;
            }
            .issue-item {
                background: white;
                border-radius: 8px;
                padding: 16px;
                margin-bottom: 12px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            }
            .issue-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 12px;
            }
            .issue-type {
                background: #333;
                color: white;
                padding: 4px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 500;
            }
            .issue-file {
                color: #666;
                font-size: 13px;
                font-family: 'Monaco', 'Menlo', monospace;
            }
            .issue-sql {
                background: #1e1e1e;
                color: #d4d4d4;
                padding: 12px;
                border-radius: 6px;
                font-size: 13px;
                overflow-x: auto;
                margin-bottom: 8px;
            }
            .issue-message {
                color: #666;
                font-size: 14px;
            }
            .type-list {
                display: flex;
                flex-direction: column;
                gap: 8px;
            }
            .type-item {
                display: flex;
                justify-content: space-between;
                padding: 12px 16px;
                background: #f8f9fa;
                border-radius: 8px;
            }
            .type-name {
                color: #333;
                font-weight: 500;
            }
            .type-count {
                background: #667eea;
                color: white;
                padding: 2px 10px;
                border-radius: 12px;
                font-size: 14px;
            }
            .success-message {
                background: white;
                border-radius: 16px;
                padding: 60px 20px;
                text-align: center;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .success-icon {
                font-size: 64px;
                margin-bottom: 16px;
            }
            .success-message h2 {
                color: #10b981;
                margin-bottom: 8px;
            }
            .success-message p {
                color: #666;
            }
            .footer {
                text-align: center;
                color: white;
                opacity: 0.9;
                font-size: 14px;
            }
            """;
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String getRelativePath(File file) {
        String basePath = new File(path).getAbsolutePath();
        String fullPath = file.getAbsolutePath();
        if (fullPath.startsWith(basePath)) {
            return fullPath.substring(basePath.length() + 1);
        }
        return file.getName();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private record SqlIssue(
            String fileName,
            String sql,
            String type,
            String severity,
            String message
    ) {}
}
