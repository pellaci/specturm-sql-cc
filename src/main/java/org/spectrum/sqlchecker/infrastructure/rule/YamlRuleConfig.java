package org.spectrum.sqlchecker.infrastructure.rule;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 基于 YAML 的规则配置实现
 * <p>
 * 支持从配置文件加载规则启用/禁用状态和例外配置
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class YamlRuleConfig implements RuleConfig {

    /**
     * 禁用的规则 ID 集合
     */
    private final Set<String> disabledRules = new HashSet<>();

    /**
     * 规则特定的例外配置
     * 规则 ID -> 例外关键字列表
     */
    private final Map<String, List<String>> ruleExceptions = new HashMap<>();

    /**
     * 规则参数配置
     * 规则 ID -> 参数映射
     */
    private final Map<String, Map<String, Object>> ruleParameters = new HashMap<>();

    /**
     * 默认构造函数，使用默认配置
     */
    public YamlRuleConfig() {
        // 尝试从默认位置加载配置
        loadFromDefaultLocations();
    }

    /**
     * 从指定路径加载配置
     */
    public YamlRuleConfig(Path configPath) {
        loadFromPath(configPath);
    }

    /**
     * 从默认位置加载配置
     */
    private void loadFromDefaultLocations() {
        // 检查当前目录下的 sqlchecker.yml
        Path localConfig = Path.of("sqlchecker.yml");
        if (Files.exists(localConfig)) {
            loadFromPath(localConfig);
            return;
        }

        // 检查用户主目录下的 .sqlchecker.yml
        Path homeConfig = Path.of(System.getProperty("user.home"), ".sqlchecker.yml");
        if (Files.exists(homeConfig)) {
            loadFromPath(homeConfig);
            return;
        }

        // 从 classpath 加载默认配置
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sqlchecker-default.yml")) {
            if (is != null) {
                loadFromInputStream(is, "classpath:sqlchecker-default.yml");
            }
        } catch (IOException e) {
            log.debug("No default configuration found, using empty config");
        }
    }

    /**
     * 从指定路径加载配置
     */
    private void loadFromPath(Path configPath) {
        try {
            String content = Files.readString(configPath);
            parseConfig(content, configPath.toString());
            log.info("Loaded rule configuration from: {}", configPath);
        } catch (IOException e) {
            log.warn("Failed to load configuration from {}: {}", configPath, e.getMessage());
        }
    }

    /**
     * 从输入流加载配置
     */
    private void loadFromInputStream(InputStream is, String source) {
        try {
            String content = new String(is.readAllBytes());
            parseConfig(content, source);
            log.debug("Loaded rule configuration from: {}", source);
        } catch (IOException e) {
            log.warn("Failed to load configuration from {}: {}", source, e.getMessage());
        }
    }

    /**
     * 解析配置内容
     */
    @SuppressWarnings("unchecked")
    private void parseConfig(String content, String source) {
        try {
            // 使用简单的键值对解析，避免依赖 SnakeYAML 的复杂 API
            Map<String, Object> config = parseSimpleYaml(content);

            // 解析禁用的规则
            Object disabled = config.get("disabledRules");
            if (disabled instanceof List) {
                ((List<?>) disabled).forEach(ruleId -> {
                    if (ruleId instanceof String) {
                        disabledRules.add((String) ruleId);
                    }
                });
                log.debug("Disabled rules: {}", disabledRules);
            }

            // 解析例外配置
            Object exceptions = config.get("exceptions");
            if (exceptions instanceof Map) {
                ((Map<?, ?>) exceptions).forEach((ruleId, exceptionList) -> {
                    if (ruleId instanceof String && exceptionList instanceof List) {
                        List<String> patterns = new ArrayList<>();
                        for (Object item : (List<?>) exceptionList) {
                            if (item instanceof String) {
                                patterns.add((String) item);
                            }
                        }
                        ruleExceptions.put((String) ruleId, patterns);
                    }
                });
                log.debug("Rule exceptions: {}", ruleExceptions);
            }

            // 解析参数配置
            Object parameters = config.get("parameters");
            if (parameters instanceof Map) {
                ((Map<?, ?>) parameters).forEach((ruleId, paramConfig) -> {
                    if (ruleId instanceof String && paramConfig instanceof Map) {
                        Map<String, Object> params = new HashMap<>();
                        ((Map<?, ?>) paramConfig).forEach((key, value) -> {
                            if (key instanceof String) {
                                params.put((String) key, value);
                            }
                        });
                        ruleParameters.put((String) ruleId, params);
                    }
                });
                log.debug("Rule parameters: {}", ruleParameters);
            }

        } catch (Exception e) {
            log.warn("Failed to parse configuration from {}: {}", source, e.getMessage());
        }
    }

    /**
     * 简单的 YAML 解析器
     * <p>
     * 解析简单的键值对格式，不处理复杂的 YAML 特性
     */
    private Map<String, Object> parseSimpleYaml(String content) {
        Map<String, Object> result = new HashMap<>();
        String currentSection = null;
        String currentKey = null;
        List<Object> currentList = null;

        for (String line : content.split("\n")) {
            String trimmed = line.trim();

            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // 顶层键
            if (!trimmed.startsWith("-") && trimmed.contains(":")) {
                int colonIdx = trimmed.indexOf(':');
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();

                if (value.isEmpty()) {
                    // 这是一个部分开始
                    if (currentList != null) {
                        if (currentSection != null) {
                            result.put(currentSection, currentList);
                        }
                        currentList = null;
                    }
                    currentSection = key;
                    currentKey = null;
                } else if (currentList != null) {
                    // 列表项中的键值对
                    currentList.add(key + ": " + value);
                } else {
                    // 简单的键值对
                    result.put(key, parseValue(value));
                }
            }
            // 列表项
            else if (trimmed.startsWith("- ")) {
                String item = trimmed.substring(2).trim();

                if (currentSection != null && currentKey == null) {
                    // 顶层列表
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.add(item);
                } else if (currentKey != null) {
                    // 嵌套列表
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.add(item);
                }
            }
            // 缩进的键值对（列表项或嵌套对象）
            else if (trimmed.startsWith("  ") && currentSection != null) {
                if (trimmed.contains(":")) {
                    int colonIdx = trimmed.indexOf(':');
                    String key = trimmed.substring(2, colonIdx).trim();
                    String value = trimmed.substring(colonIdx + 1).trim();

                    if ("exceptions".equals(currentSection) || "parameters".equals(currentSection)) {
                        if (value.isEmpty()) {
                            // 这是嵌套对象开始
                            currentKey = key;
                            if (currentList != null) {
                                // 保存之前的列表
                                result.put(currentKey, currentList);
                                currentList = null;
                            }
                        } else {
                            if (currentList != null) {
                                currentList.add(key + ": " + value);
                            }
                        }
                    }
                }
            }
        }

        // 保存最后的列表
        if (currentList != null && currentSection != null) {
            result.put(currentSection, currentList);
        }

        return result;
    }

    /**
     * 解析值
     */
    private Object parseValue(String value) {
        value = value.trim();

        // 布尔值
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        // 数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // 忽略，作为字符串处理
        }

        // 字符串（去除引号）
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    @Override
    public boolean isRuleEnabled(String ruleId) {
        if (ruleId == null) {
            return false;
        }
        return !disabledRules.contains(ruleId);
    }

    @Override
    public boolean isInExceptions(String ruleId, String sql) {
        if (ruleId == null || sql == null) {
            return false;
        }

        List<String> exceptions = ruleExceptions.get(ruleId);
        if (exceptions == null || exceptions.isEmpty()) {
            return false;
        }

        return exceptions.stream().anyMatch(sql::contains);
    }

    @Override
    public String getParameter(String ruleId, String paramKey, String defaultValue) {
        Map<String, Object> params = ruleParameters.get(ruleId);
        if (params == null) {
            return defaultValue;
        }

        Object value = params.get(paramKey);
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    public Map<String, Object> getParameters(String ruleId) {
        Map<String, Object> params = ruleParameters.get(ruleId);
        return params != null ? params : Collections.emptyMap();
    }

    /**
     * 获取禁用的规则集合
     */
    public Set<String> getDisabledRules() {
        return Collections.unmodifiableSet(disabledRules);
    }

    /**
     * 获取例外配置
     */
    public Map<String, List<String>> getRuleExceptions() {
        return Collections.unmodifiableMap(ruleExceptions);
    }

    /**
     * 获取参数配置
     */
    public Map<String, Map<String, Object>> getRuleParameters() {
        return Collections.unmodifiableMap(ruleParameters);
    }
}
