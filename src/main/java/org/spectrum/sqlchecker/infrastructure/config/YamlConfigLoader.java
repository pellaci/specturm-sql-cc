package org.spectrum.sqlchecker.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * YAML 配置加载器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class YamlConfigLoader {

    private final Yaml yaml = new Yaml();

    /**
     * 加载配置文件
     *
     * @param configPath 配置文件路径
     * @return 配置 Map
     */
    public Map<String, Object> loadConfig(String configPath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (input == null) {
                log.info("Config file not found: {}, using defaults", configPath);
                return new HashMap<>();
            }
            return yaml.load(input);
        } catch (Exception e) {
            log.error("Failed to load config: {}", configPath, e);
            return new HashMap<>();
        }
    }

    /**
     * 加载外部配置文件
     *
     * @param filePath 文件路径
     * @return 配置 Map
     */
    public Map<String, Object> loadExternalConfig(String filePath) {
        try (InputStream input = new java.io.FileInputStream(filePath)) {
            return yaml.load(input);
        } catch (Exception e) {
            log.error("Failed to load external config: {}", filePath, e);
            return new HashMap<>();
        }
    }
}
