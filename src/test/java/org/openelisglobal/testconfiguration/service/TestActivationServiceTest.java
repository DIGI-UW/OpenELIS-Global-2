package org.openelisglobal.testconfiguration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class TestActivationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestActivationService testActivationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/test-activation-service.xml");
    }

    @org.junit.Test
    public void updateAll_shouldActivateAndDeactivateTestsAndSampleTypes() throws Exception {
        // Fetch existing tests and verify their initial state
        Test test1ToDeactivate = testService.get("1");
        assertEquals("Test 1 should initially be active", "Y", test1ToDeactivate.getIsActive());

        Test test2ToRemainActive = testService.get("2");
        assertEquals("Test 2 should initially be active", "Y", test2ToRemainActive.getIsActive());

        Test test3ToActivate = testService.get("3");
        assertEquals("Test 3 should initially be inactive", "N", test3ToActivate.getIsActive());

        TypeOfSample sample1ToDeactivate = typeOfSampleService.get("1");
        assertTrue("Sample type 1 should initially be active", sample1ToDeactivate.getIsActive());

        TypeOfSample sample2ToActivate = typeOfSampleService.get("2");
        assertFalse("Sample type 2 should initially be inactive", sample2ToActivate.getIsActive());

        List<Test> deactivateTests = new ArrayList<>();
        test1ToDeactivate.setIsActive("N");
        deactivateTests.add(test1ToDeactivate);

        List<Test> activateTests = new ArrayList<>();
        test3ToActivate.setIsActive("Y");
        activateTests.add(test3ToActivate);

        List<TypeOfSample> deactivateSampleTypes = new ArrayList<>();
        sample1ToDeactivate.setIsActive(false);
        deactivateSampleTypes.add(sample1ToDeactivate);

        List<TypeOfSample> activateSampleTypes = new ArrayList<>();
        sample2ToActivate.setIsActive(true);
        activateSampleTypes.add(sample2ToActivate);

        testActivationService.updateAll(deactivateTests, activateTests, deactivateSampleTypes, activateSampleTypes);

        Test updatedTest1 = testService.get("1");
        assertEquals("Test 1 should be deactivated", "N", updatedTest1.getIsActive());

        Test updatedTest3 = testService.get("3");
        assertEquals("Test 3 should be activated", "Y", updatedTest3.getIsActive());

        TypeOfSample updatedSample1 = typeOfSampleService.get("1");
        assertFalse("Sample type 1 should be deactivated", updatedSample1.getIsActive());

        TypeOfSample updatedSample2 = typeOfSampleService.get("2");
        assertTrue("Sample type 2 should be activated", updatedSample2.getIsActive());

        Test updatedTest2 = testService.get("2");
        assertEquals("Test 2 should remain active", "Y", updatedTest2.getIsActive());
    }

    @org.junit.Test
    public void updateAll_emptyListsShouldNotThrowExceptionsAndNotModifyData() throws Exception {
        testActivationService.updateAll(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        assertEquals("Test 1 should remain active", "Y", testService.get("1").getIsActive());
        assertTrue("Sample type 1 should remain active", typeOfSampleService.get("1").getIsActive());
    }
}
