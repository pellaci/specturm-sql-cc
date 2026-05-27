package org.spectrum.sqlchecker.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * VersionCommand 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VersionCommand 单元测试")
class VersionCommandTest {

    private VersionCommand command;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        command = new VersionCommand();
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Nested
    @DisplayName("命令执行测试")
    class ExecutionTests {

        @Test
        @DisplayName("应该成功执行并返回 0")
        void should_execute_successfully() throws Exception {
            Integer result = command.call();

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("应该输出版本信息")
        void should_print_version_info() throws Exception {
            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("SQL Checker v");
            assertThat(output).contains("1.2.0");
        }

        @Test
        @DisplayName("应该输出构建时间")
        void should_print_build_time() throws Exception {
            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("Built:");
            assertThat(output).contains("2026-05-27");
        }

        @Test
        @DisplayName("应该输出 Java 版本")
        void should_print_java_version() throws Exception {
            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("Java:");
            assertThat(output).contains(System.getProperty("java.version"));
        }

        @Test
        @DisplayName("应该输出操作系统信息")
        void should_print_os_info() throws Exception {
            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("OS:");
            assertThat(output).contains(System.getProperty("os.name"));
            assertThat(output).contains(System.getProperty("os.arch"));
        }

        @Test
        @DisplayName("输出格式应该正确")
        void should_have_correct_output_format() throws Exception {
            command.call();

            String output = outputStream.toString();
            String[] lines = output.trim().split("\\r?\\n");

            assertThat(lines).hasSize(4);
            assertThat(lines[0]).matches("SQL Checker v\\d+\\.\\d+\\.\\d+");
            assertThat(lines[1]).matches("Built: \\d{4}-\\d{2}-\\d{2}");
            assertThat(lines[2]).startsWith("Java:");
            assertThat(lines[3]).startsWith("OS:");
        }
    }

    @Nested
    @DisplayName("常量测试")
    class ConstantTests {

        @Test
        @DisplayName("版本号应该是有效的语义化版本")
        void version_should_be_valid_semver() throws Exception {
            // 通过反射访问私有常量
            var versionField = VersionCommand.class.getDeclaredField("VERSION");
            versionField.setAccessible(true);
            String version = (String) versionField.get(null);

            assertThat(version).matches("\\d+\\.\\d+\\.\\d+");
        }

        @Test
        @DisplayName("构建时间应该是有效的日期格式")
        void build_time_should_be_valid_date() throws Exception {
            var buildTimeField = VersionCommand.class.getDeclaredField("BUILD_TIME");
            buildTimeField.setAccessible(true);
            String buildTime = (String) buildTimeField.get(null);

            assertThat(buildTime).matches("\\d{4}-\\d{2}-\\d{2}");
        }
    }
}
