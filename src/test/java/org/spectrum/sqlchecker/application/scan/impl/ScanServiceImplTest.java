package org.spectrum.sqlchecker.application.scan.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.scan.dto.ScanProgress;
import org.spectrum.sqlchecker.application.scan.dto.ScanRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;

import static org.assertj.core.api.Assertions.*;

/**
 * ScanServiceImpl 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@DisplayName("ScanServiceImpl 单元测试")
class ScanServiceImplTest {

    private ScanServiceImpl scanService;

    @BeforeEach
    void setUp() {
        scanService = new ScanServiceImpl();
    }

    @Nested
    @DisplayName("scan 方法测试")
    class ScanTests {

        @Test
        @DisplayName("应该成功执行扫描")
        void should_execute_scan_successfully() {
            ScanRequest request = createScanRequest();

            ScanResult result = scanService.scan(request);

            assertThat(result).isNotNull();
            assertThat(result.getScanId()).isNotEmpty();
            assertThat(result.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        }

        @Test
        @DisplayName("应该返回 SQL 语句列表")
        void should_return_sql_statements() {
            ScanRequest request = createScanRequest();

            ScanResult result = scanService.scan(request);

            assertThat(result.getSqlStatements()).isNotEmpty();
        }

        @Test
        @DisplayName("应该设置正确的扫描统计信息")
        void should_set_scan_statistics() {
            ScanRequest request = createScanRequest();

            ScanResult result = scanService.scan(request);

            assertThat(result.getFilesScanned()).isGreaterThan(0);
            assertThat(result.getSqlFound()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("getResult 方法测试")
    class GetResultTests {

        @Test
        @DisplayName("应该返回已存在的扫描结果")
        void should_return_existing_result() {
            ScanRequest request = createScanRequest();
            ScanResult scanResult = scanService.scan(request);

            ScanResult result = scanService.getResult(scanResult.getScanId());

            assertThat(result).isNotNull();
            assertThat(result.getScanId()).isEqualTo(scanResult.getScanId());
        }

        @Test
        @DisplayName("应该返回 null 当扫描不存在")
        void should_return_null_when_scan_not_exists() {
            ScanResult result = scanService.getResult("non-existent-id");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getProgress 方法测试")
    class GetProgressTests {

        @Test
        @DisplayName("应该返回扫描进度")
        void should_return_scan_progress() {
            ScanRequest request = createScanRequest();
            ScanResult scanResult = scanService.scan(request);

            ScanProgress progress = scanService.getProgress(scanResult.getScanId());

            assertThat(progress).isNotNull();
            assertThat(progress.getScanId()).isEqualTo(scanResult.getScanId());
            assertThat(progress.getStatus()).isEqualTo(ScanStatus.COMPLETED);
            assertThat(progress.getProgress()).isEqualTo(100);
        }

        @Test
        @DisplayName("应该返回 null 当扫描不存在")
        void should_return_null_for_non_existent_scan() {
            ScanProgress progress = scanService.getProgress("non-existent-id");

            assertThat(progress).isNull();
        }
    }

    @Nested
    @DisplayName("cancelScan 方法测试")
    class CancelScanTests {

        @Test
        @DisplayName("应该取消扫描")
        void should_cancel_scan() {
            ScanRequest request = createScanRequest();
            ScanResult scanResult = scanService.scan(request);

            scanService.cancelScan(scanResult.getScanId());

            ScanProgress progress = scanService.getProgress(scanResult.getScanId());
            assertThat(progress.getStatus()).isEqualTo(ScanStatus.CANCELLED);
        }

        @Test
        @DisplayName("应该安全地处理不存在的扫描")
        void should_handle_non_existent_scan_safely() {
            assertThatCode(() -> scanService.cancelScan("non-existent-id"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("scanAsync 方法测试")
    class ScanAsyncTests {

        @Test
        @DisplayName("应该返回扫描 ID")
        void should_return_scan_id() {
            ScanRequest request = createScanRequest();

            String scanId = scanService.scanAsync(request);

            assertThat(scanId).isNotEmpty();
        }
    }

    /**
     * 创建扫描请求
     */
    private ScanRequest createScanRequest() {
        return ScanRequest.builder()
                .repositoryPath("/tmp/test")
                .build();
    }
}
