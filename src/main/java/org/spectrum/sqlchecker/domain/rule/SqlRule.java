package org.spectrum.sqlchecker.domain.rule;

import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;

import java.util.Set;

/**
 * SQL 规则接口
 * <p>
 * 所有 SQL 检测规则必须实现此接口。每个规则定义一个或多个对 AST 节点的检查逻辑。
 * <p>
 * 规则类需要使用 {@link RuleMeta} 注解标注规则元数据，并通过 Spring 的组件扫描机制注册。
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public interface SqlRule {

    /**
     * 获取规则元数据
     * <p>
     * 从类的 {@link RuleMeta} 注解中读取元信息
     *
     * @return 规则元数据注解
     */
    RuleMeta getMeta();

    /**
     * 初始化规则
     * <p>
     * 在规则注册后调用，可用于加载规则配置或初始化资源
     *
     * @param config 规则配置，可能为 null
     */
    default void initialize(RuleConfig config) {
        // 默认空实现
    }

    /**
     * 访问 AST 节点
     * <p>
     * 当规则引擎遍历 AST 时，遇到规则支持的节点类型会调用此方法。
     * 规则实现者应在此方法中实现具体的检测逻辑。
     *
     * @param node    AST 节点实例
     * @param context 规则执行上下文，用于报告问题
     */
    void visit(Object node, RuleContext context);

    /**
     * 获取支持的节点类型
     * <p>
     * 返回此规则需要检查的 JSqlParser AST 节点类型集合。
     * 规则引擎只会对支持的节点类型调用 {@link #visit} 方法。
     *
     * @return 支持的节点类型集合
     */
    Set<Class<?>> supportedNodeTypes();

    /**
     * 获取规则优先级
     * <p>
     * 值越小优先级越高，用于控制规则执行顺序。
     *
     * @return 优先级值，默认 100
     */
    default int getPriority() {
        return 100;
    }
}
