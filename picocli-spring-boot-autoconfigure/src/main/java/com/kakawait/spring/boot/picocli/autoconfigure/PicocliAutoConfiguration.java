package com.kakawait.spring.boot.picocli.autoconfigure;

import com.kakawait.spring.boot.picocli.autoconfigure.support.ClassHierarchyPicocliCommandRegistrator;
import com.kakawait.spring.boot.picocli.autoconfigure.support.DefaultPicocliMainCommandLocator;
import com.kakawait.spring.boot.picocli.autoconfigure.support.PicocliCommandRegistrator;
import com.kakawait.spring.boot.picocli.autoconfigure.support.PicocliMainCommandSelector;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Collection;
import java.util.List;

import static com.kakawait.spring.boot.picocli.autoconfigure.PicocliAutoConfiguration.*;
import static picocli.CommandLine.*;

/**
 * @author Thibaud Leprêtre
 */
@Configuration
@ConditionalOnClass(CommandLine.class)
@EnableConfigurationProperties(value = PicocliProperties.class)
@Import(value = { CommandlineConfiguration.class, PicocliCommandLineRunnerConfiguration.class,
        ClassHierarchyCommandRegistratorConfiguration.class })
class PicocliAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IFactory.class)
    IFactory commandFactory(ApplicationContext applicationContext) {
        return new ApplicationContextAwarePicocliFactory(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(PicocliMainCommandSelector.class)
    PicocliMainCommandSelector picocliMainCommandSelector() {
        return new DefaultPicocliMainCommandLocator();
    }

    @Bean
    @ConditionalOnMissingBean(IVersionProvider.class)
    IVersionProvider versionProvider(ApplicationContext applicationContext,
            PicocliProperties properties) {
        return new SpringPropertyVersionProvider(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean(IParseResultHandler2.class)
    IParseResultHandler2<List<Object>> parseResultHandler() {
        return new RunLast();
    }

    @Bean
    @ConditionalOnMissingBean(IExceptionHandler2.class)
    IExceptionHandler2 exceptionHandler() {
        return new DefaultExceptionHandler();
    }

    @Conditional(CommandCondition.class)
    @ConditionalOnBean(CommandLine.class)
    @ConditionalOnMissingBean(PicocliCommandLineRunner.class)
    static class PicocliCommandLineRunnerConfiguration {
        private final CommandLine commandLine;

        private final IParseResultHandler2 parseResultHandler;

        private final IExceptionHandler2 exceptionHandler;

        public PicocliCommandLineRunnerConfiguration(CommandLine commandLine,
                IParseResultHandler2 parseResultHandler, IExceptionHandler2 exceptionHandler) {
            this.commandLine = commandLine;
            this.parseResultHandler = parseResultHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Bean
        CommandLineRunner picocliCommandLineRunner() {
            return new PicocliCommandLineRunner(commandLine, parseResultHandler, exceptionHandler);
        }
    }

    @ConditionalOnProperty(value = "picocli.class-hierarchy-scanning.enabled", matchIfMissing = true)
    static class ClassHierarchyCommandRegistratorConfiguration {
        private final IFactory picocliFactory;

        public ClassHierarchyCommandRegistratorConfiguration(IFactory picocliFactory) {
            this.picocliFactory = picocliFactory;
        }

        @Bean
        PicocliCommandRegistrator classHierarchyPicocliCommandRegistrator() {
            return new ClassHierarchyPicocliCommandRegistrator(picocliFactory);
        }
    }

    @ConditionalOnMissingBean(CommandLine.class)
    static class CommandlineConfiguration {

        private final List<PicocliConfigurer> configurers;

        private final IFactory picocliFactory;

        private final PicocliMainCommandSelector mainCommandSelector;

        private final List<PicocliCommandRegistrator> registrators;

        public CommandlineConfiguration(List<PicocliConfigurer> configurers, IFactory picocliFactory,
                PicocliMainCommandSelector mainCommandSelector, List<PicocliCommandRegistrator> registrators) {
            this.configurers = configurers;
            this.picocliFactory = picocliFactory;
            this.mainCommandSelector = mainCommandSelector;
            this.registrators = registrators;
        }

        @Bean
        CommandLine picocliCommandLine(ApplicationContext applicationContext) {
            Collection<Object> commands = applicationContext.getBeansWithAnnotation(Command.class).values();
            Object mainCommand = mainCommandSelector.select(commands).orElse(new MainCommand());
            commands.remove(mainCommand);

            CommandLine cli = new CommandLine(mainCommand, picocliFactory);
            registrators.forEach(r -> r.register(cli, commands));
            configurers.forEach(c -> c.configure(cli));
            return cli;
        }
    }

    static class CommandCondition extends SpringBootCondition implements ConfigurationCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String[] commands = context.getBeanFactory().getBeanNamesForAnnotation(Command.class);
            ConditionMessage.Builder message = ConditionMessage.forCondition("@Command Condition");
            if (commands.length == 0) {
                return ConditionOutcome.noMatch(message.didNotFind("@Command beans").atAll());
            } else {
                return ConditionOutcome.match(message.found("@Command beans").items((Object[]) commands));
            }
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }

    @Command(mixinStandardHelpOptions = true, versionProvider = IVersionProvider.class)
    private static class MainCommand {}

}
