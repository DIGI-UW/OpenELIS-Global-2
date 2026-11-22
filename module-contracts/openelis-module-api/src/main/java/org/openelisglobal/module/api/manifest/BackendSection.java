package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * What the module contributes to the backend. {@code basePackages} and
 * {@code activator} apply to both kinds; a provider may declare neither
 * {@code entities} nor {@code liquibase}.
 */
public class BackendSection {

    private List<String> basePackages = new ArrayList<>();
    private String activator;
    private List<String> entities = new ArrayList<>();
    private String liquibase;
    private List<ExtensionDeclaration> extensions = new ArrayList<>();
    private List<String> libraries = new ArrayList<>();

    /**
     * Packages scanned for the module's beans; must not fall under a core scan
     * root.
     */
    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages == null ? new ArrayList<>() : basePackages;
    }

    /** The class implementing {@code OpenElisModule}; optional. */
    public String getActivator() {
        return activator;
    }

    public void setActivator(String activator) {
        this.activator = activator;
    }

    /**
     * Fully qualified JPA entity classes added to the core persistence unit at
     * bootstrap.
     */
    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities == null ? new ArrayList<>() : entities;
    }

    /**
     * Class-path location of the module changelog inside the JAR, for example
     * {@code liquibase/base.xml}, which includes the module's changelog files in
     * order.
     */
    public String getLiquibase() {
        return liquibase;
    }

    public void setLiquibase(String liquibase) {
        this.liquibase = liquibase;
    }

    public List<ExtensionDeclaration> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<ExtensionDeclaration> extensions) {
        this.extensions = extensions == null ? new ArrayList<>() : extensions;
    }

    /**
     * Maven coordinates of the libraries bundled under {@code lib/}, for the
     * operator's audit.
     */
    public List<String> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<String> libraries) {
        this.libraries = libraries == null ? new ArrayList<>() : libraries;
    }

    /**
     * One extension-point implementation: the point's name ({@code report}, ...)
     * and the implementing class.
     */
    public static class ExtensionDeclaration {

        private String spi;
        private String className;
        private int order = 100;

        public String getSpi() {
            return spi;
        }

        public void setSpi(String spi) {
            this.spi = spi;
        }

        /** Serialised as {@code class} in the manifest. */
        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        /**
         * Order among the providers of a multiple-implementation point; lower runs
         * first.
         */
        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }
}
