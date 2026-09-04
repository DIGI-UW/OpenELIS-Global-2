package org.openelisglobal.testconfiguration.service;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
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
    public void update_ShouldDeleteExistingAndCreateNew_WhenFlagsAreTrue() {
        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        Assert.assertNotNull("Sample type should exist", sampleType1001);

        List<TypeOfSampleTest> existingLinks = typeOfSampleTestService.getTypeOfSampleTestsForTest("2001");
        Assert.assertEquals("Should have 1 existing link", 1, existingLinks.size());
        String linkIdToDelete = existingLinks.get(0).getId();

        sampleType1001.setLocalAbbreviation("upd_abbrev");
        sampleTypeTestAssignService.update(sampleType1001, "2002", Arrays.asList(linkIdToDelete), "1001", true, true,
                null, "1");

        List<TypeOfSampleTest> remainingLinksFor2001 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2001");
        Assert.assertEquals("Old link should be deleted", 0, remainingLinksFor2001.size());

        List<TypeOfSampleTest> newLinksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        Assert.assertTrue("Should have new link",
                newLinksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));

        TypeOfSample updatedSampleType = typeOfSampleService.get("1001");
        Assert.assertEquals("Sample type abbreviation should be updated", "upd_abbrev",
                updatedSampleType.getLocalAbbreviation());
    }

    @Test
    public void update_ShouldDeactivateSampleType_WhenProvided() {
        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        TypeOfSample sampleType1002 = typeOfSampleService.get("1002");

        Assert.assertTrue(sampleType1002.getIsActive());

        sampleType1002.setIsActive(false);

        sampleTypeTestAssignService.update(sampleType1001, "2002", null, "1001", false, false, sampleType1002, "1");

        TypeOfSample deactivatedSampleType = typeOfSampleService.get("1002");
        Assert.assertFalse("Sample type 1002 should be deactivated", deactivatedSampleType.getIsActive());

        List<TypeOfSampleTest> newLinksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        Assert.assertTrue("Should have new link for 1001",
                newLinksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));
    }

    @Test
    public void update_ShouldOnlyCreateNewLink_WhenFlagsAreFalse() {
        TypeOfSample sampleType1001 = typeOfSampleService.get("1001");
        String originalAbbrev = sampleType1001.getLocalAbbreviation();
        sampleType1001.setLocalAbbreviation("should_not_save");

        sampleTypeTestAssignService.update(sampleType1001, "2002", null, "1001", false, false, null, "1");

        TypeOfSample reloadedSampleType = typeOfSampleService.get("1001");
        Assert.assertEquals("Sample type should not be updated", originalAbbrev,
                reloadedSampleType.getLocalAbbreviation());

        List<TypeOfSampleTest> linksFor2002 = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        Assert.assertTrue("Should have new link for 1001",
                linksFor2002.stream().anyMatch(l -> "1001".equals(l.getTypeOfSampleId())));
    }
}
