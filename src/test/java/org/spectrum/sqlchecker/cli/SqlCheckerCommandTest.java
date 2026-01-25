package org.spectrum.sqlchecker.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlCheckerCommand 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SqlCheckerCommand 单元测试")
class SqlCheckerCommandTest {

    private SqlCheckerCommand command;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        command = new SqlCheckerCommand();
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Nested
    @DisplayName("命令执行测试")
    class ExecutionTests {

        @Test
        @DisplayName("run 方法应该显示欢迎信息")
        void should_print_welcome_message() {
            command.run();

            String output = outputStream.toString();

            assertThat(output).contains("SQL Checker");
            assertThat(output).contains("SQL Quality Detection Tool");
            assertThat(output).contains("v2.0.0");
        }

        @Test
        @DisplayName("应该提示用户使用 --help 查看命令")
        void should_suggest_using_help() {
            command.run();

            String output = outputStream.toString();

            assertThat(output).contains("--help");
            assertThat(output).contains("available commands");
        }
    }

    @Nested
    @DisplayName("参数配置测试")
    class ParameterTests {

        @Test
        @DisplayName("verbose 默认应该是 false")
        void verbose_should_be_false_by_default() throws Exception {
            Field verboseField = SqlCheckerCommand.class.getDeclaredField("verbose");
            verboseField.setAccessible(true);

            boolean verbose = verboseField.getBoolean(command);

            assertThat(verbose).isFalse();
        }

        @Test
        @DisplayName("应该能够设置 verbose 选项")
        void should_allow_verbose_option() throws Exception {
            Field verboseField = SqlCheckerCommand.class.getDeclaredField("verbose");
            verboseField.setAccessible(true);
            verboseField.setBoolean(command, true);

            boolean verbose = verboseField.getBoolean(command);

            assertThat(verbose).isTrue();
        }
    }

    @Nested
    @DisplayName("@Component 注解测试")
    class ComponentTests {

        @Test
        @DisplayName("应该有 @Component 注解")
        void should_have_component_annotation() {
            Class<SqlCheckerCommand> clazz = SqlCheckerCommand.class;

            assertThat(clazz.isAnnotationPresent(org.springframework.stereotype.Component.class))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("@Command 注解测试")
    class CommandAnnotationTests {

        @Test
        @DisplayName("应该有 @Command 注解")
        void should_have_command_annotation() {
            Class<SqlCheckerCommand> clazz = SqlCheckerCommand.class;

            assertThat(clazz.isAnnotationPresent(picocli.CommandLine.Command.class))
                    .isTrue();
        }

        @Test
        @DisplayName("命令名称应该是 sqlchecker")
        void should_have_correct_command_name() {
            picocli.CommandLine.Command annotation =
                    SqlCheckerCommand.class.getAnnotation(picocli.CommandLine.Command.class);

            assertThat(annotation.name()).isEqualTo("sqlchecker");
        }

        @Test
        @DisplayName("版本应该是 2.0.0")
        void should_have_correct_version() {
            picocli.CommandLine.Command annotation =
                    SqlCheckerCommand.class.getAnnotation(picocli.CommandLine.Command.class);

            assertThat(annotation.version()[0]).isEqualTo("sqlchecker 2.0.0");
        }

        @Test
        @DisplayName("应该启用 mixinStandardHelpOptions")
        void should_enable_standard_help_options() {
            picocli.CommandLine.Command annotation =
                    SqlCheckerCommand.class.getAnnotation(picocli.CommandLine.Command.class);

            assertThat(annotation.mixinStandardHelpOptions()).isTrue();
        }
    }
}
