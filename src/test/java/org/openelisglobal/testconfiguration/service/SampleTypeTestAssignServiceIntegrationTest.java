package org.openelisglobal.testconfiguration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
        executeDataSetWithStateManagement("testdata/sample-type-test.xml");
    }

    @Test
    public void updateShouldDeleteExistingTypeOfSampleTestLinksAndCreateNewLinkWithUpdatedAbbreviationWhenDeleteAndUpdateFlagsAreTrue() {
        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        assertEquals("Pre-condition: sample type 1001 must exist with description 'whole blood'", "whole blood",
                sampleType1001.getDescription());
        assertEquals("Pre-condition: sample type 1001 abbreviation must be 'blood'", "blood",
                sampleType1001.getLocalAbbreviation());

        List<TypeOfSampleTest> existingLinks = typeOfSampleTestService.getTypeOfSampleTestsForTest("2001");
        assertEquals("Fixture must start with exactly 1 link for test 2001", 1, existingLinks.size());
        assertEquals("Existing link must belong to sample type 1001", "1001", existingLinks.get(0).getTypeOfSampleId());
        String linkIdToDelete = existingLinks.get(0).getId();

        sampleType1001.setLocalAbbreviation("upd_abbrev");
        sampleTypeTestAssignService.update(sampleType1001, "2002", Arrays.asList(linkIdToDelete), "1001", true, true,
                null, "1");

        List<TypeOfSampleTest> remainingLinksFor2001 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2001");
        assertEquals("Old link to test 2001 must be deleted", 0, remainingLinksFor2001.size());

        List<TypeOfSampleTest> newLinksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        assertEquals("Exactly 3 links must exist for test 2002", 3, newLinksFor2002.size());
        assertTrue("New link for test 2002 must belong to sample type 1001",
                newLinksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));

        TypeOfSample updatedSampleType = typeOfSampleService.get("1001");
        assertEquals("Abbreviation must be persisted as 'upd_abbrev' after update", "upd_abbrev",
                updatedSampleType.getLocalAbbreviation());
    }

    @Test
    public void updateShouldDeactivateSampleTypeAndCreateNewLinkWhenDeactivateObjectIsProvided() {
        TypeOfSample sampleType1002 = typeOfSampleService.get("1002");
        assertEquals("Pre-condition: sample type 1002 must be active", true, sampleType1002.getIsActive());
        assertEquals("Pre-condition: sample type 1002 description must be 'Urine'", "Urine",
                sampleType1002.getDescription());

        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        sampleType1002.setIsActive(false);

        sampleTypeTestAssignService.update(sampleType1001, "2002", null, "1001", false, false, sampleType1002, "1");

        TypeOfSample deactivatedSampleType = typeOfSampleService.get("1002");
        assertFalse("Sample type 1002 must be inactive after deactivation", deactivatedSampleType.getIsActive());
        assertEquals("Sample type 1002 description must remain 'Urine' after deactivation", "Urine",
                deactivatedSampleType.getDescription());

        List<TypeOfSampleTest> newLinksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        assertEquals("Exactly 3 links must exist for test 2002 after update", 3, newLinksFor2002.size());
        assertTrue("New link for test 2002 must belong to sample type 1001",
                newLinksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));
    }

    @Test
    public void updateShouldCreateNewLinkWithoutPersistingAbbreviationChangeWhenUpdateFlagIsFalse() {
        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        assertEquals("Pre-condition: abbreviation must be 'blood' before update attempt", "blood",
                sampleType1001.getLocalAbbreviation());

        sampleType1001.setLocalAbbreviation("should_not_save");

        sampleTypeTestAssignService.update(sampleType1001, "2002", null, "1001", false, false, null, "1");

        TypeOfSample reloadedSampleType = typeOfSampleService.get("1001");
        assertEquals("Abbreviation must remain 'blood' because updateSampleType flag is false", "blood",
                reloadedSampleType.getLocalAbbreviation());

        List<TypeOfSampleTest> linksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        assertEquals("Exactly 3 links must exist for test 2002", 3, linksFor2002.size());
        assertTrue("New link for test 2002 must belong to sample type 1001",
                linksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));
    }
}
