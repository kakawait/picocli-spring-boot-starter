package com.kakawait.spring.boot.picocli.autoconfigure;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

/**
 * @author Thibaud Leprêtre
 */
public class SpringPropertyVersionProvider implements CommandLine.IVersionProvider {

    private final ApplicationContext applicationContext;

    private final PicocliProperties picocliProperties;

    public SpringPropertyVersionProvider(ApplicationContext applicationContext, PicocliProperties picocliProperties) {
        this.applicationContext = applicationContext;
        this.picocliProperties = picocliProperties;
    }

    @Override
    public String[] getVersion() {
        String applicationName = applicationContext.getApplicationName();
        if (StringUtils.hasText(applicationName)) {
            applicationName += " ";
        }
        return new String[] {
            applicationName + "version " + picocliProperties.getCommandLineVersion()
        };
    }
}
