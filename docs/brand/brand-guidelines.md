# 光谱 SQL Checker 品牌设计规范

## 设计理念

**光谱（Spectrum）** 是光的分解，象征着：
- **分析**：将复杂的 SQL 分解为可理解的问题
- **全面**：覆盖从安全到性能的完整检测维度
- **层次**：用光谱色彩表达问题的严重程度

> 就像三棱镜将白光分解为七彩光谱，光谱 SQL Checker 将 SQL 代码分解为多维度的质量报告。

---

## Logo 设计

### 主 Logo

```
     ╱╲
    ╱  ╲    ═══▶ 🔴🟠🟡🟢🔵🟣
   ╱    ╲
  ╱______╲
     SQL
   SPECTRUM
```

**设计元素**：
- **三棱镜**：代表分析和分解能力
- **入射光**：代表输入的 SQL 代码
- **光谱射线**：代表多维度的检测结果
- **深色背景圆**：代表专业和技术感

### Logo 文件

| 格式 | 用途 | 文件 |
|------|------|------|
| SVG | Web/矢量 | `logo.svg` |
| PNG | 通用 | `logo.png` (待生成) |
| ICO | Favicon | `favicon.ico` (待生成) |

---

## 色彩系统

### 主色板 - 光谱七色

基于可见光光谱，从红色到紫色：

| 颜色 | 名称 | HEX | RGB | 用途 |
|------|------|-----|-----|------|
| 🔴 | 光谱红 Spectrum Red | `#FF4D4F` | 255, 77, 79 | Critical 严重问题 |
| 🟠 | 光谱橙 Spectrum Orange | `#FF7A45` | 255, 122, 69 | Warning 警告 |
| 🟡 | 光谱黄 Spectrum Yellow | `#FFC53D` | 255, 197, 61 | Caution 注意 |
| 🟢 | 光谱绿 Spectrum Green | `#52C41A` | 82, 196, 26 | Success 成功/通过 |
| 🔵 | 光谱蓝 Spectrum Blue | `#1890FF` | 24, 144, 255 | Info 信息 |
| 💜 | 光谱靛 Spectrum Indigo | `#597EF7` | 89, 126, 247 | Accent 强调 |
| 🟣 | 光谱紫 Spectrum Violet | `#722ED1` | 114, 46, 209 | Premium 高级 |

### 语义色彩映射

| 级别 | 颜色 | 背景色 | 边框色 | 说明 |
|------|------|--------|--------|------|
| **CRITICAL** | `#FF4D4F` | `#FFF1F0` | `#FFA39E` | 必须立即修复 |
| **WARNING** | `#FF7A45` | `#FFF7E6` | `#FFD591` | 建议修复 |
| **INFO** | `#1890FF` | `#E6F7FF` | `#91D5FF` | 可以优化 |
| **SUCCESS** | `#52C41A` | `#F6FFED` | `#B7EB8F` | 检测通过 |

### 中性色

| 名称 | HEX | 用途 |
|------|-----|------|
| 背景深 | `#0D1117` | Logo 背景、深色模式 |
| 背景浅 | `#F6F8FA` | 页面背景 |
| 边框 | `#D0D7DE` | 分隔线、边框 |
| 文字主 | `#24292F` | 主要文字 |
| 文字次 | `#57606A` | 次要文字 |
| 文字弱 | `#8B949E` | 辅助文字 |

### 渐变色

```css
/* 光谱渐变 - 用于强调元素 */
.spectrum-gradient {
  background: linear-gradient(90deg,
    #FF4D4F 0%,
    #FF7A45 17%,
    #FFC53D 33%,
    #52C41A 50%,
    #1890FF 67%,
    #597EF7 83%,
    #722ED1 100%
  );
}

/* 头部渐变 - 深色到光谱 */
.header-gradient {
  background: linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1890FF 100%);
}
```

---

## 字体系统

### 字体栈

```css
/* 代码字体 */
--font-mono: 'SF Mono', 'Fira Code', 'JetBrains Mono', Monaco, Consolas, 'Courier New', monospace;

/* 正文字体 */
--font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans SC', 'PingFang SC', 'Hiragino Sans GB', sans-serif;

/* 标题字体 */
--font-display: 'Inter', 'SF Pro Display', -apple-system, sans-serif;
```

### 字体大小

| 级别 | 大小 | 行高 | 用途 |
|------|------|------|------|
| Display | 32px | 1.25 | 页面标题 |
| H1 | 24px | 1.3 | 章节标题 |
| H2 | 20px | 1.4 | 子标题 |
| H3 | 16px | 1.5 | 小标题 |
| Body | 14px | 1.6 | 正文 |
| Small | 12px | 1.5 | 辅助文字 |
| Code | 13px | 1.5 | 代码 |

---

## 组件样式

### 徽章 (Badge)

```html
<!-- Critical -->
<span class="badge badge-critical">
  <span class="badge-dot"></span>
  CRITICAL
</span>

<!-- Warning -->
<span class="badge badge-warning">
  <span class="badge-dot"></span>
  WARNING
</span>

<!-- Info -->
<span class="badge badge-info">
  <span class="badge-dot"></span>
  INFO
</span>
```

