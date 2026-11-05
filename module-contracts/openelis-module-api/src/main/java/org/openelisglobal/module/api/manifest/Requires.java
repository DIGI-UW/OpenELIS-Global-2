package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * The compatibility section. Ranges use npm-style semver syntax
 * ({@code >=3.4.0}, {@code ^1.0}, {@code >=3.4.0 <4}). {@code openelis} is
 * mandatory and must carry a lower bound: the minimum OpenELIS version the
 * module was built and tested against.
 */
public class Requires {

    private String openelis;
    private String coreApi;
    private String spi;
    private String frontendContract;
    private List<ModuleDependency> modules = new ArrayList<>();

    public String getOpenelis() {
        return openelis;
    }

    public void setOpenelis(String openelis) {
        this.openelis = openelis;
    }

    /** Range on the {@code openelis-module-api} version; optional. */
    public String getCoreApi() {
        return coreApi;
    }

    public void setCoreApi(String coreApi) {
        this.coreApi = coreApi;
    }

    /**
     * Range on the {@code openelis-spi} version; mandatory when the manifest
     * declares extensions.
     */
    public String getSpi() {
        return spi;
    }

    public void setSpi(String spi) {
        this.spi = spi;
    }

    /**
     * Range on the frontend contract ({@code @openelis/module-sdk}) version;
     * mandatory when a frontend is declared.
     */
    public String getFrontendContract() {
        return frontendContract;
    }

    public void setFrontendContract(String frontendContract) {
        this.frontendContract = frontendContract;
    }

    /** Other modules this one needs, started before it. */
    public List<ModuleDependency> getModules() {
        return modules;
    }

    public void setModules(List<ModuleDependency> modules) {
        this.modules = modules == null ? new ArrayList<>() : modules;
    }

    /**
     * One required module: its id and the version range this module was built
     * against.
     */
    public static class ModuleDependency {

        private String id;
        private String version;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
