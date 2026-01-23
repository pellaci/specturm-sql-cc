package org.spectrum.sqlchecker.cli;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import picocli.CommandLine;

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

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SqlCheckerApplication.class);
        app.setRegisterShutdownHook(false); // 让 picocli 处理退出

        // 运行 Spring Boot 应用
        System.exit(SpringApplication.exit(app.run(args)));
    }
}