```css
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.badge-critical {
  background: #FFF1F0;
  color: #CF1322;
  border: 1px solid #FFA39E;
}

.badge-critical .badge-dot {
  background: #FF4D4F;
  box-shadow: 0 0 8px #FF4D4F;
}

.badge-warning {
  background: #FFF7E6;
  color: #D46B08;
  border: 1px solid #FFD591;
}

.badge-warning .badge-dot {
  background: #FF7A45;
}

.badge-info {
  background: #E6F7FF;
  color: #096DD9;
  border: 1px solid #91D5FF;
}

.badge-info .badge-dot {
  background: #1890FF;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
```

### 卡片 (Card)

```css
.card {
  background: #FFFFFF;
  border: 1px solid #D0D7DE;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #D0D7DE;
  background: linear-gradient(180deg, #F6F8FA 0%, #FFFFFF 100%);
}

.card-body {
  padding: 20px;
}

/* 带光谱边框的卡片 */
.card-spectrum {
  border: none;
  position: relative;
}

.card-spectrum::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #FF4D4F, #FF7A45, #FFC53D, #52C41A, #1890FF, #722ED1);
}
```

### 统计数字

```css
.stat-number {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #1890FF 0%, #722ED1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 12px;
  color: #57606A;
  text-transform: uppercase;
  letter-spacing: 1px;
}
```

---

## 报告模板设计

### 报告头部

```
┌─────────────────────────────────────────────────────────────────┐
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (光谱渐变条)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   [Logo]  光谱 SQL Checker                                      │
│           SQL Quality Analysis Report                           │
│                                                                 │
│   项目: my-project          生成时间: 2025-01-25 14:30         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 统计概览

```
┌────────────────────────────────────────────────────────────────┐
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   156    │  │    45    │  │    3     │  │   85%    │       │
│  │  Files   │  │   SQL    │  │ Critical │  │  Score   │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                │
│  [━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━] │
│   🔴 3 Critical   🟠 12 Warning   🔵 8 Info   🟢 22 Passed     │
└────────────────────────────────────────────────────────────────┘
```

### 问题列表

```
┌────────────────────────────────────────────────────────────────┐
│  ● CRITICAL  SELECT * FROM users WHERE id = ?                  │
│  ├─ 📍 UserMapper.xml:45                                       │
│  ├─ 🔍 select-star: 避免使用 SELECT *                          │
│  └─ 💡 建议: 明确列出需要的字段                                │
├────────────────────────────────────────────────────────────────┤
│  ● WARNING   DELETE FROM logs WHERE created_at < ?             │
│  ├─ 📍 LogService.java:128                                     │
│  ├─ 🔍 missing-index: WHERE 字段可能未建索引                   │
│  └─ 💡 建议: 确认 created_at 字段是否有索引                    │
└────────────────────────────────────────────────────────────────┘
```

---

## CLI 输出样式

### ASCII Art Logo

```
  ╭──────────────────────────────────────────╮
  │                                          │
  │     ╱╲                                   │
  │    ╱  ╲    ══════▶ ═══════              │
  │   ╱    ╲            ═══════             │
  │  ╱──────╲            ═══════            │
  │                                          │
  │   光谱 SQL Checker v1.0.0                │
  │   SQL 质量检测工具                       │
  │                                          │
  ╰──────────────────────────────────────────╯
```

### 进度条配色

```
扫描中...
[██████████████████████░░░░░░░░] 75%
 ▲ 光谱渐变（红→橙→黄→绿→蓝→紫）
```

### 结果摘要

```
╭─ 扫描完成 ─────────────────────────────────╮
│                                            │
│  📁 扫描文件:  156                         │
│  📝 发现 SQL:  45                          │
│  ⏱️  耗时:     1.2s                        │
│                                            │
│  问题统计:                                 │
│  ● 严重    3   ████                       │
│  ● 警告   12   ████████████               │
│  ● 提示    8   ████████                   │
│                                            │
│  📊 报告: ./reports/sql-report.html        │
│                                            │
╰────────────────────────────────────────────╯
```

---

## 设计资源

### 图标建议

使用 [Lucide Icons](https://lucide.dev/) 或 [Heroicons](https://heroicons.com/)：

| 场景 | 图标 | 用途 |
|------|------|------|
| Critical | `alert-circle` | 严重问题 |
| Warning | `alert-triangle` | 警告 |
| Info | `info` | 信息 |
| Success | `check-circle` | 通过 |
| File | `file-code` | 源文件 |
| SQL | `database` | SQL 语句 |
| Report | `file-text` | 报告 |
| Settings | `settings` | 配置 |

---

## 品牌应用示例

### GitHub README 徽章

```markdown
![Version](https://img.shields.io/badge/version-1.0.0-722ED1)
![Java](https://img.shields.io/badge/Java-17+-1890FF)
![License](https://img.shields.io/badge/license-MIT-52C41A)
```

### 报告页脚

```
───────────────────────────────────────────────────────
光谱 SQL Checker v1.0.0 | Generated at 2025-01-25 14:30
https://github.com/spectrum/sql-checker
───────────────────────────────────────────────────────
```

---

**品牌色彩速查**

```
Critical: #FF4D4F    Warning: #FF7A45    Info: #1890FF
Success:  #52C41A    Accent:  #597EF7    Premium: #722ED1
```
