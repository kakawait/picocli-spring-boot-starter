package com.kakawait.spring.boot.picocli.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Thibaud Leprêtre
 */
@ConfigurationProperties(prefix = "picocli")
public class PicocliProperties {

    private String commandLineVersion = "1.0.0";

    private ClassHierarchyScanning classHierarchyScanning = new ClassHierarchyScanning();

    public String getCommandLineVersion() {
        return commandLineVersion;
    }

    public void setCommandLineVersion(String commandLineVersion) {
        this.commandLineVersion = commandLineVersion;
    }

    public ClassHierarchyScanning getClassHierarchyScanning() {
        return classHierarchyScanning;
    }

    public void setClassHierarchyScanning(ClassHierarchyScanning classHierarchyScanning) {
        this.classHierarchyScanning = classHierarchyScanning;
    }

    public static class ClassHierarchyScanning {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
