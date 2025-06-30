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
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestNamesProviderRestController {

    private final TestService testService;

    public TestNamesProviderRestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/TestNamesProvider")
    public ResponseEntity<JSONObject> processRequest(@RequestParam("testId") String testId) {

        String jResult;
        JSONObject jsonResult = new JSONObject();
        String jString;

        if (GenericValidator.isBlankOrNull(testId)) {
            jResult = "INVALID";
            jString = "Internal error, please contact Admin and file bug report";
            return ResponseEntity.badRequest().body(createErrorResponse(jString));
        } else {
            try {
                jResult = createJsonTestNames(testId, jsonResult);
                jString = jsonResult.toJSONString();
            } catch (IllegalStateException e) {
                LogEvent.logDebug(e);
                jResult = "INVALID";
                jString = "Internal error, please contact Admin and file bug report";
                return ResponseEntity.status(500).body(createErrorResponse(jString));
            }
        }
        return ResponseEntity.ok(jsonResult);
    }

    @SuppressWarnings("unchecked")
    private String createJsonTestNames(String testId, JSONObject jsonResult) throws IllegalStateException {

        String INVALID = "invalid";
        String VALID = "valid";

        if (GenericValidator.isBlankOrNull(testId)) {
            throw new IllegalStateException("TestNamesProvider testId was blank.  It must have a value");
        }

        Test test = SpringContext.getBean(TestService.class).get(testId);
        if (test != null) {
            Localization nameLocalization = test.getLocalizedTestName();
            Localization reportNameLocalization = test.getLocalizedReportingName();

            JSONObject nameObject = new JSONObject();
            addAllLocalizations(nameObject, nameLocalization);
            // nameObject.put("english", nameLocalization.getEnglish());
            // nameObject.put("french", nameLocalization.getFrench());
            jsonResult.put("name", nameObject);

            JSONObject reportingNameObject = new JSONObject();
            addAllLocalizations(reportingNameObject, reportNameLocalization);
            // reportingNameObject.put("english", reportNameLocalization.getEnglish());
            // reportingNameObject.put("french", reportNameLocalization.getFrench());
            jsonResult.put("reportingName", reportingNameObject);

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

    @SuppressWarnings("unchecked")
    private JSONObject createErrorResponse(String message) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        return errorResponse;
    }
}
