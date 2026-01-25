package org.spectrum.sqlchecker.application.scan.orchestrator;

import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;

/**
 * 扫描编排器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ScanOrchestrator {

    ScanExecutionResult execute(ScanExecutionRequest request, ScanProgressListener listener);
}
