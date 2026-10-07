package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;

public class MicroOrderRoutingServiceTest {
    private MicroCaseService caseService;
    private MicroCaseAnalysisService caseAnalysisService;
    private TestService testService;
    private MicroOrderRoutingService service;
    private SampleItem specimen;

    @Before
    public void setUp() {
        caseService = mock(MicroCaseService.class);
        caseAnalysisService = mock(MicroCaseAnalysisService.class);
        testService = mock(TestService.class);
        service = new MicroOrderRoutingServiceImpl(caseService, caseAnalysisService, testService);
        specimen = new SampleItem();
        specimen.setId("1001");
        Sample order = new Sample();
        order.setId("2001");
        specimen.setSample(order);
    }

    @Test
    public void ordinaryTestsDoNotMakeAMicrobiologyOrder() {
        assertFalse(service.isMicrobiologyOrder(List.of(new org.openelisglobal.test.valueholder.Test())));
        assertFalse(service.isMicrobiologyOrder(null));
        assertFalse(service.isMicrobiologyOrder(List.of()));
    }

    @Test
    public void anOrderWithoutTestsDoesNotOpenACase() {
        assertTrue(service.routeAnalysesForSampleItem(specimen, List.of(), "1").isEmpty());
        Mockito.verifyZeroInteractions(caseService, caseAnalysisService, testService);
    }

    @Test
    public void aDirectMicroTestOpensACaseWithoutWorkflowOrProtocol() {
        org.openelisglobal.test.valueholder.Test catalog = catalog("41", "DIRECT", false);
        Analysis direct = analysis("301", "41");
        MicroCase owner = owner("case-direct");
        when(caseService.createOrGetCase(specimen, catalog, "1")).thenReturn(owner);

        assertEquals(List.of(owner), service.routeAnalysesForSampleItem(specimen, List.of(direct), "1"));
        verify(caseAnalysisService).linkAnalysis(owner, direct, "1");
    }

    @Test
    public void submittedCatalogFlagsCannotOpenAnOrdinaryTestCase() {
        org.openelisglobal.test.valueholder.Test catalog = catalog("41", "DIRECT", false);
        catalog.setOpensMicrobiologyCase(false);
        Analysis ordinary = analysis("301", "41");
        ordinary.getTest().setOpensMicrobiologyCase(true);

        assertTrue(service.routeAnalysesForSampleItem(specimen, List.of(ordinary), "1").isEmpty());
        verify(caseService, never()).createOrGetCase(specimen, catalog, "1");
    }

    @Test
    public void setsAreRoutedBeforeOtherTestsOnTheirBottle() {
        org.openelisglobal.test.valueholder.Test direct = catalog("41", "DIRECT", false);
        org.openelisglobal.test.valueholder.Test sets = catalog("42", "CULTURE", true);
        Analysis directAnalysis = analysis("301", "41");
        Analysis setsAnalysis = analysis("302", "42");
        MicroCase owner = owner("case-sets");
        when(caseService.createOrGetCase(specimen, direct, "1")).thenReturn(owner);
        when(caseService.createOrGetCase(specimen, sets, "1")).thenReturn(owner);

        assertEquals(List.of(owner),
                service.routeAnalysesForSampleItem(specimen, List.of(directAnalysis, setsAnalysis), "1"));
        InOrder ordered = Mockito.inOrder(caseService, caseAnalysisService);
        ordered.verify(caseService).createOrGetCase(specimen, sets, "1");
        ordered.verify(caseAnalysisService).linkAnalysis(owner, setsAnalysis, "1");
        ordered.verify(caseService).createOrGetCase(specimen, direct, "1");
        ordered.verify(caseAnalysisService).linkAnalysis(owner, directAnalysis, "1");
    }

    @Test
    public void resaveKeepsOriginalOwnershipAfterCatalogChangesAndFinalRelease() {
        org.openelisglobal.test.valueholder.Test catalog = catalog("41", "DIRECT", false);
        catalog.setOpensMicrobiologyCase(false);
        Analysis existing = analysis("301", "41");
        MicroCase owner = owner("case-original");
        owner.setFinalReleaseState("FINAL_RELEASED");
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setCaseId(owner.getId());
        link.setAnalysisId(existing.getId());
        when(caseAnalysisService.getAnalysisLink(existing.getId())).thenReturn(link);
        when(caseService.getCase(owner.getId())).thenReturn(owner);

        assertEquals(List.of(owner), service.routeAnalysesForSampleItem(specimen, List.of(existing), "1"));
        verify(caseService, never()).createOrGetCase(specimen, catalog, "1");
        verify(caseAnalysisService, never()).linkAnalysis(owner, existing, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unpersistedAnalysesCannotBeRouted() {
        catalog("41", "DIRECT", false);
        service.routeAnalysesForSampleItem(specimen, List.of(analysis(null, "41")), "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void anExistingAnalysisFromAnotherSpecimenCannotBeSubmitted() {
        Analysis other = analysis("301", "41");
        SampleItem wrong = new SampleItem();
        wrong.setId("1002");
        other.setSampleItem(wrong);
        service.routeAnalysesForSampleItem(specimen, List.of(other), "1");
    }

    @Test(expected = IllegalStateException.class)
    public void corruptOwnershipFromAnotherOrderIsNotAcceptedAsIdempotent() {
        Analysis existing = analysis("301", "41");
        MicroCase owner = owner("case-other-order");
        owner.setSampleId("2002");
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setCaseId(owner.getId());
        when(caseAnalysisService.getAnalysisLink(existing.getId())).thenReturn(link);
        when(caseService.getCase(owner.getId())).thenReturn(owner);
        service.routeAnalysesForSampleItem(specimen, List.of(existing), "1");
    }

    private org.openelisglobal.test.valueholder.Test catalog(String id, String role, boolean sets) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setOpensMicrobiologyCase(true);
        test.setMicrobiologyCaseRole(role);
        test.setCollectedInSets(sets);
        when(testService.get(id)).thenReturn(test);
        return test;
    }

    private Analysis analysis(String id, String testId) {
        org.openelisglobal.test.valueholder.Test reference = new org.openelisglobal.test.valueholder.Test();
        reference.setId(testId);
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setSampleItem(specimen);
        analysis.setTest(reference);
        return analysis;
    }

    private MicroCase owner(String id) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setSampleId("2001");
        return microCase;
    }
}
