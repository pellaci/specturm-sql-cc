# SQL Checker 规则引擎架构设计文档

## 一、当前架构问题分析

### 1.1 现状问题

```java
// 当前 ScanCommand 中的硬编码检查
private List<SqlIssue> checkSelectStar(String fileName, String sql, Statement stmt) {
    List<SqlIssue> issues = new ArrayList<>();
    String upper = sql.toUpperCase();  // ❌ 字符串匹配，未使用 AST
    if (upper.contains("SELECT *")) {
        issues.add(new SqlIssue(fileName, sql, "SELECT_STAR", "CRITICAL", "..."));
    }
    return issues;
}
```

**核心问题：**

| 问题 | 描述 | 影响 |
|------|------|------|
| 硬编码规则 | 规则逻辑分散在 ScanCommand 的各个方法中 | 添加规则需修改主类 |
| 字符串匹配 | 使用 `toUpperCase()` + `contains()` 检测 | 误报率高，无法精准定位 |
| 未充分利用 AST | `Statement stmt` 参数传入但未使用 | 无法做语法级别的分析 |
| 违反开闭原则 | 扩展需修改已有代码 | 维护成本高 |
| 无规则配置 | 规则固定，无法禁用/调整 | 灵活性差 |

### 1.2 改进目标

1. **规则插件化**：规则独立于主代码，可动态加载
2. **AST 驱动**：基于 JSqlParser AST 进行精准分析
3. **声明式配置**：通过配置文件控制规则启用/禁用
4. **可扩展性**：第三方可编写自定义规则

---

## 二、架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         SQL Checker                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐     ┌──────────────────┐                 │
│  │   ScanCommand    │     │  RuleEngine     │                 │
│  │   (入口层)        │────▶│  (协调层)        │                 │
│  └──────────────────┘     └────────┬─────────┘                 │
│                                    │                              │
│                                    ▼                              │
│                          ┌──────────────────────┐               │
│                          │  RuleRegistry       │               │
│                          │  (规则注册)         │               │
│                          └────────┬─────────────┘               │
│                                   │                             │
│              ┌────────────────────┼────────────────────┐        │
│              ▼                    ▼                    ▼        │
│      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐  │
│      │ BuiltinRules │      │ PluginRules │      │ RuleConfig  │  │
│      │ (内置规则)   │      │ (插件规则)   │      │ (规则配置)   │  │
│      └─────────────┘      └─────────────┘      └─────────────┘  │
│                                                                       │
└───────────────────────────────────────────────────────────────────┘
```

### 2.2 核心接口设计

#### 2.2.1 规则接口

```java
package org.spectrum.sqlchecker.domain.rule;

/**
 * SQL 规则接口
 *
 * 每个规则定义一个或多个对 AST 节点的检查逻辑
 */
public interface SqlRule {

    /**
     * 获取规则元数据
     */
    RuleMeta getMeta();

    /**
     * 初始化规则（可用于加载配置）
     */
    default void initialize(RuleConfig config) {}

    /**
     * 访问 AST 节点
     *
     * @param node AST 节点
     * @param context 执行上下文
     */
    void visit(Object node, RuleContext context);

    /**
     * 支持的节点类型
     */
    Set<Class<? extends Node>> supportedNodeTypes();
}
```

#### 2.2.2 规则元数据注解

```java
package org.spectrum.sqlchecker.domain.rule;

import java.lang.annotation.*;

/**
 * 规则元数据注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuleMeta {

    /**
     * 规则唯一标识（如 "select-star", "missing-where"）
     */
    String id();

    /**
     * 规则名称
     */
    String name();

    /**
     * 规则描述
     */
    String description();

    /**
     * 规则类型
     */
    RuleType type() default RuleType.PROBLEM;

    /**
     * 严重级别
     */
    Severity severity() default Severity.MAJOR;

    /**
     * 规则标签（用于分类）
     */
    String[] tags() default {};

    /**
     * 规则类别
     */
    RuleCategory category() default RuleCategory.BEST_PRACTICE;

    /**
     * 是否已废弃
     */
    boolean deprecated() default false;
}
```

#### 2.2.3 枚举定义

```java
// 规则类型
public enum RuleType {
    PROBLEM,    // 代码问题（必须修复）
    SUGGESTION, // 建议（可选优化）
    LAYOUT      // 格式（暂时不实现）
}

