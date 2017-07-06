package com.kakawait.spring.boot.picocli.autoconfigure;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static picocli.CommandLine.Command;

/**
 * @author Thibaud Leprêtre
 */
@Configuration
@ConditionalOnClass(CommandLine.class)
@Import(PicocliAutoConfiguration.CommandlineConfiguration.class)
class PicocliAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PicocliCommandLineRunner.class)
    CommandLineRunner picocliCommandLineRunner(CommandLine cli) {
        return new PicocliCommandLineRunner(cli);
    }

    @ConditionalOnMissingBean(CommandLine.class)
    static class CommandlineConfiguration {

        @Bean
        CommandLine picocliCommandLine(ApplicationContext applicationContext) {
            Collection<Object> commands = applicationContext.getBeansWithAnnotation(Command.class).values();
            Object main = getMainCommand(commands);
            commands.remove(main);

            CommandLine cli = new CommandLine(main);
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

        private Object getMainCommand(Collection<Object> candidates) {
            Object mainCommand = null;
            for (Object candidate : candidates) {
                Class<?> clazz = AopUtils.getTargetClass(candidate);
                Method method = ReflectionUtils.findMethod(Command.class, "name");
                if (clazz.isAnnotationPresent(Command.class)
                        && method != null
                        && clazz.getAnnotation(Command.class).name().equals(method.getDefaultValue())) {
                    mainCommand = candidate;
                    break;
                }
            }
            if (mainCommand == null) {
                mainCommand = new PicocliCommand() {};
            }
            return mainCommand;
        }

        private Map<Node, List<Object>> findCommands(Collection<Object> commands) {
            Map<Node, List<Object>> tree = new LinkedHashMap<>();

            commands.stream()
                    .sorted((o1, o2) -> {
                        int l1 = getNestedLevel(AopUtils.getTargetClass(o1));
                        int l2 = getNestedLevel(AopUtils.getTargetClass(o2));
                        if (l1 > l2) {
                            return 1;
                        } else if (l1 < l2) {
                            return -1;
                        } else {
                            return 0;
                        }
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
                List<Object> children = entry.getValue();
                Object command = node.getObject();
                String commandName = getCommandName(node.getClazz());
                if (parents.containsKey(node.getParent())) {
                    current = parents.get(node.getParent());
                } else if (node.getParent() == null) {
                    current = cli;
                }
                if (children.isEmpty()) {
                    current.addSubcommand(commandName, command);
                } else {
                    CommandLine sub = new CommandLine(command);
                    current.addSubcommand(commandName, sub);
                    for (Object child : children) {
                        sub.addSubcommand(getCommandName(child), new CommandLine(child));
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

                return clazz != null ? clazz.equals(node.clazz) : node.clazz == null;
            }

            @Override
            public int hashCode() {
                return clazz != null ? clazz.hashCode() : 0;
            }

            @Override
            public String toString() {
                StringBuffer sb = new StringBuffer("Node{");
                sb.append("clazz=").append(clazz);
                sb.append(", parent=").append(parent);
                sb.append('}');
                return sb.toString();
            }
        }
    }
}
