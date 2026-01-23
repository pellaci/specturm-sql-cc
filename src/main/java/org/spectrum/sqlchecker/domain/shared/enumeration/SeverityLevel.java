package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 严重等级
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum SeverityLevel {

    /**
     * 严重 - 必须修复
     */
    CRITICAL,

    /**
     * 警告 - 建议修复
     */
    WARNING,

    /**
     * 提示 - 可以优化
     */
    INFO;

    /**
     * 获取等级值（用于排序比较）
     */
    public int getValue() {
        return switch (this) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }

    /**
     * 从多个等级中获取最高等级
     */
    public static SeverityLevel max(SeverityLevel... levels) {
        if (levels == null || levels.length == 0) {
            return INFO;
        }

        SeverityLevel max = INFO;
        for (SeverityLevel level : levels) {
            if (level != null && level.getValue() > max.getValue()) {
                max = level;
            }
        }
        return max;
    }
}
