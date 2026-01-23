package org.spectrum.sqlchecker.cli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * 初始化命令
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
@Command(
        name = "init",
        description = "Initialize SQL Checker configuration",
        mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @Option(
            names = {"-d", "--dir"},
            description = "Directory to create config file",
            defaultValue = "."
    )
    private String directory;

    @Option(
            names = {"-f", "--force"},
            description = "Overwrite existing config file"
    )
    private boolean force;

    @Override
    public Integer call() throws Exception {
        Path configPath = Paths.get(directory, "sqlchecker.yml");

        // 检查文件是否存在
        if (Files.exists(configPath) && !force) {
            System.out.println("Config file already exists: " + configPath);
            System.out.println("Use --force to overwrite.");
            return 1;
        }

        // 生成默认配置
        String defaultConfig = """
                # SQL Checker Configuration

                # Database connections for EXPLAIN analysis
                database:
                  connections:
                    default:
                      name: Default MySQL
                      type: mysql
                      host: localhost
                      port: 3306
                      database: your_database
                      username: your_username
                      password: your_password
                      parameters:
                        useSSL: false
                        serverTimezone: UTC

                # Scan settings
                scan:
                  includes:
                    - "**/*.java"
                    - "**/*.xml"
                    - "**/*.js"
                    - "**/*.ts"
                  excludes:
                    - "**/node_modules/**"
                    - "**/target/**"
                    - "**/build/**"
                    - "**/.git/**"
                  maxFileSizeMb: 10
                  parallelism: 4

                # Analysis settings
                analysis:
                  explainEnabled: true
                  expertEnabled: true
                  timeoutSeconds: 30
                  maxThreads: 4

                # Report settings
                report:
                  theme: feishu
                  defaultOutputDir: ./reports
                  includeSqlDetails: true
                  includeCodeSnippets: true
                  themeColor:
                    primary: "#3370FF"
                    success: "#00B628"
                    warning: "#FF8800"
                    error: "#F54A45"
                    info: "#4C5C76"
                """;

        Files.writeString(configPath, defaultConfig);

        System.out.println("Config file created: " + configPath);
        System.out.println("Please edit the configuration and run 'sqlchecker scan'.");

        return 0;
    }
}
