package com.kakawait.spring.boot.picocli.autoconfigure;

import picocli.CommandLine;

/**
 * @author Thibaud Leprêtre
 */
public interface PicocliConfigurer {

    void configure(CommandLine commandLine);
}
