/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.common.provider.query.rest;

import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.renamemethod.service.RenameMethodService;
import org.openelisglobal.renametestsection.service.RenameTestSectionService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class EntityNamesProviderRestController {

    @Autowired
    private PanelService panelService;

    @Autowired
    private RenameTestSectionService renameTestSectionService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private RenameMethodService renameMethodService;

    public static final String PANEL = "panel";
    public static final String SAMPLE_TYPE = "sampleType";
    public static final String TEST_SECTION = "testSection";
    public static final String UNIT_OF_MEASURE = "unitOfMeasure";
    public static final String METHOD = "method";
    String INVALID = "invalid";
    String VALID = "valid";

    @SuppressWarnings("unchecked")
    @GetMapping("/EntityNamesProvider")
    public ResponseEntity<JSONObject> processRequest(@RequestParam("entityId") String id,
            @RequestParam("entityName") String entityName) {

        if (GenericValidator.isBlankOrNull(id) || GenericValidator.isBlankOrNull(entityName)) {
            String errorMessage = "Internal error, please contact Admin and file bug report";
            JSONObject errorJson = new JSONObject();
            errorJson.put("status", INVALID);
            errorJson.put("message", errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson);
        }

        JSONObject jsonResult = new JSONObject();
        String status = createJsonTestNames(id, entityName, jsonResult);

        if (status.equals(VALID)) {
            return ResponseEntity.ok(jsonResult);
        } else {
            JSONObject errorJson = new JSONObject();
            errorJson.put("status", INVALID);
            errorJson.put("message", "No localization found for the given entity.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorJson);
        }
    }

    @SuppressWarnings("unchecked")
    private String createJsonTestNames(String id, String entityName, JSONObject jsonResult)
            throws IllegalStateException {

        Localization localization = null;

        if (PANEL.equals(entityName)) {
            localization = getLocalizationForPanel(id);
        } else if (SAMPLE_TYPE.equals(entityName)) {
            localization = getLocalizationForSampleType(id);
        } else if (TEST_SECTION.equals(entityName)) {
            localization = getLocalizationForRenameTestSection(id);
        } else if (UNIT_OF_MEASURE.equals(entityName)) {
            localization = getLocalizationForUnitOfMeasure(id);
        } else if (METHOD.equals(entityName)) {
            localization = getLocalizationForRenameMethod(id);
        }
        // add entity types as needed

        if (localization != null) {

            JSONObject nameObject = new JSONObject();
            addAllLocalizations(nameObject, localization);
            // nameObject.put("english", localization.getEnglish());
            // nameObject.put("french", localization.getFrench());
            jsonResult.put("name", nameObject);

            return VALID;
        }

        return INVALID;
    }

    @SuppressWarnings("unchecked")
    private void addAllLocalizations(JSONObject jsonObject, Localization localization) {
        for (Locale locale : localization.getLocalesWithValue()) {
            jsonObject.put(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                    localization.getLocalizedValue(locale));
        }
    }

    private Localization getLocalizationForPanel(String id) {
        return panelService.getLocalizationForPanel(id);
    }

    private Localization getLocalizationForSampleType(String id) {
        return typeOfSampleService.getLocalizationForSampleType(id);
    }

    private Localization getLocalizationForRenameTestSection(String id) {
        return renameTestSectionService.getLocalizationForRenameTestSection(id);
    }

    private Localization getLocalizationForUnitOfMeasure(String id) {
        return unitOfMeasureService.getLocalizationForUnitOfMeasure(id);
    }

    private Localization getLocalizationForRenameMethod(String id) {
        return renameMethodService.getLocalizationForRenameMethod(id);
    }
}
