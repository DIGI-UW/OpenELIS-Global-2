package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;

@RunWith(MockitoJUnitRunner.class)
public class MicroIsolateServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    @Mock
    private MicroIdentificationHistoryService identificationHistoryService;

    private MicroIsolateService service;

    @Before
    public void setUp() {
        service = new MicroIsolateServiceImpl(caseDAO, isolateDAO, activityDAO, amendmentDAO,
                identificationHistoryService);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
        MicroIsolateIdentificationEvent event = new MicroIsolateIdentificationEvent();
        event.setId("event-1");
        lenient().when(identificationHistoryService.recordChange(any(MicroIsolate.class), any(MicroIsolate.class),
                org.mockito.ArgumentMatchers.nullable(String.class), any(String.class))).thenReturn(event);
    }

    @Test
    public void createIsolateRequiresCaseAndRecordsActivity() {
        MicroIsolate isolate = service.createIsolate("case-1", "ISO-1", "org-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertEquals("case-1", isolate.getCaseId());
        assertEquals("ISO-1", isolate.getIsolateLabel());
        assertEquals("org-1", isolate.getOrganismId());
        assertEquals(MicroIsolateIdentificationStatus.PRELIMINARY.name(), isolate.getIdentificationStatus());
        verify(isolateDAO).insert(isolate);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createIsolateNormalizesBlankOrganismIdToNull() {
        MicroIsolate isolate = service.createIsolate("case-1", "ISO-1", "", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertNull(isolate.getOrganismId());
    }

    @Test
    public void createIsolateDuringAmendmentLinksDraftToOpenAmendment() {
        MicroCase amendmentCase = mutableCase();
        amendmentCase.setStage(MicroCaseStage.AMENDED.name());
        amendmentCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(amendmentCase));
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);

        MicroIsolate isolate = service.createIsolate("case-1", "ISO-2", "org-2", "Second isolate",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "9");

        assertEquals("amendment-1", isolate.getAmendmentId());
        assertNull(isolate.getCancelledAt());
        verify(isolateDAO).insert(isolate);
    }

    @Test
    public void updateIdentificationPreservesCaseActivityTrail() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "org-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED, "1");

        assertEquals("org-1", updated.getOrganismId());
        assertEquals(MicroIsolateIdentificationStatus.CONFIRMED.name(), updated.getIdentificationStatus());
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void updateIdentificationNormalizesBlankOrganismIdToNull() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "  ", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED, "1");

        assertNull(updated.getOrganismId());
    }

    @Test
    public void updateIdentificationRejectsCancelledAmendmentIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-cancelled");
        isolate.setCaseId("case-1");
        isolate.setCancelledAt(MicroCaseServiceImpl.now());
        when(isolateDAO.get("iso-cancelled")).thenReturn(Optional.of(isolate));

        try {
            service.updateIdentification("iso-cancelled", "org-1", "E. coli",
                    MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED, "1");
            fail("Expected cancelled amendment isolate to remain immutable");
        } catch (IllegalStateException expected) {
            assertEquals("ISOLATE_CANCELLED", expected.getMessage());
        }
    }

    @Test
    public void reidentificationDuringAmendmentRequiresReasonAndRecordsBeforeAfterHistory() {
        MicroCase amendmentCase = mutableCase();
        amendmentCase.setStage(MicroCaseStage.AMENDED.name());
        amendmentCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(amendmentCase));
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        isolate.setOrganismId("org-old");
        isolate.setPreliminaryOrganismText("Escherichia coli");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "org-new", "Klebsiella pneumoniae",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "Corrected after confirmatory identification", "9");

        assertEquals("org-new", updated.getOrganismId());
        verify(identificationHistoryService).recordChange(any(MicroIsolate.class), any(MicroIsolate.class),
                org.mockito.ArgumentMatchers.eq("Corrected after confirmatory identification"),
                org.mockito.ArgumentMatchers.eq("9"));
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(
                "Isolate ISO-1 identification changed from Escherichia coli to Klebsiella pneumoniae: Corrected after confirmatory identification",
                activity.getValue().getNote());
    }

    @Test(expected = IllegalStateException.class)
    public void createIsolateRejectsFinalReleasedCases() {
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.createIsolate("case-1", "ISO-1", "org-1", "E. coli", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                "1");
    }

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }
}
