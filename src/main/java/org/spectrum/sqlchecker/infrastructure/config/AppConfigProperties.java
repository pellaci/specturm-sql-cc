package org.spectrum.sqlchecker.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用配置属性
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "sqlchecker")
public class AppConfigProperties {

    /**
     * 扫描配置
     */
    private ScanConfig scan = new ScanConfig();

    /**
     * 分析配置
     */
    private AnalysisConfig analysis = new AnalysisConfig();

    /**
     * 报告配置
     */
    private ReportConfig report = new ReportConfig();

    /**
     * 数据库配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * 扫描配置
     */
    @Data
    public static class ScanConfig {
        /**
         * 默认包含的文件扩展名
         */
        private List<String> defaultIncludes = List.of("**/*.java", "**/*.xml", "**/*.js", "**/*.ts");

        /**
         * 默认排除的目录
         */
        private List<String> defaultExcludes = List.of("**/node_modules/**", "**/target/**", "**/build/**", "**/.git/**");

        /**
         * 最大文件大小（MB）
         */
        private int maxFileSizeMb = 10;

        /**
         * 并行度
         */
        private int parallelism = Runtime.getRuntime().availableProcessors();

        /**
         * 是否启用增量扫描
         */
        private boolean incrementalEnabled = true;
    }

    /**
     * 分析配置
     */
    @Data
    public static class AnalysisConfig {
        /**
         * 是否启用 EXPLAIN 分析
         */
        private boolean explainEnabled = true;

        /**
         * 是否启用专家分析
         */
        private boolean expertEnabled = true;

        /**
         * 分析超时时间（秒）
         */
        private int timeoutSeconds = 30;

        /**
         * 最大分析线程数
         */
        private int maxThreads = 4;
    }

    /**
     * 报告配置
     */
    @Data
    public static class ReportConfig {
        /**
         * 报告主题
         */
        private String theme = "feishu";

        /**
         * 默认输出目录
         */
        private String defaultOutputDir = "./reports";

        /**
         * 是否包含 SQL 细节
         */
        private boolean includeSqlDetails = true;

        /**
         * 是否包含代码片段
         */
        private boolean includeCodeSnippets = true;

        /**
         * 主题颜色配置
         */
        private ThemeColor themeColor = new ThemeColor();
    }

    /**
     * 主题颜色配置
     */
    @Data
    public static class ThemeColor {
        /**
         * 主色（飞书蓝）
         */
        private String primary = "#3370FF";

        /**
         * 成功色
         */
        private String success = "#00B628";

        /**
         * 警告色
         */
        private String warning = "#FF8800";

        /**
         * 错误色
         */
        private String error = "#F54A45";

        /**
         * 信息色
         */
        private String info = "#4C5C76";

        /**
         * 背景色
         */
        private String background = "#F5F6F7";

        /**
         * 卡片背景色
         */
        private String cardBackground = "#FFFFFF";

        /**
         * 边框色
         */
        private String border = "#DEE0E3";

        /**
         * 文本色
         */
        private String text = "#1F2329";

        /**
         * 次要文本色
         */
        private String textSecondary = "#646A73";
    }

    /**
     * 数据库配置
     */
    @Data
    public static class DatabaseConfig {
        /**
         * 数据库连接配置
         */
        private Map<String, ConnectionConfig> connections = new HashMap<>();

        /**
         * 默认连接名称
         */
        private String defaultConnection = "default";
    }

    /**
     * 连接配置
     */
    @Data
    public static class ConnectionConfig {
        /**
         * 连接名称
         */
        private String name;

        /**
         * 数据库类型
         */
        private String type = "mysql";

        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口
         */
        private int port = 3306;

        /**
         * 数据库名称
         */
        private String database;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 编码
         */
        private String charset = "utf8mb4";

        /**
         * 连接参数
         */
        private Map<String, String> parameters = new HashMap<>();

        /**
         * 获取 JDBC URL
         */
        public String getJdbcUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append("jdbc:").append(type).append("://");
            sb.append(host).append(":").append(port);
            if (database != null && !database.isEmpty()) {
                sb.append("/").append(database);
            }
            if (parameters != null && !parameters.isEmpty()) {
                sb.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    if (!first) {
                        sb.append("&");
                    }
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            return sb.toString();
        }
    }
}
