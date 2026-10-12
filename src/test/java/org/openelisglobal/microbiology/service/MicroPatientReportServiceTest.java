package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;

public class MicroPatientReportServiceTest {
    @Mock
    private MicroCaseDAO cases;
    @Mock
    private MicroCaseAnalysisDAO links;
    @Mock
    private MicroCaseInoculationDAO cultures;
    @Mock
    private MicroIsolateDAO isolates;
    @Mock
    private MicroOrganismDAO organisms;
    @Mock
    private MicroAntibioticDAO antibiotics;
    @Mock
    private MicroAstRunDAO runs;
    @Mock
    private MicroAstReportChoiceService choices;
    @Mock
    private MicroBreakpointStandardDAO standards;
    @Mock
    private MicroCaseTimelineService timeline;
    @Mock
    private MicroDstService dst;
    @Mock
    private ResultService results;
    @Mock
    private IStatusService statuses;
    @Mock
    private TestSectionService units;
    @Mock
    private MicroCaseAuthorizationService authorization;
    @Mock
    private MicroCaseActivityDAO activities;
    @Mock
    private MicroReportVersionDAO versions;
    @Mock
    private TestResultComponentService components;
    @Mock
    private MicrobiologyCaseAccessService access;
    @InjectMocks
    private MicroPatientReportService service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "components", components);
    }

    @Test
    public void dictionaryQuantificationStaysWithItsVisibleParentAndHiddenComponentsStayOut() {
        var c = new MicroCase();
        c.setId("c");
        c.setLabUnitId("unit");
        when(cases.get("c")).thenReturn(Optional.of(c));
        var unit = mock(org.openelisglobal.test.valueholder.TestSection.class);
        when(unit.getLocalizedName()).thenReturn("Case unit");
        when(units.get("unit")).thenReturn(unit);
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test");
        test.setName("Quantified microscopy");
        var analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setId("analysis");
        analysis.setTest(test);
        analysis.setStatusId("final");
        when(links.getAnalyses("c")).thenReturn(List.of(analysis));
        when(statuses.getAnalysisStatusForID("final")).thenReturn(AnalysisStatus.Finalized);
        var link = new MicroCaseAnalysis();
        link.setPlacement("INITIAL");
        when(links.getByCaseAndAnalysis("c", "analysis")).thenReturn(link);
        var primary = new org.openelisglobal.testresultcomponent.valueholder.TestResultComponent();
        primary.setId("primary");
        primary.setIsPrimary(true);
        primary.setLabel("Organism quantity");
        var hidden = new org.openelisglobal.testresultcomponent.valueholder.TestResultComponent();
        hidden.setId("hidden");
        hidden.setShowOnReport(false);
        when(components.getComponentsByTestId("test")).thenReturn(List.of(primary, hidden));
        var parent = new Result();
        parent.setId("parent");
        parent.setResultType("D");
        var child = new Result();
        child.setId("quantity");
        child.setResultType("A");
        child.setParentResult(parent);
        var privateChild = new Result();
        privateChild.setId("private");
        privateChild.setResultType("A");
        privateChild.setParentResult(parent);
        privateChild.setIsReportable("N");
        var hiddenResult = new Result();
        hiddenResult.setId("hidden-result");
        var option = new org.openelisglobal.testresult.valueholder.TestResult();
        option.setComponentId("hidden");
        hiddenResult.setTestResult(option);
        when(results.getResultsByAnalysis(analysis)).thenReturn(List.of(parent, child, privateChild, hiddenResult));
        when(results.getSimpleResultValue(parent)).thenReturn("Bacteria present");
        when(results.getSimpleResultValue(child)).thenReturn("10^5 CFU/mL");
        var noDst = new MicroDstForm();
        noDst.classification = "NOT_APPLICABLE";
        when(dst.evaluate("c")).thenReturn(noDst);
        var report = service.preview("c");
        assertEquals(1, report.validatedResults);
        assertEquals(1, report.initial.get(0).values().size());
        assertEquals("Bacteria present 10^5 CFU/mL", report.initial.get(0).values().get(0).value());
        verify(results, never()).getSimpleResultValue(privateChild);
        verify(results, never()).getSimpleResultValue(hiddenResult);
    }
}
