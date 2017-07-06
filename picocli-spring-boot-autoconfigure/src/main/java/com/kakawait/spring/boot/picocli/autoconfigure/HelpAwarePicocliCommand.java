package com.kakawait.spring.boot.picocli.autoconfigure;

import static picocli.CommandLine.Option;

/**
 * @author Thibaud Leprêtre
 */
@SuppressWarnings("unused")
public abstract class HelpAwarePicocliCommand extends PicocliCommand {
    @Option(names = {"-h", "--help"}, help = true, description = "Prints this help message and exits")
    private boolean helpRequested;
}
