package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.infrastructure.config.YamlConfigLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * EXPLAIN analysis settings loaded from sqlchecker.yml when available.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplainAnalysisSettings {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_ROWS_THRESHOLD = 10000L;

    private final YamlConfigLoader yamlConfigLoader;

    @Getter
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    @Getter
    private long rowsThreshold = DEFAULT_ROWS_THRESHOLD;

    @Getter
    private boolean explainEnabled = true;

    @PostConstruct
    public void load() {
        Map<String, Object> config = loadConfig();
        if (config.isEmpty()) {
            return;
        }

        Map<String, Object> analysis = getMap(config.get("analysis"));
        if (!analysis.isEmpty()) {
            explainEnabled = getBoolean(analysis, "explainEnabled", explainEnabled);
            timeoutSeconds = getInt(analysis, "timeoutSeconds", timeoutSeconds);
            rowsThreshold = getLong(analysis, "rowsThreshold", rowsThreshold);
            rowsThreshold = getLong(analysis, "rows-threshold", rowsThreshold);

            Map<String, Object> thresholds = getMap(analysis.get("thresholds"));
            timeoutSeconds = getInt(thresholds, "explain-timeout", timeoutSeconds);
            rowsThreshold = getLong(thresholds, "scan-rows", rowsThreshold);
            rowsThreshold = getLong(thresholds, "rows-threshold", rowsThreshold);
        }

        log.debug("Explain analysis settings loaded: timeoutSeconds={}, rowsThreshold={}, explainEnabled={}",
                timeoutSeconds, rowsThreshold, explainEnabled);
    }

    private Map<String, Object> loadConfig() {
        Path local = Path.of("sqlchecker.yml");
        if (Files.exists(local)) {
            return yamlConfigLoader.loadExternalConfig(local.toString());
        }

        Path home = Path.of(System.getProperty("user.home"), ".sqlchecker.yml");
        if (Files.exists(home)) {
            return yamlConfigLoader.loadExternalConfig(home.toString());
        }

        return yamlConfigLoader.loadConfig("sqlchecker.yml");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
