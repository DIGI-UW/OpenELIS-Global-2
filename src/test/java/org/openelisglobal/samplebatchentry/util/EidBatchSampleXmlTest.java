package org.openelisglobal.samplebatchentry.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntrySaveForm.EidSelection;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;

public class EidBatchSampleXmlTest {
    private TestService tests;
    private TypeOfSampleService specimens;
    private TypeOfSampleTestService links;
    private EidBatchSampleXml builder;
    private EidSelection selection;

    @Before
    public void setUp() {
        tests = mock(TestService.class);
        specimens = mock(TypeOfSampleService.class);
        links = mock(TypeOfSampleTestService.class);
        builder = new EidBatchSampleXml(tests, specimens, links);
        selection = new EidSelection();
        selection.setDnaPCR(true);
        selection.setDryTubeTaken(true);
        TypeOfSample dry = new TypeOfSample();
        dry.setId("24");
        TypeOfSample dbs = new TypeOfSample();
        dbs.setId("26");
        when(specimens.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenAnswer(
                call -> "Dry Tube".equals(((TypeOfSample) call.getArgument(0)).getDescription()) ? dry : dbs);
        org.openelisglobal.test.valueholder.Test lower = new org.openelisglobal.test.valueholder.Test();
        lower.setId("1");
        org.openelisglobal.test.valueholder.Test higher = new org.openelisglobal.test.valueholder.Test();
        higher.setId("2");
        when(tests.getActiveTestByName("DNA PCR")).thenReturn(List.of(lower, higher));
        TypeOfSampleTest dryLink = new TypeOfSampleTest();
        dryLink.setTypeOfSampleId("24");
        TypeOfSampleTest dbsLink = new TypeOfSampleTest();
        dbsLink.setTypeOfSampleId("26");
        when(links.getTypeOfSampleTestsForTest("1")).thenReturn(List.of(dbsLink));
        when(links.getTypeOfSampleTestsForTest("2")).thenReturn(List.of(dryLink));
    }

    @Test
    public void eachSpecimenUsesItsMappedDnaPcrEvenWhenAnotherTestHasALowerId() throws Exception {
        selection.setDbsTaken(true);
        var samples = org.dom4j.DocumentHelper.parseText(builder.build(selection, "02/10/2026")).getRootElement()
                .elements();
        assertEquals(2, samples.size());
        assertEquals("2", samples.get(0).attributeValue("tests"));
        assertEquals("1", samples.get(1).attributeValue("tests"));
    }

    @Test
    public void anUnmappedSelectedSpecimenRejectsTheWholeOrder() {
        selection.setDbsTaken(true);
        when(links.getTypeOfSampleTestsForTest("1")).thenReturn(List.of());
        assertEquals("", builder.build(selection, "02/10/2026"));
    }

    @Test
    public void removingACatalogLinkIsRespectedOnTheNextSave() {
        assertFalse(builder.build(selection, "02/10/2026").isEmpty());
        when(links.getTypeOfSampleTestsForTest("2")).thenReturn(List.of());
        assertEquals("", builder.build(selection, "02/10/2026"));
    }
}
