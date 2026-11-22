package org.openelisglobal.test.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
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

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @GetMapping(value = "/test-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTestSampleTypes(
            @RequestParam(required = false, defaultValue = "") String testIds,
            @RequestParam(required = false, defaultValue = "") String panelIds) {
        try {
            List<Map<String, Object>> testsResult = new ArrayList<>();
            for (String testId : splitIds(testIds)) {
                Test test = testService.get(testId);
                if (test == null) {
                    continue;
                }
                Map<String, Object> testData = new HashMap<>();
                testData.put("testId", test.getId());
                testData.put("testName",
                        test.getLocalizedTestName() != null ? test.getLocalizedTestName().getLocalizedValue()
                                : test.getName());
                List<String> sampleTypeIds = new ArrayList<>();
                for (TypeOfSampleTest sampleTest : typeOfSampleTestService.getTypeOfSampleTestsForTest(testId)) {
                    sampleTypeIds.add(sampleTest.getTypeOfSampleId());
                }
                testData.put("compatibleSampleTypes", activeSampleTypes(sampleTypeIds));
                testsResult.add(testData);
            }

            List<Map<String, Object>> panelsResult = new ArrayList<>();
            for (String panelId : splitIds(panelIds)) {
                Map<String, Object> panelData = new HashMap<>();
                panelData.put("panelId", panelId);
                List<String> sampleTypeIds = new ArrayList<>();
                for (TypeOfSamplePanel samplePanel : typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panelId)) {
                    sampleTypeIds.add(samplePanel.getTypeOfSampleId());
                }
                panelData.put("compatibleSampleTypes", activeSampleTypes(sampleTypeIds));
                panelsResult.add(panelData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tests", testsResult);
            response.put("panels", panelsResult);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getTestSampleTypes",
                    "Error getting test sample types: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<String> splitIds(String ids) {
        List<String> result = new ArrayList<>();
        if (GenericValidator.isBlankOrNull(ids)) {
            return result;
        }
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<Map<String, Object>> activeSampleTypes(List<String> sampleTypeIds) {
        List<Map<String, Object>> compatibleTypes = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String sampleTypeId : sampleTypeIds) {
            if (sampleTypeId == null || seen.contains(sampleTypeId)) {
                continue;
            }
            seen.add(sampleTypeId);
            TypeOfSample sampleType = typeOfSampleService.get(sampleTypeId);
            if (sampleType != null && sampleType.getIsActive()) {
                Map<String, Object> typeData = new HashMap<>();
                typeData.put("id", sampleType.getId());
                typeData.put("name", sampleType.getLocalizedName() != null ? sampleType.getLocalizedName()
                        : sampleType.getDescription());
                compatibleTypes.add(typeData);
            }
        }
        return compatibleTypes;
    }
}
