package com.kakawait.spring.boot.picocli.autoconfigure;

import picocli.CommandLine;

/**
 * @author Thibaud Leprêtre
 */
public abstract class PicocliConfigurerAdapter implements PicocliConfigurer {

    @Override
    public void configure(CommandLine commandLine) {
    }
}
