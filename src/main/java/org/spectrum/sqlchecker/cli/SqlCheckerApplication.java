package org.spectrum.sqlchecker.cli;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

/**
 * SQL Checker 应用程序入口
 * 使用 Picocli 作为 CLI 框架，集成 Spring Boot
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.spectrum.sqlchecker")
public class SqlCheckerApplication {

    static final Map<String, Object> QUIET_DEFAULT_PROPERTIES = Map.of(
            "debug", "false",
            "logging.level.root", "WARN",
            "logging.level.org.springframework", "WARN",
            "logging.level.org.spectrum.sqlchecker", "WARN"
    );

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SqlCheckerApplication.class);
        configureForCli(app);

        // 运行 Spring Boot 应用
        System.exit(SpringApplication.exit(app.run(args)));
    }

    static void configureForCli(SpringApplication app) {
        applyQuietSystemPropertyDefaults();
        app.setRegisterShutdownHook(false); // 让 picocli 处理退出
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.setDefaultProperties(QUIET_DEFAULT_PROPERTIES);
    }

    static void applyQuietSystemPropertyDefaults() {
        QUIET_DEFAULT_PROPERTIES.forEach((key, value) -> {
            if (System.getProperty(key) == null) {
                System.setProperty(key, value.toString());
            }
        });
    }
}
