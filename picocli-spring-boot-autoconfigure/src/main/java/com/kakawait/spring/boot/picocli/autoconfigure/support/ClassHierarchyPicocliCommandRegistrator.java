package com.kakawait.spring.boot.picocli.autoconfigure.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thibaud Leprêtre
 */
public class ClassHierarchyPicocliCommandRegistrator implements PicocliCommandRegistrator {

    private static final Logger logger = LoggerFactory.getLogger(ClassHierarchyPicocliCommandRegistrator.class);

    private final CommandLine.IFactory picocliFactory;

    private final ClassHierarchyPicocliCommandTreeDiscoverer commandTreeDiscoverer =
            new ClassHierarchyPicocliCommandTreeDiscoverer();

    public ClassHierarchyPicocliCommandRegistrator(CommandLine.IFactory picocliFactory) {
        this.picocliFactory = picocliFactory;
    }

    @Override
    public void register(CommandLine cli, Collection<Object> commands) {
        CommandLine current = cli;
        Map<Class<?>, CommandLine> parents = new HashMap<>();
        for (Map.Entry<CommandNode, List<Object>> entry : commandTreeDiscoverer.discover(commands).entrySet()) {
            CommandNode node = entry.getKey();
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
                    sub = new CommandLine(command, picocliFactory);
                    current.addSubcommand(commandName, sub);
                } else {
                    // get the reference of subCommands from current, instead of creating new one
                    sub = current.getSubcommands().get(commandName);
                }

                for (Object child : children) {
                    String childCommandName = getCommandName(child);
                    if (!sub.getSubcommands().containsKey(childCommandName)) {
                        sub.addSubcommand(childCommandName, new CommandLine(child, picocliFactory));
                    }
                }
                current = sub;
            }
            parents.put(node.getClazz(), current);
        }
    }

    private String getCommandName(Object command) {
        if (command == null) {
            return null;
        }
        return AopUtils.getTargetClass(command).getAnnotation(CommandLine.Command.class).name();
    }

    private String getCommandName(Class<?> commandClass) {
        if (commandClass == null) {
            return null;
        }
        return commandClass.getAnnotation(CommandLine.Command.class).name();
    }
}