// 严重级别
public enum Severity {
    CRITICAL,  // 严重问题
    MAJOR,     // 重要问题
    MINOR,     // 次要问题
    INFO       // 信息提示
}

// 规则类别
public enum RuleCategory {
    PERFORMANCE,    // 性能问题
    SECURITY,       // 安全问题
    MAINTENABILITY, // 可维护性
    BEST_PRACTICE,  // 最佳实践
    STYLE           // 代码风格
}
```

---

## 三、AST 访问者设计

### 3.1 基于 JSqlParser Visitor 的规则引擎

```java
package org.spectrum.sqlchecker.infrastructure.rule;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.operators.conditional.*;
import net.sf.jsqlparser.schema.Table;
import org.spectrum.sqlchecker.domain.rule.*;
import org.springframework.stereotype.Component;

/**
 * SQL 规则引擎
 *
 * 使用 JSqlParser Visitor 模式遍历 AST，调用注册的规则
 */
@Component
public class SqlRuleEngine {

    private final RuleRegistry ruleRegistry;

    public SqlRuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * 分析 SQL 并返回问题列表
     */
    public List<RuleIssue> analyze(String sqlId, String sql) {
        RuleContext context = new RuleContext(sqlId, sql);

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);

            // 创建规则访问者
            RuleVisitor visitor = new RuleVisitor(context, ruleRegistry);

            // 遍历 AST
            stmt.accept(visitor);

        } catch (JSQLParserException e) {
            // 解析失败，记录但不中断
            context.addParsingError(e.getMessage());
        }

        return context.getIssues();
    }

    /**
     * 批量分析
     */
    public Map<String, List<RuleIssue>> analyzeBatch(Map<String, String> sqls) {
        Map<String, List<RuleIssue>> results = new HashMap<>();
        sqls.forEach((id, sql) -> results.put(id, analyze(id, sql)));
        return results;
    }
}
```

### 3.2 统一规则访问者

```java
/**
 * 统一规则访问者
 *
 * 将 JSqlParser 的各种 Visitor 接口统一，分发到注册的规则
 */
public class RuleVisitor implements StatementVisitor, SelectVisitor,
        ExpressionVisitor, FromItemVisitor, SelectItemVisitor {

    private final RuleContext context;
    private final RuleRegistry registry;

    public RuleVisitor(RuleContext context, RuleRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    // ========== Statement Visitor ==========

    @Override
    public void visit(Select select) {
        // 通知所有关注 Select 的规则
        registry.getRulesForNode(Select.class)
            .forEach(rule -> rule.visit(select, context));

        select.getSelectBody().accept(this);
    }

    @Override
    public void visit(Insert insert) {
        registry.getRulesForNode(Insert.class)
            .forEach(rule -> rule.visit(insert, context));
    }

    // ... 其他 Statement 类型

    // ========== Select Visitor ==========

    @Override
    public void visit(PlainSelect plainSelect) {
        // 通知关注 PlainSelect 的规则
        registry.getRulesForNode(PlainSelect.class)
            .forEach(rule -> rule.visit(plainSelect, context));

        // 遍历子节点
        if (plainSelect.getFromItem() != null) {
            plainSelect.getFromItem().accept(this);
        }

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                join.accept(this);
            }
        }

        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(this);
        }

        if (plainSelect.getSelectItems() != null) {
            for (SelectItem item : plainSelect.getSelectItems()) {
                item.accept(this);
            }
        }

        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement element : plainSelect.getOrderByElements()) {
                element.getExpression().accept(this);
            }
        }
    }

    // ========== Expression Visitor ==========

    @Override
    public void visit(AndExpression expr) {
        registry.getRulesForNode(AndExpression.class)
            .forEach(rule -> rule.visit(expr, context));

        if (expr.getLeftExpression() != null) {
            expr.getLeftExpression().accept(this);
        }
        if (expr.getRightExpression() != null) {
            expr.getRightExpression().accept(this);
        }
    }

    @Override
    public void visit(LikeExpression expr) {
        registry.getRulesForNode(LikeExpression.class)
            .forEach(rule -> rule.visit(expr, context));
    }

    @Override
    public void visit(Function function) {
        registry.getRulesForNode(Function.class)
            .forEach(rule -> rule.visit(function, context));
    }

    // ... 其他 Expression 类型

    // ========== FromItem Visitor ==========

    @Override
    public void visit(Table table) {
        registry.getRulesForNode(Table.class)
            .forEach(rule -> rule.visit(table, context));
    }

    @Override
    public void visit(SubJoin subJoin) {
        registry.getRulesForNode(SubJoin.class)
            .forEach(rule -> rule.visit(subJoin, context));
    }

    // ========== SelectItem Visitor ==========

    @Override
    public void visit(AllColumns allColumns) {
        registry.getRulesForNode(AllColumns.class)
            .forEach(rule -> rule.visit(allColumns, context));
    }

    @Override
    public void visit(SelectExpressionItem item) {
        if (item.getExpression() != null) {
            item.getExpression().accept(this);
        }
    }
}
```

---

## 四、规则实现示例

### 4.1 SELECT * 规则（使用 AST）

```java
package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.statement.select.*;
import org.spectrum.sqlchecker.domain.rule.*;
import org.springframework.stereotype.Component;

