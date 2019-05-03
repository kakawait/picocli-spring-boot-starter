package com.kakawait.spring.boot.picocli.autoconfigure.support;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;
import picocli.CommandLine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Thibaud Leprêtre
 */
public class DefaultPicocliMainCommandLocator implements PicocliMainCommandSelector {

    @Override
    public Optional<Object> select(Collection<Object> commands) {
        List<Object> mainCommands = new ArrayList<>();
        for (Object candidate : commands) {
            Class<?> clazz = AopUtils.getTargetClass(candidate);
            Method method = ReflectionUtils.findMethod(CommandLine.Command.class, "name");
            if (clazz.isAnnotationPresent(CommandLine.Command.class)
                    && method != null
                    && clazz.getAnnotation(CommandLine.Command.class).name().equals(method.getDefaultValue())) {
                mainCommands.add(candidate);
            }
        }
        if (mainCommands.size() > 1) {
            throw new RuntimeException("Multiple mains command founds: " + Collections.singletonList(mainCommands));
        }
        if (mainCommands.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(mainCommands.iterator().next());
    }
}
