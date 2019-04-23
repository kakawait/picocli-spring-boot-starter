package com.kakawait.spring.boot.picocli.autoconfigure;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ReflectionUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * @author Thibaud Leprêtre
 */
@Configuration
@ConditionalOnClass(CommandLine.class)
@Import(PicocliAutoConfiguration.CommandlineConfiguration.class)
class PicocliAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CommandLine.IFactory.class)
    CommandLine.IFactory applicationAwarePicocliFactory(ApplicationContext applicationContext) {
        return new ApplicationContextAwarePicocliFactory(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(PicocliCommandLineRunner.class)
    @ConditionalOnBean(CommandLine.class)
    CommandLineRunner picocliCommandLineRunner(CommandLine cli) {
        return new PicocliCommandLineRunner(cli);
    }

    @ConditionalOnMissingBean(CommandLine.class)
    @Conditional(CommandCondition.class)
    static class CommandlineConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(CommandlineConfiguration.class);

        private final CommandLine.IFactory applicationAwarePicocliFactory;

        public CommandlineConfiguration(CommandLine.IFactory applicationAwarePicocliFactory) {
            this.applicationAwarePicocliFactory = applicationAwarePicocliFactory;
        }

        @Bean
        CommandLine picocliCommandLine(ApplicationContext applicationContext) {
            Collection<Object> commands = applicationContext.getBeansWithAnnotation(Command.class).values();
            List<Object> mainCommands = getMainCommands(commands);
            Object mainCommand = mainCommands.isEmpty() ? new HelpAwarePicocliCommand() {} : mainCommands.get(0);
            if (mainCommands.size() > 1) {
                throw new RuntimeException("Multiple mains command founds: " + Collections.singletonList(mainCommands));
            }
            commands.removeAll(mainCommands);

            CommandLine cli = new CommandLine(mainCommand, applicationAwarePicocliFactory);
            registerCommands(cli, commands);

            applicationContext.getBeansOfType(PicocliConfigurer.class).values().forEach(c -> c.configure(cli));
            return cli;
        }

        private String getCommandName(Object command) {
            if (command == null) {
                return null;
            }
            return AopUtils.getTargetClass(command).getAnnotation(Command.class).name();
        }

        private String getCommandName(Class<?> commandClass) {
            if (commandClass == null) {
                return null;
            }
            return commandClass.getAnnotation(Command.class).name();
        }

        private int getNestedLevel(Class clazz) {
            int level = 0;
            Class parent = clazz.getEnclosingClass();
            while (parent != null && parent.isAnnotationPresent(Command.class)) {
                parent = parent.getEnclosingClass();
                level += 1;
            }
            return level;
        }

        private Optional<Class> getParentClass(Class clazz) {
            Class parentClass = clazz.getEnclosingClass();
            if (parentClass == null || !parentClass.isAnnotationPresent(Command.class)) {
                return Optional.empty();
            }
            return Optional.of(parentClass);
        }

        private List<Object> getMainCommands(Collection<Object> candidates) {
            List<Object> mainCommands = new ArrayList<>();
            for (Object candidate : candidates) {
                Class<?> clazz = AopUtils.getTargetClass(candidate);
                Method method = ReflectionUtils.findMethod(Command.class, "name");
                if (clazz.isAnnotationPresent(Command.class)
                        && method != null
                        && clazz.getAnnotation(Command.class).name().equals(method.getDefaultValue())) {
                    mainCommands.add(candidate);
                }
            }
            return mainCommands;
        }

        private Map<Node, List<Object>> findCommands(Collection<Object> commands) {
            Map<Node, List<Object>> tree = new LinkedHashMap<>();

            commands.stream()
                    .sorted((o1, o2) -> {
                        int l1 = getNestedLevel(AopUtils.getTargetClass(o1));
                        int l2 = getNestedLevel(AopUtils.getTargetClass(o2));
                        return Integer.compare(l1, l2);
                    })
                    .forEach(o -> {
                        Class<?> clazz = AopUtils.getTargetClass(o);
                        Optional<Class> parentClass = getParentClass(clazz);
                        parentClass.ifPresent(c -> {
                            List<Object> objects = tree.get(new Node(c, null, null));
                            if (objects != null) {
                                objects.add(o);
                            }
                        });
                        tree.put(new Node(clazz, o, parentClass.orElse(null)), new ArrayList<>());
                    });

            return tree;
        }

        private void registerCommands(CommandLine cli, Collection<Object> commands) {
            CommandLine current = cli;
            Map<Class<?>, CommandLine> parents = new HashMap<>();
            for (Map.Entry<Node, List<Object>> entry : findCommands(commands).entrySet()) {
                Node node = entry.getKey();
                // Avoid parent "adopting" orphan node (I know is hard for orphan children but life is hard)
                if (node.getParent() != null && !node.getParent().equals(current.getCommand().getClass())) {
                    logger.warn("Orphan command may be detected {}, skipped!", node.getObject());
                    continue;
                }
                List<Object> children = entry.getValue();
                Object command = node.getObject();
                String commandName = getCommandName(node.getClazz());
                if (parents.containsKey(node.getParent())) {
                    current = parents.get(node.getParent());
                } else if (node.getParent() == null) {
                    current = cli;
                }
                
                if (children.isEmpty()) {
                    if (!current.getSubcommands().containsKey(commandName)) {
                        current.addSubcommand(commandName, command);
                    }
                } else {
                    CommandLine sub;
                    if (!current.getSubcommands().containsKey(commandName)) {
                        sub = new CommandLine(command, applicationAwarePicocliFactory);
                        current.addSubcommand(commandName, sub);
                    } else {
                        // get the reference of subCommands from current, instead of creating new one
                        sub = current.getSubcommands().get(commandName);
                    }
                    
                    for (Object child : children) {
                        String childCommandName = getCommandName(child);
                        if (!sub.getSubcommands().containsKey(childCommandName)) {
                            sub.addSubcommand(childCommandName, new CommandLine(child, applicationAwarePicocliFactory));
                        }
                    }
                    current = sub;
                }
                parents.put(node.getClazz(), current);
            }
        }

        private static class Node {
            private final Class<?> clazz;

            private final Object object;

            private final Class<?> parent;

            Node(Class<?> clazz, Object object, Class<?> parent) {
                this.clazz = clazz;
                this.object = object;
                this.parent = parent;
            }

            Class<?> getClazz() {
                return clazz;
            }

            Object getObject() {
                return object;
            }

            Class<?> getParent() {
                return parent;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Node)) return false;

                Node node = (Node) o;

                return Objects.equals(clazz, node.clazz);
            }

            @Override
            public int hashCode() {
                return clazz != null ? clazz.hashCode() : 0;
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append("Node [clazz=");
                builder.append(clazz);
                builder.append(", object=");
                builder.append(object);
                builder.append(", parent=");
                builder.append(parent);
                builder.append("]");
                return builder.toString();
            }
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
}
