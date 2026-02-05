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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reportdefinition.controller;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reportdefinition.form.ReportDefinitionForm;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Report Definition management.
 *
 * <p>
 * Provides read-only endpoints for report definitions. This is the entry point
 * for the generic reporting framework.
 */
@RestController
@RequestMapping("/rest/reports")
public class ReportDefinitionRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ReportDefinitionRestController.class);

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    /**
     * Get all report definitions.
     *
     * @return list of all report definitions or empty list
     */
    @GetMapping("/definitions")
    public ResponseEntity<?> getDefinitions() {
        try {
            List<ReportDefinition> definitions = reportDefinitionService.getAll();
            List<ReportDefinitionForm> forms = new ArrayList<>();

            for (ReportDefinition definition : definitions) {
                forms.add(toForm(definition));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            logger.error("Error retrieving report definitions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving report definitions");
        }
    }

    /**
     * Get all active report definitions.
     *
     * @return list of active report definitions
     */
    @GetMapping("/definitions/active")
    public ResponseEntity<?> getActiveDefinitions() {
        try {
            List<ReportDefinition> definitions = reportDefinitionService.getActiveDefinitions();
            List<ReportDefinitionForm> forms = new ArrayList<>();

            for (ReportDefinition definition : definitions) {
                forms.add(toForm(definition));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            logger.error("Error retrieving active report definitions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving active report definitions");
        }
    }

    /**
     * Get report definitions by category.
     *
     * @param category report category
     * @return list of report definitions in the category
     */
    @GetMapping("/definitions/category/{category}")
    public ResponseEntity<?> getDefinitionsByCategory(@PathVariable String category) {
        try {
            List<ReportDefinition> definitions = reportDefinitionService.getDefinitionsByCategory(category);
            List<ReportDefinitionForm> forms = new ArrayList<>();

            for (ReportDefinition definition : definitions) {
                forms.add(toForm(definition));
            }

            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            logger.error("Error retrieving report definitions by category: " + category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving report definitions");
        }
    }

    /**
     * Get a specific report definition by ID.
     *
     * @param id report definition ID
     * @return report definition form or 404 if not found
     */
    @GetMapping("/definitions/{id}")
    public ResponseEntity<?> getDefinition(@PathVariable String id) {
        try {
            ReportDefinition definition = reportDefinitionService.get(id);
            if (definition == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Report definition not found");
            }
            return ResponseEntity.ok(toForm(definition));
        } catch (org.hibernate.ObjectNotFoundException e) {
            logger.debug("Report definition not found for id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Report definition not found");
        } catch (Exception e) {
            logger.error("Error retrieving report definition with id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving report definition");
        }
    }

    /**
     * Convert ReportDefinition valueholder to ReportDefinitionForm for API
     * response.
     *
     * @param definition report definition valueholder
     * @return report definition form
     */
    private ReportDefinitionForm toForm(ReportDefinition definition) {
        ReportDefinitionForm form = new ReportDefinitionForm();
        form.setId(definition.getId());
        form.setName(definition.getName());
        form.setDescription(definition.getDescription());
        form.setCategory(definition.getCategory());
        form.setDefinitionJson(definition.getDefinitionJson());
        form.setCreatedBy(definition.getCreatedBy());
        form.setCreatedDate(definition.getCreatedDate());
        form.setLastupdated(definition.getLastupdated());
        form.setIsActive(definition.getIsActive());
        return form;
    }
}
