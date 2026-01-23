-- Spectrum SQL Checker 数据库初始化脚本
-- H2 数据库（MySQL 兼容模式）

-- 代码库表
CREATE TABLE IF NOT EXISTS code_repository (
    id VARCHAR(36) PRIMARY KEY,
    path VARCHAR(1024) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 源文件表
CREATE TABLE IF NOT EXISTS source_file (
    id VARCHAR(36) PRIMARY KEY,
    repo_id VARCHAR(36) NOT NULL,
    path VARCHAR(1024) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    line_count BIGINT,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (repo_id) REFERENCES code_repository(id) ON DELETE CASCADE
);

-- SQL 语句表
CREATE TABLE IF NOT EXISTS sql_statement (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(36) NOT NULL,
    abstract_sql TEXT NOT NULL,
    original_sql TEXT NOT NULL,
    sql_type VARCHAR(50) NOT NULL,
    line_number INT,
    method_name VARCHAR(255),
    hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES source_file(id) ON DELETE CASCADE
);

-- SQL 位置表
CREATE TABLE IF NOT EXISTS sql_location (
    id VARCHAR(36) PRIMARY KEY,
    sql_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    line INT NOT NULL,
    column INT,
    class_name VARCHAR(512),
    method_name VARCHAR(255),
    source_type VARCHAR(50),
    FOREIGN KEY (sql_id) REFERENCES sql_statement(id) ON DELETE CASCADE
);

-- 分析结果表
CREATE TABLE IF NOT EXISTS analysis_result (
    id VARCHAR(36) PRIMARY KEY,
    sql_id VARCHAR(36) NOT NULL,
    scan_id VARCHAR(36),
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sql_id) REFERENCES sql_statement(id) ON DELETE CASCADE
);

-- 静态分析表
CREATE TABLE IF NOT EXISTS static_analysis (
    id VARCHAR(36) PRIMARY KEY,
    result_id VARCHAR(36) NOT NULL,
    issues JSON NOT NULL,
    severity VARCHAR(20) NOT NULL,
    FOREIGN KEY (result_id) REFERENCES analysis_result(id) ON DELETE CASCADE
);

-- EXPLAIN 分析表
CREATE TABLE IF NOT EXISTS explain_analysis (
    id VARCHAR(36) PRIMARY KEY,
    result_id VARCHAR(36) NOT NULL,
    explain_plan JSON NOT NULL,
    issues JSON,
    severity VARCHAR(20) NOT NULL,
    FOREIGN KEY (result_id) REFERENCES analysis_result(id) ON DELETE CASCADE
);

-- 专家分析表
CREATE TABLE IF NOT EXISTS expert_analysis (
    id VARCHAR(36) PRIMARY KEY,
    result_id VARCHAR(36) NOT NULL,
    recommendations JSON NOT NULL,
    severity VARCHAR(20) NOT NULL,
    FOREIGN KEY (result_id) REFERENCES analysis_result(id) ON DELETE CASCADE
);

-- 数据库连接表
CREATE TABLE IF NOT EXISTS database_connection (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password_encrypted TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分析规则表
CREATE TABLE IF NOT EXISTS analysis_rule (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    threshold INT,
    config JSON,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 扫描历史表
CREATE TABLE IF NOT EXISTS scan_history (
    id VARCHAR(36) PRIMARY KEY,
    repo_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    files_scanned INT,
    sql_found INT,
    issues_found INT,
    duration_ms BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (repo_id) REFERENCES code_repository(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_source_file_repo ON source_file(repo_id);
CREATE INDEX IF NOT EXISTS idx_source_file_type ON source_file(file_type);
CREATE INDEX IF NOT EXISTS idx_sql_statement_file ON sql_statement(file_id);
CREATE INDEX IF NOT EXISTS idx_sql_statement_hash ON sql_statement(hash);
CREATE INDEX IF NOT EXISTS idx_sql_statement_type ON sql_statement(sql_type);
CREATE INDEX IF NOT EXISTS idx_sql_statement_created ON sql_statement(created_at);
CREATE INDEX IF NOT EXISTS idx_analysis_result_sql ON analysis_result(sql_id);
CREATE INDEX IF NOT EXISTS idx_analysis_result_severity ON analysis_result(severity);
CREATE INDEX IF NOT EXISTS idx_analysis_result_scan ON analysis_result(scan_id);
CREATE INDEX IF NOT EXISTS idx_scan_history_repo ON scan_history(repo_id);
CREATE INDEX IF NOT EXISTS idx_scan_history_status ON scan_history(status);
