package com.kakawait.spring.boot.picocli.autoconfigure.support;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Thibaud Leprêtre
 */
public interface PicocliMainCommandSelector {

    Optional<Object> select(Collection<Object> commands);
}
