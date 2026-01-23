package org.spectrum.sqlchecker.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

/**
 * SQL Checker CLI 主入口
 * 使用 Spring Boot 集成 Picocli
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Component
public class SqlCheckerCli implements CommandLineRunner {

    private final PicocliSpringFactory picocliFactory;
    private final ApplicationContext applicationContext;

    public SqlCheckerCli(PicocliSpringFactory picocliFactory, ApplicationContext applicationContext) {
        this.picocliFactory = picocliFactory;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        SqlCheckerCommand rootCommand = new SqlCheckerCommand();
        CommandLine commandLine = new CommandLine(rootCommand, picocliFactory);

        // 手动从 Spring 容器获取子命令并注册
        commandLine.addSubcommand("scan", applicationContext.getBean(org.spectrum.sqlchecker.cli.command.ScanCommand.class));
        commandLine.addSubcommand("init", applicationContext.getBean(org.spectrum.sqlchecker.cli.command.InitCommand.class));
        commandLine.addSubcommand("version", applicationContext.getBean(org.spectrum.sqlchecker.cli.command.VersionCommand.class));

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
