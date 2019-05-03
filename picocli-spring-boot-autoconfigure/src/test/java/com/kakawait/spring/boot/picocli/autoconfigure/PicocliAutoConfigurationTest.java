package com.kakawait.spring.boot.picocli.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.Collection;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * @author Thibaud Leprêtre
 */
public class PicocliAutoConfigurationTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    private AnnotationConfigApplicationContext context;

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void autoConfiguration_EmptyConfiguration_Skipped() {
        load(EmptyConfiguration.class);
        assertThatThrownBy(() -> context.getBean(CommandLine.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(PicocliCommandLineRunner.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    public void autoConfiguration_CommandLineBean_UsesUserDefinedBean() {
        load(CommandLineConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);
        assertThat(runner.getCommandLine()).isSameAs(context.getBean(CommandLine.class));
    }

    @Test
    public void autoConfiguration_MissingMainCommand_ConfiguresDefaultHelpAwareCommand() throws Exception {
        load(SimpleConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        Object command = runner.getCommandLine().getCommand();
        //assertThat(command).isInstanceOf(HelpAwarePicocliCommand.class);

        runner.run("-h");

        Pattern pattern = Pattern.compile(".*-h, --help\\s+Prints this help message and exits.*", Pattern.DOTALL);
        outputCapture.expect(matchesPattern(pattern));
    }

    @Test
    public void autoConfiguration_BasicBeanDefinition_CreateSubCommands() {
        load(SimpleConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);
        Collection<Object> commands = context.getBeansWithAnnotation(Command.class).values();

        assertThat(runner.getCommandLine().getSubcommands().values())
                .hasSameSizeAs(commands)
                .extractingResultOf("getCommand")
                .containsExactlyElementsOf(commands)
                .doNotHave(new Condition<>(SimpleConfiguration.NoBeanCommand.class::isInstance, "NoBeanCommand"));
    }

    @Test
    public void autoConfiguration_NestedBeanDefinition_CreateNestedSubCommands() {
        load(NestedCommandConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);
        Collection<Object> commands = context.getBeansWithAnnotation(Command.class).values();

        Function<CommandLine, Collection<CommandLine>> extractor = input -> input.getSubcommands().values();
        assertThat(commands).hasSize(5);

        CommandLine current = runner.getCommandLine();
        this.logCommandTree(current);

        assertThat(runner.getCommandLine().getSubcommands().values())
                .hasSize(1)
                .haveExactly(1, new Condition<>(e -> {
                    Class clazz = NestedCommandConfiguration.Level0Command.class;
                    return e.getCommand().getClass().equals(clazz);
                }, "Class Level0Command"))
                .flatExtracting(extractor)
                .hasSize(2)
                .haveExactly(1, new Condition<>(e -> {
                    Class clazz = NestedCommandConfiguration.Level0Command.Level1Command.class;
                    return e.getCommand().getClass().equals(clazz);
                }, "Class Level1Command"))
                .haveExactly(1, new Condition<>(e -> {
                    Class clazz = NestedCommandConfiguration.Level0Command.Level1bCommand.class;
                    return e.getCommand().getClass().equals(clazz);
                }, "Class Level1bCommand"))
                .doNotHave(new Condition<>(e -> {
                    Class clazz = NestedCommandConfiguration.Level0Command.NoBeanCommand.class;
                    return e.getCommand().getClass().equals(clazz);
                }, "Class NoBeanCommand"))
                .flatExtracting(extractor)
                .hasSize(1)
                .haveExactly(1, new Condition<>(e -> {
                    Class clazz = NestedCommandConfiguration.Level0Command.Level1Command.Level2Command.class;
                    return e.getCommand().getClass().equals(clazz);
                }, "Class Level2Command"));
    }

    private void logCommandTree(CommandLine current) {
        for (String command : current.getSubcommands().keySet()) {
            CommandLine subCommand = current.getSubcommands().get(command);
            System.out.println(
                    "current command: " + current.getCommandName() + " sub command: " + subCommand.getCommandName());
            this.logCommandTree(subCommand);
        }
    }

    @Test(expected = RuntimeException.class)
    public void autoConfiguration_MultipleMainCommands_RandomUses() {
        load(MainCommandsConflictConfiguration.class);
    }

    @Test
    public void autoConfiguration_WithPicocliConfigurerAdapter_Apply() {
        load(SimpleConfiguration.class, CustomPicocliConfigurerAdapter.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        assertThat(runner.getCommandLine().getSeparator()).isEqualTo("¯\\_(ツ)_/¯");
    }

    @Test
    public void autoConfiguration_WithMultiplePicocliConfigurerAdapters_ApplyAll() {
        load(SimpleConfiguration.class, CustomPicocliConfigurerAdapter.class, Custom2PicocliConfigurerAdapter.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        assertThat(runner.getCommandLine().getSeparator()).isEqualTo("¯\\_(ツ)_/¯");
        assertThat(runner.getCommandLine().getSubcommands()).containsKeys("¯\\_(ツ)_/¯");
    }

    @Test
    public void autoConfiguration_WithSubCommandsParameter_GetBeanIfExists() {
        load(SubCommandConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        assertThat(runner.getCommandLine().getSubcommands().get("basic").getSubcommands().values())
                .isNotEmpty()
                .extracting(CommandLine::getCommand)
                .allMatch(c -> c instanceof SubCommandConfiguration.DummyBeanCommand)
                .allMatch(c -> ((SubCommandConfiguration.DummyBeanCommand) c).getDummyBean() != null);
    }

    @Test
    public void autoConfiguration_WithSubCommandsParameterAndNested_OnlyOneCommand() {
        load(SubSubCommandConfiguration.class);
        PicocliCommandLineRunner runner = context.getBean(PicocliCommandLineRunner.class);

        assertThat(runner.getCommandLine().getSubcommands().get("basic").getSubcommands().values())
                .hasSize(1)
                .extracting(CommandLine::getCommand)
                .allMatch(c -> c instanceof SubSubCommandConfiguration.BasicCommand.NestedSubCommand)
                .allMatch(c -> ((SubSubCommandConfiguration.BasicCommand.NestedSubCommand) c).getDummyBean() != null);
    }

    @Configuration
    static class EmptyConfiguration {
    }

    @Configuration
    static class CommandLineConfiguration {
        @Bean
        CommandLine commandLine() {
            return new CommandLine(new DummyCommand());
        }

        @Command
        static class DummyCommand {
        }
    }

    @Configuration
    static class SimpleConfiguration {

        @Component
        @Command(name = "basic")
        static class BasicCommand {}

        @Component
        @Command(name = "extends command", mixinStandardHelpOptions = true)
        static class ExtendsCommand {}

        @Command(name = "No bean command, not considered")
        static class NoBeanCommand {}
    }

    @Configuration
    static class NestedCommandConfiguration {

        @Bean
        Level0Command.NoBeanCommand.OrphanCommand orphanCommand() {
            return new Level0Command.NoBeanCommand.OrphanCommand();
        }

        @Component
        @Command(name = "level 0")
        static class Level0Command {

            @Component
            @Command(name = "level 1")
            static class Level1Command {

                @Component
                @Command(name = "level 2")
                static class Level2Command {}
            }

            @Component
            @Command(name = "level 1 b")
            static class Level1bCommand {

            }

            @Command(name = "No bean command, not considered")
            static class NoBeanCommand {

                @Command(name = "orphan command")
                static class OrphanCommand {}
            }
        }
    }

    @Configuration
    static class MainCommandsConflictConfiguration {

        @Component
        @Command
        static class MainCommand {}

        @Component
        @Command
        static class MainCommand2 {}
    }

    @Configuration
    static class CustomPicocliConfigurerAdapter extends PicocliConfigurerAdapter {
        @Override
        public void configure(CommandLine commandLine) {
            commandLine.setSeparator("¯\\_(ツ)_/¯");
        }
    }

    @Configuration
    static class Custom2PicocliConfigurerAdapter extends PicocliConfigurerAdapter {
        @Override
        public void configure(CommandLine commandLine) {
            commandLine.addSubcommand("¯\\_(ツ)_/¯", new BasicCommand());
        }

        @Command
        static class BasicCommand {}
    }

    @Configuration
    static class SubCommandConfiguration {
        @Bean
        DummyBean dummyBean() {
            return new DummyBean("¯\\_(ツ)_/¯");
        }

        static class DummyBean {
            private String title;

            public DummyBean(String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }
        }

        @Component
        @Command(name = "basic",
                subcommands = { ConstructorInjectionSubCommand.class, SetterInjectionSubCommand.class })
        static class BasicCommand {

            @Component
            @Command(name = "sub3")
            static class NestedSubCommand implements DummyBeanCommand {
                @Autowired
                private DummyBean dummyBean;

                public DummyBean getDummyBean() {
                    return dummyBean;
                }
            }
        }

        interface DummyBeanCommand {
            DummyBean getDummyBean();
        }

        @Component
        @Command(name = "sub1")
        static class ConstructorInjectionSubCommand implements DummyBeanCommand {
            private final DummyBean dummyBean;

            public ConstructorInjectionSubCommand(DummyBean dummyBean) {
                this.dummyBean = dummyBean;
            }

            public DummyBean getDummyBean() {
                return dummyBean;
            }
        }

        @Component
        @Command(name = "sub2")
        static class SetterInjectionSubCommand implements DummyBeanCommand {
            @Autowired
            private DummyBean dummyBean;

            public DummyBean getDummyBean() {
                return dummyBean;
            }
        }
    }

    @Configuration
    static class SubSubCommandConfiguration {
        @Bean
        DummyBean dummyBean() {
            return new DummyBean("¯\\_(ツ)_/¯");
        }

        static class DummyBean {
            private String title;

            public DummyBean(String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }
        }

        @Component
        @Command(name = "basic",
                subcommands = { BasicCommand.NestedSubCommand.class })
        static class BasicCommand {

            @Component
            @Command(name = "sub3")
            static class NestedSubCommand {
                @Autowired
                private DummyBean dummyBean;

                public DummyBean getDummyBean() {
                    return dummyBean;
                }
            }
        }
    }

    private void load(Class<?>... configs) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(configs);
        context.register(PicocliAutoConfiguration.class);
        context.refresh();
        this.context = context;
    }

}
