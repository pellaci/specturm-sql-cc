package org.spectrum.sqlchecker.application.report;

import org.spectrum.sqlchecker.application.report.dto.ReportSummary;
import org.spectrum.sqlchecker.application.scan.ScanService;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.domain.shared.exception.ScanException;

import java.io.OutputStream;

/**
 * 报告生成服务
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ReportService {

    /**
     * 生成 HTML 报告
     *
     * @param scanResult 扫描结果
     * @param outputPath 输出路径
     * @throws ScanException 生成失败
     */
    void generateHtmlReport(ScanResult scanResult, String outputPath) throws ScanException;

    /**
     * 生成 HTML 报告到输出流
     *
     * @param scanResult 扫描结果
     * @param outputStream 输出流
     * @throws ScanException 生成失败
     */
    void generateHtmlReport(ScanResult scanResult, OutputStream outputStream) throws ScanException;

    /**
     * 生成结构化 JSON 诊断报告
     *
     * @param scanResult 扫描结果
     * @param outputPath 输出路径
     * @throws ScanException 生成失败
     */
    void generateJsonReport(ScanResult scanResult, String outputPath) throws ScanException;

    /**
     * 生成报告摘要
     *
     * @param scanResult 扫描结果
     * @return 报告摘要
     */
    ReportSummary generateSummary(ScanResult scanResult);

    /**
     * 根据 Scan ID 生成报告
     *
     * @param scanId 扫描 ID
     * @param outputPath 输出路径
     * @throws ScanException 生成失败
     */
    void generateHtmlReportByScanId(String scanId, String outputPath) throws ScanException;
}
