package org.openelisglobal.testconfiguration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleTypeTestAssignServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTypeTestAssignService sampleTypeTestAssignService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-type-test-assign-service.xml");
    }

    @Test
    public void updateShouldInsertNewTypeOfSampleTestWhenGivenValidInputs() {
        TypeOfSample sampleType = typeOfSampleService.get("9901");
        String testId = "9900";

        sampleTypeTestAssignService.update(sampleType, testId, Collections.emptyList(), sampleType.getId(), false,
                false, null, "1");

        List<TypeOfSampleTest> testsForSampleType = typeOfSampleTestService.getTypeOfSampleTestsForSampleType("9901");
        assertEquals(1, testsForSampleType.size());
        assertEquals("9900", testsForSampleType.get(0).getTestId());
        assertEquals("9901", testsForSampleType.get(0).getTypeOfSampleId());
    }

    @Test
    public void updateShouldDeleteExistingTypeOfSampleTestsWhenDeleteFlagIsTrue() {
        TypeOfSample sampleType = typeOfSampleService.get("9900");
        String testId = "9901";

        List<TypeOfSampleTest> existing = typeOfSampleTestService.getTypeOfSampleTestsForSampleType("9900");
        assertEquals(1, existing.size());
        assertEquals("9900", existing.get(0).getId());

        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add("9900");

        sampleTypeTestAssignService.update(sampleType, testId, idsToDelete, sampleType.getId(), true, false, null, "1");

        List<TypeOfSampleTest> testsForSampleType = typeOfSampleTestService.getTypeOfSampleTestsForSampleType("9900");
        assertEquals("Should only have the newly assigned test", 1, testsForSampleType.size());
        assertEquals("9901", testsForSampleType.get(0).getTestId());
    }

    @Test
    public void updateShouldUpdateTypeOfSampleWhenUpdateFlagIsTrue() {
        TypeOfSample sampleType = typeOfSampleService.get("9900");
        sampleType.setDescription("Updated Blood");
        String testId = "9901";

        sampleTypeTestAssignService.update(sampleType, testId, Collections.emptyList(), sampleType.getId(), false, true,
                null, "1");

        TypeOfSample updatedSampleType = typeOfSampleService.get("9900");
        assertEquals("Updated Blood", updatedSampleType.getDescription());
    }

    @Test
    public void updateShouldDeactivateTypeOfSampleWhenDeactivateObjectIsProvided() {
        TypeOfSample sampleType = typeOfSampleService.get("9901");
        TypeOfSample deActivateTypeOfSample = typeOfSampleService.get("9902");
        deActivateTypeOfSample.setActive(false);
        String testId = "9900";

        assertTrue(typeOfSampleService.get("9902").isActive());

        sampleTypeTestAssignService.update(sampleType, testId, Collections.emptyList(), sampleType.getId(), false,
                false, deActivateTypeOfSample, "1");

        TypeOfSample updatedDeactivated = typeOfSampleService.get("9902");
        assertFalse("Should be deactivated", updatedDeactivated.isActive());
    }
}