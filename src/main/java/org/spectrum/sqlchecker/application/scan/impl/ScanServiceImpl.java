package org.spectrum.sqlchecker.application.scan.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.scan.ScanService;
import org.spectrum.sqlchecker.application.scan.dto.ScanProgress;
import org.spectrum.sqlchecker.application.scan.dto.ScanRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExpertAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final Map<String, ScanProgress> progressMap = new ConcurrentHashMap<>();
    private final Map<String, ScanResult> resultMap = new ConcurrentHashMap<>();

    @Override
    public ScanResult scan(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();
        log.info("Starting scan: {}", scanId);

        // 初始化进度
        ScanProgress progress = ScanProgress.builder()
                .scanId(scanId)
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.SCANNING)
                .stage(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStage.FILE_SCANNING)
                .progress(0)
                .filesScanned(0)
                .totalFiles(0)
                .sqlFound(0)
                .sqlAnalyzed(0)
                .build();
        progressMap.put(scanId, progress);

        // 执行扫描（模拟）
        List<SqlStatementDto> sqlStatements = performScan(request, scanId);

        // 创建结果
        ScanResult result = ScanResult.builder()
                .scanId(scanId)
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                .filesScanned(sqlStatements.size())
                .sqlFound(sqlStatements.size())
                .uniqueSqlFound(sqlStatements.size())
                .durationMs(1000)
                .sqlStatements(sqlStatements)
                .errors(new ArrayList<>())
                .build();

        resultMap.put(scanId, result);

        // 更新进度
        progress.setStatus(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED);
        progress.setProgress(100);

        return result;
    }

    @Override
    public ScanResult getResult(String scanId) {
        return resultMap.get(scanId);
    }

    @Override
    public ScanProgress getProgress(String scanId) {
        return progressMap.get(scanId);
    }

    @Override
    public void cancelScan(String scanId) {
        ScanProgress progress = progressMap.get(scanId);
        if (progress != null) {
            progress.setStatus(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.CANCELLED);
        }
    }

    @Override
    public String scanAsync(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();
        // 异步扫描（简单实现）
        new Thread(() -> {
            scan(request);
        }).start();
        return scanId;
    }

    /**
     * 执行扫描（模拟实现）
     */
    private List<SqlStatementDto> performScan(ScanRequest request, String scanId) {
        List<SqlStatementDto> statements = new ArrayList<>();

        // 模拟一些 SQL 语句用于演示
        for (int i = 1; i <= 3; i++) {
            SqlStatementDto dto = SqlStatementDto.builder()
                    .id(UUID.randomUUID().toString())
                    .originalSql("SELECT * FROM users WHERE id = " + i)
                    .abstractSql("SELECT * FROM users WHERE id = ?")
                    .sqlHash(UUID.randomUUID().toString())
                    .sqlType(org.spectrum.sqlchecker.domain.shared.enumeration.SqlType.SELECT)
                    .locations(new ArrayList<>())
                    .severity(i == 1 ? SeverityLevel.CRITICAL : SeverityLevel.WARNING)
                    .score(i == 1 ? 50 : 70)
                    .build();

            // 添加分析结果
            dto.setStaticAnalysis(createMockStaticAnalysis(dto.getId()));
            dto.setExplainAnalysis(createMockExplainAnalysis(dto.getId()));
            dto.setExpertAnalysis(createMockExpertAnalysis(dto.getId()));

            statements.add(dto);
        }

        return statements;
    }

    private StaticAnalysisDto createMockStaticAnalysis(String sqlId) {
        return StaticAnalysisDto.builder()
                .sqlId(sqlId)
                .severity(SeverityLevel.WARNING)
                .issues(new ArrayList<>())
                .score(70)
                .durationMs(100)
                .build();
    }

    private ExplainAnalysisDto createMockExplainAnalysis(String sqlId) {
        return ExplainAnalysisDto.builder()
                .sqlId(sqlId)
                .severity(SeverityLevel.INFO)
                .issues(new ArrayList<>())
                .durationMs(200)
                .build();
    }

    private ExpertAnalysisDto createMockExpertAnalysis(String sqlId) {
        return ExpertAnalysisDto.builder()
                .sqlId(sqlId)
                .recommendations(new ArrayList<>())
                .score(80)
                .durationMs(150)
                .build();
    }
}
