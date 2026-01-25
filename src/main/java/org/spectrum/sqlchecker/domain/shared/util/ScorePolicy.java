package org.spectrum.sqlchecker.domain.shared.util;

/**
 * 得分计算策略
 *
 * @author Spectrum SQL Checker
 * @since 1.1.0
 */
public final class ScorePolicy {

    public static final int BASE_SCORE = 100;
    public static final int CRITICAL_DEDUCTION = 30;
    public static final int WARNING_DEDUCTION = 15;
    public static final int INFO_DEDUCTION = 5;

    private ScorePolicy() {
    }

    public static String formulaDescription() {
        return "平均得分=各 SQL 得分平均值；单条得分=基础分 "
                + BASE_SCORE
                + " - 严重x"
                + CRITICAL_DEDUCTION
                + " - 警告x"
                + WARNING_DEDUCTION
                + " - 提示x"
                + INFO_DEDUCTION
                + "（按触发规则累积扣分，最低为 0）";
    }
}