/**
 * 检测 SELECT * 规则
 *
 * 使用 AST 精准检测，避免字符串匹配的误报
 */
@Component
@RuleMeta(
    id = "select-star",
    name = "Avoid SELECT *",
    description = "使用 SELECT * 会查询所有列，可能造成不必要的 I/O 和网络开销",
    type = RuleType.PROBLEM,
    severity = Severity.CRITICAL,
    tags = {"performance", "readability"},
    category = RuleCategory.PERFORMANCE
)
public class SelectStarRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<? extends Node>> supportedNodeTypes() {
        return Set.of(PlainSelect.class, AllColumns.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof PlainSelect select) {
            checkSelect(select, context);
        } else if (node instanceof AllColumns allColumns) {
            reportAllColumns(allColumns, context);
        }
    }

    private void checkSelect(PlainSelect select, RuleContext context) {
        // 检查是否包含 AllColumns (即 *)
        boolean hasStar = select.getSelectItems().stream()
                .anyMatch(AllColumns.class::isInstance);

        if (hasStar) {
            SelectItem starItem = select.getSelectItems().stream()
                    .filter(AllColumns.class::isInstance)
                    .findFirst()
                    .orElse(null);

            context.reportIssue(RuleIssue.builder()
                    .ruleId(getMeta().id())
                    .severity(getMeta().severity())
                    .location(RuleLocation.builder()
                            .sqlId(context.getSqlId())
                            .lineNumber(getLineNumber(starItem))
                            .columnNumber(getColumnNumber(starItem))
                            .build())
                    .message(getMeta().description())
                    .suggestion("明确列出需要的列名，减少数据传输")
                    .build());
        }
    }
}
```

### 4.2 LIKE 通配符规则（使用 AST）

```java
package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import org.spectrum.sqlchecker.domain.rule.*;
import org.springframework.stereotype.Component;

/**
 * 检测 LIKE 以通配符开头
 *
 * 使用 AST 的 LikeExpression 节点精确检测
 */
@Component
@RuleMeta(
    id = "like-leading-wildcard",
    name = "Avoid LIKE with leading wildcard",
    description = "LIKE 以通配符开头无法使用索引，会导致全表扫描",
    type = RuleType.PROBLEM,
    severity = Severity.CRITICAL,
    tags = {"performance"},
    category = RuleCategory.PERFORMANCE
)
public class LikeLeadingWildcardRule implements SqlRule {

    @Override
    public Set<Class<? extends Node>> supportedNodeTypes() {
        return Set.of(LikeExpression.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof LikeExpression likeExpr) {
            checkLikeExpression(likeExpr, context);
        }
    }

