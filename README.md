# 光谱 SQL Checker

![光谱 SQL Checker Logo](docs/brand/logo.svg)

**🔍 面向 Java/MyBatis 的 SQL 质量检测与诊断报告工具**

_真实扫描 + 静态规则 + 安全可选 EXPLAIN + HTML/JSON 双报告_

![Version](https://img.shields.io/badge/version-1.2.0-722ED1)
![Java](https://img.shields.io/badge/Java-17+-1890FF)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-52C41A)
![License](https://img.shields.io/badge/license-MIT-597EF7)

---

## 目录

- [项目简介](#项目简介)
- [功能现状（对齐 PRD/ADD）](#功能现状对齐-prdadd)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [报告说明](#报告说明)
- [内置规则](#内置规则)
- [架构简述](#架构简述)
- [限制与注意事项](#限制与注意事项)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目简介

光谱 SQL Checker 面向 **Java/MyBatis 技术栈**，在代码库中提取 SQL 并进行质量检测，输出给技术负责人阅读的 HTML 诊断报告，以及可被流水线和后续工具消费的 JSON 诊断数据。

当前已实现：

- 静态分析（基于 JSqlParser AST 的规则检测）
- 安全可选 EXPLAIN（默认不依赖数据库；启用后只分析允许的只读 SQL）
- 单文件 HTML 报告 + 同目录 JSON 报告

规划中：

- 专家规则分析（PRD/ADD 里的“专家规则层”尚未完成）

相关文档：

- PRD：`docs/sql-checker-prd.md`
- ADD：`docs/sql-checker-add.md`
- DDD：`docs/sql-checker-ddd.md`

---

## 功能现状（对齐 PRD/ADD）

| 功能 | PRD 优先级 | 现状 | 说明 |
|------|------------|------|------|
| 代码库扫描与 SQL 提取 | P0 | ✅ 已实现 | Java / MyBatis XML / 注解 / JS/TS |
| 静态 SQL 分析 | P0 | ✅ 已实现 | 内置 20 条规则 |
| 动态 EXPLAIN 分析 | P0 | ✅ 已实现 | 默认关闭；启用后仅允许安全 SQL |
| 专家规则分析 | P0 | 🚧 未实现 | 仅保留开关与结构位 |
| HTML 报告生成 | P0 | ✅ 已实现 | 总览 / 热点 / 洞察 / 修复队列 / 明细筛选 |
| JSON 诊断数据 | P0 | ✅ 已实现 | 与 HTML 使用同一份 `DiagnosticReport` 模型 |
| 交互式命令行界面 | P0 | ✅ 已实现 | Picocli + 进度展示 |
| 多数据库支持 | P1 | ⚠️ 部分 | 当前以 MySQL 为主 |
| 配置管理 | P1 | ✅ 已实现 | `sqlchecker.yml` |
| 历史报告对比 | P2 | 🚧 未实现 | 规划中 |
| CI/CD 集成 | P2 | ⚠️ 部分 | CLI 方式可用于流水线 |

---

## 快速开始

### 1) 构建

```bash
mvn -q -DskipTests clean package
```

### 2) 扫描（生成报告）

```bash
java -jar target/sql-checker-1.2.0.jar scan \
  -p /path/to/project \
  -o report.html
```

命令会同时生成：

- `report.html`：给技术负责人阅读的单文件静态诊断报告
- `report.json`：完整机器可消费诊断数据

### 3) 启用 EXPLAIN（需要数据库连接）

```bash
java -Dsqlchecker.database.auto-init=true -jar target/sql-checker-1.2.0.jar scan \
  -p /path/to/project \
  -o report.html \
  --enable-explain \
  --db-connection default \
  --init-schema
```

主要参数：

- `-p, --path`：代码库路径
- `-o, --output`：报告输出路径
- `--enable-explain`：启用 EXPLAIN 分析
- `--db-connection`：连接配置名称（默认 `default`）
- `--init-schema`：自动初始化/推断表结构
- `--schema-path`：DDL 文件路径（可选）

---

## 配置说明

配置文件：`sqlchecker.yml`

示例（注意：当前密码支持明文，仅建议本地使用）：

```yaml
database:
  connections:
    default:
      type: mysql
      host: 127.0.0.1
      port: 3306
      database: ssqlchecker
      username: spec
      password: spec123
      parameters:
        useSSL: false
        serverTimezone: UTC
        unix_socket: /tmp/mysql.sock

analysis:
  explainEnabled: true
  expertEnabled: true
  timeoutSeconds: 30
  thresholds:
    scan-rows: 10000
```

说明：

- `expertEnabled` 目前尚未生效（专家规则未实现）。
- 如果 JVM 不支持 Unix Domain Socket，会自动回退到 TCP（日志会提示）。

---

## 报告说明

报告包含：

- 总览：风险等级、总分、扫描范围、SQL 数、问题数、解析率、EXPLAIN 覆盖率
- 热点：Top 风险文件、Top 规则、严重级别分布
- 洞察：重复 SQL、解析/模板待确认、EXPLAIN 跳过、危险 DML、潜在注入、全表扫描/无索引
- 修复队列：按严重级别和证据聚合的优先处理清单
- 明细：按问题状态、严重级别、规则、文件和 SQL 内容筛选；问题 SQL 可展开原始 SQL 和抽象 SQL

几个指标口径：

- `Parse failures = 0` 表示抽取到的 SQL 已被预处理/分类管道接住，不代表所有动态模板都能自动证明安全。
- `Manual review` 表示需要人工确认的动态模板、跳过 EXPLAIN 或无法由静态规则充分证明的 SQL。
- `EXPLAIN skipped` 表示未配置数据库、SQL 非只读、模板占位符不安全或当前策略拒绝执行计划分析。

输出 HTML 文件名由 `-o` 指定；JSON 文件自动写到同目录同名 `.json`。

---

## 内置规则

当前内置 20 条规则（以实际代码/配置为准）：

- NOT IN with potential NULL issue
- Multi-column OR condition
- Implicit JOIN detected
- SELECT without WHERE clause
- Multi-column IN detected
- Long SQL statement
- Complex subquery detected
- HAVING without WHERE
- Avoid LIKE with leading wildcard
- INSERT without column list
- Avoid SELECT *
- Hardcoded secrets detected
- Incorrect NULL comparison
- DELETE/UPDATE without WHERE
- Unnecessary DISTINCT
- IN with subquery
- Magic numbers detected
- DROP/TRUNCATE TABLE detected
- Function on indexed column in WHERE
- ORDER BY without LIMIT

---

## 架构简述

- 分层：CLI → Application → Domain → Infrastructure
- 管道：扫描 → 提取 → 预处理 → 静态分析/EXPLAIN → 报告

更完整的设计细节请参考 `docs/sql-checker-add.md` / `docs/sql-checker-ddd.md`。

---

## 限制与注意事项

- 专家规则分析尚未实现（PRD/ADD 中为规划功能）。
- 默认扫描不需要数据库。EXPLAIN 需要提供可用数据库连接，并且只对安全的只读 SQL 生效。
- PostgreSQL 策略禁止 `EXPLAIN ANALYZE` 这类会执行语句的形式，只使用只读 EXPLAIN。
- 复杂动态 SQL 即使能进入报告，也可能被标记为人工复核项；不要把 100% 解析覆盖率理解为 100% 业务语义正确。
- 密码暂支持明文配置，仅建议本地/内网环境使用。

---

## 贡献指南

欢迎提交 Issue / PR。

建议流程：

```bash
mvn test
```

---

## 许可证

MIT
