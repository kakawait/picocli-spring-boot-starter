package com.kakawait.spring.boot.picocli.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static picocli.CommandLine.Help.Ansi;
import static picocli.CommandLine.usage;

/**
 * @author Thibaud Leprêtre
 */
public class PicocliCommandLineRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PicocliCommandLineRunner.class);

    private final CommandLine cli;

    PicocliCommandLineRunner(CommandLine cli) {
        this.cli = cli;
    }

    @Override
    public void run(String... args) throws Exception {
        List<CommandLine> commands;
        try {
            commands = cli.parse(args);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            cli.usage(System.err, Ansi.AUTO);
            return;
        }
        if (isHelpRequested((Object) cli.getCommand())) {
            cli.usage(System.out, Ansi.AUTO);
            return;
        }
        Optional<CommandLine> helpCommand = commands
                .stream()
                .filter(this::isHelpRequested)
                .findFirst();
        if (helpCommand.isPresent()) {
            usage(helpCommand.get(), System.out);
            return;
        }

        for (CommandLine commandLine : commands) {
            Object command = commandLine.getCommand();
            Object result = null;

            if (command instanceof PicocliCommand) {
                PicocliCommand picocliCommand = (PicocliCommand) command;
                picocliCommand.setContext(commandLine);
                picocliCommand.setRootContext(cli);
                picocliCommand.setParsedCommands(commands);
                result = picocliCommand.call();
            } else if (command instanceof Runnable) {
                ((Runnable) command).run();
            } else if (command instanceof Callable) {
                result = ((Callable) command).call();
            } else {
                logger.debug("Command {} is triggered but does not implement {} neither {}",
                        command, Runnable.class, Callable.class);
            }

            if (result instanceof ExitStatus && result == ExitStatus.TERMINATION) {
                break;
            }
        }
    }

    public CommandLine getCommandLine() {
        return cli;
    }

    private boolean isHelpRequested(CommandLine commandLine) {
        Object command = commandLine.getCommand();
        return isHelpRequested(command);
    }

    private boolean isHelpRequested(Object command) {
        AtomicBoolean result = new AtomicBoolean(false);
        ReflectionUtils.doWithFields(AopUtils.getTargetClass(command),
                f -> {
                    f.setAccessible(true);
                    if (f.getBoolean(command)) {
                        result.set(true);
                    }
                },
                f -> f.isAnnotationPresent(Option.class) && f.getAnnotation(Option.class).help());
        return result.get();
    }

}
