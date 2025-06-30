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

package org.openelisglobal.common.rest.provider;

import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class AllTestsForSampleTypeProviderRestController extends BaseRestController {

    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);

    @GetMapping("/AllTestsForSampleTypeProvider")
    public ResponseEntity<Object> processRequest(@RequestParam String sampleTypeId) {
        if (sampleTypeId == null || sampleTypeId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Internal error, please contact Admin and file bug report");
        }

        try {
            JSONObject jsonResult = createJsonGroupedTestNames(sampleTypeId);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error, please contact Admin and file bug report");
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject createJsonGroupedTestNames(String sampleTypeId) throws IllegalStateException {
        List<Test> tests = typeOfSampleService.getAllTestsBySampleTypeId(sampleTypeId);

        JSONArray testArray = new JSONArray();

        for (Test test : tests) {
            JSONObject testObject = new JSONObject();
            testObject.put("name", test.getLocalizedTestName().getLocalizedValue());
            testObject.put("id", test.getId());
            testObject.put("isActive", test.getIsActive());
            testArray.add(testObject);
        }
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("tests", testArray);
        return jsonResult;
    }
}
