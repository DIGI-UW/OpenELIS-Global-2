package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseProtocolServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroCaseActivityDAO activityDAO;
    @Mock
    private MicroCaseAnalysisDAO caseAnalysisDAO;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private TestMethodService testMethodService;
    @Mock
    private MethodService methodService;
    @Mock
    private MicrobiologyReferenceService referenceService;

    private MicroCaseProtocolService service;
    private MicroCase microCase;
    private org.openelisglobal.test.valueholder.Test orderedTest;

    @Before
    public void setUp() {
        service = new MicroCaseProtocolServiceImpl(caseDAO, activityDAO, caseAnalysisDAO, analysisService,
                testMethodService, methodService, referenceService, new ObjectMapper());
        microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("sample-item-1");
        microCase.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        microCase.setCultureMethodId("method-old");
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.NOT_READY.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setId("link-1");
        link.setCaseId("case-1");
        link.setAnalysisId("analysis-1");
        link.setReportableTestAnalyteId("analyte-1");
        link.setProjectedResultId("result-1");
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        Analysis analysis = new Analysis();
        analysis.setId("analysis-1");
        when(analysisService.getAnalysisById("analysis-1")).thenReturn(analysis);
        orderedTest = new org.openelisglobal.test.valueholder.Test();
        orderedTest.setId("test-1");
        orderedTest.setCultureWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        when(analysisService.getTest(analysis)).thenReturn(orderedTest);
    }

    @Test
    public void optionsContainOnlyActiveLinkedMethodsAndRetainInactiveIncumbent() {
        TestMethodDto oldLink = linkedMethod("method-old", "Old protocol");
        TestMethodDto nextLink = linkedMethod("method-next", "Next protocol");
        when(testMethodService.getLinkedMethodDtos("test-1")).thenReturn(List.of(oldLink, nextLink));
        MicroCaseAnalysis wrongWorkflowCaseLink = new MicroCaseAnalysis();
        wrongWorkflowCaseLink.setCaseId("case-1");
        wrongWorkflowCaseLink.setAnalysisId("analysis-tb");
        MicroCaseAnalysis primaryLink = caseAnalysisDAO.getByCaseId("case-1").get(0);
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(primaryLink, wrongWorkflowCaseLink));
        Analysis wrongWorkflowAnalysis = new Analysis();
        wrongWorkflowAnalysis.setId("analysis-tb");
        org.openelisglobal.test.valueholder.Test wrongWorkflowTest = new org.openelisglobal.test.valueholder.Test();
        wrongWorkflowTest.setId("test-tb");
        wrongWorkflowTest.setCultureWorkflowType(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name());
        when(analysisService.getAnalysisById("analysis-tb")).thenReturn(wrongWorkflowAnalysis);
        when(analysisService.getTest(wrongWorkflowAnalysis)).thenReturn(wrongWorkflowTest);
        activeMethod("method-next", "Next protocol");
        inactiveMethod("method-old", "Old protocol");
        compatibleSetup("method-next", MicroWorkflowType.BACTERIOLOGY, "BAP", "48 hours", "aerobic");

        List<MicroCaseProtocolOptionForm> options = service.getProtocolOptions("case-1");

        assertEquals(2, options.size());
        MicroCaseProtocolOptionForm current = option(options, "method-old");
        assertTrue(current.current);
        assertFalse(current.active);
        assertEquals("Old protocol", current.label);
        MicroCaseProtocolOptionForm next = option(options, "method-next");
        assertFalse(next.current);
        assertTrue(next.active);
        assertEquals("BAP", next.mediaDefaults);
        assertEquals("48 hours", next.incubationDefaults);
        assertEquals("aerobic", next.atmosphereDefaults);
        assertFalse(options.stream().anyMatch(option -> "method-tb".equals(option.id)));
    }

    @Test
    public void optionsRetainDanglingIncumbentIdWhenReferenceRecordIsMissing() {
        when(testMethodService.getLinkedMethodDtos("test-1")).thenReturn(List.of());

        MicroCaseProtocolOptionForm incumbent = option(service.getProtocolOptions("case-1"), "method-old");

        assertEquals("method-old", incumbent.id);
        assertEquals("method-old", incumbent.label);
        assertTrue(incumbent.current);
        assertFalse(incumbent.active);
    }

    @Test
    public void optionsRetainActiveLinkedMethodWhenRecipeMetadataIsMissing() {
        when(testMethodService.getLinkedMethodDtos("test-1"))
                .thenReturn(List.of(linkedMethod("method-next", "Next protocol")));
        activeMethod("method-next", "Next protocol");

        MicroCaseProtocolOptionForm option = option(service.getProtocolOptions("case-1"), "method-next");

        assertTrue(option.active);
        assertEquals("Next protocol", option.label);
        assertEquals(null, option.mediaDefaults);
        assertEquals(null, option.incubationDefaults);
        assertEquals(null, option.atmosphereDefaults);
    }

    @Test
    public void changeProtocolUpdatesOnlyMethodAndRecordsCompleteAudit() throws Exception {
        when(testMethodService.getLinkedMethodDtos("test-1"))
                .thenReturn(List.of(linkedMethod("method-next", "Next protocol")));
        activeMethod("method-old", "Old protocol");
        activeMethod("method-next", "Next protocol");
        compatibleSetup("method-next", MicroWorkflowType.BACTERIOLOGY, "BAP", "48 hours", "aerobic");
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCase updated = service.changeProtocol("case-1", "method-next", "Growth requires alternate media",
                "42");

        assertEquals("method-next", updated.getCultureMethodId());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), updated.getWorkflowType());
        assertEquals(MicroCaseStage.INCUBATING.name(), updated.getStage());
        assertEquals(MicroCaseFinalReleaseState.NOT_READY.name(), updated.getFinalReleaseState());
        verify(caseDAO).update(microCase);
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.CULTURE_PROTOCOL_CHANGED.name(), activity.getValue().getActivityType());
        assertEquals("42", activity.getValue().getPerformedBy());
        assertNotNull(activity.getValue().getOccurredAt());
        assertNull(activity.getValue().getNote());
        JsonNode audit = new ObjectMapper().readTree(activity.getValue().getStructuredData());
        assertEquals("method-old", audit.get("fromMethodId").asText());
        assertEquals("method-next", audit.get("toMethodId").asText());
        assertEquals("Growth requires alternate media", audit.get("reason").asText());

        MicroCaseAnalysis preserved = caseAnalysisDAO.getByCaseId("case-1").get(0);
        assertEquals("analysis-1", preserved.getAnalysisId());
        assertEquals("analyte-1", preserved.getReportableTestAnalyteId());
        assertEquals("result-1", preserved.getProjectedResultId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeProtocolRejectsMethodNotLinkedWithinCurrentWorkflow() {
        when(testMethodService.getLinkedMethodDtos("test-1")).thenReturn(List.of());
        activeMethod("method-tb", "TB protocol");
        try {
            service.changeProtocol("case-1", "method-tb", "Wrong workflow", "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeProtocolRequiresReason() {
        try {
            service.changeProtocol("case-1", "method-next", " ", "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeProtocolRejectsUnchangedMethod() {
        try {
            service.changeProtocol("case-1", "method-old", "No actual change", "42");
        } finally {
            verify(caseDAO, never()).update(any(MicroCase.class));
        }
    }

    @Test(expected = MicroCaseLockedException.class)
    public void changeProtocolRejectsFinalReleasedCase() {
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());

        service.changeProtocol("case-1", "method-next", "Too late", "42");
    }

    private TestMethodDto linkedMethod(String methodId, String name) {
        TestMethodDto link = new TestMethodDto();
        link.methodId = methodId;
        link.methodName = name;
        return link;
    }

    private Method activeMethod(String methodId, String name) {
        Method method = method(methodId, name, IActionConstants.YES);
        when(methodService.findById(methodId)).thenReturn(method);
        return method;
    }

    private Method inactiveMethod(String methodId, String name) {
        Method method = method(methodId, name, IActionConstants.NO);
        when(methodService.findById(methodId)).thenReturn(method);
        return method;
    }

    private Method method(String methodId, String name, String active) {
        Method method = new Method();
        method.setId(methodId);
        method.setMethodName(name);
        method.setIsActive(active);
        return method;
    }

    private void compatibleSetup(String methodId, MicroWorkflowType workflowType, String media, String incubation,
            String atmosphere) {
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(methodId);
        setup.setWorkflowType(workflowType.name());
        setup.setMediaDefaults(media);
        setup.setIncubationDefaults(incubation);
        setup.setAtmosphereDefaults(atmosphere);
        when(referenceService.getActiveCultureSetupForMethod(methodId, workflowType)).thenReturn(setup);
    }

    private MicroCaseProtocolOptionForm option(List<MicroCaseProtocolOptionForm> options, String methodId) {
        return options.stream().filter(option -> methodId.equals(option.id)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing protocol option " + methodId));
    }
}
