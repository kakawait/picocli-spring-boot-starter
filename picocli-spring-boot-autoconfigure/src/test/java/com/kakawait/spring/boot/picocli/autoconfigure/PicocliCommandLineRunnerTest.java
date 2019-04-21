package com.kakawait.spring.boot.picocli.autoconfigure;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static picocli.CommandLine.Help.Ansi;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParameterException;
import static picocli.CommandLine.usage;

/**
 * @author Thibaud Leprêtre
 */
@RunWith(MockitoJUnitRunner.class)
public class PicocliCommandLineRunnerTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Mock
    private CommandLine cli;

    private PicocliCommandLineRunner runner;

    @Before
    public void setup() {
        runner = new PicocliCommandLineRunner(cli);
    }

    @Test
    public void run_ExceptionDuringParsing_PrintUsageAndStop() throws Exception {
        when(cli.parse(any())).thenThrow(new ParameterException(cli, "Error when parsing"));

        runner.run("parsing error or something else");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(anyString());
        inOrder.verify(cli, times(1)).usage(System.err, Ansi.AUTO);
        verifyNoMoreInteractions(cli);
    }

    @Test
    public void run_MainCommandHelpRequested_PrintUsageAndStop() throws Exception {
        HelpCommand command = new HelpCommand(true);

        when(cli.getCommand()).thenReturn(command);
        doAnswer(invocation -> {
            usage(command, invocation.getArgument(0), ((Ansi) invocation.getArgument(1)));
            return null;
        }).when(cli).usage(System.out, Ansi.AUTO);

        runner.run("-h");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse("-h");
        inOrder.verify(cli, times(1)).getCommand();
        inOrder.verify(cli, times(1)).usage(System.out, Ansi.AUTO);
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Usage: main \\[-h\\].*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_SubCommandHelpRequested_PrintUsageAndStop() throws Exception {
        when(cli.parse(any())).thenReturn(Collections.singletonList(new CommandLine(new HelpSubCommand(true))));
        when(cli.getCommand()).thenReturn(new HelpCommand(false));

        runner.run("subcommand -h");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse("subcommand -h");
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Usage: subcommand \\[-h\\].*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_NestedSubCommandHelpRequested_PrintUsageAndStop() throws Exception {
        List<CommandLine> commandLines = Arrays.asList(
                new CommandLine(new HelpSubCommand(false)),
                new CommandLine(new HelpNestedSubCommand(true))
        );
        when(cli.parse(any())).thenReturn(commandLines);
        when(cli.getCommand()).thenReturn(new HelpCommand(false));

        runner.run("subcommand nested-subcommand -h");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse("subcommand nested-subcommand -h");
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Usage: nested-subcommand \\[-h\\].*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_HelpRequestedConflict_PrintFirstChildrenUsageAndStop() throws Exception {
        List<CommandLine> commandLines = Arrays.asList(
                new CommandLine(new HelpSubCommand(true)),
                new CommandLine(new HelpNestedSubCommand(true))
        );
        when(cli.parse(any())).thenReturn(commandLines);
        when(cli.getCommand()).thenReturn(new HelpCommand(false));

        runner.run("subcommand -h nested-subcommand -h");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse("subcommand -h nested-subcommand -h");
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Usage: subcommand \\[-h\\].*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_MainRunnableCommand_Execute() throws Exception {
        Runnable mainCommand = makeRunnableCommand("main", () -> System.out.println("Main runnable command"));
        when(cli.parse(any())).thenReturn(Collections.singletonList(new CommandLine(mainCommand)));
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Main runnable command.*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_MainCallableCommand_Execute() throws Exception {
        Callable<?> mainCommand = makeCallableCommand("test", (Callable<Void>) () -> {
            System.out.println("Main callable command");
            return null;
        });
        when(cli.parse(any())).thenReturn(Collections.singletonList(new CommandLine(mainCommand)));
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Main callable command.*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_MainPicocliCommand_InjectContextThenExecute() throws Exception {
        PicocliCommand mainCommand = makePicocliCommand("main", () -> System.out.println("Main picocli command"));

        List<CommandLine> commandLines = Collections.singletonList(new CommandLine(mainCommand));
        when(cli.parse(any())).thenReturn(commandLines);
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("");

        assertThat(mainCommand.getRootContext()).isSameAs(cli);
        assertThat(mainCommand.getContext()).isSameAs(commandLines.get(0));
        assertThat(mainCommand.getParsedCommands()).isSameAs(commandLines);

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern.compile(".*Main picocli command.*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_MultipleRunnableOrCallableCommands_AllExecuted() throws Exception {
        PicocliCommand mainCommand = makePicocliCommand("main", () -> System.out.println("Main picocli command"));
        Runnable subCommand = makeRunnableCommand("subcommand", () -> System.out.println("Sub runnable command"));
        Callable<?> subSubCommand = makeCallableCommand("subsubcommand", (Callable<Void>) () -> {
            System.out.println("Sub sub callable command");
            return null;
        });
        List<CommandLine> commandLines = Arrays.asList(
                new CommandLine(mainCommand),
                new CommandLine(subCommand),
                new CommandLine(subSubCommand)
        );

        when(cli.parse(any())).thenReturn(commandLines);
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("subcommand subsubcommand");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        Pattern pattern = Pattern
                .compile(".*Main picocli command.*Sub runnable command.*Sub sub callable command.*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void run_FlowControlWithExitStatus_BreakOnTermination() throws Exception {
        PicocliCommand mainCommand = makePicocliCommand("main", (Callable<ExitStatus>) () -> {
            System.out.println("Hello");
            return ExitStatus.OK;
        });
        PicocliCommand subCommand = makePicocliCommand("subcommand", (Callable<ExitStatus>) () -> {
            System.out.println("World!");
            return ExitStatus.TERMINATION;
        });
        Runnable subSubCommand = makeRunnableCommand("subsubcommand", () -> System.out.println("Ignore me..."));
        List<CommandLine> commandLines = Arrays.asList(
                new CommandLine(mainCommand),
                new CommandLine(subCommand),
                new CommandLine(subSubCommand)
        );

        when(cli.parse(any())).thenReturn(commandLines);
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("subcommand subsubcommand");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);

        outputCapture.expect(matchesPattern(Pattern.compile(".*Hello.*World!\\n*", Pattern.DOTALL)));
        outputCapture.expect(not(matchesPattern(Pattern.compile(".*Ignore me\\.\\.\\..*"))));
    }

    @Test
    public void run_NorRunnableNorCallableCommand_Nothing() throws Exception {
        Object mainCommand = new EmptyCommand();
        when(cli.parse(any())).thenReturn(Collections.singletonList(new CommandLine(mainCommand)));
        when(cli.getCommand()).thenReturn(mainCommand);

        runner.run("");

        InOrder inOrder = inOrder(cli);
        inOrder.verify(cli, times(1)).parse(any());
        inOrder.verify(cli, times(1)).getCommand();
        verifyNoMoreInteractions(cli);
    }

    private AnnotationDescription getCommandAnnotationDescription(String commandName) {
        return AnnotationDescription
                .Builder
                .ofType(Command.class)
                .define("name", commandName)
                .build();
    }

    private Runnable makeRunnableCommand(String commandName, Runnable runnable)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return make(commandName, DelegateRunnable.class).getDeclaredConstructor(Runnable.class).newInstance(runnable);
    }

    private Callable<?> makeCallableCommand(String commandName, Callable<?> callable)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return make(commandName, DelegateCallable.class).getDeclaredConstructor(Callable.class).newInstance(callable);
    }

    private PicocliCommand makePicocliCommand(String commandName, Runnable runnable)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return make(commandName, DelegatePicocliCommand.class)
                .getDeclaredConstructor(Runnable.class)
                .newInstance(runnable);
    }

    private PicocliCommand makePicocliCommand(String commandName, Callable<?> callable)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return make(commandName, DelegatePicocliCommand.class)
                .getDeclaredConstructor(Callable.class)
                .newInstance(callable);
    }

    private <T> Class<? extends T> make(String commandName, Class<T> type) {
        try {
            return new ByteBuddy()
                    .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS)
                    .annotateType(getCommandAnnotationDescription(commandName))
                    .make()
                    .load(getClass().getClassLoader(), getClassLoadingStrategy(type))
                    .getLoaded();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate dynamic class", e);
        }
    }

    private ClassLoadingStrategy<ClassLoader> getClassLoadingStrategy(Class targetClass)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoadingStrategy<ClassLoader> strategy;
        if (ClassInjector.UsingLookup.isAvailable()) {
            Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
            Object lookup = methodHandles.getMethod("lookup").invoke(null);
            Method privateLookupIn = methodHandles.getMethod("privateLookupIn",
                    Class.class,
                    Class.forName("java.lang.invoke.MethodHandles$Lookup"));
            Object privateLookup = privateLookupIn.invoke(null, targetClass, lookup);
            strategy = ClassLoadingStrategy.UsingLookup.of(privateLookup);
        } else if (ClassInjector.UsingReflection.isAvailable()) {
            strategy = ClassLoadingStrategy.Default.INJECTION;
        } else {
            throw new IllegalStateException("No code generation strategy available");
        }

        return strategy;
    }

    private static class DelegateRunnable implements Runnable {
        private final Runnable delegate;

        public DelegateRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            delegate.run();
        }
    }

    private static class DelegateCallable<V> implements Callable<V> {
        private final Callable<V> delegate;

        public DelegateCallable(Callable<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public V call() throws Exception {
            return delegate.call();
        }
    }

    private static class DelegatePicocliCommand extends PicocliCommand {
        private Callable<ExitStatus> delegateCallable;

        private Runnable delegateRunnable;

        public DelegatePicocliCommand(Callable<ExitStatus> delegate) {
            this.delegateCallable = delegate;
        }

        public DelegatePicocliCommand(Runnable delegate) {
            this.delegateRunnable = delegate;
        }

        @Override
        public ExitStatus call() throws Exception {
            if (delegateCallable == null) {
                return super.call();
            }
            return delegateCallable.call();
        }

        @Override
        public void run() {
            delegateRunnable.run();
        }
    }

    @Command(name = "main")
    private static class HelpCommand {
        @Option(names = {"-h", "--help"}, help = true, description = "Prints this help message and exits")
        private boolean helpRequested;

        HelpCommand(boolean helpRequested) {
            this.helpRequested = helpRequested;
        }
    }

    @Command(name = "subcommand")
    private static class HelpSubCommand extends HelpCommand {
        HelpSubCommand(boolean helpRequested) {
            super(helpRequested);
        }
    }

    @Command(name = "nested-subcommand")
    private static class HelpNestedSubCommand extends HelpCommand {
        HelpNestedSubCommand(boolean helpRequested) {
            super(helpRequested);
        }
    }

    @Command
    private static class EmptyCommand {}
}
