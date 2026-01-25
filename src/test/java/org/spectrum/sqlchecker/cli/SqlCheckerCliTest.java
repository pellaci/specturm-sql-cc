package org.spectrum.sqlchecker.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import picocli.spring.PicocliSpringFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlCheckerCli 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SqlCheckerCli 单元测试")
class SqlCheckerCliTest {

    @Mock
    private PicocliSpringFactory picocliFactory;

    @Mock
    private ApplicationContext applicationContext;

    private SqlCheckerCli cli;

    @BeforeEach
    void setUp() {
        cli = new SqlCheckerCli(picocliFactory, applicationContext);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确注入依赖")
        void should_inject_dependencies() {
            assertThat(cli).isNotNull();
        }

        @Test
        @DisplayName("应该接受 null 作为 applicationContext（可选依赖）")
        void should_accept_null_application_context() {
            assertThatCode(() -> new SqlCheckerCli(picocliFactory, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CommandLineRunner 接口测试")
    class CommandLineRunnerTests {

        @Test
        @DisplayName("应该实现 CommandLineRunner 接口")
        void should_implement_command_line_runner() {
            assertThat(cli).isInstanceOf(CommandLineRunner.class);
        }
    }

    @Nested
    @DisplayName("@Component 注解测试")
    class ComponentTests {

        @Test
        @DisplayName("应该有 @Component 注解")
        void should_have_component_annotation() {
            Class<SqlCheckerCli> clazz = SqlCheckerCli.class;

            assertThat(clazz.isAnnotationPresent(org.springframework.stereotype.Component.class))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("依赖注入验证测试")
    class DependencyInjectionTests {

        @Test
        @DisplayName("PicocliSpringFactory 应该被正确注入")
        void should_have_picocli_factory_injected() throws Exception {
            var field = SqlCheckerCli.class.getDeclaredField("picocliFactory");
            field.setAccessible(true);

            Object factory = field.get(cli);

            assertThat(factory).isSameAs(picocliFactory);
        }

        @Test
        @DisplayName("ApplicationContext 应该被正确注入")
        void should_have_application_context_injected() throws Exception {
            var field = SqlCheckerCli.class.getDeclaredField("applicationContext");
            field.setAccessible(true);

            Object context = field.get(cli);

            assertThat(context).isSameAs(applicationContext);
        }
    }
}
