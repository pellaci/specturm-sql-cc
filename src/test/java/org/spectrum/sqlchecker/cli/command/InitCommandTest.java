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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * InitCommand 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InitCommand 单元测试")
class InitCommandTest {

    private InitCommand command;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        command = new InitCommand();
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    /**
     * 使用反射设置私有字段
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 使用反射获取私有字段
     */
    private <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    @Nested
    @DisplayName("配置文件创建测试")
    class ConfigFileCreationTests {

        @Test
        @DisplayName("应该在当前目录创建配置文件")
        void should_create_config_in_current_directory(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            Integer result = command.call();

            assertThat(result).isZero();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            assertThat(Files.exists(configFile)).isTrue();
        }

        @Test
        @DisplayName("应该在指定目录创建配置文件")
        void should_create_config_in_specified_directory(@TempDir Path tempDir) throws Exception {
            Path subDir = tempDir.resolve("subdir");
            Files.createDirectories(subDir);

            setField(command, "directory", subDir.toString());

            Integer result = command.call();

            assertThat(result).isZero();

            Path configFile = subDir.resolve("sqlchecker.yml");
            assertThat(Files.exists(configFile)).isTrue();
        }

        @Test
        @DisplayName("创建的配置文件应该包含必要的内容")
        void should_create_config_with_required_content(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            // 验证关键配置项存在
            assertThat(content).contains("# SQL Checker Configuration");
            assertThat(content).contains("database:");
            assertThat(content).contains("connections:");
            assertThat(content).contains("scan:");
            assertThat(content).contains("analysis:");
            assertThat(content).contains("report:");
        }

        @Test
        @DisplayName("配置文件应该包含数据库连接配置")
        void should_contain_database_config(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            assertThat(content).contains("type: mysql");
            assertThat(content).contains("host: localhost");
            assertThat(content).contains("port: 3306");
            assertThat(content).contains("username:");
            assertThat(content).contains("password:");
        }

        @Test
        @DisplayName("配置文件应该包含扫描设置")
        void should_contain_scan_settings(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            assertThat(content).contains("includes:");
            assertThat(content).contains("**/*.java");
            assertThat(content).contains("**/*.xml");
            assertThat(content).contains("excludes:");
            assertThat(content).contains("**/node_modules/**");
        }

        @Test
        @DisplayName("配置文件应该包含分析设置")
        void should_contain_analysis_settings(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            assertThat(content).contains("explainEnabled: true");
            assertThat(content).contains("expertEnabled: true");
            assertThat(content).contains("timeoutSeconds: 30");
        }

        @Test
        @DisplayName("配置文件应该包含报告设置")
        void should_contain_report_settings(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            assertThat(content).contains("theme: feishu");
            assertThat(content).contains("defaultOutputDir:");
            assertThat(content).contains("includeSqlDetails: true");
            assertThat(content).contains("themeColor:");
        }
    }

    @Nested
    @DisplayName("配置文件已存在测试")
    class ConfigFileExistsTests {

        @Test
        @DisplayName("当配置文件已存在时应该返回错误")
        void should_return_error_when_config_exists(@TempDir Path tempDir) throws Exception {
            // 创建已存在的配置文件
            Path configFile = tempDir.resolve("sqlchecker.yml");
            Files.writeString(configFile, "# existing config");

            setField(command, "directory", tempDir.toString());
            setField(command, "force", false);

            Integer result = command.call();

            assertThat(result).isOne();
        }

        @Test
        @DisplayName("当配置文件已存在时应该显示提示信息")
        void should_show_message_when_config_exists(@TempDir Path tempDir) throws Exception {
            Path configFile = tempDir.resolve("sqlchecker.yml");
            Files.writeString(configFile, "# existing config");

            setField(command, "directory", tempDir.toString());
            setField(command, "force", false);

            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("Config file already exists");
            assertThat(output).contains("--force");
        }

        @Test
        @DisplayName("当配置文件已存在且使用 --force 时应该覆盖")
        void should_overwrite_when_force_enabled(@TempDir Path tempDir) throws Exception {
            Path configFile = tempDir.resolve("sqlchecker.yml");
            Files.writeString(configFile, "# old config");

            setField(command, "directory", tempDir.toString());
            setField(command, "force", true);

            Integer result = command.call();

            assertThat(result).isZero();

            String content = Files.readString(configFile);
            assertThat(content).contains("# SQL Checker Configuration");
            assertThat(content).doesNotContain("# old config");
        }
    }

    @Nested
    @DisplayName("输出消息测试")
    class OutputMessageTests {

        @Test
        @DisplayName("成功创建时应该显示成功消息")
        void should_show_success_message(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("Config file created:");
            assertThat(output).contains("sqlchecker.yml");
        }

        @Test
        @DisplayName("应该显示后续操作提示")
        void should_show_next_steps_message(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            String output = outputStream.toString();

            assertThat(output).contains("Please edit the configuration");
            assertThat(output).contains("sqlchecker scan");
        }
    }

    @Nested
    @DisplayName("参数配置测试")
    class ParameterTests {

        @Test
        @DisplayName("应该能够设置自定义目录")
        void should_allow_custom_directory() throws Exception {
            setField(command, "directory", "/custom/path");

            String directory = getField(command, "directory", String.class);
            assertThat(directory).isEqualTo("/custom/path");
        }

        @Test
        @DisplayName("force 默认应该是 false")
        void force_should_be_false_by_default() throws Exception {
            boolean force = getField(command, "force", Boolean.class);
            assertThat(force).isFalse();
        }

        @Test
        @DisplayName("应该能够设置 force 参数")
        void should_allow_force_parameter() throws Exception {
            setField(command, "force", true);

            boolean force = getField(command, "force", Boolean.class);
            assertThat(force).isTrue();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理绝对路径")
        void should_handle_absolute_path(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toAbsolutePath().toString());

            Integer result = command.call();

            assertThat(result).isZero();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            assertThat(Files.exists(configFile)).isTrue();
        }
    }

    @Nested
    @DisplayName("YAML 格式测试")
    class YamlFormatTests {

        @Test
        @DisplayName("生成的配置应该是有效的 YAML 格式")
        void should_generate_valid_yaml(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            // 验证 YAML 基本格式正确
            assertThat(content).doesNotContain("	"); // 不应包含制表符
            assertThat(content).contains("\n"); // 应该有换行
            assertThat(content).contains("database:");
            assertThat(content).contains("connections:");
        }

        @Test
        @DisplayName("YAML 应该包含注释")
        void should_contain_yaml_comments(@TempDir Path tempDir) throws Exception {
            setField(command, "directory", tempDir.toString());

            command.call();

            Path configFile = tempDir.resolve("sqlchecker.yml");
            String content = Files.readString(configFile);

            assertThat(content).contains("#");
            assertThat(content).contains("# SQL Checker Configuration");
        }
    }
}
