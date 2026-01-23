package org.spectrum.sqlchecker.application.scan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;
import org.spectrum.sqlchecker.domain.shared.enumeration.ProjectType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanScope;

import java.util.List;

/**
 * 扫描请求
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanRequest {

    /**
     * 代码库路径
     */
    @NotBlank(message = "代码库路径不能为空")
    private String repositoryPath;

    /**
     * 扫描范围
     */
    private ScanScope scope = ScanScope.FULL;

    /**
     * 包含的文件模式（Ant 风格）
     */
    private List<String> includes = List.of("**/*.java", "**/*.xml", "**/*.sql");

    /**
     * 排除的文件模式
     */
    private List<String> excludes = List.of("**/target/**", "**/build/**", "**/.git/**");

    /**
     * 项目类型（自动检测，可手动指定）
     */
    private ProjectType projectType;

    /**
     * 是否增量扫描
     */
    private boolean incremental = false;

    /**
     * 增量扫描基准（上次扫描 ID）
     */
    private String baselineScanId;

    /**
     * 并行度（0=自动检测）
     */
    @Min(value = 0, message = "并行度不能为负数")
    private int parallelism = 0;

    /**
     * 是否跳过 EXPLAIN 分析
     */
    private boolean skipExplain = false;

    /**
     * 数据库连接 ID（用于 EXPLAIN 分析）
     */
    private String connectionId;
}
