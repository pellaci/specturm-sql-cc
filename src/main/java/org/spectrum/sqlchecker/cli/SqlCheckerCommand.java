package org.spectrum.sqlchecker.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * SQL Checker 根命令
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Component
@Command(
        name = "sqlchecker",
        description = "SQL quality checker - Scan, analyze, and report on SQL statements",
        mixinStandardHelpOptions = true,
        version = "sqlchecker 1.2.0"
)
public class SqlCheckerCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Verbose mode")
    private boolean verbose;

    @Override
    public void run() {
        System.out.println("SQL Checker - SQL Quality Detection Tool (v1.2.0)");
        System.out.println("Use 'sqlchecker --help' to see available commands");
    }
}
