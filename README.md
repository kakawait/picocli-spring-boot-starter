# Spring boot Picocli starter

[![Travis](https://img.shields.io/travis/kakawait/picocli-spring-boot-starter.svg)](https://travis-ci.org/kakawait/picocli-spring-boot-starter)
[![SonarQube Coverage](https://img.shields.io/sonar/https/sonarcloud.io/com.kakawait%3Apicocli-spring-boot-starter-parent/coverage.svg)](https://sonarcloud.io/component_measures?id=com.kakawait%3Apicocli-spring-boot-starter-parent&metric=coverage)
[![Maven Central](https://img.shields.io/maven-central/v/com.kakawait/picocli-spring-boot-starter.svg)](https://search.maven.org/#artifactdetails%7Ccom.kakawait%7Cpicocli-spring-boot-starter%7C0.1.0%7Cjar)
[![license](https://img.shields.io/github/license/kakawait/picocli-spring-boot-starter.svg)](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/LICENSE.md)

> A Spring boot starter for [Picocli](http://picocli.info/) command line tools. That let you easily write CLI for Spring boot application!

## Features

- Automatic integration with Spring [`CommandLineRunner`](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-command-line-runner)
- Automatic `@Command` beans registration
- Display usage on _help_
- Automatically run `@Command` if it implements `java.lang.Runnable` or `java.lang.Callable`
- Flow control using `java.lang.Callable` and `ExitStatus`
- Advance configuration through [PicocliConfigurerAdapter](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-autoconfigure/src/main/java/com/kakawait/spring/boot/picocli/autoconfigure/PicocliConfigurerAdapter.java)

## Setup

Add the Spring boot starter to your project

```xml
<dependency>
  <groupId>com.kakawait</groupId>
  <artifactId>picocli-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Usage

There is multiple ways to define a new picocli commands.

You should start looking [sample application](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-sample/src/main/java/com/kakawait/PicocliSpringBootSampleApplication.java) to get more advance sample.

### `@Command` beans

First and simplest way, is to register a new bean with `@Command` annotation inside your _Spring_ context, example:
 
```java
@Component
@Command(name = "greeting")
class GreetingCommand implements Runnable {

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
```

Your bean can implements `Runnable` or `Callable<ExitStatus>` (in order to control flow) or extends [`PicocliCommand`](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-autoconfigure/src/main/java/com/kakawait/spring/boot/picocli/autoconfigure/PicocliCommand.java) or [`HelpAwarePicocliCommand`](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-autoconfigure/src/main/java/com/kakawait/spring/boot/picocli/autoconfigure/HelpAwarePicocliCommand.java) (to magically have `-h/--help` option to display usage) if you need to execute something when command is called.

In addition, for advance usage [`PicocliCommand`](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-autoconfigure/src/main/java/com/kakawait/spring/boot/picocli/autoconfigure/PicocliCommand.java) let you get access to 

```java
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
```

That might be useful.

#### Main command using beans

Picocli is waiting for a _Main_ command, cf: `new CommandLine(mainCommand)`. To determine which `@Command` beans will be the _Main_ command, starter will apply the following logic:

> _Main_ command will be the first `@Command` (if multiple found) bean with default `name` argument.

For example

```java
@Command
class MainCommand {}
```

or 

```java
@Command(description = "main command")
class MainCommand {}
```

But the following example will not be candidate for _Main_ command

```java
@Command(name = "my_command")
class MainCommand {}
```

#### Nested sub-commands using beans

Picocli allows [_nested sub-commands_](http://picocli.info/#_nested_sub_subcommands), in order to describe a _nested sub-command_, starter is offering you nested classes scanning capability.

That means, if you're defining **bean** structure like following:

```java
@Component
@Command(name = "flyway")
class FlywayCommand extends HelpAwareContainerPicocliCommand {

    @Component
    @Command(name = "migrate")
    class MigrateCommand implements Runnable {

        private final Flyway flyway;

        public MigrateCommand(Flyway flyway) {
            this.flyway = flyway;
        }

        @Override
        public void run() {
            flyway.migrate();
        }
    }

    @Component
    @Command(name = "repair")
    class RepairCommand implements Runnable {
        private final Flyway flyway;

        public RepairCommand(Flyway flyway) {
            this.flyway = flyway;
        }

        @Override
        public void run() {
            flyway.repair();
        }
    }
}
```

Will generate command line 

```
Commands:
   flyway [-h, --help]
     migrate
     repair
```

Thus `java -jar <name>.jar flyway migrate` will execute _Flyway_ migration.

**ATTENTION** every classes must be a bean (`@Component`) with `@Command` annotation without forgetting to file `name` attribute.

There is **no limitation** about nesting level.

### Additional configuration

If you need to set additional configuration options simply register within _Spring_ application context instance of [`PicocliConfigurerAdapter`](https://github.com/kakawait/picocli-spring-boot-starter/blob/master/picocli-spring-boot-autoconfigure/src/main/java/com/kakawait/spring/boot/picocli/autoconfigure/PicocliConfigurerAdapter.java)

```java
@Configuration
class CustomPicocliConfiguration extends PicocliConfigurerAdapter {
    @Override
    public void configure(CommandLine commandLine) {
        // Here you can configure Picocli commandLine
        // You can add additional sub-commands or register converters.
    }
}
```

Otherwise you can define your own bean `CommandLine` but attention that will disable automatic `@Command` bean registration explained above.

## Exit status

If you defined following command line:

```java
@Component
@Command
class MainCommand extends HelpAwarePicocliCommand {
    @Option(names = {"-v", "--version"}, description = "display version info")
    boolean versionRequested;

    @Override
    public void run() {
        if (versionRequested) {
            System.out.println("0.1.0");
        }
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
```

And you execute `java -jar <name>.jar -v greeting Thibaud` the output will looks like:

```
0.1.0
Hello Thibaud!
```

While you wanted that `-v` will break execution and other involved commands not executed. To achieve that you must replace `run()` method with `ExitStatus call()`.

```java
@Component
@Command
class MainCommand extends HelpAwarePicocliCommand {
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
```

The main difference is `ExitStatus.TERMINATION` that will tell the starter to stop other executions. (`ExitStatus.OK` is default status).

## Help & usage

Picocli [documentation](http://picocli.info/#_help_options) and principle about `help` argument is not exactly the same on this starter.

Indeed Picocli only consider option with `help` argument like following:

> if one of the command line arguments is a "help" option, picocli will stop parsing the remaining arguments and will not check for required options.

While this starter in addition will force displaying _usage_ if `help` was requested.

Thus following example from Picocli documentation:

```java
@Option(names = {"-V", "--version"}, help = true, description = "display version info")
boolean versionRequested;

@Option(names = {"-h", "--help"}, help = true, description = "display this help message")
boolean helpRequested;
```

If you run program with `-V` or `--version` that will display usage and stop execution. It may not what you need. Thus you have to think about `help` argument is to displaying usage and stop execution.

Following example is much more starter compliant:

```java
@Option(names = {"-V", "--version"}, help = false, description = "display version info")
boolean versionRequested;

@Option(names = {"-h", "--help"}, help = true, description = "display this help message")
boolean helpRequested;
```

## License

MIT License
