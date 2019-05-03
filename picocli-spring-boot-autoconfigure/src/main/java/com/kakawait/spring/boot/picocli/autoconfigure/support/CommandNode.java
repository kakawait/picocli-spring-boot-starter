package com.kakawait.spring.boot.picocli.autoconfigure.support;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author Thibaud Leprêtre
 */
class CommandNode {
    private final Class<?> clazz;

    private final Object object;

    private final Class<?> parent;

    CommandNode(Class<?> clazz, Object object, Class<?> parent) {
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandNode)) {
            return false;
        }
        CommandNode that = (CommandNode) o;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CommandNode.class.getSimpleName() + "[", "]").add("clazz=" + clazz).add(
                "object=" + object).add("parent=" + parent).toString();
    }
}
