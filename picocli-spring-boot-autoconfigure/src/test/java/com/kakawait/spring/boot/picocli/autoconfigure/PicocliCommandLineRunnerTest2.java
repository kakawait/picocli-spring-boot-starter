package com.kakawait.spring.boot.picocli.autoconfigure;

import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static picocli.CommandLine.usage;

/**
 * @author Thibaud Leprêtre
 */
public class PicocliCommandLineRunnerTest2 {

    private AnnotationConfigApplicationContext context;

    @Test
    public void run_MainCommandHelpRequested_PrintUsageAndStop() throws Exception {
//        HelpCommand command = new HelpCommand(true);
//
//        CommandLine cli = context.getBean(CommandLine.class);
//
//        when(cli.getCommand()).thenReturn(command);
//        doAnswer(invocation -> {
//            usage(command, invocation.getArgument(0), ((CommandLine.Help.Ansi) invocation.getArgument(1)));
//            return null;
//        }).when(cli).usage(System.out, CommandLine.Help.Ansi.AUTO);

        load(HelpCommand.GreetingCommand.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        runner.run("-V");

//        InOrder inOrder = inOrder(cli);
//        inOrder.verify(cli, times(1)).parse("-h");
//        inOrder.verify(cli, times(1)).getCommand();
//        inOrder.verify(cli, times(1)).usage(System.out, CommandLine.Help.Ansi.AUTO);
//        verifyNoMoreInteractions(cli);
//
//        Pattern pattern = Pattern.compile(".*Usage: main \\[-h\\].*", Pattern.DOTALL);
//        outputCapture.expect(matchesPattern(pattern));
    }

    private void load(Class<?>... configs) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(configs);
        context.register(PicocliAutoConfiguration.class);
        context.refresh();
        this.context = context;
    }

    @CommandLine.Command
    private static class HelpCommand implements Runnable {
        @CommandLine.Option(names = {"-V", "--version"}, description = "display version info")
        boolean versionRequested;

        @Override
        public void run() {
            if (versionRequested) {
                System.out.println("0.1.0");
            }
        }

        @CommandLine.Command(name = "greeting")
        static class GreetingCommand implements Runnable {

            @CommandLine.Parameters(paramLabel = "NAME", description = "name", arity = "0..1")
            String name;

            @Override
            public void run() {
                if (StringUtils.hasText(name)) {
                    System.out.println("Hello " + name + "!");
                } else {
                    System.out.println("Hello world!");
                }
            }
        }
    }
}
