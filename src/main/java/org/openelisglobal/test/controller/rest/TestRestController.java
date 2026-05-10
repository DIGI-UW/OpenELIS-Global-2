package org.openelisglobal.test.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestRestController {

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @GetMapping(value = "/test-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTestSampleTypes(@RequestParam String testIds) {
        try {
            List<Map<String, Object>> testsResult = new ArrayList<>();

            if (GenericValidator.isBlankOrNull(testIds)) {
                Map<String, Object> response = new HashMap<>();
                response.put("tests", testsResult);
                return ResponseEntity.ok(response);
            }

            String[] ids = testIds.split(",");
            for (String testId : ids) {
                String trimmedId = testId.trim();
                if (trimmedId.isEmpty()) {
                    continue;
                }

                Test test = testService.get(trimmedId);
                if (test == null) {
                    continue;
                }

                Map<String, Object> testData = new HashMap<>();
                testData.put("testId", test.getId());
                testData.put("testName",
                        test.getLocalizedTestName() != null ? test.getLocalizedTestName().getLocalizedValue()
                                : test.getName());

                List<TypeOfSampleTest> sampleTests = typeOfSampleTestService.getTypeOfSampleTestsForTest(trimmedId);
                List<Map<String, Object>> compatibleTypes = new ArrayList<>();

                for (TypeOfSampleTest sampleTest : sampleTests) {
                    TypeOfSample sampleType = typeOfSampleService.get(sampleTest.getTypeOfSampleId());
                    if (sampleType != null && "Y".equals(sampleType.getIsActive())) {
                        Map<String, Object> typeData = new HashMap<>();
                        typeData.put("id", sampleType.getId());
                        typeData.put("name", sampleType.getLocalizedName() != null ? sampleType.getLocalizedName()
                                : sampleType.getDescription());
                        typeData.put("code", "all");
                        compatibleTypes.add(typeData);
                    }
                }

                testData.put("compatibleSampleTypes", compatibleTypes);
                testsResult.add(testData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tests", testsResult);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getTestSampleTypes",
                    "Error getting test sample types: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
