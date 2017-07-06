package com.kakawait.spring.boot.picocli.autoconfigure;

import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Thibaud Leprêtre
 */
public abstract class PicocliCommand implements Callable<ExitStatus> {

    private List<CommandLine> parsedCommands;

    private CommandLine context;

    private CommandLine rootContext;

    @Override
    public ExitStatus call() throws Exception {
        run();
        return ExitStatus.OK;
    }

    @SuppressWarnings("WeakerAccess")
    public void run() {
    }

    /**
     * Returns result of {@link CommandLine#parse(String...)}.
     * @return Picocli parsing result which results on collection of every command involve regarding your input.
     */
    protected List<CommandLine> getParsedCommands() {
        return parsedCommands;
    }

    /**
     * Returns the current {@link CommandLine}.
     * @return current {@link CommandLine} context
     */
    protected CommandLine getContext() {
        return context;
    }

    /**
     * Returns the root {@link CommandLine}.
     * @return root {@link CommandLine} context that must contains (or equals) the {@link #getContext()}.
     */
    protected CommandLine getRootContext() {
        return rootContext;
    }

    void setParsedCommands(List<CommandLine> parsedCommands) {
        this.parsedCommands = parsedCommands;
    }

    void setContext(CommandLine context) {
        this.context = context;
    }

    void setRootContext(CommandLine rootContext) {
        this.rootContext = rootContext;
    }
}
