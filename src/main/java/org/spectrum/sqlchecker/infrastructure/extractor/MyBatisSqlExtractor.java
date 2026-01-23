package org.spectrum.sqlchecker.infrastructure.extractor;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.scanner.service.extractor.SqlExtractor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis XML SQL 提取器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class MyBatisSqlExtractor implements SqlExtractor {

    private static final List<String> SQL_ELEMENTS = List.of(
            "select", "insert", "update", "delete",
            "selectKey", "sql", "include"
    );

    @Override
    public List<String> extract(String content) throws SqlExtractionException {
        List<String> sqls = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(content));
            org.w3c.dom.Document doc = builder.parse(is);

            doc.getDocumentElement().normalize();

            // 提取各种 SQL 元素
            extractSqlFromElements(doc, sqls);

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
     * 从 XML 元素中提取 SQL
     */
    private void extractSqlFromElements(org.w3c.dom.Node node, List<String> sqls) {
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String nodeName = node.getNodeName().toLowerCase();

            if (SQL_ELEMENTS.contains(nodeName)) {
                String sql = extractTextContent(element);
                if (isValidSql(sql)) {
                    sqls.add(sql.trim());
                }
            }
        }

        // 递归处理子节点
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            extractSqlFromElements(children.item(i), sqls);
        }
    }

    /**
     * 提取文本内容（处理 CDATA）
     */
    private String extractTextContent(Element element) {
        StringBuilder sb = new StringBuilder();

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE
                    || child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                sb.append(child.getTextContent());
            } else if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                // 处理嵌套元素（如 include, if 等）
                sb.append(extractTextContent((Element) child));
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * 检查是否是有效的 SQL
     */
    private boolean isValidSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String upper = sql.toUpperCase().trim();
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
