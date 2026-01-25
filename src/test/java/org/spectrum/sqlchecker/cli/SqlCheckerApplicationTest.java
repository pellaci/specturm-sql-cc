package org.spectrum.sqlchecker.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import static org.assertj.core.api.Assertions.*;

/**
 * SqlCheckerApplication 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("SqlCheckerApplication 单元测试")
class SqlCheckerApplicationTest {

    @Nested
    @DisplayName("类注解测试")
    class AnnotationTests {

        @Test
        @DisplayName("应该有 @SpringBootApplication 注解")
        void should_have_spring_boot_application_annotation() {
            Class<SqlCheckerApplication> clazz = SqlCheckerApplication.class;

            assertThat(clazz.isAnnotationPresent(SpringBootApplication.class))
                    .isTrue();
        }

        @Test
        @DisplayName("应该有 @ComponentScan 注解")
        void should_have_component_scan_annotation() {
            Class<SqlCheckerApplication> clazz = SqlCheckerApplication.class;

            assertThat(clazz.isAnnotationPresent(ComponentScan.class))
                    .isTrue();
        }

        @Test
        @DisplayName("@ComponentScan 应该扫描 org.spectrum.sqlchecker 包")
        void should_scan_correct_package() {
            ComponentScan annotation = SqlCheckerApplication.class.getAnnotation(ComponentScan.class);

            assertThat(annotation.basePackages()).contains("org.spectrum.sqlchecker");
        }
    }

    @Nested
    @DisplayName("main 方法测试")
    class MainMethodTests {

        @Test
        @DisplayName("应该有 main 方法")
        void should_have_main_method() throws NoSuchMethodException {
            Class<SqlCheckerApplication> clazz = SqlCheckerApplication.class;

            assertThat(clazz.getMethod("main", String[].class))
                    .isNotNull();
        }

        @Test
        @DisplayName("main 方法应该是 public static void")
        void main_method_should_be_public_static_void() throws NoSuchMethodException {
            var method = SqlCheckerApplication.class.getMethod("main", String[].class);

            assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                    .isTrue();
            assertThat(java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                    .isTrue();
            assertThat(method.getReturnType().getName()).isEqualTo("void");
        }
    }

    @Nested
    @DisplayName("应用配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("SpringBootApplication 应该启用自动配置")
        void should_enable_auto_configuration() {
            SpringBootApplication annotation =
                    SqlCheckerApplication.class.getAnnotation(SpringBootApplication.class);

            assertThat(annotation).isNotNull();
        }
    }

    @Nested
    @DisplayName("包结构测试")
    class PackageTests {

        @Test
        @DisplayName("应该在正确的包中")
        void should_be_in_correct_package() {
            assertThat(SqlCheckerApplication.class.getPackage().getName())
                    .isEqualTo("org.spectrum.sqlchecker.cli");
        }

        @Test
        @DisplayName("类名应该是 SqlCheckerApplication")
        void should_have_correct_class_name() {
            assertThat(SqlCheckerApplication.class.getSimpleName())
                    .isEqualTo("SqlCheckerApplication");
        }
    }
}
