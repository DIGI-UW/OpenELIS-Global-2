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
package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service for managing AnalyzerType entities.
 *
 * <p>
 * AnalyzerType represents the capability definition of an analyzer plugin,
 * separate from physical Analyzer instances. This enables multiple physical
 * analyzers of the same type.
 */
public interface AnalyzerTypeService extends BaseObjectService<AnalyzerType, String> {

    /**
     * Find an analyzer type by its unique name.
     *
     * @param name The type name (e.g., "HoribaMicros60")
     * @return The AnalyzerType or null if not found
     */
    AnalyzerType getAnalyzerTypeByName(String name);

    /**
     * Find an analyzer type by its plugin class name.
     *
     * @param pluginClassName Fully qualified class name of the plugin
     * @return Optional containing the AnalyzerType if found
     */
    Optional<AnalyzerType> getByPluginClassName(String pluginClassName);

    /**
     * Get all active analyzer types.
     *
     * @return List of active AnalyzerType entities
     */
    List<AnalyzerType> getAllActiveTypes();

    /**
     * Get all generic plugin types (dashboard-configurable).
     *
     * @return List of generic plugin AnalyzerType entities
     */
    List<AnalyzerType> getGenericPluginTypes();

    /**
     * Find the analyzer type that matches the given identifier pattern.
     *
     * @param identifier The identifier string to match (e.g., from ASTM header)
     * @return Optional containing the matching AnalyzerType
     */
    Optional<AnalyzerType> findMatchingType(String identifier);

    /**
     * Get all analyzer instances for a given type.
     *
     * @param analyzerTypeId The ID of the analyzer type
     * @return List of Analyzer instances of this type
     */
    List<Analyzer> getInstancesForType(String analyzerTypeId);

    /**
     * Create a default analyzer instance for a type if none exists.
     *
     * @param analyzerType The analyzer type
     * @return The created or existing default Analyzer instance
     */
    Analyzer getOrCreateDefaultInstance(AnalyzerType analyzerType);
}
