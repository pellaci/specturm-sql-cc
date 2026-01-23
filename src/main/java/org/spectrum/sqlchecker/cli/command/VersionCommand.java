package org.spectrum.sqlchecker.cli.command;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * 版本命令
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Command(
        name = "version",
        description = "Show version information",
        mixinStandardHelpOptions = true
)
public class VersionCommand implements Callable<Integer> {

    private static final String VERSION = "1.0.0";
    private static final String BUILD_TIME = "2024-01-01";

    @Override
    public Integer call() throws Exception {
        System.out.println("SQL Checker v" + VERSION);
        System.out.println("Built: " + BUILD_TIME);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        return 0;
    }
}
