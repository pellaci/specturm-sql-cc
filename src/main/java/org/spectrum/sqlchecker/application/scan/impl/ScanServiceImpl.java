package org.spectrum.sqlchecker.application.scan.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.scan.ScanService;
import org.spectrum.sqlchecker.application.scan.dto.ScanProgress;
import org.spectrum.sqlchecker.application.scan.dto.ScanRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
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

        // Legacy API placeholder. Real CLI scans must go through ScanOrchestratorImpl.
        Instant now = Instant.now();
        ScanResult result = ScanResult.builder()
                .scanId(scanId)
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                .scanPath(request != null ? request.getRepositoryPath() : null)
                .filesScanned(0)
                .totalFiles(0)
                .sqlFound(0)
                .uniqueSqlFound(0)
                .durationMs(0)
                .sqlStatements(new ArrayList<>())
                .errors(new ArrayList<>())
                .startTime(now)
                .endTime(now)
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
}
