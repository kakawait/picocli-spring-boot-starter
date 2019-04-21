package com.kakawait.spring.boot.picocli.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

import java.lang.reflect.Constructor;

/**
 * @author Thibaud Leprêtre
 */
public class ApplicationContextAwarePicocliFactory implements CommandLine.IFactory {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContextAwarePicocliFactory.class);

    private final ApplicationContext applicationContext;

    public ApplicationContextAwarePicocliFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <K> K create(Class<K> aClass) throws Exception {
        try {
            return applicationContext.getBean(aClass);
        } catch (Exception e) {
            logger.info("unable to get bean of class {}, use standard factory creation", aClass);
            try {
                return aClass.newInstance();
            } catch (Exception ex) {
                Constructor<K> constructor = aClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
        }
    }
}