    private void checkLikeExpression(LikeExpression likeExpr, RuleContext context) {
        Expression left = likeExpr.getLeftExpression();
        Expression right = likeExpr.getRightExpression();

        // 检查右侧模式是否以通配符开头
        if (right instanceof StringValue) {
            StringValue pattern = (StringValue) right;
            String value = pattern.getValue();

            if (value.startsWith("%") && !value.equals("%")) {
                // 排除纯 "%" 的情况（可能是特殊业务需求）
                context.reportIssue(RuleIssue.builder()
                        .ruleId(getMeta().id())
                        .severity(getMeta().severity())
                        .location(RuleLocation.builder()
                                .sqlId(context.getSqlId())
                                .lineNumber(getLineNumber(right))
                                .columnNumber(getColumnNumber(right))
                                .build())
                        .message(getMeta().description())
                        .suggestion("考虑使用全文索引、反转索引或后缀索引")
                        .build());
            }
        }
    }
}
```

### 4.3 缺少 WHERE 规则（使用 AST）

```java
package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.statement.select.*;
import org.spectrum.sqlchecker.domain.rule.*;
import org.springframework.stereotype.Component;

/**
 * 检测缺少 WHERE 的 SELECT
 */
@Component
@RuleMeta(
    id = "missing-where",
    name = "SELECT without WHERE clause",
    description = "SELECT 语句缺少 WHERE 条件可能导致全表扫描",
    type = RuleType.PROBLEM,
    severity = Severity.MAJOR,
    tags = {"performance"},
    category = RuleCategory.PERFORMANCE
)
public class MissingWhereRule implements SqlRule {

    @Override
    public Set<Class<? extends Node>> supportedNodeTypes() {
        return Set.of(PlainSelect.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof PlainSelect select) {
            checkSelect(select, context);
        }
    }

    private void checkSelect(PlainSelect select, RuleContext context) {
        // 有 LIMIT 则放过
        if (select.getLimit() != null) {
            return;
        }

        // 有 WHERE 则放过
        if (select.getWhere() != null) {
            return;
        }

        // 检查是否是聚合查询（COUNT/SUM 等）
        boolean isAggregation = select.getSelectItems().stream()
                .anyMatch(item -> item.getExpression() instanceof Function);

        if (isAggregation) {
            return;
        }

        // 报告问题
        context.reportIssue(RuleIssue.builder()
                .ruleId(getMeta().id())
                .severity(getMeta().severity())
                .location(RuleLocation.builder()
                        .sqlId(context.getSqlId())
                        .lineNumber(getLineNumber(select))
                        .build())
                .message(getMeta().description())
                .suggestion("添加 WHERE 条件或使用 LIMIT 限制结果集")
                .build());
    }
}
```

---

## 五、规则注册表

### 5.1 规则注册表接口

```java
package org.spectrum.sqlchecker.infrastructure.rule;

import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则注册表
 *
 * 管理所有已注册的规则，支持按节点类型查询
 */
@Component
public class RuleRegistry {

    // 节点类型 -> 规则列表
    private final Map<Class<?>, List<SqlRule>> rulesByNodeType = new ConcurrentHashMap<>();

    // 规则 ID -> 规则
    private final Map<String, SqlRule> rulesById = new ConcurrentHashMap<>();

    // 规则配置
    private RuleConfig ruleConfig;

    /**
     * 注册规则
     */
    public void register(SqlRule rule) {
        rulesById.put(rule.getMeta().id(), rule);

        for (Class<?> nodeType : rule.supportedNodeTypes()) {
            rulesByNodeType.computeIfAbsent(nodeType, k -> new ArrayList<>())
                    .add(rule);
        }
    }

    /**
     * 获取指定节点的所有规则
     */
    public List<SqlRule> getRulesForNode(Class<?> nodeType) {
        List<SqlRule> rules = new ArrayList<>();

        // 精确匹配
        rules.addAll(rulesByNodeType.getOrDefault(nodeType, Collections.emptyList()));

        // 接口匹配（查找实现该接口的所有类）
        for (Map.Entry<Class<?>, List<SqlRule>> entry : rulesByNodeType.entrySet()) {
            if (nodeType.isAssignableFrom(entry.getKey())) {
                rules.addAll(entry.getValue());
            }
        }

        return rules;
    }

