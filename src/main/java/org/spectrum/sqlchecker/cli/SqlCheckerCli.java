package org.spectrum.sqlchecker.cli;

import org.spectrum.sqlchecker.cli.command.InitCommand;
import org.spectrum.sqlchecker.cli.command.ScanCommand;
import org.spectrum.sqlchecker.cli.command.VersionCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * SQL Checker CLI 主入口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Command(
        name = "sqlchecker",
        description = "SQL quality checker - Scan, analyze, and report on SQL statements",
        mixinStandardHelpOptions = true,
        version = "sqlchecker 1.0.0",
        subcommands = {
                ScanCommand.class,
                InitCommand.class,
                VersionCommand.class
        }
)
public class SqlCheckerCli implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Verbose mode")
    private boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SqlCheckerCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("SQL Checker - SQL Quality Detection Tool");
        System.out.println("Use 'sqlchecker --help' to see available commands");
    }
}
