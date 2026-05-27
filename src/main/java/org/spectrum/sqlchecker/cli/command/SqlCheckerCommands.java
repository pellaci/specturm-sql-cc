package org.spectrum.sqlchecker.cli.command;

import picocli.CommandLine;

/**
 * 主命令集合
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@picocli.CommandLine.Command(
        name = "sqlchecker",
        description = "SQL quality checker - Scan, analyze, and report on SQL statements in your codebase",
        mixinStandardHelpOptions = true,
        version = "sqlchecker 1.2.0",
        subcommands = {
                ScanCommand.class,
                InitCommand.class,
                VersionCommand.class
        }
)
public class SqlCheckerCommands {

    // 主命令类，包含所有子命令
}
