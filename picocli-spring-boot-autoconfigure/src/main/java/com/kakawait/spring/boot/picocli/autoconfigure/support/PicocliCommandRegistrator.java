package com.kakawait.spring.boot.picocli.autoconfigure.support;

import picocli.CommandLine;

import java.util.Collection;

/**
 * @author Thibaud Leprêtre
 */
public interface PicocliCommandRegistrator {

    void register(CommandLine cli, Collection<Object> commands);
}
