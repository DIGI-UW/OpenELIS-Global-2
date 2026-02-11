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
package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for AnalyzerType management.
 *
 * <p>
 * Handles CRUD operations for analyzer types (plugin capability definitions)
 * and provides endpoints for managing the type-instance relationship.
 */
@RestController
@RequestMapping("/rest/analyzer-types")
public class AnalyzerTypeRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerTypeRestController.class);

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    /**
     * GET /rest/analyzer-types Retrieve all analyzer types with optional filtering.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAnalyzerTypes(@RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean genericOnly, @RequestParam(required = false) String search) {
        try {
            List<AnalyzerType> types;

            if (Boolean.TRUE.equals(genericOnly)) {
                types = analyzerTypeService.getGenericPluginTypes();
            } else if (Boolean.TRUE.equals(active)) {
                types = analyzerTypeService.getAllActiveTypes();
            } else {
                types = analyzerTypeService.getAll();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            Set<String> loadedPlugins = getLoadedPluginClassNames();

            for (AnalyzerType type : types) {
                // Apply search filter
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    if (!type.getName().toLowerCase().contains(searchLower) && (type.getDescription() == null
                            || !type.getDescription().toLowerCase().contains(searchLower))) {
                        continue;
                    }
                }

                response.add(analyzerTypeToMap(type, false, loadedPlugins));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * GET /rest/analyzer-types/{id} Retrieve a specific analyzer type by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAnalyzerType(@PathVariable String id) {
        try {
            AnalyzerType type = analyzerTypeService.get(id);
            if (type == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer type not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            return ResponseEntity.ok(analyzerTypeToMap(type, true, getLoadedPluginClassNames()));
        } catch (Exception e) {
            logger.error("Error retrieving analyzer type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer-types/{id}/instances Get all analyzer instances of a
     * specific type.
     */
    @GetMapping("/{id}/instances")
    public ResponseEntity<List<Map<String, Object>>> getInstances(@PathVariable String id) {
        try {
            AnalyzerType type = analyzerTypeService.get(id);
            if (type == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }

            List<Analyzer> instances = type.getInstances();
            List<Map<String, Object>> response = new ArrayList<>();

            for (Analyzer instance : instances) {
                Map<String, Object> instanceMap = new HashMap<>();
                instanceMap.put("id", instance.getId());
                instanceMap.put("name", instance.getName());
                instanceMap.put("description", instance.getDescription());
                instanceMap.put("location", instance.getLocation());
                instanceMap.put("machineId", instance.getMachineId());
                instanceMap.put("isActive", instance.isActive());
                response.add(instanceMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving instances for type: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer-types/{id}/instances Create a new analyzer instance for a
     * type.
     */
    @PostMapping("/{id}/instances")
    public ResponseEntity<Map<String, Object>> createInstance(@PathVariable String id,
            @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            AnalyzerType type = analyzerTypeService.get(id);
            if (type == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer type not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            String name = (String) request.get("name");
            ResponseEntity<Map<String, Object>> nameValidation = validateRequiredField(name, "Instance name");
            if (nameValidation != null) {
                return nameValidation;
            }

            // Check for duplicate name
            Analyzer existingAnalyzer = analyzerService.getAnalyzerByName(name);
            if (existingAnalyzer != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer with name '" + name + "' already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Create new instance
            Analyzer instance = new Analyzer();
            instance.setName(name);
            instance.setDescription((String) request.get("description"));
            instance.setLocation((String) request.get("location"));
            instance.setMachineId((String) request.get("machineId"));
            instance.setAnalyzerType(type);
            instance.setActive(true);
            instance.setSysUserId(getSysUserId(httpRequest));

            String instanceId = analyzerService.insert(instance);

            // Return created instance
            Analyzer createdInstance = analyzerService.get(instanceId);
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdInstance.getId());
            response.put("name", createdInstance.getName());
            response.put("description", createdInstance.getDescription());
            response.put("location", createdInstance.getLocation());
            response.put("machineId", createdInstance.getMachineId());
            response.put("isActive", createdInstance.isActive());
            response.put("analyzerTypeId", type.getId());
            response.put("analyzerTypeName", type.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating instance for type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer-types Create a new analyzer type (for generic plugins).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAnalyzerType(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            String name = (String) request.get("name");
            ResponseEntity<Map<String, Object>> nameValidation = validateRequiredField(name, "Type name");
            if (nameValidation != null) {
                return nameValidation;
            }

            // Check for duplicate name
            AnalyzerType existingType = analyzerTypeService.getAnalyzerTypeByName(name);
            if (existingType != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer type with name '" + name + "' already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            AnalyzerType type = new AnalyzerType();
            type.setName(name);
            type.setDescription((String) request.get("description"));
            type.setProtocol((String) request.getOrDefault("protocol", "ASTM"));
            type.setPluginClassName((String) request.get("pluginClassName"));
            type.setIdentifierPattern((String) request.get("identifierPattern"));
            type.setGenericPlugin(Boolean.TRUE.equals(request.get("isGenericPlugin")));
            type.setActive(true);
            type.setSysUserId(getSysUserId(httpRequest));

            analyzerTypeService.insert(type);

            // Return created type
            AnalyzerType createdType = analyzerTypeService.getAnalyzerTypeByName(name);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(analyzerTypeToMap(createdType, false, getLoadedPluginClassNames()));
        } catch (Exception e) {
            logger.error("Error creating analyzer type", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer-types/{id} Update an analyzer type.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAnalyzerType(@PathVariable String id,
            @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            AnalyzerType type = analyzerTypeService.get(id);
            if (type == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer type not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Update fields if provided
            if (request.containsKey("description")) {
                type.setDescription((String) request.get("description"));
            }
            if (request.containsKey("protocol")) {
                type.setProtocol((String) request.get("protocol"));
            }
            if (request.containsKey("identifierPattern")) {
                type.setIdentifierPattern((String) request.get("identifierPattern"));
            }
            if (request.containsKey("isActive")) {
                type.setActive(Boolean.TRUE.equals(request.get("isActive")));
            }
            if (request.containsKey("isGenericPlugin")) {
                type.setGenericPlugin(Boolean.TRUE.equals(request.get("isGenericPlugin")));
            }

            type.setSysUserId(getSysUserId(httpRequest));
            analyzerTypeService.update(type);

            return ResponseEntity.ok(analyzerTypeToMap(type, false, getLoadedPluginClassNames()));
        } catch (Exception e) {
            logger.error("Error updating analyzer type: " + id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Validates that a required string field is not null or empty.
     * 
     * @param value     the field value to validate
     * @param fieldName the name of the field for error messages
     * @return ResponseEntity with BAD_REQUEST if invalid, null if valid
     */
    private ResponseEntity<Map<String, Object>> validateRequiredField(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", fieldName + " is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return null;
    }

    /**
     * Convert AnalyzerType to Map for JSON response.
     */
    private Map<String, Object> analyzerTypeToMap(AnalyzerType type, boolean includeInstances,
            Set<String> loadedPlugins) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", type.getId());
        map.put("name", type.getName());
        map.put("description", type.getDescription());
        map.put("protocol", type.getProtocol());
        map.put("pluginClassName", type.getPluginClassName());
        map.put("identifierPattern", type.getIdentifierPattern());
        map.put("isGenericPlugin", type.isGenericPlugin());
        map.put("isActive", type.isActive());
        map.put("pluginLoaded", type.getPluginClassName() != null && loadedPlugins.contains(type.getPluginClassName()));
        map.put("instanceCount", type.getInstances().size());

        if (includeInstances) {
            List<Map<String, Object>> instances = new ArrayList<>();
            for (Analyzer instance : type.getInstances()) {
                Map<String, Object> instanceMap = new HashMap<>();
                instanceMap.put("id", instance.getId());
                instanceMap.put("name", instance.getName());
                instanceMap.put("location", instance.getLocation());
                instanceMap.put("isActive", instance.isActive());
                instances.add(instanceMap);
            }
            map.put("instances", instances);
        }

        return map;
    }

    private Set<String> getLoadedPluginClassNames() {
        return pluginAnalyzerService.getAnalyzerPlugins().stream().map(plugin -> plugin.getClass().getName())
                .collect(Collectors.toSet());
    }
}
