package com.kakawait.spring.boot.picocli.autoconfigure.support;

import org.springframework.aop.support.AopUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Thibaud Leprêtre
 */
class ClassHierarchyPicocliCommandTreeDiscoverer {

    Map<CommandNode, List<Object>> discover(Collection<Object> commands) {
        Map<CommandNode, List<Object>> tree = new LinkedHashMap<>();

        Collection<Class> excludes = getExcludedClasses(commands);

        commands.stream()
                .sorted((o1, o2) -> {
                    int l1 = getNestedLevel(AopUtils.getTargetClass(o1));
                    int l2 = getNestedLevel(AopUtils.getTargetClass(o2));
                    return Integer.compare(l1, l2);
                })
                .forEach(o -> {
                    Class<?> clazz = AopUtils.getTargetClass(o);
                    if (!excludes.contains(clazz)) {
                        Optional<Class> parentClass = getParentClass(clazz);
                        parentClass.ifPresent(c -> {
                            List<Object> objects = tree.get(new CommandNode(c, null, null));
                            if (objects != null) {
                                objects.add(o);
                            }
                        });
                        tree.put(new CommandNode(clazz, o, parentClass.orElse(null)), new ArrayList<>());
                    }
                });

        return tree;
    }

    private Collection<Class> getExcludedClasses(Collection<Object> commands) {
        return commands
                .stream()
                .map(AopUtils::getTargetClass)
                .filter(c -> c.isAnnotationPresent(CommandLine.Command.class))
                .map(c -> c.getAnnotation(CommandLine.Command.class))
                .filter(a -> a.subcommands().length > 0)
                .flatMap(a -> Arrays.stream(a.subcommands()))
                .collect(Collectors.toList());
    }

    private int getNestedLevel(Class clazz) {
        int level = 0;
        Class parent = clazz.getEnclosingClass();
        while (parent != null && parent.isAnnotationPresent(CommandLine.Command.class)) {
            parent = parent.getEnclosingClass();
            level += 1;
        }
        return level;
    }

    private Optional<Class> getParentClass(Class clazz) {
        Class parentClass = clazz.getEnclosingClass();
        if (parentClass == null || !parentClass.isAnnotationPresent(CommandLine.Command.class)) {
            return Optional.empty();
        }
        return Optional.of(parentClass);
    }

}
