package com.kakawait;

import com.google.common.base.CaseFormat;
import com.kakawait.spring.boot.picocli.autoconfigure.ExitStatus;
import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import com.kakawait.spring.boot.picocli.autoconfigure.PicocliConfigurerAdapter;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static picocli.CommandLine.Command;

/**
 * Picocli spring boot starter sample
 *
 * This sample will create following CLI
 *
 * <pre>
 * {@code
 * Usage: <main class> [-vh]
 *   -v, --version               display version info
 *   -h, --help                  Prints this help message and exits
 * Commands:
 *   flyway [-h, --help]
 *     migrate
 *     repair
 *   greeting [-h, --help] [NAME]
 *   health [-h, --help]
 *     db
 *     disk-space
 * }
 * </pre>
 * Thus running following commands should output following:
 * <pre>
 * {@code
 * $> java -jar <name>.jar -v
 * 0.1.0
 *
 * $> java -jar <name>.jar -h
 * Usage: <main class> [-vh]
 *   -v, --version               display version info
 *   -h, --help                  Prints this help message and exits
 * Commands:
 *   flyway
 *   greeting
 *   health
 *
 * $> java -jar <name>.jar flyway
 * Usage: flyway [-h]
 *   -h, --help                  Prints this help message and exits
 * Commands:
 *   migrate
 *   repair
 *
 * $> java -jar <name>.jar flyway migrate
 * 2017-07-06 11:21:14.560  INFO 77637 --- [main] o.f.core.internal.util.VersionPrinter    : Flyway 3.2.1 by Boxfuse
 * 2017-07-06 11:21:14.567  INFO 77637 --- [main] o.f.c.i.dbsupport.DbSupportFactory       : Database: jdbc:h2:mem:testdb (H2 1.4)
 * 2017-07-06 11:21:14.601  INFO 77637 --- [main] o.f.core.internal.command.DbValidate     : Validated 2 migrations (execution time 00:00.013s)
 * 2017-07-06 11:21:14.621  INFO 77637 --- [main] o.f.c.i.metadatatable.MetaDataTableImpl  : Creating Metadata table: "PUBLIC"."schema_version"
 * 2017-07-06 11:21:14.638  INFO 77637 --- [main] o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": << Empty Schema >>
 * 2017-07-06 11:21:14.638  INFO 77637 --- [main] o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version 1 - init
 * 2017-07-06 11:21:14.666  INFO 77637 --- [main] o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version 2 - add
 * 2017-07-06 11:21:14.672  INFO 77637 --- [main] o.f.core.internal.command.DbMigrate      : Successfully applied 2 migrations to schema "PUBLIC" (execution time 00:00.053s).
 *
 * $> java -jar <name>.jar health disk-space
 * UP {total=420143575040, free=41032192000, threshold=10485760}
 *
 * $> java -jar <name>.jar health db
 * UP {database=H2, hello=1}
 * }
 * </pre>
 * @author Thibaud Leprêtre
 */
@SpringBootApplication
public class PicocliSpringBootSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PicocliSpringBootSampleApplication.class, args);
    }

    /**
     * Disable flyway automatic migration on startup.
     * I will be piloted using CLI
     * @return No operation flyway migration strategy
     */
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {};
    }

    @Configuration
    static class PicocliConfiguration extends PicocliConfigurerAdapter {

        private static final Logger logger = LoggerFactory.getLogger(PicocliConfiguration.class);

        private static final Pattern HEALTH_PATTERN = Pattern.compile("^(\\w+?)HealthIndicator$");

        private final Map<String, HealthIndicator> healthIndicators;

        public PicocliConfiguration(Map<String, HealthIndicator> healthIndicators) {
            this.healthIndicators = healthIndicators;
        }

        @Override
        public void configure(CommandLine cli) {
            CommandLine healthCli = new CommandLine(new HelpAwareContainerPicocliCommand() {});
            for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                Matcher matcher = HEALTH_PATTERN.matcher(entry.getKey());
                if (matcher.matches()) {
                    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, matcher.group(1));
                    healthCli.addSubcommand(name, new PrintCommand(entry.getValue().health()));
                } else {
                    logger.warn("Unable to determine a correct name for given indicator: \"{}\", skip it!",
                            entry.getKey());
                }
            }
            cli.addSubcommand("health", healthCli);
        }

        @Command
        private static class PrintCommand implements Runnable {
            private final Object object;

            PrintCommand(Object object) {
                this.object = object;
            }

            @Override
            public void run() {
                System.out.println(object);
            }
        }
    }

    @Component
    @Command
    static class MainCommand extends HelpAwarePicocliCommand {
        @Option(names = {"-v", "--version"}, description = "display version info")
        boolean versionRequested;

        @Override
        public ExitStatus call() {
            if (versionRequested) {
                System.out.println("0.1.0");
                return ExitStatus.TERMINATION;
            }
            return ExitStatus.OK;
        }
    }

    @Component
    @Command(name = "greeting")
    static class GreetingCommand extends HelpAwarePicocliCommand {

        @Parameters(paramLabel = "NAME", description = "name", arity = "0..1")
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

    @Component
    @Command(name = "flyway", subcommands = { RepairCommand.class })
    static class FlywayCommand extends HelpAwareContainerPicocliCommand {

        @Component
        @Command(name = "migrate")
        static class MigrateCommand implements Runnable {

            private final Flyway flyway;

            public MigrateCommand(Flyway flyway) {
                this.flyway = flyway;
            }

            @Override
            public void run() {
                flyway.migrate();
            }
        }
    }

    @Component
    @Command(name = "repair")
    static class RepairCommand implements Runnable {
        private final Flyway flyway;

        public RepairCommand(Flyway flyway) {
            this.flyway = flyway;
        }

        @Override
        public void run() {
            flyway.repair();
        }
    }

    @Command
    private static abstract class HelpAwareContainerPicocliCommand extends HelpAwarePicocliCommand {
        @Override
        public ExitStatus call() {
            if (getParsedCommands().get(getParsedCommands().size() - 1).getCommand().equals(this)) {
                getContext().usage(System.out, Ansi.AUTO);
                return ExitStatus.TERMINATION;
            }
            return ExitStatus.OK;
        }
    }

}