    /**
     * 获取所有启用的规则
     */
    public List<SqlRule> getEnabledRules() {
        if (ruleConfig == null) {
            return new ArrayList<>(rulesById.values());
        }

        return rulesById.values().stream()
                .filter(rule -> ruleConfig.isRuleEnabled(rule.getMeta().id()))
                .toList();
    }

    /**
     * 根据 ID 获取规则
     */
    public Optional<SqlRule> getRule(String id) {
        return Optional.ofNullable(rulesById.get(id));
    }

    /**
     * 设置规则配置
     */
    public void setRuleConfig(RuleConfig config) {
        this.ruleConfig = config;
    }
}
```

### 5.2 自动规则发现（Spring 注入）

```java
package org.spectrum.sqlchecker.infrastructure.rule;

import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 规则自动注册器
 *
 * 自动发现并注册所有 SqlRule Bean
 */
@Component
public class RuleAutoRegistrar {

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private List<SqlRule> allRules;

    @PostConstruct
    public void registerRules() {
        for (SqlRule rule : allRules) {
            ruleRegistry.register(rule);
        }
    }
}
```

---

## 六、规则配置

### 6.1 配置文件格式（sqlchecker.yml）

```yaml
rules:
  # 内置规则配置
  select-star:
    enabled: true
    severity: CRITICAL  # 可覆盖默认级别

  missing-where:
    enabled: true
    exceptions:
      - "查询配置表"  # 包含此注释的 SQL 不检查
      - "系统初始化"

  like-leading-wildcard:
    enabled: true

  orderby-without-limit:
    enabled: false  # 禁用规则

  # 自定义规则
  custom-rules:
    - name: "table-naming-convention"
      enabled: true
      pattern: "^tbl_.*"
      message: "表名应以 tbl_ 开头"

    - name: "column-count-limit"
      enabled: true
      maxCount: 20
      message: "单表查询列数不应超过 20"

# 规则集（预设配置）
rule-sets:
  strict:
    - select-star
    - missing-where
    - like-leading-wildcard
    - implicit-join
    - n-plus-one
    - no-index

  relaxed:
    - select-star
    - like-leading-wildcard
```

### 6.2 配置类

```java
package org.spectrum.sqlchecker.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 规则配置属性
 */
@Component
@ConfigurationProperties(prefix = "sqlchecker.rules")
@Data
public class RuleConfigProperties {

    private Map<String, RuleConfig> rules = new HashMap<>();
    private List<CustomRuleConfig> customRules = new ArrayList<>();
    private List<String> ruleSets = new ArrayList<>();

    @Data
    public static class RuleConfig {
        private boolean enabled = true;
        private String severity;
        private List<String> exceptions = new ArrayList<>();
    }

    @Data
    public static class CustomRuleConfig {
        private String name;
        private boolean enabled = true;
        private String pattern;
        private String message;
        private Map<String, Object> options = new HashMap<>();
    }
}
```

---

## 七、规则上下文

### 7.1 规则上下文

```java
package org.spectrum.sqlchecker.domain.rule;

import lombok.*;
import net.sf.jsqlparser.statement.Statement;

/**
 * 规则执行上下文
 *
 * 在规则分析过程中传递信息，收集问题
 */
@Data
@Builder
public class RuleContext {

    /**
     * SQL ID
     */
    private final String sqlId;

    /**
     * 原始 SQL
     */
    private final String sql;

    /**
     * 解析后的 Statement（可选）
     */
    private Statement statement;

    /**
     * 收集的问题列表
     */
    @Builder.Default
    private final List<RuleIssue> issues = new ArrayList<>();

    /**
     * 解析错误信息
     */
    @Builder.Default
    private final List<String> parsingErrors = new ArrayList<>();

    /**
     * 规则配置
     */
    private RuleConfig ruleConfig;

    /**
     * 报告问题
     */
    public void reportIssue(RuleIssue issue) {
        // 检查规则是否启用
        if (ruleConfig != null && !ruleConfig.isRuleEnabled(issue.getRuleId())) {
            return;
        }

        // 检查是否在例外列表中
        if (isInExceptions(issue)) {
            return;
        }

        issues.add(issue);
    }

