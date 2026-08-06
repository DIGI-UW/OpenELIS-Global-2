package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseInoculationServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroCaseInoculationDAO inoculationDAO;
    @Mock
    private MicroCaseActivityDAO activityDAO;
    @Mock
    private MicroReagentLotService reagentLotService;

    private MicroCaseInoculationService service;
    private MicroCase microCase;

    @Before
    public void setUp() {
        service = new MicroCaseInoculationServiceImpl(caseDAO, inoculationDAO, activityDAO, reagentLotService,
                new ObjectMapper());
        microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        microCase.setCultureMethodId("method-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.update(microCase)).thenReturn(microCase);
    }

    @Test
    public void recordsPrimaryInoculationTimelineStageAndLotsAtomically() throws Exception {
        List<MicroLotSelection> lots = List.of(new MicroLotSelection("analysis-1", "link-1", 7L));

        MicroCaseInoculation result = service.record("case-1", null, "BOTTLE-001", "Blood agar", "24h at 35 C",
                "Ambient", lots, "42");

        assertNull(result.getSourceInoculationId());
        assertEquals("method-1", result.getMethodId());
        assertEquals("42", result.getPerformedBy());
        assertEquals(MicroCaseStage.INCUBATING.name(), microCase.getStage());
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.INOCULATION_RECORDED.name(), activity.getValue().getActivityType());
        assertEquals(result.getId(),
                new ObjectMapper().readTree(activity.getValue().getStructuredData()).get("inoculationId").asText());
        verify(inoculationDAO).insert(result);
        verify(reagentLotService).recordSelections("case-1", MicroInventoryUsageContext.CULTURE_SETUP,
                activity.getValue().getId(), lots, "42");
    }

    @Test
    public void recordsSubcultureWithSameCaseParentAndTypedTimeline() {
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        MicroCaseInoculation parent = new MicroCaseInoculation();
        parent.setId("inoculation-1");
        parent.setCaseId("case-1");
        when(inoculationDAO.get("inoculation-1")).thenReturn(Optional.of(parent));

        MicroCaseInoculation result = service.record("case-1", "inoculation-1", "PLATE-002", "MacConkey agar", "18h",
                "Ambient", List.of(), "42");

        assertEquals("inoculation-1", result.getSourceInoculationId());
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.SUBCULTURE_RECORDED.name(), activity.getValue().getActivityType());
        verify(caseDAO, never()).update(any(MicroCase.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSubcultureParentFromAnotherCase() {
        MicroCaseInoculation parent = new MicroCaseInoculation();
        parent.setId("inoculation-2");
        parent.setCaseId("case-2");
        when(inoculationDAO.get("inoculation-2")).thenReturn(Optional.of(parent));

        service.record("case-1", "inoculation-2", "PLATE-002", "MacConkey agar", null, null, List.of(), "42");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInoculationBeforeWorkflowClassification() {
        microCase.setWorkflowType(MicroWorkflowType.UNASSIGNED.name());

        service.record("case-1", null, "BOTTLE-001", "Blood agar", null, null, List.of(), "42");
    }

    @Test(expected = MicroCaseLockedException.class)
    public void rejectsFinalReleasedCase() {
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());

        service.record("case-1", null, "BOTTLE-001", "Blood agar", null, null, List.of(), "42");
    }
}
