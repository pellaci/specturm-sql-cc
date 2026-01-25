package org.spectrum.sqlchecker.infrastructure.extractor;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.scanner.service.extractor.SqlExtractor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * MyBatis XML SQL 提取器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class MyBatisSqlExtractor implements SqlExtractor {

    private static final Set<String> SQL_STATEMENT_ELEMENTS = Set.of(
            "select", "insert", "update", "delete", "selectkey"
    );

    @Override
    public List<String> extract(String content) throws SqlExtractionException {
        List<String> sqls = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());

            Map<String, String> sqlFragments = extractSqlFragments(doc);
            extractSqlStatements(doc, sqlFragments, sqls);

            log.debug("Extracted {} SQL statements from MyBatis XML", sqls.size());
            return sqls;

        } catch (Exception e) {
            throw new SqlExtractionException("Failed to extract SQL from MyBatis XML", e);
        }
    }

    @Override
    public String getName() {
        return "MyBatisSqlExtractor";
    }

    @Override
    public SqlSourceType getSourceType() {
        return SqlSourceType.MYBATIS;
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType.isXml();
    }

    /**
     * 提取 <sql> 片段
     */
    private Map<String, String> extractSqlFragments(Document doc) {
        Map<String, String> fragments = new HashMap<>();
        Elements fragmentElements = doc.select("sql");
        for (Element element : fragmentElements) {
            String id = element.attr("id");
            if (id == null || id.isBlank()) {
                continue;
            }
            String sql = extractSqlFromElement(element, fragments, 0, false);
            if (!sql.isBlank()) {
                fragments.put(id, sql.trim());
                if (id.contains(".")) {
                    fragments.put(id.substring(id.lastIndexOf('.') + 1), sql.trim());
                }
            }
        }
        return fragments;
    }

    /**
     * 提取 SQL 语句
     */
    private void extractSqlStatements(Document doc, Map<String, String> fragments, List<String> sqls) {
        for (String tag : SQL_STATEMENT_ELEMENTS) {
            Elements elements = doc.select(tag);
            for (Element element : elements) {
                boolean skipSelectKey = !"selectkey".equals(tag);
                String sql = extractSqlFromElement(element, fragments, 0, skipSelectKey).trim();
                if (tag.equals("select") && !startsWithSelect(sql)) {
                    sql = "SELECT " + sql;
                }
                if (isValidSql(sql)) {
                    sqls.add(sql.trim());
                }
            }
        }
    }

    private String extractSqlFromElement(Element element, Map<String, String> fragments, int depth, boolean skipSelectKey) {
        if (depth > 5) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                sb.append(((TextNode) node).getWholeText()).append(" ");
            } else if (node instanceof Element) {
                Element child = (Element) node;
                String tagName = child.tagName().toLowerCase(Locale.ROOT);

                if ("selectkey".equals(tagName) && skipSelectKey) {
                    continue;
                }

                if ("include".equals(tagName)) {
                    String refid = child.attr("refid");
                    String fragment = resolveInclude(refid, fragments, depth + 1);
                    if (fragment != null) {
                        sb.append(fragment).append(" ");
                    } else if (!refid.isBlank()) {
                        sb.append("/* INCLUDE:").append(refid).append(" */ ");
                    }
                } else if ("choose".equals(tagName)) {
                    sb.append(extractSqlFromChoose(child, fragments, depth + 1, skipSelectKey)).append(" ");
                } else if ("foreach".equals(tagName)) {
                    sb.append(extractSqlFromForeach(child)).append(" ");
                } else if ("where".equals(tagName)) {
                    sb.append(extractSqlFromWhere(child, fragments, depth + 1)).append(" ");
                } else if ("set".equals(tagName)) {
                    sb.append(extractSqlFromSet(child, fragments, depth + 1)).append(" ");
                } else if ("trim".equals(tagName)) {
                    sb.append(extractSqlFromTrim(child, fragments, depth + 1)).append(" ");
                } else if ("bind".equals(tagName)) {
                    String name = child.attr("name");
                    String value = child.attr("value");
                    sb.append("/* BIND:").append(name).append("=").append(value).append(" */ ");
                } else if ("if".equals(tagName) || "when".equals(tagName) || "otherwise".equals(tagName)) {
                    sb.append(extractSqlFromElement(child, fragments, depth + 1, skipSelectKey)).append(" ");
                } else {
                    sb.append(extractSqlFromElement(child, fragments, depth + 1, skipSelectKey)).append(" ");
                }
            }
        }

        return sb.toString().trim();
    }

    private String extractSqlFromChoose(Element chooseElement, Map<String, String> fragments, int depth, boolean skipSelectKey) {
        String otherwiseSql = "";
        for (Element child : chooseElement.children()) {
            String tagName = child.tagName().toLowerCase(Locale.ROOT);
            if ("when".equals(tagName)) {
                String sql = extractSqlFromElement(child, fragments, depth + 1, skipSelectKey);
                if (!sql.isBlank()) {
                    return sql.trim();
                }
            } else if ("otherwise".equals(tagName)) {
                otherwiseSql = extractSqlFromElement(child, fragments, depth + 1, skipSelectKey);
            }
        }
        return otherwiseSql.trim();
    }

    private String extractSqlFromForeach(Element foreachElement) {
        String open = foreachElement.attr("open");
        String close = foreachElement.attr("close");
        StringBuilder sb = new StringBuilder();
        if (open != null && !open.isBlank()) {
            sb.append(open);
        }
        sb.append("1");
        if (close != null && !close.isBlank()) {
            sb.append(close);
        }
        return sb.toString().trim();
    }

    private String extractSqlFromWhere(Element whereElement, Map<String, String> fragments, int depth) {
        String inner = extractSqlFromElement(whereElement, fragments, depth + 1, true);
        inner = stripLeadingLogicalOperators(inner);
        if (inner.isBlank()) {
            return "";
        }
        return "WHERE " + inner.trim();
    }

    private String extractSqlFromSet(Element setElement, Map<String, String> fragments, int depth) {
        String inner = extractSqlFromElement(setElement, fragments, depth + 1, true);
        inner = stripLeadingCommas(inner);
        inner = stripTrailingCommas(inner);
        if (inner.isBlank()) {
            return "";
        }
        return "SET " + inner.trim();
    }

    private String extractSqlFromTrim(Element trimElement, Map<String, String> fragments, int depth) {
        String inner = extractSqlFromElement(trimElement, fragments, depth + 1, true);
        String prefixOverrides = trimElement.attr("prefixOverrides");
        String suffixOverrides = trimElement.attr("suffixOverrides");
        inner = applyOverrides(inner, prefixOverrides, suffixOverrides);
        if (inner.isBlank()) {
            return "";
        }
        String prefix = trimElement.attr("prefix");
        String suffix = trimElement.attr("suffix");
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            sb.append(prefix).append(" ");
        }
        sb.append(inner.trim());
        if (suffix != null && !suffix.isBlank()) {
            sb.append(" ").append(suffix);
        }
        return sb.toString().trim();
    }

    private String resolveInclude(String refid, Map<String, String> fragments, int depth) {
        if (refid == null || refid.isBlank()) {
            return null;
        }
        String fragment = fragments.get(refid);
        if (fragment == null && refid.contains(".")) {
            fragment = fragments.get(refid.substring(refid.lastIndexOf('.') + 1));
        }
        if (fragment == null) {
            return null;
        }
        if (fragment.contains("/* INCLUDE:") && depth < 5) {
            return fragment;
        }
        return fragment;
    }

    private boolean startsWithSelect(String sql) {
        if (sql == null) {
            return false;
        }
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT") || upper.startsWith("WITH") || upper.startsWith("EXPLAIN");
    }

    private String stripLeadingLogicalOperators(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceFirst("(?i)^\\s*(AND|OR)\\s+", "");
    }

    private String stripLeadingCommas(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceFirst("^\\s*,\\s*", "");
    }

    private String stripTrailingCommas(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceFirst("\\s*,\\s*$", "");
    }

    private String applyOverrides(String sql, String prefixOverrides, String suffixOverrides) {
        String result = sql == null ? "" : sql.trim();
        if (prefixOverrides != null && !prefixOverrides.isBlank()) {
            for (String token : prefixOverrides.split("\\|")) {
                String trimmedToken = token.trim();
                if (!trimmedToken.isEmpty()
                        && result.regionMatches(true, 0, trimmedToken, 0, trimmedToken.length())) {
                    result = result.substring(trimmedToken.length()).trim();
                    break;
                }
            }
        }
        if (suffixOverrides != null && !suffixOverrides.isBlank()) {
            for (String token : suffixOverrides.split("\\|")) {
                String trimmedToken = token.trim();
                if (!trimmedToken.isEmpty()
                        && result.toUpperCase().endsWith(trimmedToken.toUpperCase())) {
                    result = result.substring(0, result.length() - trimmedToken.length()).trim();
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 检查是否是有效的 SQL
     */
    private boolean isValidSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String trimmed = sql.trim();
        if ("SELECT".equalsIgnoreCase(trimmed)) {
            return false;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT")
                || upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")
                || upper.startsWith("CREATE")
                || upper.startsWith("ALTER")
                || upper.startsWith("DROP")
                || upper.startsWith("TRUNCATE")
                || upper.startsWith("REPLACE")
                || upper.startsWith("WITH")
                || upper.startsWith("CALL");
    }
}
