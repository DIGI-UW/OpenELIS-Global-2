/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.valueholder;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerType represents the capability definition of an analyzer plugin.
 *
 * <p>
 * This entity separates "what an analyzer CAN do" (type/plugin) from "a
 * specific physical device" (Analyzer instance). This enables:
 *
 * <ul>
 * <li>Multiple physical analyzers of the same type (1:N relationship)
 * <li>Centralized plugin capability management
 * <li>Instance-specific configuration (location, IP, etc.) on Analyzer
 * </ul>
 *
 * <p>
 * Architecture: AnalyzerType (1) ---> (*) Analyzer instances
 */
@Deprecated(forRemoval = true)
public class AnalyzerType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String description;

    private String protocol = "ASTM";

    private String pluginClassName;

    private String identifierPattern;

    private boolean genericPlugin = false;

    private boolean active = true;

    private List<Analyzer> instances = new ArrayList<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
    }

    public String getIdentifierPattern() {
        return identifierPattern;
    }

    public void setIdentifierPattern(String identifierPattern) {
        this.identifierPattern = identifierPattern;
    }

    public boolean isGenericPlugin() {
        return genericPlugin;
    }

    public void setGenericPlugin(boolean genericPlugin) {
        this.genericPlugin = genericPlugin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Analyzer> getInstances() {
        return instances;
    }

    public void setInstances(List<Analyzer> instances) {
        this.instances = instances;
    }

    /**
     * Checks if this analyzer type matches the given identifier using the
     * identifier pattern.
     *
     * @param identifier The identifier string to match (e.g., from ASTM header)
     * @return true if the identifier matches this type's pattern, false otherwise
     */
    public boolean matchesIdentifier(String identifier) {
        if (identifierPattern == null || identifierPattern.isEmpty()) {
            return false;
        }
        if (identifier == null) {
            return false;
        }
        return identifier.matches(identifierPattern);
    }
}