    /**
     * 获取节点行号（简化实现）
     */
    public int getLineNumber(Object node) {
        // TODO: 从 Token 获取行号
        return 0;
    }

    /**
     * 获取节点列号（简化实现）
     */
    public int getColumnNumber(Object node) {
        // TODO: 从 Token 获取列号
        return 0;
    }

    private boolean isInExceptions(RuleIssue issue) {
        if (ruleConfig == null) {
            return false;
        }

        RuleConfigProperties.RuleConfig config = ruleConfig.getRuleConfig(issue.getRuleId());
        if (config == null || config.getExceptions() == null) {
            return false;
        }

        return config.getExceptions().stream()
                .anyMatch(exception -> sql.contains(exception));
    }
}
```

---

## 八、迁移路径

### 8.1 阶段一：创建基础设施（1-2 天）

1. 创建核心接口和注解
   - `SqlRule` 接口
   - `@RuleMeta` 注解
   - `RuleContext` 类
   - `RuleRegistry` 类

2. 创建规则引擎骨架
   - `SqlRuleEngine` 类
   - `RuleVisitor` 类

### 8.2 阶段二：迁移现有规则（2-3 天）

| 原规则 | 新规则类 | 优先级 |
|-------|---------|-------|
| `checkSelectStar` | `SelectStarRule` | P0 |
| `checkMissingWhere` | `MissingWhereRule` | P0 |
| `checkLikeLeadingWildcard` | `LikeLeadingWildcardRule` | P0 |
| `checkOrderByWithoutLimit` | `OrderByWithoutLimitRule` | P1 |
| `checkJoinType` | `ImplicitJoinRule` | P1 |
| `checkSubquery` | `ComplexSubqueryRule` | P2 |

### 8.3 阶段三：完善配置系统（1-2 天）

1. 实现配置文件加载
2. 实现规则启用/禁用
3. 实现规则例外机制

### 8.4 阶段四：测试与文档（1 天）

1. 为每个规则编写单元测试
2. 更新 API 文档
3. 编写规则开发指南

---

## 九、文件结构

```
sql-checker/
├── domain/
│   └── rule/
│       ├── SqlRule.java                    # 规则接口
│       ├── RuleMeta.java                   # 规则注解
│       ├── RuleContext.java                # 规则上下文
│       ├── RuleIssue.java                  # 问题对象
│       ├── RuleLocation.java               # 位置信息
│       ├── RuleType.java                   # 规则类型枚举
│       ├── Severity.java                   # 严重级别枚举
│       └── RuleCategory.java               # 规则分类枚举
├── infrastructure/
│   └── rule/
│       ├── SqlRuleEngine.java              # 规则引擎
│       ├── RuleVisitor.java                # AST 访问者
│       ├── RuleRegistry.java               # 规则注册表
│       └── RuleAutoRegistrar.java          # 自动注册
├── domain/rule/
│   └── impl/                               # 规则实现
│       ├── SelectStarRule.java
│       ├── MissingWhereRule.java
│       ├── LikeLeadingWildcardRule.java
│       ├── OrderByWithoutLimitRule.java
│       ├── ImplicitJoinRule.java
│       └── ComplexSubqueryRule.java
└── infrastructure/config/
    └── RuleConfigProperties.java          # 配置类
```

---

## 十、总结

### 改进效果对比

| 维度 | 改进前 | 改进后 |
|------|-------|-------|
| 规则扩展性 | 修改 ScanCommand | 新建规则类 |
| 检测精度 | 字符串匹配 | AST 语法分析 |
| 配置灵活性 | 硬编码 | YAML 配置 |
| 规则管理 | 分散在方法中 | 独立规则类 |
| 第三方扩展 | 不支持 | 插件机制 |
| 单元测试 | 难以隔离 | 规则独立测试 |

### 下一步行动

1. 创建核心接口和基础设施
2. 实现 1-2 个示例规则验证架构
3. 逐步迁移现有规则
4. 完善配置系统
