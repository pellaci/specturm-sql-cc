package org.spectrum.sqlchecker.application.scan.orchestrator;

import org.spectrum.sqlchecker.application.scan.dto.ScanProgressSnapshot;
import org.spectrum.sqlchecker.application.scan.dto.ScanStartInfo;

/**
 * 扫描进度监听器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ScanProgressListener {

    void onStart(ScanStartInfo info);

    void onProgress(ScanProgressSnapshot snapshot);

    void onComplete();
}
