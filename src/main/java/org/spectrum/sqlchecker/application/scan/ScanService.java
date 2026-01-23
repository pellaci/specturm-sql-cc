package org.spectrum.sqlchecker.application.scan;

import org.spectrum.sqlchecker.application.scan.dto.ScanProgress;
import org.spectrum.sqlchecker.application.scan.dto.ScanRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;

/**
 * 扫描应用服务接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ScanService {

    /**
     * 扫描代码库
     *
     * @param request 扫描请求
     * @return 扫描结果
     */
    ScanResult scan(ScanRequest request);

    /**
     * 获取扫描进度
     *
     * @param scanId 扫描 ID
     * @return 进度信息
     */
    ScanProgress getProgress(String scanId);

    /**
     * 取消扫描
     *
     * @param scanId 扫描 ID
     */
    void cancelScan(String scanId);

    /**
     * 异步扫描（非阻塞）
     *
     * @param request 扫描请求
     * @return 扫描 ID
     */
    String scanAsync(ScanRequest request);

    /**
     * 获取扫描结果
     *
     * @param scanId 扫描 ID
     * @return 扫描结果
     */
    ScanResult getResult(String scanId);
}
