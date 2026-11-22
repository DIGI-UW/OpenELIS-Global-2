package org.openelisglobal.orderentry.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandlingRequirementServiceImpl implements HandlingRequirementService {

    static final int MAX_TESTS = 100;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSampleHandlingService testSampleHandlingService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> requirements(List<String> testIds) {
        Set<String> wanted = new LinkedHashSet<>();
        if (testIds != null) {
            testIds.stream().filter(id -> id != null && GenericValidator.isInt(id.trim())).map(String::trim)
                    .limit(MAX_TESTS).forEach(wanted::add);
        }
        List<Map<String, Object>> requirements = new ArrayList<>();
        for (String testId : wanted) {
            Test test = testService.getTestById(testId);
            if (test == null) {
                continue;
            }
            TestSampleHandling handling = testSampleHandlingService.getByTestId(testId);
            Map<String, Object> requirement = new LinkedHashMap<>();
            requirement.put("testId", testId);
            requirement.put("storageCondition", handling == null ? null : handling.getStorageCondition());
            requirement.put("storageConditionCustom", handling == null ? null : handling.getStorageConditionCustom());
            requirement.put("holdingMinutes", holdingMinutes(test.getTimeHolding()));
            requirements.add(requirement);
        }
        return requirements;
    }

    private static Integer holdingMinutes(String timeHolding) {
        if (timeHolding == null || !GenericValidator.isInt(timeHolding.trim())) {
            return null;
        }
        int minutes = Integer.parseInt(timeHolding.trim());
        return minutes > 0 ? minutes : null;
    }
}
