package com.kakawait.spring.boot.picocli.autoconfigure;

import org.springframework.boot.CommandLineRunner;
import picocli.CommandLine;

import static picocli.CommandLine.IExceptionHandler2;
import static picocli.CommandLine.IParseResultHandler2;

/**
 * @author Thibaud Leprêtre
 */
public class PicocliCommandLineRunner implements CommandLineRunner {

    private final CommandLine cli;

    private final IParseResultHandler2 parseResultHandler;

    private final IExceptionHandler2 exceptionHandler;

    PicocliCommandLineRunner(CommandLine cli, IParseResultHandler2 parseResultHandler,
            IExceptionHandler2 exceptionHandler) {
        this.cli = cli;
        this.parseResultHandler = parseResultHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run(String... args) {
        //noinspection unchecked
        cli.parseWithHandlers(parseResultHandler, exceptionHandler, args);
    }

    public CommandLine getCommandLine() {
        return cli;
    }
}
